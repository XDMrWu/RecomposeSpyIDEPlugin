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
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.basic.BasicSplitPaneUI

/**
 * @Author: wulinpeng
 * @Date: 2025/7/3 23:31
 * @Description:
 */
class RecomposeSpyContent(val project: Project) {

    private val json = Json { ignoreUnknownKeys = true }
    private var model: RecomposeSpyTrackNode? = null

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

    private val emptyPanel = JLabel("No Recompose Data", JLabel.CENTER).apply {
        font = Font("JetBrains Mono", Font.BOLD, 16)
    }

    private val component by lazy {
        JPanel(BorderLayout()).apply {
            add(emptyPanel, BorderLayout.CENTER)
        }
    }

    init {
        tree.addTreeSelectionListener(TreeSelectionListener { e: TreeSelectionEvent? ->
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            val node = selectedNode?.getTrackNode()
            detailPanel.updateNode(model!!, node!!)
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

    fun getContent(): Component {
        return component
    }

    fun onReceivedData(data: String) {
        val model = json.decodeFromString(RecomposeSpyTrackNode.serializer(), data)
        updateModel(model)
    }

    private fun updateModel(model: RecomposeSpyTrackNode) {
        if (component.getComponent(0) != splitPane) {
            component.removeAll()
            component.add(splitPane, BorderLayout.CENTER)
        }
        this.model = model
        rootNode.removeAllChildren()
        rootNode.add(buildTreeNode(model))
        treeModel.reload(rootNode)
        expandAllNodes()
        // 默认选中根节点并显示属性
        tree.addSelectionRow(1)
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
}