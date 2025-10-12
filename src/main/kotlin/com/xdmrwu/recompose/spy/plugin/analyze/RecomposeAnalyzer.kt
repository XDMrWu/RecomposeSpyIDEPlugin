package com.xdmrwu.recompose.spy.plugin.analyze

import androidx.compose.ui.graphics.Color
import com.intellij.openapi.project.Project
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.ui.state.AnnotatedContent
import com.xdmrwu.recompose.spy.plugin.utils.openFileAndHighlight


/**
 * @Author: wulinpeng
 * @Date: 2025/7/17 23:16
 * @Description:
 */

fun RecomposeSpyTrackNode.recomposeReason(project: Project, parentNodes: List<RecomposeSpyTrackNode>): List<AnnotatedContent> {

    val result = mutableListOf<AnnotatedContent>()

    // TODO 通过 remember 支持判断是否第一次重组
    if (!recomposeState.forceRecompose && recomposeState.readStates.isEmpty()) {
        if (parentNodes.isNotEmpty()) {
            result.appendLine("本次重组由父级 Composable 触发，")
        } else {
            // TODO subcompose
            result.appendLine("未找到相关信息")
        }
    } else if (recomposeState.readStates.isEmpty()) {
        // TODO subcompose
        result.appendLine("未找到相关信息")
    } else if (recomposeState.readStates.any { it.currentComposableRead }) {
        result.appendLine("本次重组由以下 State 或 CompositionLocal 变化触发")
        recomposeState.readStates.filter {
            it.currentComposableRead
        }.forEach { readState ->
            // TODO index
            result.jumpFile(project, "State", readState.file, readState.startLine, readState.startOffset, readState.endOffset)
        }
    } else {
        // TODO 不一定是 child，也可能是 parent inline，自己也是 inline 的场景
        result.appendLine("本次重组由以下 State 或 CompositionLocal 变化触发")
        recomposeState.readStates.forEach { readState ->
            var childNode: RecomposeSpyTrackNode? = null
            traverseNoRecomposeScopeChildren {
                if (childNode == null && it.recomposeState.readStates.contains(readState)) {
                    childNode = it
                }
            }
            if (childNode != null) {
                // TODO 更多信息
                result.appendLine("${childNode.getDisplayName(true)} -> ${readState.file}: ${readState.startLine}")
            } else {
                result.jumpFile(project, "State", readState.file, readState.startLine, readState.startOffset, readState.endOffset)
            }
        }
    }
    return result
}

fun RecomposeSpyTrackNode.nonSkipReason(): List<AnnotatedContent> {
    val reason = StringBuilder()
    when {
        compositionCount == 1 -> reason.appendLine("此方法首次进入组合，无法跳过")
        nonSkippable -> reason.appendLine("此方法被标记为 @NonSkippableComposable, 无法跳过重组")
        nonRestartable -> reason.appendLine("此方法被标记为 @NonRestartableComposable, 无法跳过重组")
        hasReturnType -> reason.appendLine("此方法有返回值, 无法跳过重组")
        inline && !isLambda -> reason.appendLine("此方法为 inline, 无法跳过重组")
        inline && isLambda -> reason.appendLine("此方法是 inline 方法的 Lambda 参数，且该参数没有被标记为 noinline, 无法跳过重组")
        recomposeState.forceRecompose || recomposeState.readStates.any { it.currentComposableRead }
                    -> reason.appendLine("此方法为本次的重组作用域，因此无法跳过重组，具体触发重组原因请参考上方信息")
        recomposeState.paramStates.any { it.changed } -> {
            reason.appendLine("此方法的参数发生了变化，无法跳过重组，具体变化参数请参考下方信息")
            reason.appendLine(changedParams())
        }
        else -> reason.appendLine("未找到相关信息")
    }
    return listOf(AnnotatedContent(reason.toString()))
}

fun RecomposeSpyTrackNode.changedParams(): String {
    val changedParams = recomposeState.paramStates.filter { it.changed }

    val changedInfo = StringBuilder()

    if (changedParams.isNotEmpty()) {
        changedParams.forEach { param ->
            changedInfo.appendLine("  - ${param.name}: used=${param.used}, static=${param.static}, uncertain=${param.uncertain}, useDefaultValue=${param.useDefaultValue}")
        }
    }

    return changedInfo.toString()
}

fun RecomposeSpyTrackNode.traverseNoRecomposeScopeChildren(
    action: (RecomposeSpyTrackNode) -> Unit
) {
    action(this)
    children.filter {
        !it.canRestart()
    }.forEach {
        action(it)
        it.traverseNoRecomposeScopeChildren(action)
    }
}

fun RecomposeSpyTrackNode.canRestart(): Boolean {
    return !inline && !hasReturnType && !nonRestartable
}

fun MutableList<AnnotatedContent>.appendLine(line: String) {
    add(AnnotatedContent("$line\n"))
}

fun MutableList<AnnotatedContent>.jumpFile(project: Project, content: String, file: String, startLine: Int, startOffset: Int, endOffset: Int) {
    add(
        AnnotatedContent(
            content,
            {
                openFileAndHighlight(project, file, startLine, startOffset, endOffset)
            }
        )
    )
}