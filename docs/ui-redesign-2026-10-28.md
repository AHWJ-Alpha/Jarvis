# Jarvis UI 整体重做 · 交付记录（2026-10-28）

## 1. 起因与目标

原界面全部挤在 `MainActivity.kt` 单个文件（2833 行），MaterialTheme 只注入 colorScheme 与 3 档 Shapes
（`MainActivity.kt:742-746`），全仓 `Typography(` 出现 0 次；页面里散落 81 处 `fontSize` 字面量、
134 处 `.dp` 字面量、**0 处阴影**。卡片唯一的分层信号是 1dp 描边，而该描边的实测对比度只有
1.27~1.50:1（`JarvisAppearance.kt:85` 把 muted 映射成 outline、把 outline 映射成 outlineVariant），
等于"卡片看不见"。深色主题 Ink 的 surface 与 background 只差约 2%。浮窗是第二套写死颜色的视觉
（`FloatingAssistantService.kt` 纯 View，面板固定 `0xff172438`）。

目标：六套主题各自独立排版、主题可插拔、风格强化二次元、补齐深色缺陷、主界面与浮窗一起改。

## 2. 决策记录（四轮访谈结论）

| # | 决策 | 理由 |
|---|---|---|
| D1 | 整体重做视觉体系，而非只做令牌或只做微调 | 用户选择；单点微调无法解决"结构上样式散落"的根因 |
| D2 | 六套主题，各自独立布局 | 深海夜航(暗·DRAWER)、樱夜校园(暗·底栏)、霓虹赛博(暗·底栏)、晨光便当(亮·底栏)、水墨山月(亮·DRAWER)、纸感极简(亮·DRAWER) |
| D3 | 主题做成可插拔模块，代码按主题/组件拆分 | 单文件无法并行、无法界定审查边界 |
| D4 | 插画由本地 A1111 SDXL 生成 | 可复现、无外部凭据；模型 `kohaku-xl-epsilon-rev1` |
| D5 | 不设样板主题，全量开工 | 用户选择 |
| D6 | 验收门槛：`assembleDebug` + user 11 真机跑通 | 用户明确要求"不只要构建通过" |
| D7 | 持久化改为字符串 `theme_id`，旧 ordinal 迁移 | 旧枚举 ordinal 已写入 SharedPreferences，不能直接改序 |

## 3. 架构

```
com.jarvis.ui.tokens/     JarvisSpacing(4dp 网格) JarvisShapes JarvisMotion JarvisElevation
                          JarvisType(8 级排版) JarvisTypography
com.jarvis.ui.theme/      ThemeMode ThemePalette(22 必填 + 36 键零回落 colorScheme())
                          ThemeArtwork ChromeStyle FloatingPaletteSpec JarvisThemeModule
                          JarvisScreenApi(623 行，四套 UiContext) JarvisThemeRegistry
                          LegacyThemeMigration(ordinal → theme_id)
com.jarvis.ui.components/ JarvisCard / JarvisInsetCard / ScreenHeader / SectionHeader / ListRow
                          SettingSwitchRow / NoticeLine / StatusBadge / DangerActionLabel
                          EmptyStatePanel / JarvisChipRow / JarvisDivider / JarvisLoading
com.jarvis.ui.themes/     六套主题 + JarvisThemes 聚合器
com.jarvis.ui.shell/      JarvisShell(296 行)：MaterialTheme 注入 + chrome + 屏分发 + 对话框槽
```

关键契约：主题只接收 `UiContext`（state + callbacks），不持有 Context、不读 SharedPreferences；
系统动作（startActivity / 权限申请 / 剪贴板）一律收成无参回调由外壳实现。

## 4. 六个主题

