# Jarvis 0.0.5：通用操作记忆

## 行为

所有通过 Jarvis 对话和设备桥接执行的应用任务共用一套操作记忆。首次成功后，智能体提取必要步骤、参数和核实条件保存；再次收到相同意图时，先查找路径，把当前任务的搜索词、目标和内容填入命名参数，在手机本地批量执行。

路径不是固定坐标宏。优先以资源 ID、文字、描述、控件类型选择唯一的可见控件，并在执行前核对应用。坐标点击、通用输入和滑动必须绑定最近读取的界面指纹。应用版本变化、控件歧义、前后置条件不匹配、工具失败或最终结果不符合时，立即停止剩余步骤，标记路径为 `stale`，返回失败步骤、已完成步骤、当前界面和原路径。智能体从当前状态重新探索，核实成功后用相同 id 更新完整路径。

已发生的外部动作不自动回滚。智能体收到失败后必须先核查已有发送/保存结果，避免重复执行。下载路径最终要验证文件存在、大小并且相对执行前新增或更新；单凭回复文字或已有文件不能算成功。

## 实现

- `runtime/OperationMemory.kt`：持久化、任务记录、检索、模板参数、前后置条件、批量重放、失败停用、更新、删除。
- `DeviceCapabilityBridge.kt`：所有重放操作仍走原设备能力及权限检查；root 在每次执行时检查完全模式；支持读取当前 Android 用户的应用版本和文件状态。
- `JarvisAccessibilityService.kt`：唯一选择器操作，完成回调确认的手势、滑动，以及界面指纹。
- `tools/jarvis-plugin/index.js` 及 APK overlay：注册 `jarvis_operation_memory`、`jarvis_accessibility_perform`、`jarvis_accessibility_swipe`，使 OpenClaw 实际能够调用。
- 每次 Jarvis 对话开始新的操作记录，并注入统一学习/复用/修复指令，适用于自定义人格提示词。
- 设置新增“操作记忆”卡片，可刷新、查看状态和删除路径。对话新消息自动滚动到末尾，便于看到执行结果。

数据位于应用私有目录 `files/jarvis/operation-memory/`，与 Android 用户空间隔离，位于可重装的 `runtime/` 目录之外。覆盖升级和修复内置运行时会保留；清除应用数据或卸载会删除。最多保存 500 条路径，每条 40 步；每条批量运行预算两分钟，每个条件等待最多八秒。长任务可拆为多段路径。

动态参数采用 JSON 字符串替换，不拼接 JSON。`sampleParameters` 只用于保存时核实，不写入长期路径；最近一次任务记录仍可能包含实际输入，存于应用私有目录，下次任务覆盖。固定 root 工具命令可以保存，禁止在 root shell 命令中直接展开动态参数；动态任务优先走结构化设备能力或固定脚本。

## 工具接口

`jarvis_operation_memory` 支持 `find/list/get/recording/save/run/invalidate/forget`。简例：

```json
{
  "action": "save",
  "recipe": {
    "id": "open-android-settings",
    "title": "打开系统设置",
    "intent": "仅打开 Android 系统设置，不修改设置",
    "package": "com.android.settings",
    "parameters": [],
    "steps": [
      {"capability": "app.launch", "arguments": {"package": "com.android.settings"}}
    ],
    "verify": {"kind": "ui", "package": "com.android.settings"}
  }
}
```

保存之前必须已完成首次操作；桥接会独立核实 `verify`。随后 `{"action":"run","id":"open-android-settings","parameters":{}}` 在本地执行并再次核实。

核实条件支持当前应用/唯一选择器/界面指纹、文件状态、只读能力字段匹配。界面指纹基于无障碍节点及布局，不能独立识别纯图片内容变化；图片、游戏画面或节点缺失的页面仍可能需要 OCR、视觉识别及重新探索。检索为本机关键词和文字匹配，未调用向量服务；最终意图和目标匹配由智能体判断。

## 验证

构建：`JAVA_HOME=D:\AndroidStadio\jbr`，`gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`。

14 项 JVM 测试通过：持久化、参数替换、错误参数无副作用、版本变化、界面失配停止和重学、坐标保护、拒绝未核实结果、已有文件不能冒充下载、权限复检、禁止 shell 参数注入、坐标记录转唯一选择器、无效选择器、失败返回原路径、损坏存储保留并报错。

真机：Redmi M2007J3SC，Android 第二空间 User 11，root 完全模式、Jarvis 无障碍；0.0.5/code 5 覆盖安装，原配置保留。首个测试包暴露 Android ICU 正则与 JVM 的差异，已将模板结束花括号显式转义，重新构建后启动及调用正常。

桥接实测：`tools/test-operation-memory-device.py smoke` 执行首次打开系统设置、保存并核实、查找、第二次批量执行、故意设置不存在的控件、在第一次点击前停止并停用、重新探索更新、修复后执行成功、返回 Jarvis。第二次单步路径约 559 ms；这不含模型时间，也不能外推复杂应用耗时。证据：`artwork/operation-memory-device-test.json`。

实际对话实测：从 Jarvis 输入任务，OpenClaw 查询并保存 `read-device-model`，模型值经工具核实为 `M2007J3SC`。再次只说“再查询一次本机手机型号”，智能体查找并复用路径，重新读取实际设备值，单步路径 9 ms，本次模型执行约三秒。证据：`artwork/operation-memory-chat-learn-reuse.log`、对应 prompt 和 routes 文件。

实际对话故障恢复：测试人员主动把 `read-device-model` 加入不存在包名的错误前置条件，模拟已失效路径。随后只发送“再查询一次本机手机型号”，没有提示故障位置或修复方法。智能体自行查找、尝试运行，被原生执行器在第一步前阻止，重新调用 `device.info`，以同一 id 保存 revision 3，并再次运行核实通过。最终落盘为 `ready`、`failures=1`，错误前置条件已移除。模型回复中的“外部改坏”是此次故障注入，不是发现了未知存储损坏。证据：`artwork/operation-memory-chat-recovery.log`、`artwork/operation-memory-recovered-procedure.json`。

最终 APK SHA256：`d061b8c678585cc991fda67112d2d598c7b98b118d64b564a2eaad892d8277e9`。构建、覆盖安装和对话恢复测试使用该 APK；恢复后当前前台仍为 Jarvis，截图为 `artwork/operation-memory-chat-final.png`。

设置界面也已真机读取确认显示 `Jarvis 0.0.5`、“操作记忆”和“已保存 2 条操作路径，2 条可复用”。证据：`artwork/operation-memory-settings-visible.json`、同名 PNG。测试结束停留在该设置卡片，便于用户查看。

不对所有第三方应用的稳定性作保证。当前实测覆盖设备只读查询和系统设置导航，先前 Pixiv 下载流程尚未在本轮重新测速；应用变化时仍需智能体探索，模型响应速度也会影响总耗时。路径的提取、泛化和失败后的探索由模型完成，路径执行及结果检查由手机桥接完成。
