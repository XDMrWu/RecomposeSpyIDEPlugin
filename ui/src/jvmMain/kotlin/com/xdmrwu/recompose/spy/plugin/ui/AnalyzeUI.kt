package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdmrwu.recompose.spy.plugin.ui.state.AnnotatedContent
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 22:18
 * @Description:
 */

@Composable
fun AnalyzeUI(state: UiState) {
    if (!state.selectedDevice?.currentRecomposition?.currentStackTrace.isNullOrEmpty() && state.stackTraceComponent != null) {
        // 展示 StackTrace
        StackTraceUI(state)
    } else {
        // 展示分析结果
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            RecomposeReasonUI(state)
            Divider(Modifier.height(10.dp), Color.Transparent)
            NonSkipReasonUI(state)
        }
    }
}

@Composable
private fun StackTraceUI(state: UiState) {
    val colors = state.colors
    val stackTrace = state.selectedDevice?.currentRecomposition?.currentStackTrace ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .background(colors.backgroundSecondaryColor)
    ) {
        BackArrowIcon(
            colors.textSecondaryColor,
            Modifier.size(18.dp)
                .clickable {
                    state.selectedDevice?.currentRecomposition?.currentStackTrace = null
                }
        )
        SwingPanel(
            background = colors.backgroundSecondaryColor,
            factory = {
                state.stackTraceComponent!!.component
            },
            update = {
                state.stackTraceComponent!!.print(stackTrace)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp)
        )
    }
}

@Composable
private fun BackArrowIcon(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.7f, size.height * 0.1f)
            lineTo(size.width * 0.3f, size.height * 0.5f)
            lineTo(size.width * 0.7f, size.height * 0.9f)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun RecomposeReasonUI(state: UiState) {
    val colors = state.colors
    val composition = state.selectedDevice?.currentRecomposition ?: return
    Column (
        Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondaryColor)
            .padding(10.dp)
    ) {
        Text(
            "Recompose Reason",
            color = colors.textColor,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        val tag = "RecomposeReasonUI"
        val annotatedString = composition.recomposeReason.buildString(tag)
        ClickableText(
            text = annotatedString,
            style = TextStyle(color = colors.textColor, fontSize = 14.sp,),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag, offset, offset)
                    .firstOrNull()?.let { annotation ->
                        val index = annotation.item.toIntOrNull() ?: return@let
                        composition.recomposeReason.getOrNull(index)?.onClick?.invoke()
                    }
            }
        )
    }
}

@Composable
private fun NonSkipReasonUI(state: UiState) {
    val colors = state.colors
    val composition = state.selectedDevice?.currentRecomposition ?: return
    Column (
        Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondaryColor)
            .padding(10.dp)
    ) {
        Text(
            "Non Skip Reason",
            color = colors.textColor,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        val tag = "NonSkipReasonUI"
        val annotatedString = composition.nonSkipReason.buildString(tag)
        ClickableText(
            text = annotatedString,
            style = TextStyle(color = colors.textColor, fontSize = 14.sp,),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag, offset, offset)
                    .firstOrNull()?.let { annotation ->
                        val index = annotation.item.toIntOrNull() ?: return@let
                        composition.nonSkipReason.getOrNull(index)?.onClick?.invoke()
                    }
            }
        )
    }
}

private fun List<AnnotatedContent>.buildString(tag: String): AnnotatedString {
    // TODO 点击
    return buildAnnotatedString {
        forEachIndexed { index, it ->
            if (it.onClick == null) {
                append(it.content)
            } else {
                pushStringAnnotation(tag, "$index")
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                    append(it.content)
                }
                pop()
            }
        }
    }
}
