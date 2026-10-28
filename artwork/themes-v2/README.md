# themes-v2 素材说明

主题插画资产（当前在用 5 套）：每套 `banner`(1536x640) / `avatar`(512x512) / `texture`(512x512，可平铺)，
外加 5 张角色插画 `portrait_room` / `portrait_header` / `scene_a` / `scene_b` / `thumb`（见下节）。
源图在 `<id>/<kind>.png`，打包产物在 `app/src/main/res/drawable-nodpi/theme_<id>_<kind>.webp`，
可复现记录在 `manifest.json`，规格自检用 `verify_assets.ps1`。

> **`neon_cyber`（霓虹赛博，赛博主题）已按用户要求移除**：代码里的 `NeonCyberTheme.kt` 已删除，
> `JarvisThemes.all` 只剩 5 套。本目录下的 `neon_cyber/` 源图与
> `app/src/main/res/drawable-nodpi/theme_neon_cyber_*.webp` **仅作留档，不参与构建**（无代码引用）。
> 持久化的 `theme_id=neon_cyber` 会在启动时回退到 `deep_night`，不会崩溃也不会白屏。

## 生成器

`tools/generate-theme-v2.py`。需要 Pillow；**系统 Python 3.12 没有 PIL**，所以用 A1111 自带运行时：

```
PY="D:\sd-webui-aki(1)\sd-webui-aki-v4.11.1-cu128\python\python.exe"

"$PY" tools/generate-theme-v2.py                                  # 全量：缺失的源图才请求 A1111
"$PY" tools/generate-theme-v2.py --theme ink_moon                 # 单套重出（幂等，不动其他主题）
"$PY" tools/generate-theme-v2.py --theme deep_night --only banner --force   # 只强制重出某一张
"$PY" tools/generate-theme-v2.py --seed-offset 500                # 整体平移 seed，换一个 seed 家族探索
"$PY" tools/generate-theme-v2.py --offline                        # 不碰 A1111：仅用现有 PNG 重建 webp + manifest
"$PY" tools/generate-theme-v2.py --list
```

`--offline` 不需要 A1111 在跑（改色/改裁切/补元数据时用它）。`--force` 与 `--offline` 互斥，会被拒绝。
幂等性：源 PNG 已存在时默认跳过 SD 请求，只重建 webp；重跑输出与上次逐字节一致。

## A1111 启动参数

```
cd D:/sd-webui-aki(1)/sd-webui-aki-v4.11.1-cu128
HF_ENDPOINT=https://hf-mirror.com HF_HUB_DISABLE_IMPLICIT_TOKEN=1 \
  ./python/python.exe -u launch.py --api --port 7861 \
  --skip-install --skip-python-version-check --opt-sdp-attention --medvram-sdxl
```

API 端口是 **7861**（不是默认 7860）。checkpoint：`sdxl\kohaku-xl-epsilon-rev1.safetensors [516047db97]`。
除启动参数外还有一项必须的设置（已写入 A1111 自己的 config.json，重装/重置后要重设）：

```
curl -s -X POST -H "Content-Type: application/json" \
     -d '{"sd_vae_decode_method":"Tiled"}' http://127.0.0.1:7861/sdapi/v1/options
```

采样参数：DPM++ 2M + Karras，steps 24，cfg 6.0，每套主题固定 seed（270601 起，按主题 +11）。

## 七个坑（都踩过：前四个让出图卡死或极慢，后三个让启动直接失败或出废图）

1. **不加 `--skip-install` 会卡死启动**：`launch.py` 停在 `prepare_environment` 不打印任何东西，看起来像在加载模型。
2. **CLIP tokenizer 缺失 + 过期 HF token**：报 `OSError: Can't load tokenizer for 'openai/clip-vit-large-patch14'`。
   本机 `~/.cache/huggingface` 里存有**过期 OAuth token**，会以 `401 OAuth token has expired` 覆盖匿名请求，
   所以要 `HF_HUB_DISABLE_IMPLICIT_TOKEN=1`；huggingface.co 直连超时，改用 `HF_ENDPOINT=https://hf-mirror.com`。
3. **注意力退化成 Doggettx**：没有 xformers 时上一版会选到 Doggettx，单张图 ETA 到 3041s。加 `--opt-sdp-attention` 后
   24 步采样只要 ~51s。日志里必须看到 `Applying attention optimization: sdp`。
4. **VAE 解码在 8GB 卡上换页**：采样结束后进度条停住不动、GPU 100% 但十分钟不返回。未优化时显存静态占用就 7680/8152MiB，
   故需 `--medvram-sdxl` + 上面的 `sd_vae_decode_method=Tiled`。修好后单张 1536x640 约 131s。

5. **第三方扩展的 `on_app_started` 崩启动**：`RuntimeError: Cannot add middleware after an application has
   started`。诡异之处在于它和 `HF_ENDPOINT` 有关——镜像通了之后 ADetailer 才下载到它那 10 个检测模型
   （此前离线只加载 4 个/失败），于是走上了会在 app 启动后注册中间件的路径。加 `--disable-extra-extensions`
   即可（本任务不需要 ADetailer / ControlNet / prompt-all-in-one）。
