package com.example.kernelmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kernelmanager.viewmodel.KernelViewModel
import com.example.kernelmanager.viewmodel.ThermalZoneInfo
import kotlin.math.roundToInt

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val vm = remember(context) { KernelViewModel(context.applicationContext) }
    Scaffold(
        topBar = { TopBar(nav) },
        bottomBar = { BottomBar(nav) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AppNavHost(nav, vm)
        }
    }
}

@Composable
private fun TopBar(nav: NavHostController) {
    TopAppBar(
        title = { Text("Kernel Manager", fontWeight = FontWeight.SemiBold) }
    )
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard: Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Monitor: Screen("monitor", "Monitor", Icons.Filled.Thermostat)
    object Tuning: Screen("tuning", "Tuning", Icons.Filled.Tune)
    object Flash: Screen("flash", "Flash", Icons.Filled.Download)
    object Backup: Screen("backup", "Backup", Icons.Filled.Build)
    object Restore: Screen("restore", "Restore", Icons.Filled.Restore)
    object Settings: Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Monitor, Screen.Tuning, Screen.Flash, Screen.Backup, Screen.Restore, Screen.Settings)
    NavigationBar {
        val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = { nav.navigate(screen.route) { launchSingleTop = true } },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, vm: KernelViewModel) {
    NavHost(navController = nav, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) { DashboardScreen(vm) }
        composable(Screen.Monitor.route) { MonitorScreen(nav, vm) }
        composable(Screen.Tuning.route) { TuningScreen(vm) }
        composable(Screen.Flash.route) { FlashScreen(vm) }
        composable(Screen.Backup.route) { BackupScreen(vm) }
        composable(Screen.Restore.route) { RestoreScreen(vm) }
        composable(Screen.Settings.route) { SettingsScreen(vm) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Device", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Model: ${'$'}{state.deviceModel}")
                Text("Android: ${'$'}{state.androidVersion}")
                Text("Kernel: ${'$'}{state.kernelVersion}")
            }
        }
        Text("Status", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Boot State: ${'$'}{state.bootState}")
                LinearProgressIndicator(progress = { state.progress })
                Text(state.progressLabel)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Avg CPU Temp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(state.avgCpuTempC?.let { String.format("%.1f C", it) } ?: "-", style = MaterialTheme.typography.titleLarge)
                }
                Divider(Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hottest Core", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    val hot = state.hottestCpuZone
                    val hotLabel = if (hot != null) {
                        val t = hot.tempC
                        val tText = if (t != null) String.format("%.1f C", t) else "-"
                        "${'$'}{hot.type}: ${'$'}tText"
                    } else "-"
                    Text(hotLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Text("CPU Temps (Live)", style = MaterialTheme.typography.titleMedium)
        CpuTempsRow(state.cpuZones, state.hotThresholdC)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuTempsRow(zones: List<ThermalZoneInfo>, hotThreshold: Float) {
    if (zones.isEmpty()) {
        AssistChip(onClick = {}, label = { Text("No CPU thermal zones detected") })
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        zones.take(4).forEach { z ->
            val hot = (z.tempC ?: Float.NEGATIVE_INFINITY) > hotThreshold
            val tText = z.tempC?.let { String.format("%.1f C", it) } ?: "-"
            val label = "${'$'}{z.type}: ${'$'}tText"
            ElevatedAssistChip(
                onClick = {},
                label = { Text(label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (hot) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (hot) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(nav: NavHostController, vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Real-time CPU Temperature", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { nav.navigate(Screen.Settings.route) }) { Text("Settings") }
        }

        val avg = state.avgCpuTempC
        val isHot = (avg ?: Float.NEGATIVE_INFINITY) > state.hotThresholdC
        ElevatedAssistChip(
            onClick = {},
            label = { Text(avg?.let { String.format("Avg: %.1f C", it) } ?: "Avg: -") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isHot) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                labelColor = if (isHot) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Polling", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.pollingEnabled, onCheckedChange = { vm.setPollingEnabled(it) })
        }

        CpuTempsRow(state.cpuZones, state.hotThresholdC)
        Divider()
        Text("Thermal Zones", style = MaterialTheme.typography.titleMedium)
        state.allZones.forEach { z ->
            val hot = (z.tempC ?: Float.NEGATIVE_INFINITY) > state.hotThresholdC
            val tText = z.tempC?.let { String.format("%.1f C", it) } ?: "-"
            ListItem(
                headlineContent = { Text(z.type.ifBlank { "thermal_zone${'$'}{z.id}" }) },
                supportingContent = { Text("ID ${'$'}{z.id}") },
                trailingContent = {
                    AssistChip(
                        onClick = {},
                        label = { Text(tText) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (hot) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (hot) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            )
            Divider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Enable polling")
            Switch(checked = state.pollingEnabled, onCheckedChange = { vm.setPollingEnabled(it) })
        }
        Text("Polling interval: ${'$'}{state.pollingIntervalMs} ms")
        Slider(
            value = state.pollingIntervalMs.toFloat(),
            onValueChange = { vm.setPollingIntervalMs(it.roundToInt()) },
            valueRange = 250f..5000f,
            steps = 18
        )
        val thText = String.format("%.0f C", state.hotThresholdC)
        Text("Hot threshold: ${'$'}thText")
        Slider(
            value = state.hotThresholdC,
            onValueChange = { vm.setHotThresholdC(it) },
            valueRange = 50f..95f,
            steps = 45
        )
        AssistChip(onClick = {}, label = { Text("Changes are saved automatically") })
    }
}

@Composable
fun TuningScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(state.preferredGovernor, state.currentGovernor) { mutableStateOf(state.preferredGovernor ?: state.currentGovernor ?: state.availableGovernors.firstOrNull()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CPU Tuning (Root)", style = MaterialTheme.typography.titleLarge)
        Text("Available governors: ${'$'}{state.availableGovernors.joinToString()}")
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                readOnly = true,
                value = selected ?: "",
                onValueChange = {},
                label = { Text("Governor") },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.availableGovernors.forEach { gov ->
                    DropdownMenuItem(text = { Text(gov) }, onClick = { selected = gov; expanded = false; vm.setPreferredGovernor(gov) })
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selected?.let { vm.applyGovernor(it) } }, enabled = selected != null && state.pollingEnabled) { Text("Apply") }
            AssistChip(onClick = {}, label = { Text("Current: ${'$'}{state.currentGovernor ?: "-"}") })
        }
        if (!state.pollingEnabled) {
            Text("Enable polling to apply tuning", color = MaterialTheme.colorScheme.error)
        }
        if (state.lastRootActionMessage.isNotEmpty()) {
            Text(state.lastRootActionMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FlashScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Flash Kernel", style = MaterialTheme.typography.titleLarge)
        Text("Demo-only UI. Tidak melakukan operasi sebenarnya.")
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Path .img / .zip") },
            placeholder = { Text("/sdcard/Download/boot.img") }
        )
        Button(
            onClick = { vm.simulateFlash(input) },
            enabled = input.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Flash")
        }
        if (state.lastActionMessage.isNotEmpty()) {
            AssistChip(onClick = { /* no-op */ }, label = { Text(state.lastActionMessage) })
        }
    }
}

@Composable
fun BackupScreen(vm: KernelViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Backup Boot Image", style = MaterialTheme.typography.titleLarge)
        Text("UI simulasi untuk backup boot.img.")
        Button(onClick = { vm.simulateBackup() }, modifier = Modifier.align(Alignment.End)) {
            Text("Backup Sekarang")
        }
    }
}

@Composable
fun RestoreScreen(vm: KernelViewModel) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Restore", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Path backup boot.img") },
            placeholder = { Text("/sdcard/Backup/boot_backup.img") }
        )
        Button(
            onClick = { vm.simulateRestore(input) },
            enabled = input.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Restore")
        }
    }
}
