package com.xdmrwu.recompose.spy.plugin.model

import kotlinx.serialization.Serializable

@Serializable
class RecomposeSpyTrackNode(
    val fqName: String,
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val inline: Boolean = false,
    val hasReturnType: Boolean = false, // 有返回值的Composable 不会是 restartable，也不会 skip
    val nonSkippable: Boolean = false,
    val nonRestartable: Boolean = false,
    val children: MutableList<RecomposeSpyTrackNode> = mutableListOf(),
    var recomposeState: RecomposeState
) {
    fun getDisplayName(): String {
        return if (fqName.isNotBlank()) {
            "$file.${fqName.last()}[$startLine:$endLine]"
        } else {
            "$file[$startLine:$endLine]"
        }
    }
}

@Serializable
data class RecomposeState(val paramStates: List<RecomposeParamState>,
                          val readStates: List<RecomposeReadState>,
                          val readCompositionLocals: List<RecomposeReadState>,
                          val forceRecompose: Boolean = false)

@Serializable
data class RecomposeParamState(
    val name: String,
    val used: Boolean,
    val static: Boolean = false,
    val changed: Boolean = false,
    val uncertain: Boolean = false, // TODO default 参数 dirty会被重置为 uncertain
    val useDefaultValue: Boolean = false,
)

@Serializable
data class RecomposeReadState(
    val name: String,
    val oldValue: String?,
    val newValue: String?,
    val changed: Boolean
)