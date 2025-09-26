package com.xdmrwu.recompose.spy.plugin.utils

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Color
import java.awt.Font
import javax.swing.Timer

/**
 * @Author: wulinpeng
 * @Date: 2025/8/19 22:55
 * @Description:
 */
fun openFileAndHighlight(project: Project, filePath: String, startLine: Int, startOffset: Int, endOffset: Int) {
    val vf = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return
    OpenFileDescriptor(project, vf, startLine, 0).navigate(true)
    highlightCode(project, startOffset, endOffset)
}

private fun highlightCode(project: Project, startOffset: Int, endOffset: Int) {
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