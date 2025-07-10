package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTabbedPane
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager
import com.xdmrwu.recompose.spy.plugin.services.DeviceWrapper
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class RecomposeSpyToolWindowContent(private val service: AdbConnectionService, project: Project) {
    private val contentPanel: JPanel = JPanel(BorderLayout())
    private val tabbedPane = JBTabbedPane(SwingConstants.TOP).apply {
        font = Font("JetBrains Mono", Font.PLAIN, 13)
    }
    private val deviceComponents = mutableMapOf<String, RecomposeSpyContent>()
    private val emptyPanel = JPanel(BorderLayout()).apply {
        val centerPanel = JPanel()
        centerPanel.layout = javax.swing.BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS)
        centerPanel.alignmentX = Component.CENTER_ALIGNMENT
        centerPanel.alignmentY = Component.CENTER_ALIGNMENT

        // 垂直居中
        centerPanel.add(javax.swing.Box.createVerticalGlue())

        val label = JLabel("No device connected", JLabel.CENTER).apply {
            font = Font("JetBrains Mono", Font.BOLD, 16)
            alignmentX = Component.CENTER_ALIGNMENT
        }
        val retryButton = JButton("重试连接").apply {
            font = Font("JetBrains Mono", Font.PLAIN, 13)
            alignmentX = Component.CENTER_ALIGNMENT
            addActionListener {
                service.connectToAdb()
            }
        }
        centerPanel.add(label)
        centerPanel.add(javax.swing.Box.createVerticalStrut(16))
        centerPanel.add(retryButton)

        // 垂直居中
        centerPanel.add(javax.swing.Box.createVerticalGlue())

        add(centerPanel, BorderLayout.CENTER)
    }

    companion object {
        // 缩略 tab 名称，超出 maxLen 时用省略号
        fun ellipsizeTabTitle(device: IDevice, maxLen: Int = 24): String {
            val title = "${device.name} (${device.serialNumber})"
            return if (title.length > maxLen) {
                title.substring(0, maxLen - 3) + "..."
            } else {
                title
            }
        }
    }

    init {
        contentPanel.add(emptyPanel, BorderLayout.CENTER)
        service.addDeviceInfoListener(object : DeviceManager.IDeviceInfoListener {
            override fun onDeviceConnected(deviceWrapper: DeviceWrapper) {
                val device = deviceWrapper.device
                SwingUtilities.invokeLater {
                    if (!deviceComponents.containsKey(device.serialNumber)) {
                        val content = RecomposeSpyContent(project, deviceWrapper)
                        deviceComponents[device.serialNumber] = content
                        val tabTitle = ellipsizeTabTitle(device)
                        tabbedPane.addTab(tabTitle, content.getContent())
                    }
                    // 切换到 tabbedPane 展示
                    if (contentPanel.getComponent(0) == emptyPanel) {
                        contentPanel.remove(emptyPanel)
                        contentPanel.add(tabbedPane, BorderLayout.CENTER)
                        contentPanel.revalidate()
                        contentPanel.repaint()
                    }
                }
            }

            override fun onDeviceDisconnected(deviceWrapper: DeviceWrapper) {
                val device = deviceWrapper.device
                SwingUtilities.invokeLater {
                    val tabTitle = ellipsizeTabTitle(device)
                    deviceComponents.remove(device.serialNumber)
                    val idx = tabbedPane.indexOfTab(tabTitle)
                    if (idx != -1) {
                        tabbedPane.removeTabAt(idx)
                    }
                    // 如果没有 device，显示 emptyPanel
                    if (deviceComponents.isEmpty()) {
                        contentPanel.remove(tabbedPane)
                        contentPanel.add(emptyPanel, BorderLayout.CENTER)
                        contentPanel.revalidate()
                        contentPanel.repaint()
                    }
                }
            }

            override fun onReceivedData(device: IDevice, data: String) {
                SwingUtilities.invokeLater {
                    deviceComponents[device.serialNumber]?.onReceivedData(data)
                }
            }
        })
    }

    fun getContentPanel(): JPanel {
        return contentPanel
    }
}
