package com.example.kernelmanager.viewmodel

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.io.File

data class ThermalZoneInfo(
    val id: Int,
    val type: String,
    val tempC: Float?
)

data class UiState(
    val deviceModel: String = "Pixel (Demo)",
    val androidVersion: String = "14 (Demo)",
    val kernelVersion: String = "5.10.x (Demo)",
    val bootState: String = "Unlocked (Demo)",
    val progress: Float = 0f,
    val progressLabel: String = "Idle",
    val lastActionMessage: String = "",

    // Monitoring
    val cpuZones: List<ThermalZoneInfo> = emptyList(),
    val allZones: List<ThermalZoneInfo> = emptyList(),
    val avgCpuTempC: Float? = null,
    val hottestCpuZone: ThermalZoneInfo? = null,

    // Governors
    val availableGovernors: List<String> = emptyList(),
    val currentGovernor: String? = null,
    val lastRootActionMessage: String = "",
    val preferredGovernor: String? = null,
    val pollingEnabled: Boolean = true,
    val pollingIntervalMs: Int = 1000,
    val hotThresholdC: Float = 70f
)

class KernelViewModel(private val context: Context) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs by lazy { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    init {
        // Load persisted settings
        val enabled = prefs.getBoolean("pollingEnabled", true)
        val interval = prefs.getInt("pollingIntervalMs", 1000).coerceIn(250, 10000)
        val threshold = prefs.getFloat("hotThresholdC", 70f)
        val prefGov = prefs.getString("preferredGovernor", null)
        _uiState.update { it.copy(pollingEnabled = enabled, pollingIntervalMs = interval, hotThresholdC = threshold, preferredGovernor = prefGov) }
        scope.launch {
            while (isActive) {
                if (!_uiState.value.pollingEnabled) {
                    delay(1000)
                    continue
                }
                val zones = readThermalZones()
                val cpuRelated = zones.filter { it.type.contains("cpu", ignoreCase = true) }
                val avg = cpuRelated.mapNotNull { it.tempC }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
                val hottest = cpuRelated.maxByOrNull { it.tempC ?: Float.MIN_VALUE }
                val (availGovs, curGov) = readGovernors()
                _uiState.update {
                    it.copy(
                        allZones = zones,
                        cpuZones = cpuRelated,
                        avgCpuTempC = avg,
                        hottestCpuZone = hottest,
                        availableGovernors = availGovs,
                        currentGovernor = curGov
                    )
                }
                delay(_uiState.value.pollingIntervalMs.toLong().coerceAtLeast(250L))
            }
        }
    }

    // Demo operations keep existing behavior
    fun simulateFlash(path: String) {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Flashed from $path", lastActionMessage = "Flash success (simulasi)") }
    }

    fun simulateBackup() {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Backup selesai", lastActionMessage = "Backup success (simulasi)") }
    }

    fun simulateRestore(path: String) {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Restore dari $path", lastActionMessage = "Restore success (simulasi)") }
    }

    fun applyGovernor(governor: String) {
        scope.launch {
            try {
                if (!_uiState.value.pollingEnabled) {
                    _uiState.update { it.copy(lastRootActionMessage = "Enable polling to apply tuning") }
                    return@launch
                }
                // Safety: confirm root first using su -c
                if (!runSu("id")) {
                    _uiState.update { it.copy(lastRootActionMessage = "Root not granted. Unable to apply governor.") }
                    return@launch
                }
                val ok = runSu("for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo -n ${'$'}governor > ${'$'}f; done")
                _uiState.update {
                    it.copy(
                        lastRootActionMessage = if (ok) "Governor applied: ${'$'}governor" else "Failed to apply governor",
                        currentGovernor = if (ok) governor else it.currentGovernor,
                        preferredGovernor = if (ok) governor else it.preferredGovernor
                    )
                }
                if (ok) setPreferredGovernor(governor)
            } catch (t: Throwable) {
                _uiState.update { it.copy(lastRootActionMessage = "Root required. Error: ${'$'}{t.message}") }
            }
        }
    }

    private fun runSu(cmd: String): Boolean {
        return try {
            val pb = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
            val p = pb.start()
            p.waitFor(5000, TimeUnit.MILLISECONDS)
            p.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun setPollingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pollingEnabled", enabled).apply()
        _uiState.update { it.copy(pollingEnabled = enabled) }
    }

    fun setPollingIntervalMs(ms: Int) {
        val v = ms.coerceIn(250, 10000)
        prefs.edit().putInt("pollingIntervalMs", v).apply()
        _uiState.update { it.copy(pollingIntervalMs = v) }
    }

    fun setHotThresholdC(c: Float) {
        val v = c.coerceIn(40f, 110f)
        prefs.edit().putFloat("hotThresholdC", v).apply()
        _uiState.update { it.copy(hotThresholdC = v) }
    }

    fun setPreferredGovernor(gov: String) {
        prefs.edit().putString("preferredGovernor", gov).apply()
        _uiState.update { it.copy(preferredGovernor = gov) }
    }

    private fun readThermalZones(): List<ThermalZoneInfo> {
        val base = File("/sys/class/thermal")
        if (!base.exists()) return emptyList()
        return base.listFiles { f -> f.isDirectory && f.name.startsWith("thermal_zone") }
            ?.mapNotNull { dir ->
                val id = dir.name.removePrefix("thermal_zone").toIntOrNull() ?: return@mapNotNull null
                val type = runCatching { File(dir, "type").readText().trim() }.getOrDefault("unknown")
                val tempRaw = runCatching { File(dir, "temp").readText().trim() }.getOrNull()
                val tempC = tempRaw?.toFloatOrNull()?.let { v -> if (v > 200) v / 1000f else v }
                ThermalZoneInfo(id = id, type = type, tempC = tempC)
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    private fun readGovernors(): Pair<List<String>, String?> {
        val avail = runCatching {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors").readText().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        val current = runCatching {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        }.getOrNull()
        return avail to current
    }
}
