# Jarvis 0.0.6 主题美化与真机检查

## 实现

- 深海「潮汐来信」：海蓝流动插画。
- 石墨「秩序之城」：雾灰纸雕建筑。
- 松林「林间手记」：鼠尾草绿自然水彩。
- 墨黑「山月无声」：银墨山水夜色。

每套包含独立生图插画、同色系二次元头像、控件与文字配色。图片在生成后映射到对应主题色板，以统一内部颜色。异常提示保留语义错误色。新增的八个 WebP 资源合计 199,450 字节。

侧栏、页面标题、设置卡片、消息正文靠左；竖屏隐藏侧栏，横屏压缩导航间距并显示四个入口。设置 → 风格与外观可以切换，保留渐变动效。状态栏和导航栏图标随深浅主题切换。

## 生图资源

采用用户此前指定的本地 A1111 SDXL，模型 Kohaku XL Epsilon rev1；未使用 OpenAI 图像 API。原图与主题资源已存入本项目。

- 完整提示词、负面词、种子与生成参数：[generation.json](../artwork/themes/generation.json)
- 可重复生成脚本：[generate-theme-art.py](../tools/generate-theme-art.py)
- 原图：`artwork/themes/{deep,graphite,forest,ink}-sdxl.png`
- 应用资源：`app/src/main/res/drawable-nodpi/theme_*.webp`
- 真机四主题预览：[device-four-themes.jpg](../artwork/themes/device-four-themes.jpg)

## 验证

2026-10-26，在连接的 Redmi M2007J3SC、Android SDK 37、用户 11 中覆盖安装调试包，保留已有数据。

- `:app:assembleDebug --no-daemon` 成功；仅既有图标废弃 API / Gradle 提示。
- 四套主题分别切换并截图，插画、头像、按钮颜色同步。
- 竖屏侧栏隐藏，横屏四个导航入口均可见。
- 修复墨黑主题状态栏图标对比度，并在最终安装版本中截图确认。
- 强制停止并重启用户 11 应用后，页面仍显示墨黑「山月无声」，主题持久化通过。
- 强制停止时系统清除了本应用的无障碍服务启用项；已恢复原先启用状态后完成检查。此项为测试观察，不表示已修复系统的强制停止行为。
- 测试结束恢复深海主题与原先的自由旋转模式。
- `git diff --check` 通过。

本轮为视觉与交互布局检查，未重新执行 QQ、下载或模型对话业务测试。

## 安装包

- 版本：0.0.6 / versionCode 6
- 路径：`app/build/outputs/apk/debug/app-debug.apk`
- 大小：243,994,078 字节
- SHA256：`AEFA5A03BC6854D43341BB84E8C3273334C742EB30E1953141C88C9AAB39969C`