| 主题 | 明暗 | chrome | 排版骨架 |
|---|---|---|---|
| 深海夜航 | 暗 | DRAWER | 38% 屏高 hero + surface 渐隐蒙版，内容自 hero 下沿上浮大卡，pill 胶囊输入 |
| 樱夜校园 | 暗 | 底栏 | 极简顶栏无 hero，卡片堆叠 + 花瓣分隔点，64dp 四图标底栏；空态为 180dp 圆角插画卡 |
| 霓虹赛博 | 暗 | 底栏 | 程序化网格底纹 + 发光描边卡，等宽数字，5 段文本底栏 + glow 下划线 |
| 晨光便当 | 亮 | 底栏 | 0.62/0.38 交替等高格栅（IntrinsicSize），不对称圆角气泡，便当胶囊底栏 |
| 水墨山月 | 亮 | DRAWER | 满幅平铺宣纸墨纹，零卡片，标题居中，分割线 + 字号分层 |
| 纸感极简 | 亮 | DRAWER | 满幅点纹，64dp 行高清单 + 3dp 选中竖条，无卡片无插画 |

## 5. 美术

- 18 张资产：每套 banner(1536×640) / avatar(512×512) / texture(512×512)。
- 生成脚本 `tools/generate-theme-v2.py`（幂等，`--theme/--only/--force/--offline`），清单
  `artwork/themes-v2/manifest.json` 记录 prompt/seed/尺寸/体积/`style_generation`/`grain`/QA 数值，
  复现说明见 `artwork/themes-v2/README.md`。
- 底纹（texture）全部量化验收：亮色对比带 3.5~4.4%、黑像素 0%、最暗单像素 WCAG ≥5.2:1；
  暗色霓虹网格 stddev 2.83、饱和像素 0%。
- 两处**由模型改为程序化生成**（SDXL 稳定产出花纹/故障块）：`paper_minimal` 噪点、`neon_cyber` 网格。
  manifest 以 `grain: procedural*` 如实标注。

## 6. 设备验收

### 已验证
| 项 | 证据 |
|---|---|
| 六套主题冷启动无崩溃 | 逐套 `final-<id>.png`，`logcat -b crash` FATAL = 0 |
| 便当主题不再崩溃 | 曾因 `LazyRow(SubcomposeLayout)` × `IntrinsicSize` 必崩，改为 `Row+horizontalScroll` 后正常 |
| 底栏导航与高亮一致 | 曾整体偏移一格（`var page = -1`），修正后逐个点击核对 |
| 顶栏头像与抽屉 story 随主题 | 三种 DRAWER 主题像素均值互不相同 |
| 横屏布局 | 左 280dp 侧栏常驻 + 右侧内容 + 底栏 |
| 满幅底纹可读性 | 水墨/纸感设置页（最密文字屏）截图 |
| 深海 hero 新插画 + 标题不被裁剪 | `14-chat-deep-real-art.png` |
| 浮窗服务可用（曾 100% 崩溃） | `float-fixed.png`：气泡可见、overlay 窗口 147x147、PID 不变、ClassCastException = 0；`float-expanded.png`：面板展开 |
| 浮窗配色随主题 | 气泡与面板取 `FloatingPaletteSpec`，四个旧硬编码色 grep = 0 |
| 深色/浅色主题状态栏图标 | 顶栏像素统计：neon 竖/横屏 mean_lum 13.3/16.7（白图标）、bento 229.7/233.4（深图标）；deep(DARK)/paper(LIGHT) 回归通过 |
| 霓虹底纹改程序化网格后观感 | `zz-final-neon.png`：近黑底 + 1px 网格，标题与正文可读 |
| 樱夜空态插画卡 | `float-expanded.png` 内可见 180dp 圆角樱花街道插画 |
| 底纹真平铺（曾把 tile 拉伸铺满） | 便当/霓虹原用 `paint(...ContentScale.Crop)` 拉伸，现改 `ImageShader(TileMode.Repeated)`；`h-bento-3.png`（细密织纹）、`h-neon-3.png`（细密网格 + glow 卡） |
| 对话发送链路（降级路径） | `zz-chat-send2.png`：无 Gateway 时返回可读错误「OpenClaw 未就绪：Gateway 已退出，请查看 …log」，无崩溃 |

### 未验证 / 限制
- **真机 user 11 未验收**：测试途中原设备 `1f5244c1`（Redmi M2007J3SC）掉线，改用本机 x86_64 模拟器
  （android-35 google_apis，手写 AVD，已 `adb root`）。模拟器只有 user 0，无第二空间、无 root 工具链。
