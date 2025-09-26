package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.xdmrwu.recompose.spy.plugin.ui.ext.ComposePanel
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState
import org.jetbrains.compose.resources.painterResource
import com.xdmrwu.recompose.spy.plugin.generated.Res
import com.xdmrwu.recompose.spy.plugin.generated.phone_dark
import com.xdmrwu.recompose.spy.plugin.generated.phone_light
import com.xdmrwu.recompose.spy.plugin.ui.state.Recomposition

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 13:22
 * @Description:
 */
class WindowToolPanel {

    val state = UiState()
    var onSelectDevice: (String) -> Unit = {}
    var onClickRecord: () -> Unit = {}
    var openFile: (Recomposition) -> Unit = {}

    init {
        // fix compose cannot get resource with wrong classloader
        Thread.currentThread().contextClassLoader = javaClass.classLoader
    }

    fun getPanel() = ComposePanel {
        WindowToolPanelUI(state, onSelectDevice, onClickRecord, openFile)
    }
}

@Composable
internal fun WindowToolPanelUI(
    state: UiState,
    onSelectDevice: (String) -> Unit = {},
    onClickRecord: () -> Unit = {},
    openFile: (Recomposition) -> Unit = {}
) {
    BoxWithConstraints {
        val colors = state.colors
        Column(
            Modifier
                .width(max(maxWidth, 10000.dp))
                .height(max(maxHeight, 600.dp))
                .background(colors.backgroundColor)
        ) {
            TitleBar(state, onSelectDevice, onClickRecord)
            Divider(color = colors.dividerColor, modifier = Modifier.height(2.dp).fillMaxWidth())
            Row(
                Modifier.weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    RecomposeStackUI(state, openFile)
                }

                Divider(color = colors.dividerColor, modifier = Modifier.fillMaxHeight().width(2.dp))

                Box(
                    Modifier
                        .weight(3f)
                        .fillMaxHeight()
                ) {
                    AnalyzeUI(state)
                }
            }
        }
    }
}