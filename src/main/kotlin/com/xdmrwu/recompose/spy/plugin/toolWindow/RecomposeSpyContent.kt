package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import kotlinx.serialization.json.Json
import java.awt.Component
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import com.intellij.ui.treeStructure.Tree

/**
 * @Author: wulinpeng
 * @Date: 2025/7/3 23:31
 * @Description:
 */
class RecomposeSpyContent {

    private val json = Json { ignoreUnknownKeys = true }
    private var model: RecomposeSpyTrackNode? = null

    private val rootNode = DefaultMutableTreeNode("Messages")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        isRootVisible = true
        showsRootHandles = true
    }

    private val component by lazy {
        JBScrollPane(tree)
    }

    fun getContent(): Component {
        return component
    }

    fun onReceivedData(data: String) {
        val model = json.decodeFromString(RecomposeSpyTrackNode.serializer(), data)
        updateModel(model)
    }

    private fun updateModel(model: RecomposeSpyTrackNode) {
        this.model = model
        rootNode.removeAllChildren()
        rootNode.add(buildTreeNode(model))
        treeModel.reload(rootNode)
        expandAllNodes()
    }

    private fun buildTreeNode(node: RecomposeSpyTrackNode): DefaultMutableTreeNode {
        val treeNode = DefaultMutableTreeNode(node.getDisplayName())
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
}