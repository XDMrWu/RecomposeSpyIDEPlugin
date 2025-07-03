package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.android.ddmlib.IDevice
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTabbedPane
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class RecomposeSpyToolWindowContent(private val service: AdbConnectionService) {
    private val contentPanel: JPanel = JPanel(BorderLayout())
    private val tabbedPane = JBTabbedPane(SwingConstants.TOP)
    private val deviceComponents = mutableMapOf<String, RecomposeSpyContent>()
    private val emptyLabel = JLabel("No device connected", JLabel.CENTER)

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
        contentPanel.add(emptyLabel, BorderLayout.CENTER)
        service.addDeviceInfoListener(object : DeviceManager.IDeviceInfoListener {
            override fun onDeviceConnected(device: IDevice) {
                SwingUtilities.invokeLater {
                    if (!deviceComponents.containsKey(device.serialNumber)) {
                        val content = RecomposeSpyContent()
                        deviceComponents[device.serialNumber] = content
                        val tabTitle = ellipsizeTabTitle(device)
                        tabbedPane.addTab(tabTitle, content.getContent())
                    }
                    // 切换到 tabbedPane 展示
                    if (contentPanel.getComponent(0) == emptyLabel) {
                        contentPanel.remove(emptyLabel)
                        contentPanel.add(tabbedPane, BorderLayout.CENTER)
                        contentPanel.revalidate()
                        contentPanel.repaint()
                    }
                }
            }

            override fun onDeviceDisconnected(device: IDevice) {
                SwingUtilities.invokeLater {
                    val tabTitle = ellipsizeTabTitle(device)
                    deviceComponents.remove(device.serialNumber)
                    val idx = tabbedPane.indexOfTab(tabTitle)
                    if (idx != -1) {
                        tabbedPane.removeTabAt(idx)
                    }
                    // 如果没有 device，显示 emptyLabel
                    if (deviceComponents.isEmpty()) {
                        contentPanel.remove(tabbedPane)
                        contentPanel.add(emptyLabel, BorderLayout.CENTER)
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
