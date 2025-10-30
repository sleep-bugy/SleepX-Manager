package com.example.kernelmanager.viewmodel

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val pollingEnabled: Boolean = true
)

class KernelViewModel {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
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
                delay(1000)
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
                // Safety: confirm root first
                val probe = Shell.su("id").exec()
                if (!probe.isSuccess) {
                    _uiState.update { it.copy(lastRootActionMessage = "Root not granted. Unable to apply governor.") }
                    return@launch
                }
                val result = Shell.su("for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo -n ${'$'}governor > ${'$'}f; done").exec()
                val ok = result.isSuccess
                _uiState.update {
                    it.copy(
                        lastRootActionMessage = if (ok) "Governor applied: ${'$'}governor" else "Failed to apply governor",
                        currentGovernor = if (ok) governor else it.currentGovernor
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(lastRootActionMessage = "Root required. Error: ${'$'}{t.message}") }
            }
        }
    }

    fun setPollingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(pollingEnabled = enabled) }
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
