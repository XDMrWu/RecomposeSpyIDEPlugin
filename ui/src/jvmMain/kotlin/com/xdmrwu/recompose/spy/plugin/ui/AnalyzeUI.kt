package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 22:18
 * @Description:
 */

@Composable
fun AnalyzeUI(state: UiState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        if (state.selectedDevice?.currentRecomposition != null) {
            SummaryUI(state)
            Divider(Modifier.padding(vertical = 15.dp), Color.Transparent)
            ChangedParamsUI(state)
            Divider(Modifier.padding(vertical = 15.dp), Color.Transparent)
            ChangedStatesUI(state)
        }
    }
}

@Composable
private fun SummaryUI(state: UiState) {
    val colors = state.colors
    val rootComposition = state.selectedDevice?.currentRecomposition?.first ?: return
    val composition = state.selectedDevice?.currentRecomposition?.second ?: return
    Column (
        Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondaryColor)
            .padding(10.dp)
    ) {
        Text(
            "Analysis Summary",
            color = colors.textColor,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(composition.reason, color = colors.textColor, fontSize = 14.sp)
    }
}

@Composable
private fun ChangedParamsUI(state: UiState) {
    val colors = state.colors
    val rootComposition = state.selectedDevice?.currentRecomposition?.first ?: return
    val composition = state.selectedDevice?.currentRecomposition?.second ?: return
    if (composition.changedParams.isEmpty()) {
        return
    }
    Column (
        Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondaryColor)
            .padding(10.dp)
    ) {
        Text(
            "Parameter Changes",
            color = colors.textColor,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        val content = composition.changedParams.joinToString("\n")
        Text(content, color = colors.textColor, fontSize = 14.sp)
    }
}

@Composable
private fun ChangedStatesUI(state: UiState) {
    val colors = state.colors
    val rootComposition = state.selectedDevice?.currentRecomposition?.first ?: return
    val composition = state.selectedDevice?.currentRecomposition?.second ?: return
    if (composition.changedStates.isEmpty()) {
        return
    }
    Column (
        Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondaryColor)
            .padding(10.dp)
    ) {
        Text(
            "State Changes",
            color = colors.textColor,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        val content = composition.changedStates.joinToString("\n")
        Text(content, color = colors.textColor, fontSize = 14.sp)
    }
}