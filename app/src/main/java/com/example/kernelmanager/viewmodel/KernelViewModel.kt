package com.example.kernelmanager.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val deviceModel: String = "Pixel (Demo)",
    val androidVersion: String = "14 (Demo)",
    val kernelVersion: String = "5.10.x (Demo)",
    val bootState: String = "Unlocked (Demo)",
    val progress: Float = 0f,
    val progressLabel: String = "Idle",
    val lastActionMessage: String = ""
)

class KernelViewModel {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun simulateFlash(path: String) {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Flashed from $path", lastActionMessage = "Flash success (simulasi)") }
    }

    fun simulateBackup() {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Backup selesai", lastActionMessage = "Backup success (simulasi)") }
    }

    fun simulateRestore(path: String) {
        _uiState.update { it.copy(progress = 1f, progressLabel = "Restore dari $path", lastActionMessage = "Restore success (simulasi)") }
    }
}
