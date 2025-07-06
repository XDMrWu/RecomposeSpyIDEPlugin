package com.xdmrwu.recompose.spy.plugin.toolWindow.ui

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.observable.util.addComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Timer
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import kotlin.compareTo
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import java.awt.Color

/**
 * @Author: wulinpeng
 * @Date: 2025/7/6 23:24
 * @Description:
 */
class RecomposeTreePanel(val models: MutableList<RecomposeSpyTrackNode>,
                         val project: Project,
                         onNodeSelect: (RecomposeSpyTrackNode, RecomposeSpyTrackNode) -> Unit): JBScrollPane() {

    private val rootNode = DefaultMutableTreeNode("Recompose Tree")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        isRootVisible = true
        showsRootHandles = true
        font = Font("JetBrains Mono", Font.PLAIN, 16)
    }

    init {
        setViewportView(tree)
        tree.addTreeSelectionListener(TreeSelectionListener { e: TreeSelectionEvent? ->
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@TreeSelectionListener
            val node = selectedNode?.getTrackNode()!!
            onNodeSelect(node.getRootNode(), node)
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
                        highlightRecomposeCode(project, node.startOffset, node.endOffset)
                    }
                    e.consume()
                }
            }
        })
        tree.setToggleClickCount(0) // 禁止通过双击展开/收起
    }

    private fun highlightRecomposeCode(project: Project, startOffset: Int, endOffset: Int) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return

        val color = Color(102, 187, 255, 120)
        val attrs = TextAttributes(null, color, null, null, Font.PLAIN)
        val markup = editor.markupModel
        val highlighter = markup.addRangeHighlighter(
            startOffset, endOffset, HighlighterLayer.SELECTION - 1, attrs, HighlighterTargetArea.EXACT_RANGE
        )
        // 高亮 700 ms 后自动消失
        Timer(700) {
            markup.removeHighlighter(highlighter)
        }.start()
    }

    fun updateTree() {
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
        val treeNode = DefaultMutableTreeNode(node.getDisplayName())
//        treeNode.userObject = node
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
        val indexList = mutableListOf<Int>()
        var currentNode: TreeNode = this
        while (currentNode != root) {
            val index = currentNode.parent.getIndex(currentNode)
            indexList.add(0, index)
            currentNode = currentNode.parent
        }
        var result: RecomposeSpyTrackNode = models[indexList.removeAt(0)]
        indexList.forEach {
            result = result.children[it]
        }

        return result
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