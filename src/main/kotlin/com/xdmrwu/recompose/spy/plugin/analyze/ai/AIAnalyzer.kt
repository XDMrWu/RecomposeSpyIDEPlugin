package com.xdmrwu.recompose.spy.plugin.analyze.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTool
import ai.koog.agents.core.tools.reflect.asToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.streaming.StreamFrame
import com.xdmrwu.recompose.spy.plugin.BuildConfig
import com.xdmrwu.recompose.spy.plugin.model.RecomposeSpyTrackNode
import com.xdmrwu.recompose.spy.plugin.utils.getFileContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @Author: wulinpeng
 * @Date: 2026/2/23 22:28
 * @Description:
 */
fun RecomposeSpyTrackNode.analyzeWithAIStreaming(): Flow<String> {
    val json = Json { ignoreUnknownKeys = true }

    val llmClient = DeepSeekLLMClient(BuildConfig.DEEPSEEK_API_KEY)
    return flow {
        val hotFlow = llmClient.executeStreaming(
            prompt = Prompt.build("") {
                system(prompt)
                user(json.encodeToString(this@analyzeWithAIStreaming))
            },
            model = DeepSeekModels.DeepSeekChat,
            tools = listOf(::readComposeCode.asToolDescriptor("readComposeCode", "read compose source code, for recompose reason analyze"))
        ).map {
            if (it is StreamFrame.ToolCall) {
                println(it.content)
            } else if (it is StreamFrame.End) {
                println(it.finishReason)
            }
            it
        }.filterIsInstance<StreamFrame.Append>().map { it.text }
        emitAll(hotFlow)
    }.scan("") { acc, value ->
        acc + value
    }
}

suspend fun List<RecomposeSpyTrackNode>.analyzeWithAI(): String {
    val json = Json { ignoreUnknownKeys = true }

    val llmClient = DeepSeekLLMClient(BuildConfig.DEEPSEEK_API_KEY)
    val agent = AIAgent(
        promptExecutor = SingleLLMPromptExecutor(llmClient),
        llmModel = DeepSeekModels.DeepSeekChat,
        strategy = singleRunStrategy(),
        systemPrompt = prompt,
        toolRegistry = ToolRegistry {
            tool(::readComposeCode.asTool())
        },
    )
    return agent.run(json.encodeToString(this))
}

fun readComposeCode(file: String, startLine: Int, endLine: Int): List<String> {
    println("readComposeCode: $file, $startLine, $endLine")
    return getFileContent(file)?.lines()?.subList(startLine - 1, endLine) ?: emptyList()
}