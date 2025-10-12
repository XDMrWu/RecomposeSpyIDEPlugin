package com.xdmrwu.recompose.spy.plugin.model

import kotlinx.serialization.Serializable

@Serializable
class RecomposeSpyTrackNode(
    val fqName: String,
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val startOffset: Int,
    val endOffset: Int,
    val hasDispatchReceiver: Boolean,
    val hasExtensionReceiver: Boolean,
    val isLambda: Boolean = false,
    val inline: Boolean = false,
    val hasReturnType: Boolean = false, // 有返回值的Composable 不会是 restartable，也不会 skip
    val nonSkippable: Boolean = false,
    val nonRestartable: Boolean = false,
    var recomposeReason: String = "",
    var compositionCount: Int = 0,
    val children: MutableList<RecomposeSpyTrackNode> = mutableListOf(),
    var recomposeState: RecomposeState
) {
    fun getDisplayName(withLines: Boolean = true): String {
        // 只保留一个匿名标识
        val hasAnonymous = fqName.contains("<anonymous>")
        val functionName = fqName.replace(".<anonymous>", "").split(".").last()
        var result =  if (hasAnonymous) {
            "$functionName.<anonymous>"
        } else {
            "$functionName"
        }
        if (withLines) {
            result = "$result[$startLine:$endLine]"
        }
        return result
    }
}

@Serializable
data class RecomposeState(val paramStates: List<RecomposeParamState>,
                          val readStates: List<RecomposeReadState>,
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
    val file: String,
    val propertyName: String,
    val startLine: Int,
    val endLine: Int,
    val startOffset: Int,
    val endOffset: Int,
    var currentComposableRead: Boolean,
    val stackTrace: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (other !is RecomposeReadState) return false
        if (this === other) return true
        return this.file == other.file &&
                this.propertyName == other.propertyName &&
                this.startLine == other.startLine &&
                this.endLine == other.endLine &&
                this.startOffset == other.startOffset &&
                this.endOffset == other.endOffset &&
                this.stackTrace == other.stackTrace
    }
}