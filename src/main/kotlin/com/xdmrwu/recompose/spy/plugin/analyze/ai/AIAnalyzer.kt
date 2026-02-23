package com.xdmrwu.recompose.spy.plugin.analyze.ai

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.streaming.StreamFrame
import com.xdmrwu.recompose.spy.plugin.BuildConfig
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @Author: wulinpeng
 * @Date: 2026/2/23 22:28
 * @Description:
 */
fun RecomposeSpyTrackNode.analyzeWithAI(): Flow<String> {
    val input = Json { ignoreUnknownKeys = true }

    val llmClient = DeepSeekLLMClient(BuildConfig.DEEPSEEK_API_KEY)
    return flow {
        val hotFlow = llmClient.executeStreaming(
            prompt = Prompt.build("") {
                system(prompt)
                user(input.encodeToString(this@analyzeWithAI))
            },
            model = DeepSeekModels.DeepSeekChat,
            tools = emptyList()
        ).filterIsInstance<StreamFrame.Append>().map { it.text }
        emitAll(hotFlow)
    }.scan("") { acc, value ->
        acc + value
    }
}