6. **不要用 `--nowebui`**：headless 模式下初始化不完整，`/sdapi/v1/txt2img` 直接 500
   `NansException: A tensor with NaNs was produced in Unet`。普通 web UI 模式（不加 `--nowebui`）正常。
7. **底纹不要开原生 `tiling`**：请求里 `tiling: true` 的 circular padding 在 SDXL 上会画出规则方格/横条，
   偶发退化成一整片死白。无缝改由脚本的 `make_tileable()` 镜像构造保证（暗色主题保留历史请求，未改动）。
   注意平铺必须**逐轴**判定：只看到横向接缝干净不代表纵向也干净，取平均会把这个缺陷藏起来。

另外：客户端 HTTP 超时要给足（脚本用 1200s）。A1111 不会因为客户端断开而取消任务，超时重投只会把任务排队叠加；
脚本在开跑前会调 `interrupt` 清掉残留任务（`wait_idle()`）。

## style_generation（两代提示词）

`manifest.json` 每张图带 `style_generation`，顶层 `style_note` 说明成因：

- `v2`：记录的 prompt 与当前脚本会发出的 prompt **完全一致**，可直接复现（`--theme <id> --force`）。
- `v1`：STYLE 前缀还没有 `Japanese animation background painting, anime key visual, cel-shaded brushwork,
  2d anime art` 时出的图，**无法用当前脚本逐字节复现**，manifest 里是历史运行记录。

当前分布：v1 共 6 张（deep_night avatar、sakura_night banner+avatar、neon_cyber banner+avatar、ink_moon avatar），
其余 12 张为 v2。**v1 的那 6 张是评审后刻意保留的**（画面已认可，不为元数据对称去替换）。
texture 不用 STYLE 前缀，两代生成结果一致，故记为 v2。

## 亮色主题底纹的特殊处理

亮色主题的 texture 会铺在正文底下，所以它不能只是"看起来淡"，必须可验证地淡。两道闸门：

- 生成后先算**源图**黑像素占比（L<64）与灰度标准差：黑占比 >2%（说明模型画出了结构）或标准差 <3（退化成一整片白）
  就换 seed 重出，最多 5 次（`TEXTURE_ATTEMPTS`）。
- 再 `paperify()`：只保留源图的纤维细节，重新居中到主题浅色上，并把灰度标准差压到约 11/255（≈4% 对比）。
  模型给什么对比度都不能带出去。

paper_minimal 例外：`texture_grain="procedural"`。原因写在代码注释里——SDXL 对这个主题稳定返回**规则纹样**
（试过网格、横条、织纹三种提示词/5 个 seed），而规则条纹正是要避免的东西，所以它的颗粒由 `procedural_grain()`
确定性生成（`Image.effect_noise` + 同一条 4% 对比带），manifest 里 `grain` 字段如实记为 `procedural`。

暗色主题同样有闸门（neon_cyber）：要求**灰度标准差 ≤12/255** 且**高饱和像素（S>0.6 且 V>0.4）占比 ≤3%**，
超了就换 seed，最多 5 次。这个门槛是实测出来必须的——SDXL 对"霓虹网格"这个题材连出 5 张都是高饱和故障块
（实测 sd 32.9–53.8、饱和像素 11.5%–24.9%），铺满屏后像渲染错误，主题作者只能加不透明标题条救场。
因此 neon_cyber 的底纹改为 `procedural_neon_grid()`：近黑底 #04070D + 1px 青/品红网格 + 极轻噪点，
manifest 记 `grain: "procedural_neon"`。三个反直觉的标定细节（改参数前务必先读）：

1. **线亮度必须按编码后标定，不能按绘制值**。q78 会把 1px 线抹掉一半以上，而且对 1px 线是阶跃响应：
   同样构造下横向可能 1.3%、也可能 3.6%，几乎没有中间值。当前 `NEON_LINE_DELTAS` 是扫参数实测选出来的，
   量的是"线中心列/行 mean L 减格心列/行 mean L"，不是看绘制时的 RGB 增量。
2. **网格整体偏移半格**（`(i + 0.5) * size / cells`）。线压在 tile 边上会让任何"边缘相似度"判据失败，
   从而触发镜像平铺，在 tile 里压出一层 2x2 对称的淡色块——正是要避免的"大色块"。
3. **噪点用 128px patch 周期平铺**，不要整张随机：这样 tile 天然无缝，`make_tileable()` 判为 `native-tiling`，
   网格不会被镜像；判据也从"接缝 ≤ 绝对阈值"改成"接缝 ≤ 相邻列基线"（噪点纹理的接缝只能跟基线比）。
   格数取 22（512/24 不整除会破平铺），实际间距 ≈23.3px，线宽 1px。
底纹着色强度可按主题覆盖（`texture_tint`，neon 设 0 以保住近黑底，否则会被调色渐变染亮）。

