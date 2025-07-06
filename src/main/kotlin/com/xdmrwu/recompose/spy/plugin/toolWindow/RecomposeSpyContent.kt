package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBScrollPane
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import kotlinx.serialization.json.Json
import java.awt.Component
import java.awt.Font
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import com.intellij.ui.treeStructure.Tree
import com.xdmrwu.recompose.spy.plugin.toolWindow.ui.TopRecordPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.JLabel
import javax.swing.SwingConstants

/**
 * @Author: wulinpeng
 * @Date: 2025/7/3 23:31
 * @Description:
 */
class RecomposeSpyContent(val project: Project) {

    private val json = Json { ignoreUnknownKeys = true }
    // 支持多个 model
    private val models = mutableListOf<RecomposeSpyTrackNode>()

    private val rootNode = DefaultMutableTreeNode("Recompose Tree")
    private val treeModel = DefaultTreeModel(rootNode)
    // 左侧树形结构
    private val tree = Tree(treeModel).apply {
        isRootVisible = true
        showsRootHandles = true
        font = Font("JetBrains Mono", Font.PLAIN, 16)
    }

    private val detailPanel = TraceNodeDetailPanel(project)

    // 主面板：左右分栏
    private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
        leftComponent = JBScrollPane(tree)
        rightComponent = detailPanel
        dividerLocation = 300
        resizeWeight = 0.3
        dividerSize = 2
        (ui as? BasicSplitPaneUI)?.divider?.background = Color.GRAY
    }

    // 顶部监听按钮
    private val topRecordPanel = TopRecordPanel { isRecording ->
        if (isRecording) {
            models.clear()
            updateTree()
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
        tree.addTreeSelectionListener(TreeSelectionListener { e: TreeSelectionEvent? ->
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@TreeSelectionListener
            val node = selectedNode?.getTrackNode()!!
            detailPanel.updateNode(node.getRootNode(), node)
        })
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selPath = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = (selPath.lastPathComponent as? DefaultMutableTreeNode)?.getTrackNode() ?: return
                    val filePath = node.file
                    val line = node.startLine
                    if (!filePath.isNullOrBlank() && line != null) {
                        val vf = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return
                        OpenFileDescriptor(project, vf, line - 1, 0).navigate(true)
                    }
                    e.consume()
                }
            }
        })
        tree.setToggleClickCount(0) // 禁止通过双击展开/收起
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
        updateTree()
        updateStatePanel()
    }

    private fun updateTree() {
        rootNode.removeAllChildren()
        for (m in models) {
            rootNode.add(buildTreeNode(m))
        }
        treeModel.reload(rootNode)
        expandAllNodes()
        // 默认选中第一个节点并显示属性
        if (rootNode.childCount > 0) {
            tree.setSelectionRow(1)
        }
    }

    private fun buildTreeNode(node: RecomposeSpyTrackNode): DefaultMutableTreeNode {
        val treeNode = DefaultMutableTreeNode(node)
        for (child in node.children) {
            treeNode.add(buildTreeNode(child))
        }
        return treeNode
    }

    private fun expandAllNodes() {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
        // 递归展开新加的节点
        var lastRow = -1
        while (tree.rowCount != lastRow) {
            lastRow = tree.rowCount
            for (i in 0 until tree.rowCount) {
                tree.expandRow(i)
            }
        }
    }

    private fun DefaultMutableTreeNode.getTrackNode(): RecomposeSpyTrackNode? {
        val obj = userObject
        return if (obj is RecomposeSpyTrackNode) obj else null
    }

    private fun RecomposeSpyTrackNode.getRootNode(): RecomposeSpyTrackNode {
        fun RecomposeSpyTrackNode.hasChildren(node: RecomposeSpyTrackNode): Boolean {
            if (this == node || children.any {it == node}) {
                return true
            }
            return children.any { child ->
                child.hasChildren(node)
            }
        }
        return models.first {
            it.hasChildren(this)
        }
    }
}