- **Gateway 无法在模拟器验证**：内置运行时是 arm64，x86_64 上 `linker` 报 `EM_AARCH64 vs EM_X86_64`，
  OpenClaw 相关功能只能验到"兼容模式"降级提示。
- **16KB page size 未覆盖**：模拟器为 4KB 页，与项目在 `build.gradle.kts` 里的 16KB 对齐诉求无关。
- 对话发送在真机已打通（模型返回真实设备信息）；模拟器上只验到降级提示。

## 7. 交接说明（会话切换）

本会话在收尾阶段检测到工具面切换为 Agent Teams，与 Dispatch 编排方式不兼容（`send_message` 参数由 `agent_id` 变为 `target`，子代理协调工具作用域被遮蔽），
按规则停止编排并交接。**用户选择：关闭 Agent Teams 插件 → 重启 Host → 新开会话继续。**

### 已完成且已验证
1. 五套主题（深海夜航/樱夜校园/晨光便当/水墨山月/纸感极简）；赛博主题已删除，残留 `theme_id=neon_cyber` 自动回退 `deep_night`（实测）。
2. 令牌与组件：`JarvisScrim`（barAlpha 0.92 / fadeLength 24dp / top·bottomBarBrush）、`JarvisStroke.hairline`、`JarvisCard.border=null` 默认无描边、行高 60dp、`sectionGap=28 / cardGap=16 / rowGap=12`。
3. 首尾栏半透明过渡：`ShellBars.kt` 已改用 `JarvisScrim`，不再出现 `Color.Transparent`。
4. 浮窗：气泡默认位 0.62H、区间 [0.45H,0.72H]、拖动记忆存 `jarvis_floating_position`；主题切换经 prefs 监听即时跟随头像与配色（实测 c1/c3 截图）。
5. 便当主题 `BentoPair` 窄屏纵向堆叠、宽屏等高；`FlowRow` 经 sources jar 确认非 SubcomposeLayout，宽屏分支实测不崩。
6. 水墨/纸感 palette 与密度已按量化口径回填（borderSubtle 1.71~1.78:1、ΔL* 4.88~4.92）。

### 在途未验收
- `592404f2` 深海/樱夜：密度与描边回填（文件已多次改动，最后一次自报双绿，未做我的独立验收）。
- `dbc8240f` 首尾栏渐变：代码已落盘并通过编译，但三张装机截图（深海顶栏淡出 / 樱夜底部无硬线 / 樱夜横屏无双导航）未回收。

### 新会话第一件事
1. `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon` 确认当前树绿（注意：并发多人构建会产出半成品 dex，必要时删 `app/build/intermediates/dex/debug/mergeProjectDexDebug` 重建）。
2. `grep -rn 'neon_cyber\|NeonCyber' app/src/main/java/` 必须 0 命中。
3. 真机（`192.168.137.151:5555`，user 11）装一次并逐套截图：重点看「不挤 / 无扎眼边框 / 首尾过渡自然 / 浮窗跟随主题」。
4. 未处理的技术债见第 7 节（主线程 socket bind 的冷启动 ANR 风险、浮窗错误靠文案关键字、便当主题 918 行需拆文件、`FilterChip` 是否已彻底无描边待复核）。
- 横屏是用 `wm size 2280x1080` 模拟的（该模拟器 display rotation 被锁死，`user_rotation` 无效），
  布局与状态栏均正确，但**不是真实旋转**，真机仍建议复核。

### 真机补充验收（Redmi M2007J3SC / user 11）
| 项 | 结果 |
|---|---|
| 装机 user 11 + 冷启动 | Success；2.7s；`topResumedActivity=u11 com.jarvis.app/.MainActivity`；FATAL = 0 |
| 旧主题迁移 | 旧 ordinal 4(樱影) → 自动迁移为 `sakura_night`，未串档 |
| 底栏导航 | 点「设置」正确高亮切页 |
| 主题切换（UI） | 点「霓虹赛博」后整屏切为 neon_cyber，徽标/A11y 描述同步 |
| 浮窗 | 气泡存活、面板可展开、头像与配色随主题（截图 r13/r14） |
| **端到端对话** | 发送 → 「正在请求模型…」spinner → 模型返回真实设备信息（Redmi K30S Ultra / Android 17 SDK37 / user 11） |

