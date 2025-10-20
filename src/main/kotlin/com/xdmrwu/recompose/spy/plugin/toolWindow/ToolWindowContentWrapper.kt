package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.android.ddmlib.IDevice
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.project.Project
import com.xdmrwu.recompose.spy.plugin.analyze.nonSkipReason
import com.xdmrwu.recompose.spy.plugin.analyze.recomposeReason
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager
import com.xdmrwu.recompose.spy.plugin.services.DeviceWrapper
import com.xdmrwu.recompose.spy.plugin.ui.WindowToolPanel
import com.xdmrwu.recompose.spy.plugin.ui.state.DeviceState
import com.xdmrwu.recompose.spy.plugin.ui.state.Recomposition
import com.xdmrwu.recompose.spy.plugin.utils.openFileAndHighlight
import com.xdmrwu.recompose.spy.plugin.utils.showNotify
import kotlinx.serialization.json.Json

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
        updateDarkMode()
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
                uiState.selectedDevice?.recompositionList?.add(model.toRecomposition(project))
            }
        })
        service.connectToAdb()
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

fun RecomposeSpyTrackNode.toRecomposition(project: Project): Recomposition {
    val recomposition = Recomposition(
        name = getDisplayName(false),
        file = file,
        startLine = startLine,
        endLine = endLine,
        startOffset = startOffset,
        endOffset = endOffset,
        recomposeReason = recomposeReason(project),
        nonSkipReason = nonSkipReason(),
        changedParams = recomposeState.paramStates.filter { it.changed }.map { it.name },
        changedStates = recomposeState.readStates.map { it.propertyName }
    )
    recomposition.children.addAll(children.map { it.toRecomposition(project) })
    return recomposition
}