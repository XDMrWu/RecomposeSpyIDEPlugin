@file:OptIn(ExperimentalMaterialApi::class)

package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xdmrwu.recompose.spy.plugin.generated.Res
import com.xdmrwu.recompose.spy.plugin.generated.icon
import com.xdmrwu.recompose.spy.plugin.generated.icon_dark
import com.xdmrwu.recompose.spy.plugin.generated.phone_dark
import com.xdmrwu.recompose.spy.plugin.generated.phone_light
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState
import org.jetbrains.compose.resources.painterResource
import kotlin.math.exp

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 16:16
 * @Description:
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TitleBar(
    state: UiState,
    onSelectDevice: (String) -> Unit = {},
    onClickRecord: () -> Unit = {}
) {
    val colors = state.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(colors.backgroundSecondaryColor).padding(10.dp)
    ) {
        val icon = if (state.darkMode) Res.drawable.icon_dark else Res.drawable.icon
        Image(
            painter = painterResource(icon),
            contentDescription = "",
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(22.dp)
        )
        Text(
            "RecomposeSPy",
            color = colors.textColor,
            modifier = Modifier.padding(horizontal = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Divider(Modifier.width(5.dp))
        DeviceUI(state, onSelectDevice)
        Divider(Modifier.width(10.dp))
        RecordButton(state, onClickRecord)
    }
}

@Composable
private fun DeviceUI(state: UiState, onSelectDevice: (String) -> Unit) {
    val colors = state.colors
    SelectedDeviceUI(state, onSelectDevice)
    Divider(Modifier.width(5.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colors.buttonBackgroundColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Canvas(Modifier.size(8.dp)) {
            drawCircle(Color.Green)
        }
        Text("${state.deviceList.size}", color = colors.textColor, modifier = Modifier.padding(start = 2.dp))
    }
}

@Composable
private fun SelectedDeviceUI(state: UiState, onSelectDevice: (String) -> Unit) {
    val colors = state.colors
    val selectedDevice = state.selectedDevice
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.background(colors.buttonBackgroundColor).width(250.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // TODO show device list menu
                }
                .background(colors.buttonBackgroundColor, RoundedCornerShape(4.dp))
                .padding(5.dp)
        ) {
            val drawable = if (state.darkMode) Res.drawable.phone_dark else Res.drawable.phone_light
            Image(
                painter = painterResource(drawable),
                contentDescription = "",
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(18.dp)
            )
            Text(
                selectedDevice?.name ?: "No Device",
                color = colors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Canvas(
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(12.dp)
                    .height(6.dp)
            ) {
                // 下箭头
                val width = size.width
                val height = size.height
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width / 2, height * 0.7f)
                    lineTo(width, 0f)
                }
                drawPath(path = path, color = colors.textColor, style = Stroke(width = 2f))
            }
        }
        if (state.deviceList.size > 1) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.buttonBackgroundColor)
            ) {
                state.deviceList.forEach { device ->
                    DropdownMenuItem(
                        onClick = {
                            onSelectDevice(device.id)
                            expanded = false
                        },
                        modifier = Modifier.background(colors.buttonBackgroundColor)
                    ) {
                        Text(
                            device.name,
                            color = colors.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordButton(state: UiState, onClickRecord: () -> Unit) {
    val colors = state.colors
    val selectedDevice = state.selectedDevice
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = selectedDevice != null) {
                onClickRecord()
            }
            .background(colors.buttonBackgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        var text = "Start Record"
        if (selectedDevice?.recording == true) {
            text = "Stop Record"
            Canvas(Modifier.size(18.dp)) {
                drawRect(Color(181, 71, 71))
            }
        } else {
            Canvas(Modifier.size(18.dp)) {
                val width = size.width
                val height = size.height

                // 创建一个三角形 Path
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width * 0.8f, height / 2)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = path,
                    color = Color.Green
                )
            }
        }
        Divider(Modifier.width(5.dp))
        Text(text,
            color = colors.textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}