## 美术方向约束

横幅左侧约 63% 会被 `JarvisAppearance.kt` 的 surface 渐变蒙版压住并叠标题文字，所以主体必须偏右、左侧留安静区域，
且画面里不能有任何文字/水印/UI。SDXL 并不稳定遵守"右三分之一"这类空间指令，脚本因此带两层兜底：

- `right_bias()`：按边缘能量质心判断，主体偏左时水平翻转；
- 每套可用 `bias_threshold` 提高翻转阈值（`ink_moon` 设为 0.75，把月亮定到右侧）。

调色：生成图与主题三色渐变做 8% 混合（texture 18%）统一色感，混合过重会压平笔触。

## 角色插画（portrait_room / portrait_header / scene_a / scene_b / thumb）

生成器 `tools/generate-portraits-v2.py`：把 `generate-theme-v2.py` 当模块加载，复用同一套 STYLE 前缀、
A1111 客户端、调色与 WebP 预算，所以两代脚本的提示词风格和出图参数不会漂移。一次请求只出一张，落盘即打印宽高与 KB。

| kind | 尺寸 | 用途 | manifest `ui_budget.max_height_dp` |
| --- | --- | --- | --- |
| portrait_room | 768x1024 | 对话空态 | 200 |
| scene_a / scene_b | 768x1024 | 空态轮换（同一次会话内不与 portrait_room 重复） | 200 |
| portrait_header | 1536x640 | 设置页头图 | 140 |
| thumb | 512x512 | 卡片小图标（56~72dp，圆形或 24dp 圆角） | 72 |

`ui_budget` 是给主题实现阶段的硬约束：竖图放在滚动内容流里、不叠在内容之上、高度封顶，
头图不得压住标题、不得当全屏背景，thumb 不得当大面积背景。图本身的构图已经按这些约束留白。

### 两条确定性排版步骤（SDXL 不遵守空间指令时的兜底）

1. **`headroom()`**：竖图上三分之一要留给文字，可 SDXL 只有约五分之一的 seed 会真的留白。
   于是把画面缩到 97% 高度、用**自身首行像素**生成一条带渐变的空带补在上方（只碰背景，不伪造人物），
   再羽化接缝。试过三种补法：拉抻真实天空会把高光糊成一大片、从左侧取样会搬来边缘瑕疵，
   最终用"首行渐变"——接缝连续且可验证（`qa.text_band.stddev`）。
2. **`place_right()`**：头图要求角色在右三分之一。整幅边缘能量质心会被栏杆/亮墙拖到 0.5，
   于是改成"只量 8%~78% 行区间"的质心，不够右就整体右移，空出的左侧用**自身最左列**延伸
   （地板、栏杆线会自然接上）再压暗，保证标题区安静。`morning_bento` 的亮墙会把质心读高，
   该套用 `HEADER_FIX = {"morning_bento": "mirror"}` 显式镜像，不再走质心判据。
3. **`icon_fit()`**：thumb 先缩小 5% 再贴到自身模糊背景上，保证 60dp 圆形裁切时头发不被切掉。

### 边界（每张都按这份清单自检，越界即换 seed）

允许：更近的镜头、贴身衣物、回眸/挑衅/害羞的表情、肌肤光泽、暧昧光氛、侧身与跪坐等姿态。
禁止：裸体、性器官、性行为、明显性暗示特写、制服被扯开、露点、未成年化。

落实手段有三层：提示词正向前缀写死 "one adult woman in her late twenties, adult character design,
fully clothed in opaque fabric that covers the shoulders and chest"；负向前缀写死
"nude, nipples, see-through, off-shoulder, deep neckline, clothes pulled open, child, loli, minor, chibi…"；
出图后逐张目检，命中禁止项就换 seed 重出，`qa.boundary` 记录该图的通过理由，`qa.rejected_attempts` 记录被剔掉的张数
（本轮 25 张共剔除 19 张）。`content_rating` 统一 `suggestive-non-explicit`。

### 源图与可复现

源 PNG 与 `<kind>.json` 旁注文件（真实 seed / prompt / 耗时）一起落在 `<id>/` 下；
`--offline` 重建 WebP 与 manifest 时从旁注取 seed，所以 manifest 里的 seed 永远指向盘上那张 PNG，
而不是该主题的基准 seed。

    "$PY" tools/generate-portraits-v2.py --theme ink_moon --kind thumb --force --seed-offset 61

## 自检

```
powershell -NoProfile -ExecutionPolicy Bypass -File artwork/themes-v2/verify_assets.ps1
```

输出 43 个文件的真实宽高、KB、是否超预算（banner/portrait_header ≤220KB / avatar/thumb ≤80KB /
texture ≤120KB / portrait_room/scene_a/scene_b ≤260KB），
以及 6 张底纹的灰度均值/标准差/对比度/黑像素占比、两轴接缝值对非相邻列基线。
亮色三套的判定带是 **黑占比 <2% 且对比 3%–6%**；暗色主题只判接缝，不套对比带（它们本来就该暗）。