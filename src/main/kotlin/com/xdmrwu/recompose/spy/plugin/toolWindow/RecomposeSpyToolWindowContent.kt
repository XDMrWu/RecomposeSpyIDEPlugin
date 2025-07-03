package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.android.ddmlib.IDevice
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class RecomposeSpyToolWindowContent(private val service: AdbConnectionService) {
    private val contentPanel: JPanel = JPanel(BorderLayout())
    private val dataDisplayArea: JBTextArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        contentPanel.add(JBScrollPane(dataDisplayArea), BorderLayout.CENTER)
        // button
        val clearButton = JButton("链接 ABD").apply {
            addActionListener {
                service.connectToAdb()
            }
        }
        contentPanel.add(clearButton, BorderLayout.SOUTH)
        service.addDeviceInfoListener(object : DeviceManager.IDeviceInfoListener {
            override fun onDeviceConnected(device: IDevice) {
                appendData("Device connected: ${device.name} (${device.serialNumber})")
            }

            override fun onDeviceDisconnected(device: IDevice) {
                appendData("Device disconnected: ${device.name} (${device.serialNumber})")
            }

            override fun onReceivedData(device: IDevice, data: String) {
                appendData("Received data from ${device.name} (${device.serialNumber}): $data")
            }

        })
    }

    fun getContentPanel(): JPanel {
        return contentPanel
    }

    // 更新 UI 的方法，确保在 EDT (事件调度线程) 上执行
    fun appendData(data: String) {
        SwingUtilities.invokeLater {
            dataDisplayArea.append("${data}\n")
            dataDisplayArea.caretPosition = dataDisplayArea.document.length // 滚动到底部
        }
    }
}