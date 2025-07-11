package com.xdmrwu.recompose.spy.plugin.toolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.xdmrwu.recompose.spy.plugin.analyze.RecomposeAnalyzer
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
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

    private val reasonArea = JTextArea().apply {
        font = Font("JetBrains Mono", Font.PLAIN, 16)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
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
        reasonArea.text = RecomposeAnalyzer.analyzeRecomposeReason(rootNode, selectedNode)
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