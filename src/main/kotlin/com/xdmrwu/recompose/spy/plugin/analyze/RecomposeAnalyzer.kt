package com.xdmrwu.recompose.spy.plugin.analyze

import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode

/**
 * @Author: wulinpeng
 * @Date: 2025/7/6 16:06
 * @Description:
 */
object RecomposeAnalyzer {
    fun analyzeRecomposeReason(rootNode: RecomposeSpyTrackNode, selectedNode: RecomposeSpyTrackNode): String {
        return selectedNode.recomposeReason()
    }
}

fun RecomposeSpyTrackNode.recomposeReason(): String {

    val reason = StringBuilder()
    when {
        nonSkippable -> reason.appendLine("此方法被标记为 @NonSkippableComposable, 无法跳过重组")
        hasReturnType -> reason.appendLine("此方法有返回值, 无法跳过重组")
        inline && !isLambda -> reason.appendLine("此方法为 inline, 无法跳过重组")
        inline && isLambda -> reason.appendLine("此方法是 inline 方法的 Lambda 参数，且该参数没有被标记为 noinline, 无法跳过重组")
        // state or compositionLocal read
        recomposeState.forceRecompose -> {
            var changedStates = ""
            var node: RecomposeSpyTrackNode? = null
            traverseInlineChildren {
                if (changedStates.isNotEmpty()) {
                    return@traverseInlineChildren
                }
                changedStates = it.changedValueInfo(state = true, compositionLocal = true)
                node = it
            }
            if (changedStates.isEmpty()) {
                reason.appendLine("此方法为本次重组的 Scope，但没有找到具体的 State 或 CompositionLocal 变化信息。")
            } else if (node != null) {
                reason.appendLine("此方法为本次重组的 Scope，因为某个 inline 子组件的以下状态或 CompositionLocal 发生了变化:")
                reason.appendLine("子组件: ${node.getDisplayName()}")
                reason.appendLine(changedStates)
            } else {
                reason.appendLine("此方法为本次重组的 Scope，因为以下 State 或 CompositionLocal 发生了变化:")
                reason.appendLine(changedStates)
            }
        }
        else -> {
            // 检查参数参数变化
            val changedInfo = changedParams()
            if (changedInfo.isNotEmpty()) {
                reason.appendLine("该 Composable 方法无法跳过重组，因为以下参数发生了变化:")
                reason.appendLine(changedInfo)
            } else {
                reason.appendLine("该 Composable 方法无法跳过重组，但没有找到具体的变化信息。")
            }

        }
    }
    return reason.toString()
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

fun RecomposeSpyTrackNode.changedValueInfo(state: Boolean = false, compositionLocal: Boolean = false): String {
    val changedStates = recomposeState.readStates.filter { it.changed }
    val changedCompositionLocals = recomposeState.readCompositionLocals.filter { it.changed }

    val changedInfo = StringBuilder()

    if (state && changedStates.isNotEmpty()) {
        changedInfo.appendLine("Changed States:")
        changedStates.forEach { state ->
            changedInfo.appendLine("  - ${state.name}: oldValue=${state.oldValue}, newValue=${state.newValue}")
        }
    }
    if (compositionLocal && changedCompositionLocals.isNotEmpty()) {
        changedInfo.appendLine("Changed Composition Locals:")
        changedCompositionLocals.forEach { local ->
            changedInfo.appendLine("  - ${local.name}: oldValue=${local.oldValue}, newValue=${local.newValue}")
        }
    }

    return changedInfo.toString()
}

fun RecomposeSpyTrackNode.traverseInlineChildren(
    action: (RecomposeSpyTrackNode) -> Unit
) {
    action(this)
    children.filter {
        it.inline
    }.forEach {
        action(it)
        it.traverseInlineChildren(action)
    }
}