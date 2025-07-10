package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.openapi.project.Project
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.services.DeviceWrapper
import com.xdmrwu.recompose.spy.plugin.toolWindow.ui.RecomposeTreePanel
import com.xdmrwu.recompose.spy.plugin.toolWindow.ui.TopRecordPanel
import com.xdmrwu.recompose.spy.plugin.toolWindow.ui.TraceNodeDetailPanel
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants
import javax.swing.plaf.basic.BasicSplitPaneUI

/**
 * @Author: wulinpeng
 * @Date: 2025/7/3 23:31
 * @Description:
 */
class RecomposeSpyContent(val project: Project, val device: DeviceWrapper) {

    private val json = Json { ignoreUnknownKeys = true }

    // 支持多个 model
    private val models = mutableListOf<RecomposeSpyTrackNode>()

    private val recomposeTreePanel = RecomposeTreePanel(models, project) { rootNode, node ->
        detailPanel.updateNode(rootNode, node)
    }
    private val detailPanel = TraceNodeDetailPanel(project)

    private val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
        topComponent = recomposeTreePanel
        bottomComponent = detailPanel
        dividerLocation = 300
        resizeWeight = 0.3
        dividerSize = 2
        (ui as? BasicSplitPaneUI)?.divider?.background = Color.GRAY
    }

    // 顶部监听按钮
    private val topRecordPanel = TopRecordPanel(device) { isRecording ->
        if (isRecording) {
            models.clear()
            recomposeTreePanel.updateTree()
        }
        updateStatePanel()
    }

    // 监听中面板
    private val listeningPanel = JPanel().apply {
        layout = BorderLayout()
        val label = JLabel("监听中", SwingConstants.CENTER).apply {
            font = Font("JetBrains Mono", Font.BOLD, 16)
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
        }
        add(label, BorderLayout.CENTER)
    }

    // 主内容面板
    private val component = JPanel(BorderLayout())

    init {
        updateStatePanel()
    }

    private fun updateStatePanel() {
        component.removeAll()
        component.add(topRecordPanel, BorderLayout.NORTH)

        when {
            models.isNotEmpty() -> component.add(splitPane, BorderLayout.CENTER)
            topRecordPanel.isRecording && models.isEmpty() -> component.add(listeningPanel, BorderLayout.CENTER)
        }

        component.revalidate()
        component.repaint()
        topRecordPanel.repaint()
    }

    fun getContent(): Component {
        return component
    }

    fun onReceivedData(data: String) {
        val model = json.decodeFromString(RecomposeSpyTrackNode.serializer(), data)
        updateModel(model)
    }

    // 支持多个 model 展示
    private fun updateModel(model: RecomposeSpyTrackNode) {
        if (!topRecordPanel.isRecording) {
            return
        }
        models.add(model)
        recomposeTreePanel.updateTree()
        updateStatePanel()
    }
}