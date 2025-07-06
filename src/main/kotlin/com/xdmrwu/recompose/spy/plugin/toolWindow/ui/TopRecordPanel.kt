package com.xdmrwu.recompose.spy.plugin.toolWindow.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton

/**
 * @Author: wulinpeng
 * @Date: 2025/7/6 14:51
 * @Description:
 */
class TopRecordPanel(onUpdate: (Boolean) -> Unit): JButton() {

    var isRecording = false

    private val button = object : JButton() {
        init {
            apply {
                preferredSize = Dimension(32, 32)
                minimumSize = Dimension(32, 32)
                maximumSize = Dimension(32, 32)
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                updateToolTip()
                addActionListener {
                    isRecording = !isRecording
                    updateToolTip()
                    onUpdate(isRecording)
                }
            }
        }

        private fun updateToolTip() {
            if (isRecording) {
                toolTipText = "停止监听"
            } else {
                toolTipText = "开始监听"
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = width
            val h = height
            if (isRecording) {
                // 红色正方形
                g2.color = Color(0xE53935)
                val size = Math.min(w, h) * 0.5
                val x = (w - size) / 2
                val y = (h - size) / 2
                g2.fillRect(x.toInt(), y.toInt(), size.toInt(), size.toInt())
            } else {
                // 绿色三角形
                g2.color = Color(0x43A047)
                val path = Path2D.Double()
                path.moveTo(w * 0.35, h * 0.25)
                path.lineTo(w * 0.7, h * 0.5)
                path.lineTo(w * 0.35, h * 0.75)
                path.closePath()
                g2.fill(path)
            }
        }
    }

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)
        background = Color(0, 0, 0, 0)
        add(button)
        add(Box.createHorizontalGlue())
    }

}