真机暴露的新缺陷：**浮窗气泡默认位置（y=240dp）会压住软键盘弹出后的发送键**，点击发送实际点到气泡。已在修（默认位改到屏幕 60~70% 并加键盘期避让）。

### 用户真机反馈后的第二轮打磨（同日）
| 反馈 | 处理 |
|---|---|
| 项目挤在一起 | 间距令牌放宽（sectionGap 24→28、新增 cardGap 16/rowGap 12）、行高 56→60dp、卡片内边距 ≥20dp、页头副标题独立成行；便当主题窄格（0.38）改纵向堆叠 |
| 首尾栏过渡生硬 | 新增 `JarvisScrim`（barAlpha 0.92、fadeLength 24dp、top/bottomBarBrush），栏面半透明 + 24dp 渐变；渐变末端用 `background.copy(alpha=0f)` 而非 `Color.Transparent`（后者是透明黑，浅色主题会淡出成灰） |
| 图标边框太显眼 | 诊断纠正：并非卡片外框（实测仅 1.70~1.78:1），而是 `FilterChip`/`OutlinedButton`/`OutlinedTextField` 走 `colorScheme.outline`；改为无描边填充样式，`JarvisCard` 默认不画描边 |
| 悬浮窗图片不随主题 | 服务注册 `jarvis_config/theme_id` 的 OnSharedPreferenceChangeListener（applicationContext、onDestroy 反注册），变化即 `syncTheme()+ensureBubbleInBand(force)`；主界面切换后兜底 `FloatingAssistantService.refresh()` |
| 删除赛博主题 | `NeonCyberTheme.kt` 删除，`app/` 下 0 命中；残留 `theme_id=neon_cyber` 自动回退 `deep_night`（已实测）；美术资产留档并标注移除 |

口径修订：原「禁止 alpha 叠层」只适用于卡片/容器底色；遮罩、栏面、渐变过渡允许半透明。

### 第二轮打磨 · 真机验收结果（Redmi M2007J3SC / user 11）
| 用户反馈 | 真机实测证据 |
|---|---|
| 挤在一起 | 卡片内边距 20dp、卡间距 16~28dp、页头三行化；便当窄格窄屏改纵向堆叠（`artwork/real2/morning_bento_settings.png`） |
| 边框太显眼 | 主题 chip / 底栏胶囊 / 开关外圈全部无描边（`FilterChip` 传 `BorderStroke(0.dp)`、`Switch` 三处 `BorderColor=Transparent`）；卡片靠 surface↔background 明度差分（深色 ΔL* 10.50、浅色 4.88~4.92） |
| 首尾栏过渡硬 | 顶栏 92% 半透明 + 24dp 渐变，末端 `background.copy(alpha=0f)`（`artwork/real2/deep_settings.png`） |
| 浮窗不随主题 | prefs 监听 + refresh 兜底；深海气泡为深蓝头像、便当为粉发头像 |
| 删赛博 | `theme_id=neon_cyber` 写入后启动 → 自动回退为有效主题，`FATAL=0`，prefs 被改写 |

真机同时确认：卡头图标已按分组语义区分（不再同一张头像）、五套主题逐一切换均 `FATAL=0`。

### 第二轮遗留 · 最终状态
| 项 | 结果 |
|---|---|
| 便当「限制模式 / root 模式 / 命令通道 / 悬浮球」标题重复 | **已修**，新增无标题开关行 `BentoSwitchLine`，真机截图 `artwork/real2/bento_settings_final.png` 确认每张卡只剩一处标题 |
| 顶栏长标题硬裁 | **已修**，`JarvisShell.kt:268` 标题列 `weight(1f, fill=false)` + `maxLines=1` + `Ellipsis`；真机截图出现「…」 |
| 浮窗气泡压住卡片内容/交互元素 | 未修（浮窗固有代价，可拖动） |
| 安装后 MIUI dexopt「正在更新…」 | 非应用问题；验收脚本需等待其结束 |

