package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.xdmrwu.recompose.spy.plugin.analyze.RecomposeAnalyzer
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * @Author: wulinpeng
 * @Date: 2025/7/4 21:17
 * @Description:
 */
class TraceNodeDetailPanel(
    private val project: Project
) : JPanel(BorderLayout()) {

    // 右侧属性面板
    private val detailArea = JTextArea().apply {
        font = Font("JetBrains Mono", Font.PLAIN, 18)
        isEditable = false
        wrapStyleWord = true
    }

    private var rootNode: RecomposeSpyTrackNode? = null
    private var selectedNode: RecomposeSpyTrackNode? = null

    private val scrollPane = JBScrollPane(detailArea)

    init {
        add(scrollPane, BorderLayout.CENTER)
    }

    fun updateNode(rootNode: RecomposeSpyTrackNode, selectedNode: RecomposeSpyTrackNode) {
        this.rootNode = rootNode
        this.selectedNode = selectedNode
        detailArea.text = RecomposeAnalyzer.analyzeRecomposeReason(rootNode, selectedNode)
    }

    // 构建详细属性文本
    private fun buildDetailText(node: RecomposeSpyTrackNode): String {
        val sb = StringBuilder()
        sb.append("fqName: ${node.fqName}\n")
        sb.append("file: ${node.file}\n")
        sb.append("line: ${node.startLine} - ${node.endLine}\n")
        sb.append("hasExtensionReceiver: ${node.hasExtensionReceiver}\n")
        sb.append("hasDispatchReceiver: ${node.hasDispatchReceiver}\n")
        sb.append("isLambda: ${node.isLambda}\n")
        sb.append("inline: ${node.inline}\n")
        sb.append("hasReturnType: ${node.hasReturnType}\n")
        sb.append("nonSkippable: ${node.nonSkippable}\n")
        sb.append("nonRestartable: ${node.nonRestartable}\n")
        sb.append("\nRecomposeState:\n")
        sb.append("  forceRecompose: ${node.recomposeState.forceRecompose}\n")
        sb.append("  paramStates:\n")
        node.recomposeState.paramStates.forEach {
            sb.append("    - ${it.name}: used=${it.used}, static=${it.static}, changed=${it.changed}, uncertain=${it.uncertain}, useDefaultValue=${it.useDefaultValue}\n")
        }
        sb.append("  readStates:\n")
        node.recomposeState.readStates.forEach {
            sb.append("    - ${it.name}: old=${it.oldValue}, new=${it.newValue}, changed=${it.changed}\n")
        }
        sb.append("  readCompositionLocals:\n")
        node.recomposeState.readCompositionLocals.forEach {
            sb.append("    - ${it.name}: old=${it.oldValue}, new=${it.newValue}, changed=${it.changed}\n")
        }
        return sb.toString()
    }
}