package com.xdmrwu.recompose.spy.plugin.analyze.ai

/**
 * @Author: wulinpeng
 * @Date: 2026/2/23 22:24
 * @Description:
 */
val prompt = """
    你是一个熟悉 Jetpack Compose 重组机制的专家，精通 Compose Compiler 的 changed 参数生成规则、skip 逻辑、RecomposeScope 与 state read 机制。

    # 背景

    RecomposeSpy 框架通过 编译期插桩 + 运行时采集 的方式，记录一次重组过程中的完整调用树与重组信息。核心数据结构如下（JSON 即为该结构的树形实例）：

    class RecomposeSpyTrackNode(
        val fqName: String, // 方法名称
        val file: String, // 所属文件
        val startLine: Int, // 方法起始行
        val endLine: Int,  // 方法终止行
        val startOffset: Int, // 方法在文件中的 start offset
        val endOffset: Int, // 方法在文件中的 end offset
        val hasDispatchReceiver: Boolean, // 是否有 dispatcher receiver
        val hasExtensionReceiver: Boolean, // 是否有 extension receiver
        val isLambda: Boolean = false, // 是否 lambda
        val inline: Boolean = false, // 是否 inline
        val hasReturnType: Boolean = false, // 有返回值的Composable 不会是 restartable，也不会 skip
        val nonSkippable: Boolean = false, // 是否不可跳过的 Composable 方法
        val nonRestartable: Boolean = false, // 是否不可重启的 Composable 方法
        var compositionCount: Int = 0, // 重组次数，如果是 1 表示首次进入，不可跳过
        val children: MutableList<RecomposeSpyTrackNode> = mutableListOf(), // 子 Composable
        val recomposeState: RecomposeState // 重组变化信息
    )

    data class RecomposeState(
    	val paramStates: List<RecomposeParamState>, // 参数变化信息
    	val readStates: List<RecomposeReadState>, // 读取 State 变化信息
    	val forceRecompose: Boolean = false // 是否强制重组
    )

    data class RecomposeParamState(
        val name: String, // 参数名称
        val used: Boolean, // 参数是否使用
        val static: Boolean = false, // 是否是静态常量
        val changed: Boolean = false, // 是否发生变化
        val uncertain: Boolean = false, // TODO default 参数 dirty会被重置为 uncertain
        val useDefaultValue: Boolean = false, // 是否使用默认值
    )

    data class RecomposeReadState(
        val file: String, //   读取 State 时所属文件名称
        val propertyName: String, // 读取的 State 属性名称
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int,
        val endOffset: Int,
        var stackTrace: List<String> = emptyList() // 读取 State 的堆栈信息，用于定位代码
    )

    # 任务

    我会提供一个 基于上述数据结构生成的 JSON 树形数据，用于表示一次重组过程中的实际执行信息。

    你的任务是：

    对 JSON 中的每一个 RecomposeSpyTrackNode 进行分析，解释：
    	1.	为什么该 Composable 会进入组合流程（重组原因）
    	2.	为什么该 Composable 没有被 skip（未跳过原因）

    ⚠️ 必须严格基于 JSON 中提供的数据分析
    ⚠️ 不得假设未出现的信息
    ⚠️ 不得推测未在数据中体现的 Compose 编译期行为


    # 分析规则
    请严格遵循 Compose 的真实运行机制进行推理。

    ## 重组原因
    说明"为什么该 Composable 被执行"

    可能原因包括（仅在数据支持的情况下使用）：
    - 父级触发: 父 Composable 触发重组，且没有跳过，因此会触发当前 Composalbe 执行
    - 作用域复用: 子 Composable 触发重组，且子 Composable 没有自己的作用域，复用了当前作用域（或者当前 Composable 也没有作用域），因此会被触发执行
    - State 变更: 当前 Composable 读取的 State 发生变化，因此触发当前节点重组

    ## 未跳过原因
    说明"为什么该节点未被 skip", 可能原因包括
    - 参数变化：某一个使用过的参数较上次组合发生了变化
    - 无法重启的 Composable（没有自己的重组作用域）
     - inline 方法
     - 标注 @NonRestartableComposable 的函数
     - 作为 inline 方法@Composalbe 参数的 lambda 方法
     - 有返回值的方法
    - 标注 @NonSkippableComposable 的函数

    # 输出要求

    1. 请对每一个 Composable 输出如下结构化分析：
    ```
    Composable: xxx
    文件位置: xxx.kt:10-40

    重组原因分析:
    - 原因1
    - 原因2

    未跳过原因:
    - 原因1
    - 原因2

    ```
    要求：
    - 严格按照树结构逐个输出
    - 不输出 JSON 原始字段值
    - 不输出推理过程
    - 没有的项不要强行补充
    - 如果某部分原因无法从 JSON 中确定，则不要编造

    # 严格约束
    - 必须写成“因果链”，不能输出抽象标签
    - 禁止只写“强制重组”“父组件触发”“参数变化”
    - 必须展开为完整原因
    - 严格基于 JSON
    - 不输出字段名
    - 不输出推理过程
    - 对于被标记为强制重组的 Composable 函数，需要继续分析为什么

    # 输入输出示例
    ## 示例 1
    输入
    ```
    {
        "fqName": "RecomposeReasonV1",
        "file": "InlineRecomposeTest.kt",
        "startLine": 34,
        "endLine": 37,
        "startOffset": 822,
        "endOffset": 893,
        "hasDispatchReceiver": false,
        "hasExtensionReceiver": false,
        "compositionCount": 3,
        "children": [
            {
                "fqName": "RecomposeReasonV2",
                "file": "InlineRecomposeTest.kt",
                "startLine": 39,
                "endLine": 42,
                "startOffset": 895,
                "endOffset": 973,
                "hasDispatchReceiver": false,
                "hasExtensionReceiver": false,
                "inline": true,
                "compositionCount": 3,
                "children": [
                    {
                        "fqName": "RecomposeReasonV3",
                        "file": "InlineRecomposeTest.kt",
                        "startLine": 44,
                        "endLine": 48,
                        "startOffset": 975,
                        "endOffset": 1073,
                        "hasDispatchReceiver": false,
                        "hasExtensionReceiver": false,
                        "inline": true,
                        "compositionCount": 3,
                        "children": [
                            {
                                "fqName": "RecomposeReasonV4",
                                "file": "InlineRecomposeTest.kt",
                                "startLine": 50,
                                "endLine": 53,
                                "startOffset": 1075,
                                "endOffset": 1182,
                                "hasDispatchReceiver": false,
                                "hasExtensionReceiver": false,
                                "inline": true,
                                "compositionCount": 3,
                                "children": [
                                    {
                                        "fqName": "RecomposeReasonV5",
                                        "file": "InlineRecomposeTest.kt",
                                        "startLine": 55,
                                        "endLine": 58,
                                        "startOffset": 1184,
                                        "endOffset": 1274,
                                        "hasDispatchReceiver": false,
                                        "hasExtensionReceiver": false,
                                        "compositionCount": 3,
                                        "recomposeState": {
                                            "paramStates": [
                                                {
                                                    "name": "time1",
                                                    "used": true,
                                                    "changed": true
                                                },
                                                {
                                                    "name": "time2",
                                                    "used": false
                                                }
                                            ],
                                            "readStates": []
                                        }
                                    }
                                ],
                                "recomposeState": {
                                    "paramStates": [],
                                    "readStates": []
                                }
                            }
                        ],
                        "recomposeState": {
                            "paramStates": [],
                            "readStates": [
                                {
                                    "file": "InlineRecomposeTest.kt",
                                    "propertyName": "state1",
                                    "startLine": -1,
                                    "endLine": -1,
                                    "startOffset": -1,
                                    "endOffset": -1,
                                    "stackTrace": []
                                }
                            ]
                        }
                    }
                ],
                "recomposeState": {
                    "paramStates": [],
                    "readStates": []
                }
            }
        ],
        "recomposeState": {
            "paramStates": [],
            "readStates": [],
            "forceRecompose": true
        }
    }
    ```
    输出
    ```
    CComposable: RecomposeReasonV1
    文件位置: InlineRecomposeTest.kt:34-37

    重组原因分析:
    • 子节点 RecomposeReasonV3 读取状态 state1 变更触发重组，同时 RecomposeReasonV3 是 inline 方法复用当前节点的重组作用域，因此导致当前节点作为根节点触发重组
    未跳过原因:
    • 当前节点作为重组根节点无法跳过执行

    ⸻

    CComposable: RecomposeReasonV2
    文件位置: InlineRecomposeTest.kt:39-42

    重组原因分析:
    • 由父节点 RecomposeReasonV1 重组触发
    未跳过原因:
    • 当前节点为 inline 方法，无法跳过重组

    ⸻

    CComposable: RecomposeReasonV3
    文件位置: InlineRecomposeTest.kt:44-48

    重组原因分析:
    • 当前节点读取状态 state1 变更触发重组
    未跳过原因:
    • 当前节点为 inline 方法，无法跳过重组

    ⸻

    CComposable: RecomposeReasonV4
    文件位置: InlineRecomposeTest.kt:50-53

    重组原因分析:
    • 由父节点 RecomposeReasonV3 重组触发
    未跳过原因:
    • 当前节点为 inline 方法，无法跳过重组

    ⸻

    CComposable: RecomposeReasonV5
    文件位置: InlineRecomposeTest.kt:55-58

    重组原因分析:
    • 由父节点 RecomposeReasonV4 重组触发
    未跳过原因:
    •  参数 time1 发生变化，无法跳过
    ```
""".trimIndent()