### 设备复位（真机 user 11）
- `theme_id` = `sakura_night`（用户原迁移值）；旧 `theme` ordinal 校正为 `4`（Anime→sakura_night），消除双源不一致。
- 前台回到 `com.miui.home`；对话数据未触碰（仅读写 prefs 的 theme 键）。
- 残留：`force-stop` 后 Worker / sticky service 会被系统重新拉起（`u11_a305 com.jarvis.app` 进程可见），属既有后台机制，非本次改动引入。

### 构建环境坑（写清免得再踩）
`win_bash` 里若 `export JAVA_HOME=/d/JDK`（MSYS 形式）偶发被 Gradle 判为 invalid directory；改用 Windows 形式 `export JAVA_HOME='D:/JDK'` 稳定通过。

### 第二轮遗留（旧记录，已全部关闭）
1. 晨光便当设置页「限制模式」「root 模式」标题重复两遍（卡头 + 行标题），已派修。
2. 顶栏长会话标题被硬裁（无省略号），已派修 `maxLines=1 + Ellipsis`。
3. 浮窗气泡会压在卡片内容/交互元素上（可拖动，但默认位仍可能遮挡）。属浮窗固有代价，记录不修。
4. 安装/重装后 MIUI 会进入「正在更新…」dexopt 阶段，此时截图为系统更新页——验收脚本必须等它结束，否则会误判为应用黑屏。


### 第三轮（真机/模拟器反馈驱动）
| 用户反馈 | 处理 |
|---|---|
| 工作区无法改名/删除/归档；只是「风味工作区」无法选文件夹 | `WorkspaceProfile` 增加 `archived`/`dirUri`/`dirLabel`；新增归档/还原、显示已归档、SAF 绑目录、只读文件清单（文本 previewable 可展开）、对话改名/移动/删除/归档；对话归档**落库**到 `ChatSession.archivedAt` 并做幂等迁移（单测覆盖） |
| 上下边栏过渡只有一个主题一个地方有效 | 两条 chrome 分支都补顶+底渐变，抽屉与对话框边缘同样收口；统一走 `JarvisScrim`，末端 `background.copy(alpha=0f)` |
| 边框改后字体/方框配色没跟上 | `JarvisContrast` 两条路：极细 `hairline` 0.5dp 边框，或不画边框靠 `surfaceContainerHigh` 与 `surface` 的明度差分块；`FilterChip` 传 `BorderStroke(0.dp)`、`Switch` 三处 `BorderColor=Transparent` |
| 每套主题要黑白两种风格、跟随系统 | `JarvisThemeModule` 增加 `darkPalette`/`lightPalette`（未填回落 `palette`）与 `palette(mode)`；`ThemeModeOverride` 三态 + `mode_override` 持久化；`isSystemInDarkTheme` 解析后经 `forMode()` 交给外壳；5 套主题各手写两套色板并自算对比度 |
| 减小上下边框与操作按钮 | 新增 `JarvisSize`：顶栏 52dp、底栏 52dp、发送键 44dp、工具图标 20dp、chip 40dp、行高 52dp、触达 44dp |
| 图片太少，且不能太大盖住内容 | 25 张新资产（每套 `portrait_room`/`portrait_header`/`scene_a`/`scene_b`/`thumb`）；manifest 写 `ui_budget`：空态 ≤200dp 且必须在滚动流内、设置页头图 ≤140dp、缩略图 ≤72dp；美术自剔 19 张（裸肩/未成年化/构图不可裁/空图），判据入 `portrait_qa.rejection_rules` |
| 自动化只能在对话里创建 | `TasksState`/`TasksCallbacks` 增加建单草稿字段与 7 个回调；`submitRuleDraft()` 校验标题/触发说明，空则写 `ruleDraftError`；5 套主题任务页都加了「新建规则」入口与表单 |

### 第三轮验收（模拟器 emulator-5554）
- **亮暗跟随系统**：`mode_override=follow` + `cmd uimode night yes` → 晨光便当渲染「夜晚厨房」暗色，切回浅色即亮，无需重启。
- **SAF 绑目录**：点「选目录」→ DocumentsUI → 选 Documents → 授权 → 回显「目录 Documents / 可用」。
- **只读文件清单**：放入 `jarvis-note.txt` 后显示「目录文件 · 1 项 · 读取权限有效 · 只读清单，本轮没有任何写文件动作 / jarvis-note.txt · 140 B · 点击预览」。
- **对话操作入口**：工作区页每条对话都有 改名/移动/归档/删除。
- **新建自动化表单**：任务页展开「新建自动化」卡（类型 pill / 标题输入 / 类型 chips）。

