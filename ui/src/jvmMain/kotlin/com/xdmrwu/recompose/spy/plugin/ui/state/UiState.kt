package com.xdmrwu.recompose.spy.plugin.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.awt.Component

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 13:12
 * @Description:
 */
@Stable
class UiState {
    var darkMode by mutableStateOf(false)
    val colors by derivedStateOf { if (darkMode) DarkColors else LightColors }
    var deviceList = mutableStateListOf<DeviceState>()
    var selectedDevice by mutableStateOf<DeviceState?>(null)
    var stackTraceComponent: StackTraceComponent? = null
}

@Stable
class DeviceState(val id: String, val name: String) {
    var recording by mutableStateOf(false)
    var recompositionList = mutableStateListOf<Recomposition>()
    var currentRecomposition by mutableStateOf<Recomposition?>(null)
}

interface StackTraceComponent {
    val component: Component
    fun print(stackTrace: List<String>)
}

@Stable
class Recomposition(
    val name: String,
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val startOffset: Int,
    val endOffset: Int,
    val cost: Long,
    val recomposeReason: List<AnnotatedContent>,
    val nonSkipReason: List<AnnotatedContent>,
    val changedParams: List<String>,
    val changedStates: List<String>
) {
    val children = mutableStateListOf<Recomposition>()
    var currentStackTrace by mutableStateOf<List<String>?>(null)

    fun getFileWithLines(): String {
        return "${file.split("/").last()}:$startLine"
    }
}

@Stable
data class AnnotatedContent(val content: String, val onClick: (() -> Unit)? = null)