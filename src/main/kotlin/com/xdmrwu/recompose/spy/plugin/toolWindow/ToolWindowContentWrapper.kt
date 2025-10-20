package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.android.ddmlib.IDevice
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.xdmrwu.recompose.spy.plugin.analyze.nonSkipReason
import com.xdmrwu.recompose.spy.plugin.analyze.recomposeReason
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager
import com.xdmrwu.recompose.spy.plugin.services.DeviceWrapper
import com.xdmrwu.recompose.spy.plugin.ui.WindowToolPanel
import com.xdmrwu.recompose.spy.plugin.ui.state.DeviceState
import com.xdmrwu.recompose.spy.plugin.ui.state.Recomposition
import com.xdmrwu.recompose.spy.plugin.ui.state.StackTraceComponent
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState
import com.xdmrwu.recompose.spy.plugin.utils.openFileAndHighlight
import com.xdmrwu.recompose.spy.plugin.utils.showNotify
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.idea.debugger.core.KotlinExceptionFilterFactory

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 13:29
 * @Description:
 */
class ToolWindowContentWrapper(val service: AdbConnectionService, val project: Project) {
    private val toolWindowPanel = WindowToolPanel()

    private val json = Json { ignoreUnknownKeys = true }
    private val uiState = toolWindowPanel.state
    private val deviceMap = mutableMapOf<String, DeviceWrapper>()

    init {
        onThemeChange = {
            updateDarkMode()
        }
        updateDarkMode()
        uiState.stackTraceComponent = createStackTraceComponent(project)
        toolWindowPanel.onClickRecord = ::onClickRecord
        toolWindowPanel.onSelectDevice = ::onSelectDevice
        toolWindowPanel.openFile = ::openFile
        // TODO dark mode change listener
        service.addDeviceInfoListener(object : DeviceManager.IDeviceInfoListener {
            override fun onDeviceConnected(deviceWrapper: DeviceWrapper) {
                val device = deviceWrapper.device
                deviceMap[device.serialNumber] = deviceWrapper
                if (uiState.deviceList.none { it.id == device.serialNumber }) {
                    val deviceState = DeviceState(device.serialNumber, device.name)
                    uiState.deviceList.add(deviceState)

                    if (uiState.selectedDevice == null) {
                        uiState.selectedDevice = deviceState
                    }
                }
            }

            override fun onDeviceDisconnected(deviceWrapper: DeviceWrapper) {
                val device = deviceWrapper.device
                deviceMap.remove(device.serialNumber)
                val deviceState = uiState.deviceList.firstOrNull {
                    it.id == device.serialNumber
                } ?: return
                uiState.deviceList.remove(deviceState)
                if (uiState.selectedDevice == deviceState) {
                    uiState.selectedDevice = null
                }
            }

            override fun onReceivedData(device: IDevice, data: String) {
                if (uiState.selectedDevice?.id != device.serialNumber) {
                    return
                }
                val model = json.decodeFromString(RecomposeSpyTrackNode.serializer(), data)
                model.fillParent()
                uiState.selectedDevice?.recompositionList?.add(model.toRecomposition(project, uiState))
            }
        })
        service.connectToAdb()
    }

    private fun createStackTraceComponent(project: Project): StackTraceComponent {
        val consoleView = ConsoleViewImpl(project, true)

        // 注册异常过滤器（支持源码跳转）
        val exceptionFilter = KotlinExceptionFilterFactory().create(project, GlobalSearchScope.allScope(project))
        consoleView.addMessageFilter(exceptionFilter)

        return object : StackTraceComponent {
            override val component = consoleView.component

            override fun print(stackTrace: List<String>) {
                consoleView.clear()
                consoleView.print(stackTrace.joinToString("\n"), ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
    }

    private fun updateDarkMode() {
        uiState.darkMode = LafManager.getInstance().currentUIThemeLookAndFeel.isDark
    }

    fun getContentPanel() = toolWindowPanel.getPanel()

    fun onClickRecord() {
        val device = getSelectedDeviceWrapper() ?: return
        val recording = uiState.selectedDevice?.recording ?: return
        val success = device.updateStatus(!recording)
        if (!success) {
            showNotify(project, "Failed to ${if (recording) "stop" else "start"} recording")
            return
        }
        // update state
        uiState.selectedDevice?.recording = !recording
        if (!recording) {
            uiState.selectedDevice?.recompositionList?.clear()
            uiState.selectedDevice?.currentRecomposition = null
        }
    }

    private fun getSelectedDeviceWrapper() = uiState.selectedDevice?.id?.let {
        deviceMap[it]
    }

    fun onSelectDevice(deviceId: String) {
        // 重置当前选中设备
        uiState.selectedDevice?.id?.let {
            resetDevice(it)
        }
        uiState.selectedDevice = uiState.deviceList.firstOrNull { it.id == deviceId }
    }

    private fun resetDevice(id: String) {
        deviceMap[id]?.updateStatus(false)
        uiState.deviceList.firstOrNull { it.id == id }?.apply {
            recording = false
            recompositionList.clear()
            currentRecomposition = null
        }
    }

    fun openFile(recomposition: Recomposition) {
        openFileAndHighlight(project, recomposition.file, recomposition.startLine, recomposition.startOffset, recomposition.endOffset)
    }

}

private fun RecomposeSpyTrackNode.fillParent() {
    children.forEach {
        it.parent = this
        it.fillParent()
    }
}

fun RecomposeSpyTrackNode.toRecomposition(project: Project, uiState: UiState): Recomposition {
    val recomposition = Recomposition(
        name = getDisplayName(false),
        file = file,
        startLine = startLine,
        endLine = endLine,
        startOffset = startOffset,
        endOffset = endOffset,
        cost = endTimestamp - startTimestamp,
        recomposeReason = recomposeReason(project, uiState),
        nonSkipReason = nonSkipReason(),
        changedParams = recomposeState.paramStates.filter { it.changed }.map { it.name },
        changedStates = recomposeState.readStates.map { it.propertyName }
    )
    recomposition.children.addAll(children.map { it.toRecomposition(project, uiState) })
    return recomposition
}