### 第四轮（会话操作 / 浮窗自由拖动 / 卡顿）
| 反馈 | 根因 | 处理 |
|---|---|---|
| 二级菜单（抽屉会话列表）不能操作会话 | `SessionRail` 每行只有点击进入，改名/移动/归档/删除只挂在工作区屏 | 会话行右加「⋯」菜单（改名 / 移动 / 归档·还原 / 删除，删除二次确认、删完保底一条）；抽屉非惰性循环补 `key` |
| 浮窗有方形阴影边框 | 气泡/面板用 View `elevation`（10/12dp）+ `clipToOutline`，阴影按矩形 wrap 投出 | 弃用 elevation，改圆形 `ViewOutlineProvider` + 1dp 描边环（颜色取 `bubbleRing`） |
| 浮窗拖不到上半屏 | 上一轮为避让输入区把区间硬卡在 `[0.45H, 0.72H]` | 放开全屏（状态栏下沿 ~ 导航栏上沿）；仅在 IME 出现时挪到最近一侧避让，键盘收起回原位 |
| 切换卡顿 | 见下 | 见下 |

**卡顿的真根因（与原假设不同）**：`JarvisShell.kt:135-140` 每次外壳重组都调用 `module.palette.colorScheme()` —— 新建 36 键 `ColorScheme` 对象且它没有 `equals`，于是每帧把整棵读 `MaterialTheme` 的树判成「值变了」。已改 `remember(palette) { palette.colorScheme() }` 与 `remember { JarvisShapes.materialShapes() }`。

次要修复：新增 `LocalJarvisMode`（`ui/theme/LocalJarvisMode.kt`，由 `JarvisShell` 提供 `module.mode`），5 套主题删掉 `colorScheme.background.luminance()` 反查、改 `remember(mode)` 缓存 palette（`grep luminance` 全仓 0 命中）；列表补 `key`；`animateContentSize()` 全仓零调用（只有 import）。

**放弃项（时间盒内）**：Crossfade 双组合过渡改造、gfxinfo 前后对比数据（`uiautomator dump` 会污染帧统计，读数不可信）。

### 工具链坑（补充）
Kotlin 守护进程在本机会偶发 `e: Daemon compilation failed: null`（0 条真实编译错误，同机多 java 进程时会触发）。加 `-Pkotlin.compiler.execution.strategy=in-process` 可稳定通过，代价是全量构建 4~6 分钟。

### 第三轮发现并修复的布局缺陷
1. 底栏遮挡内容：BOTTOM_BAR 分支只预留 52dp、未计导航栏 inset，任务页空态按钮与建单表单下半被盖住 —— 已派壳层修（`selfDrawnBottomBar` 落地 + inset 合并）。
2. 工作区卡被拆碎：信息卡与「当前/编辑/删除」分两张、中间夹空表单卡 —— 便当改为 `BentoPairCard`（单卡内 info/ops 分区），空表单不再渲染。

### 第四轮收口状态（最终）
- 会话「⋯」菜单：`MainActivity.kt:2193-2245`（改名/移动/归档·还原/删除），改名 `:2106-2140`、移动 `:2141-2165`、删除二次确认 `:2166-2186`；行 `key(conversation.id)` `:2079`；抽屉「归档 N / 隐藏归档」`:2049-2051`。
- 浮窗：`elevation` 全删（本文件 0 命中），面板 1dp 描边 `FloatingAssistantService.kt:1038-1040`、气泡圆形轮廓 + 1dp ring `:341-343/:1042-1052`；拖动区间改 `[statusBar下沿, 导航栏上沿]`（`bubbleRange() :696-699`），键盘期 `imeAvoidY() :714-722` 只改临时 y、不覆盖用户位置。
- 卡顿：`JarvisShell.kt:137` `remember(palette)` 止住每帧重建 `ColorScheme`（真根因）；`LocalJarvisMode` + 主题 `remember(mode)`；`grep luminance` 全仓 0。
- 最终验证（模拟器 emulator-5554，装最终 APK）：冷启动 `topResumedActivity=MainActivity`、`logcat FATAL/Exception = 0`，樱夜深色对话页正常渲染；随后 emulator 已 `emu kill` 释放内存。
- 时间盒内放弃：Crossfade 双组合改造、gfxinfo 前后数据、面板半透明背衬、气泡柔光、浮窗键盘两态的 UI 实拍。

