package com.example.kernelmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kernelmanager.viewmodel.KernelViewModel
import com.example.kernelmanager.viewmodel.ThermalZoneInfo

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm = remember { KernelViewModel() }
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
    SmallTopAppBar(
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
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Monitor, Screen.Tuning, Screen.Flash, Screen.Backup, Screen.Restore)
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
        composable(Screen.Monitor.route) { MonitorScreen(vm) }
        composable(Screen.Tuning.route) { TuningScreen(vm) }
        composable(Screen.Flash.route) { FlashScreen(vm) }
        composable(Screen.Backup.route) { BackupScreen(vm) }
        composable(Screen.Restore.route) { RestoreScreen(vm) }
    }
}

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
        // Compact CPU temperature card
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Avg CPU Temp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(state.avgCpuTempC?.let { String.format("%.1f°C", it) } ?: "-", style = MaterialTheme.typography.titleLarge)
                }
                Divider(Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hottest Core", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    val hot = state.hottestCpuZone
                    Text(hot?.let { "${'$'}{it.type}: ${'$'}{it.tempC?.let { t -> String.format("%.1f°C", t) } ?: "-"}" } ?: "-",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Text("CPU Temps (Live)", style = MaterialTheme.typography.titleMedium)
        CpuTempsRow(state.cpuZones)
    }
}

@Composable
fun CpuTempsRow(zones: List<ThermalZoneInfo>) {
    if (zones.isEmpty()) {
        AssistChip(onClick = {}, label = { Text("No CPU thermal zones detected") })
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        zones.take(4).forEach { z ->
            ElevatedAssistChip(
                onClick = {},
                label = { Text("${'$'}{z.type}: ${'$'}{z.tempC?.let { String.format("%.1f°C", it) } ?: "-"}") }
            )
        }
    }
}

@Composable
fun MonitorScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 Real-time CPU Temperature", style = MaterialTheme.typography.titleLarge)
        CpuTempsRow(state.cpuZones)
        Divider()
        Text("🌡️ Thermal Zones", style = MaterialTheme.typography.titleMedium)
        state.allZones.forEach { z ->
            ListItem(
                headlineContent = { Text(z.type.ifBlank { "thermal_zone${'$'}{z.id}" }) },
                supportingContent = { Text("ID ${'$'}{z.id}") },
                trailingContent = { Text(z.tempC?.let { String.format("%.1f°C", it) } ?: "-") }
            )
            Divider()
        }
    }
}

@Composable
fun TuningScreen(vm: KernelViewModel) {
    val state by vm.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(state.currentGovernor) { mutableStateOf(state.currentGovernor ?: state.availableGovernors.firstOrNull()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("⚙️ CPU Tuning (Root)", style = MaterialTheme.typography.titleLarge)
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
                    DropdownMenuItem(text = { Text(gov) }, onClick = { selected = gov; expanded = false })
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selected?.let { vm.applyGovernor(it) } }, enabled = selected != null) { Text("Apply") }
            AssistChip(onClick = {}, label = { Text("Current: ${'$'}{state.currentGovernor ?: "-"}") })
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
