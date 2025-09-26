package com.xdmrwu.recompose.spy.plugin.toolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.queryParameters
import com.xdmrwu.recompose.spy.plugin.analyze.recomposeReason
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.utils.openFileAndHighlight
import java.awt.BorderLayout
import java.awt.Font
import java.net.URI
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.table.DefaultTableModel

/**
 * @Author: wulinpeng
 * @Date: 2025/7/4 21:17
 * @Description:
 */
class TraceNodeDetailPanel(
    private val project: Project
) : JPanel(BorderLayout()) {

    companion object {
        const val BUTTON_TEXT_ORIGINAL = "展示原始数据"
        const val BUTTON_TEXT_ANALYZED = "展示智能归因结果"
    }

    private var rootNode: RecomposeSpyTrackNode? = null
    private var selectedNode: RecomposeSpyTrackNode? = null

    private val tableModel = object : DefaultTableModel(arrayOf("属性", "值"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JTable(tableModel).apply {
        // TODO 长文本展示 & param、state 展示
        font = Font("JetBrains Mono", Font.PLAIN, 16)
        rowHeight = 28
        autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        setShowGrid(false)
        setFillsViewportHeight(true)
    }
    private val tablePanel = JBScrollPane(table)

    private val reasonArea = JTextPane().apply {
        contentType = "text/html"
        // 让 HTML 使用组件字体
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
        font = Font("JetBrains Mono", Font.PLAIN, 16)
        isEditable = false
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                // 处理链接点击事件
                val uri = runCatching { e.url?.toURI() ?: URI(e.description) }.getOrNull() ?: return@addHyperlinkListener
                if (uri.scheme == "action" && uri.host == "state") {
                    val index = uri.queryParameters.get("index")?.toInt() ?: return@addHyperlinkListener
                    val state = selectedNode?.recomposeState?.readStates?.getOrNull(index) ?: return@addHyperlinkListener
                    // 在这里处理点击的 State，可能是打开相关文件或跳转到相关位置
                    openFileAndHighlight(project, state.file, state.startLine, state.startOffset, state.endOffset)
                }
            }
        }
        addHyperlinkListener {

        }
    }
    private val reasonScrollPane = JBScrollPane(reasonArea)

    private var showRecomposeReason = true

    private val analyzeButton = JButton(BUTTON_TEXT_ORIGINAL).apply {
        font = Font("JetBrains Mono", Font.BOLD, 16)
        addActionListener {
            showRecomposeReason = !showRecomposeReason
            if (!showRecomposeReason) {
                // 展示原始数据
                text = BUTTON_TEXT_ANALYZED
            } else {
                // 展示智能归因结果
                text = BUTTON_TEXT_ORIGINAL
            }
            updateNode(rootNode!!, selectedNode!!)
        }
    }

    private val topPanel = JPanel(BorderLayout()).apply {
        add(analyzeButton, BorderLayout.WEST)
    }

    init {
        add(topPanel, BorderLayout.NORTH)
    }

    fun updateNode(rootNode: RecomposeSpyTrackNode, selectedNode: RecomposeSpyTrackNode) {
        this.rootNode = rootNode
        this.selectedNode = selectedNode

        if (showRecomposeReason) {
            remove(tablePanel)
            add(reasonScrollPane, BorderLayout.CENTER)
            updateReasonArea(rootNode, selectedNode)
        } else {
            remove(reasonScrollPane)
            add(tablePanel, BorderLayout.CENTER)
            updateTableData(rootNode, selectedNode)
        }
    }

    private fun updateReasonArea(rootNode: RecomposeSpyTrackNode, selectedNode: RecomposeSpyTrackNode) {
        reasonArea.text = selectedNode.recomposeReason()
    }

    private fun updateTableData(rootNode: RecomposeSpyTrackNode, selectedNode: RecomposeSpyTrackNode) {
        // 展示属性表格
        tableModel.setRowCount(0)

        tableModel.addRow(arrayOf("fqName", selectedNode.fqName))
        tableModel.addRow(arrayOf("filePath", selectedNode.file))
        tableModel.addRow(arrayOf("startLine", selectedNode.startLine.toString()))
        tableModel.addRow(arrayOf("endLine", selectedNode.endLine.toString()))
        tableModel.addRow(arrayOf("startOffset", selectedNode.startOffset.toString()))
        tableModel.addRow(arrayOf("endOffset", selectedNode.endOffset.toString()))
        tableModel.addRow(arrayOf("hasDispatchReceiver", selectedNode.hasDispatchReceiver.toString()))
        tableModel.addRow(arrayOf("hasExtensionReceiver", selectedNode.hasExtensionReceiver.toString()))
        tableModel.addRow(arrayOf("isLambda", selectedNode.isLambda.toString()))
        tableModel.addRow(arrayOf("inline", selectedNode.inline.toString()))
        tableModel.addRow(arrayOf("hasReturnType", selectedNode.hasReturnType.toString()))
        tableModel.addRow(arrayOf("nonSkippable", selectedNode.nonSkippable.toString()))
        tableModel.addRow(arrayOf("nonRestartable", selectedNode.nonRestartable.toString()))
    }
}