### 第三轮收口状态（最终）
- 5 套主题全部具备空态与建单草案卡互斥（`rules.isEmpty() && !creatingRule`；深海:211 / 樱夜:255 / 水墨:494 / 纸感:550 / 便当互斥渲染）。
- 最终构建：`assembleDebug + testDebugUnitTest` BUILD SUCCESSFUL；主题文件合计 5776 行；`drawable-nodpi` 共 53 个 webp（旧 10 + 首轮 18 + 角色 25）。
- 真机（user 11）实物确认：深海暗色对话空态的 180dp 角色插画在卡片内、不占屏；便当任务页空态 CTA 完整可见。
- 设备复位：`theme_id=sakura_night`、旧 `theme`=4、系统浅色、前台桌面；清掉 `InkClone`/`DnOwn` 两个 AVD 副本。

### 第三轮仍未验证（如实记录）
- 对话改名/移动/删除/归档的**端到端点击**（只看到入口）；
- 目录文件预览的**展开实拍**（有代码依据，设备被并发抢占未截到）；
- 便当宽屏（≥420dp）工作区卡内分栏未截图；
- 25 张新图在**真机**上的实际占屏效果（模拟器只验了逻辑）。

### 编排教训（下次同类任务必读）
1. **设备必须串行**：本轮曾让 5 个代理同时用同一台 `emulator-5554` 装机/旋转/改 `theme_id`，导致 launcher 与 App 双 ANR、多张截图拍到半成品甚至黑屏。
2. **构建也必须串行**：并发跑 gradle 会产出缺类的半成品 dex（`ClassNotFoundException: MainActivity`），必要时删 `intermediates/dex/debug/mergeProjectDexDebug` 重建。
3. **改设备状态前先备份原值**：`theme_id`、`wm size/density`、`user_rotation`、动画 scale 都要在收尾还原，否则会污染用户手机与后续代理。

## 8. 已知技术债
1. `JarvisAppearance.kt` 的旧枚举 `JarvisTheme` 仍保留（持久化迁移依赖），但已不驱动视觉；
   `ThemeIllustration` 已成死 composable（全库 0 调用），可删。
2. `res/drawable-nodpi` 里旧 10 张 `theme_{deep,graphite,forest,ink,anime}` 与 `theme_avatar_*` 仅被旧枚举引用。
3. `BentoPair` 依赖 `IntrinsicSize`，隐式约束"槽内禁放 SubcomposeLayout"，已在代码内加注释钉死。
4. 浮窗错误判定依赖文案关键字（`ERROR_HINTS`），成功文案含"失败/无法"会误报红点；根因在
   `FloatingAssistantController` 缺 error 位。
5. 远程访问的 `serverRunning` 取自持久化开关而非真实监听态。
6. 主题行数 674~784 行（上限 900），若继续加功能需按屏拆文件。
7. **冷启动 ANR 风险**：`JarvisBridgeService.onCreate` 在**主线程**做 `JarvisBridgeServer.start()` 的 socket bind 与 `synchronizeRemoteAccess()`，`onDestroy` 还有 `runBlocking { OpenClawProcessManager.stop() }`。慢设备（emulator/SwiftShader）上出现过 `Jarvis isn't responding`；真机冷启动实测 2.7s 正常。后续应把 bind 与 stop 移出主线程。
8. 浮窗错误判定仍靠文案关键字；`FloatingAssistantController` 缺 error 位。

## 9. 规模
新架构 `ui/` 共约 9.2k 行；`MainActivity.kt` 2833 → 1848 行；`FloatingAssistantService.kt` 745 → 935 行
（含主题化与错误角标）。单测 `LegacyThemeMigrationTest` 3 例通过。
