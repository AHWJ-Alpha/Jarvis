package com.jarvis.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs

/** A small system overlay for voice tasks, quick chat and accessibility progress. */
class FloatingAssistantService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var content: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var appearance = FloatingAssistantPreferences()
    private var bubbleView: ImageView? = null
    /** 气泡下方的 1dp 圆形描边环（取代 elevation 的方形阴影）。 */
    private var bubbleRingView: View? = null
    private var errorBadge: TextView? = null
    private var errorText: String? = null
    private var acknowledgedResult = ""
    private var compactProgress: TextView? = null
    private var compactForTask = false
    private var hiddenUntil = 0L
    private var pendingVoiceText: String? = null
    private var pendingVoiceSubmit: Runnable? = null
    private val bubbleGesture = BubbleGestureInterpreter()
    private var themeColors: FloatingThemeColors? = null
    private var panelBackground: GradientDrawable? = null
    private var taskRunning = false
    /** 折叠态气泡的窗口 y（屏幕坐标）；-1 表示尚未定位。 */
    private var bubbleY = -1
    /** 用户手动拖动过气泡：此时位置优先于默认停靠点（P1 修复：只在没拖过时回默认位）。 */
    private var bubbleMoved = false
    /** 当前软键盘高度（px），用于把气泡挡在键盘上沿之上；0 表示无键盘。 */
    private var imeHeight = 0
    @Volatile private var submittedTask: String? = null
    private val history = mutableListOf<Pair<Boolean, String>>()

    private var headerRow: LinearLayout? = null
    private var titleText: TextView? = null
    private var returnLabel: TextView? = null
    private val themedButtons = mutableListOf<Button>()
    private var statusText: TextView? = null
    private var resultText: TextView? = null
    private var progressText: TextView? = null
    private var inputText: EditText? = null
    private var returnRow: LinearLayout? = null
    private var returnButton: Button? = null
    private var voiceButton: Button? = null
    private var chatButton: Button? = null
    private var sendButton: Button? = null

    private val observer: (FloatingAssistantSnapshot) -> Unit = { state -> mainHandler.post { render(state) } }

    /**
     * 主题切换的唯一可靠触发点：设置页只写 jarvis_config/theme_id，浮窗可能长时间收不到任何
     * 快照事件，只靠 render() 首行的 syncTheme() 会把旧头像/旧配色一直挂着。这里直接监听该键。
     */
    private val themePrefs by lazy {
        applicationContext.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
    }
    private val themeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // theme_id（换主题）与 mode_override（换明暗）任一变化都要重上色：只切明暗时 themeId 不变，
        // 靠 themeId 比较会漏掉，所以两个键走同一条重绘路径，判断交给 syncTheme 里的 (id, mode) 比较。
        if (key != KEY_THEME_ID && key != KEY_MODE_OVERRIDE) return@OnSharedPreferenceChangeListener
        mainHandler.post {
            if (syncTheme()) applyBubblePosition(force = true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        appearance = FloatingAssistantPreferences.load(this)
        restoreBubblePosition()
        loadHistory()
        startForeground(NOTIFICATION_ID, buildNotification())
        themePrefs.registerOnSharedPreferenceChangeListener(themeListener)
        FloatingAssistantController.observe(observer)
        if (Settings.canDrawOverlays(this)) attachWindow() else {
            FloatingAssistantController.reportLaunchFailure("悬浮窗权限未开启")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                removeWindow()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SHOW -> if (Settings.canDrawOverlays(this)) { attachWindow(); refreshAppearance() }
            ACTION_REFRESH -> refreshAppearance()
            ACTION_OPEN_CHAT -> { attachWindow(); expand() }
            ACTION_VOICE -> openVoiceCapture()
            ACTION_MAIN_CHAT -> openMainActivity(FloatingLaunchAction.OPEN_MAIN_CHAT)
            ACTION_QUICK_TASK -> {
                attachWindow()
                expand()
                intent?.getStringExtra(EXTRA_QUICK_TASK)?.trim()?.takeIf { it.isNotBlank() }?.let { prompt ->
                    val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                    if (taskId != null && submittedTask == taskId) return@let
                    submittedTask = taskId
                    mainHandler.post {
                        inputText?.setText(prompt)
                        submitPrompt()
                    }
                }
            }
            ACTION_VOICE_RESULT -> {
                val transcript = intent.getStringExtra(EXTRA_QUICK_TASK)?.trim().orEmpty()
                if (transcript.isNotBlank()) stageVoiceResult(transcript)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        themePrefs.unregisterOnSharedPreferenceChangeListener(themeListener)
        FloatingAssistantController.removeObserver(observer)
        pendingVoiceSubmit?.let(mainHandler::removeCallbacks)
        removeWindow()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** 旋转/分辨率变化后屏幕高度变了，安全区间要重算；拖过的位置保留（只做 clamp，不回默认位）。 */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imeHeight = 0
        mainHandler.post {
            // follow 模式下系统切深浅色不会写 prefs，只能靠配置变化触发重算。
            syncTheme()
            applyBubblePosition(force = true)
        }
    }

    private fun attachWindow() {
        if (content != null) return
        val view = buildWindow()
        val overlayParams = WindowManager.LayoutParams(
            dp(appearance.sizeDp), dp(appearance.sizeDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = if (bubbleY >= 0) bubbleY else defaultBubbleY()
        }
        runCatching {
            windowManager.addView(view, overlayParams)
            content = view
            params = overlayParams
            // The window is first attached at bubble size. Hide the panel children
            // before drawing so they cannot cover the bubble or receive stray taps.
            collapse()
        }.onFailure { FloatingAssistantController.reportLaunchFailure("无法显示悬浮窗：${it.message ?: "系统拒绝"}") }
        render(FloatingAssistantController.snapshot)
    }

    private fun buildWindow(): View {
        themedButtons.clear()
        val colors = colors()
        // 不用 elevation：overlay 窗口的 elevation 阴影按矩形 wrap 投影，圆角面板会带方框影。
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelDrawable(colors)
            clipToOutline = true
        }
        // 软键盘弹出/收起时重新算一次气泡停靠点（部分机型不给 overlay 窗口派发 IME insets，此时退化为 render 时 clamp）。
        root.setOnApplyWindowInsetsListener { view, insets ->
            val keyboard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.ime()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            if (keyboard != imeHeight) {
                imeHeight = keyboard
                android.util.Log.i("JarvisFloating", "ime=" + keyboard + " range=" + bubbleRange() + " userY=" + bubbleY)
                applyBubblePosition(force = true)
            }
            view.onApplyWindowInsets(insets)
        }
        panelBackground = root.background as? GradientDrawable
        compactProgress = TextView(this).apply {
            text = "Jarvis 正在操作…"
            textSize = 12f
            setTextColor(colors.title)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(colors.header, 12)
            visibility = View.GONE
        }
        root.addView(compactProgress, LinearLayout.LayoutParams(-1, dp(32)))
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(8), dp(8))
            background = rounded(colors.header, 0)
        }
        headerRow = header
        val title = TextView(this).apply {
            text = "JARVIS  ·  随身助手"
            setTextColor(colors.title)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        titleText = title
        val collapse = button("−") { collapse() }.apply { minWidth = dp(38); minHeight = dp(36) }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(collapse)
        root.addView(header)

        statusText = TextView(this).apply {
            setTextColor(colors.body); textSize = 12f
            setPadding(dp(14), dp(8), dp(14), dp(3))
        }
        root.addView(statusText)
        progressText = TextView(this).apply {
            setTextColor(colors.accent); textSize = 12f
            setPadding(dp(14), dp(2), dp(14), dp(6))
        }
        root.addView(progressText)

        val scroll = ScrollView(this).apply { isFillViewport = false }
        resultText = TextView(this).apply {
            setTextColor(colors.title); textSize = 13f
            setPadding(dp(14), dp(7), dp(14), dp(10))
            text = "语音说任务，或在下方输入问题。需要控制手机时会显示操作进度。"
        }
        scroll.addView(resultText)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        returnRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(colors.notice, 12)
            visibility = View.GONE
        }
        returnLabel = TextView(this).apply {
            text = "Jarvis 已完成屏幕操作。要切回原应用吗？"
            setTextColor(colors.title); textSize = 12f
        }
        returnRow?.addView(returnLabel)
        val returnActions = LinearLayout(this).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        returnButton = button("切回原应用") { returnToPreviousApp() }
        returnActions.addView(button("留在当前界面") { FloatingAssistantController.dismissReturnChoice() })
        returnActions.addView(returnButton)
        returnRow?.addView(returnActions)
        root.addView(returnRow)

        val entry = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(5), dp(9), dp(8))
        }
        inputText = EditText(this).apply {
            hint = "输入要做的事…"
            setTextColor(colors.title)
            setHintTextColor(colors.muted)
            textSize = 13f
            minLines = 1
            maxLines = 2
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = rounded(colors.field, 14, colors.bubbleRing)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { submitPrompt(); true } else false
            }
        }
        entry.addView(inputText, LinearLayout.LayoutParams(-1, dp(48)))
        voiceButton = button("语音") { openVoiceCapture() }
        chatButton = button("键盘") { focusChatInput() }
        sendButton = button("发送") { submitPrompt() }
        val actions = LinearLayout(this).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        listOf(voiceButton, chatButton, sendButton).forEach { child ->
            actions.addView(child, LinearLayout.LayoutParams(0, dp(40), 1f))
        }
        entry.addView(actions, LinearLayout.LayoutParams(-1, dp(42)))
        root.addView(entry)

        val bubble = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Jarvis 悬浮助手。单点打开，长按语音输入"
        }
        bubbleView = bubble
        styleBubble(bubble)
        bubble.setOnTouchListener(makeDragListener(bubble))
        // 角标要叠在头像上，所以头像外面包一层同尺寸 FrameLayout；窗口折叠时尺寸与此容器一致，不会裁切。
        val wrap = FrameLayout(this).apply { clipChildren = false }
        // 气泡自身不再用 elevation（圆球会带方框阴影）；改成垫在图下方的圆形描边环，只露出外缘 1dp。
        val ring = View(this).apply { background = bubbleRingDrawable(colors) }
        bubbleRingView = ring
        wrap.addView(ring, FrameLayout.LayoutParams(dp(appearance.sizeDp), dp(appearance.sizeDp)))
        wrap.addView(bubble, FrameLayout.LayoutParams(dp(appearance.sizeDp), dp(appearance.sizeDp)))
        errorBadge = TextView(this).apply {
            text = "!"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(colors.onError)
            background = rounded(colors.error, 9, colors.panel)
            contentDescription = "出错了，点开悬浮球查看详情"
            visibility = View.GONE
        }
        wrap.addView(errorBadge, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.END))
        root.tag = wrap
        root.addView(wrap, LinearLayout.LayoutParams(dp(appearance.sizeDp), dp(appearance.sizeDp)))
        root.visibility = View.VISIBLE
        return root
    }

    private fun makeDragListener(bubble: View) = object : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var originX = 0
        private var originY = 0
        private var moved = false
        private var longPressed = false
        private val longPress = Runnable {
            if (!moved && !longPressed) {
                longPressed = true
                bubbleGesture.reset(mainHandler)
                openVoiceCapture()
            }
        }
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val window = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    originX = window.x; originY = window.y; moved = false; longPressed = false
                    mainHandler.postDelayed(longPress, 500)
                    pressDown(view)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) + abs(dy) > dp(8)) {
                        moved = true
                        mainHandler.removeCallbacks(longPress)
                    }
                    if (moved) {
                        window.x = (originX - dx).coerceAtLeast(0)
                        // 全屏自由拖动，只做系统栏之间的边界 clamp（不再限制 0.45H~0.72H）。
                        window.y = clampBubbleY(originY + dy)
                        bubbleY = window.y
                        bubbleMoved = true
                        runCatching { windowManager.updateViewLayout(content, window) }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    mainHandler.removeCallbacks(longPress)
                    pressUp(view)
                    if (moved) persistBubblePosition(bubbleY)
                    if (!moved && !longPressed) {
                        if (pendingVoiceText != null) editPendingVoice()
                        else bubbleGesture.tap(System.currentTimeMillis(), mainHandler) { count ->
                            when (count) {
                                1 -> expand()
                                2 -> performGesture(appearance.doubleTap, appearance.doubleTapTask)
                                3 -> performGesture(appearance.tripleTap, appearance.tripleTapTask)
                            }
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPress)
                    pressUp(view)
                    if (moved) persistBubblePosition(bubbleY)
                    return true
                }
            }
            return false
        }
    }

    private fun expand() {
        val root = content as? LinearLayout ?: return
        val window = params ?: return
        compactForTask = false
        root.visibility = View.VISIBLE
        expanded = true
        acknowledgedResult = FloatingAssistantController.snapshot.result
        clearError()
        panelBackground = panelBackground ?: panelDrawable(colors())
        root.background = panelBackground
        root.clipToOutline = true
        for (index in 0 until root.childCount) {
            root.getChildAt(index).visibility = if (root.getChildAt(index) === root.tag || root.getChildAt(index) === compactProgress) View.GONE else View.VISIBLE
        }
        window.width = minOf(dp(360), (resources.displayMetrics.widthPixels - dp(24)).coerceAtLeast(dp(240)))
        window.height = minOf(dp(370), (resources.displayMetrics.heightPixels - dp(80)).coerceAtLeast(dp(240)))
        window.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        window.alpha = 1f
        window.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        window.gravity = Gravity.TOP or Gravity.END
        // Keep the action row above the IME even on compact phone displays.
        window.y = dp(16)
        runCatching { windowManager.updateViewLayout(root, window) }
        render(FloatingAssistantController.snapshot)
    }

    private fun collapse() {
        val root = content as? LinearLayout ?: return
        val window = params ?: return
        expanded = false
        compactForTask = false
        root.visibility = View.VISIBLE
        hideKeyboard()
        root.background = ColorDrawable(android.graphics.Color.TRANSPARENT)
        root.clipToOutline = false
        val bubble = root.tag as? View
        for (index in 0 until root.childCount) {
            root.getChildAt(index).visibility = if (root.getChildAt(index) === bubble) View.VISIBLE else View.GONE
        }
        window.width = dp(appearance.sizeDp)
        window.height = dp(appearance.sizeDp)
        window.gravity = Gravity.TOP or Gravity.END
        window.x = dp(12)
        bubbleY = clampBubbleY(if (bubbleY >= 0) bubbleY else defaultBubbleY())
        window.y = bubbleY
        window.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        window.alpha = 1f
        inputText?.clearFocus()
        runCatching { windowManager.updateViewLayout(root, window) }
        // 键盘还开着时先让开输入区；键盘收起后这次调用会把气泡放回用户位置。
        applyBubblePosition(force = true)
    }

    private fun compactProgress(state: FloatingAssistantSnapshot) {
        val root = content as? LinearLayout ?: return
        val window = params ?: return
        if (!compactForTask) {
            hideKeyboard()
            inputText?.clearFocus()
        }
        compactForTask = true
        expanded = false
        root.visibility = View.VISIBLE
        root.background = ColorDrawable(android.graphics.Color.TRANSPARENT)
        root.clipToOutline = false
        for (index in 0 until root.childCount) {
            root.getChildAt(index).visibility = if (root.getChildAt(index) === compactProgress) View.VISIBLE else View.GONE
        }
        compactProgress?.text = "Jarvis · ${operationLabel(state.operation.ifBlank { "执行中" })}  ${state.completedOperations}/${state.attemptedOperations}"
        window.width = dp(230)
        window.height = dp(32)
        window.gravity = Gravity.TOP or Gravity.START
        window.x = dp(8)
        window.y = dp(26)
        window.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        // Android 12+ only passes touches through an untrusted overlay below
        // the maximum obscuring opacity (0.8 by default).
        window.alpha = 0.7f
        runCatching { windowManager.updateViewLayout(root, window) }
        android.util.Log.i("JarvisFloating", "compact progress: ${state.taskLabel} ${state.operation}")
    }

    private fun styleBubble(bubble: ImageView) {
        bubble.setImageResource(colors().avatarRes)
        val radius = when (appearance.shape) {
            FloatingShape.Circle -> appearance.sizeDp / 2f
            FloatingShape.Rounded -> appearance.sizeDp * .28f
            FloatingShape.Square -> 5f
        }
        bubble.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(radius.toInt()).toFloat())
            }
        }
        bubble.clipToOutline = true
        bubble.invalidateOutline()
        bubbleRingView?.let { ring ->
            ring.background = bubbleRingDrawable(colors())
            ring.invalidate()
        }
    }

    private fun colors(): FloatingThemeColors =
        themeColors ?: FloatingThemePalette.read(this).also { themeColors = it }

    /**
     * 主题可能在设置页被切走（装配代理写 jarvis_config/theme_id）。每次 render 重读一次缓存里的
     * SharedPreferences，themeId 变了就整套重上色，没变 immediate 返回。
     */
    private fun syncTheme(): Boolean {
        val next = FloatingThemePalette.read(this)
        if (next.themeId == themeColors?.themeId && next.mode == themeColors?.mode && panelBackground != null) return false
        themeColors = next
        applyTheme(next)
        return true
    }

    private fun applyTheme(colors: FloatingThemeColors) {
        panelBackground = panelDrawable(colors)
        applyPanelBackground()
        headerRow?.background = rounded(colors.header, 0)
        titleText?.setTextColor(colors.title)
        statusText?.setTextColor(colors.body)
        resultText?.setTextColor(colors.title)
        progressText?.setTextColor(colors.accent)
        compactProgress?.apply {
            setTextColor(colors.title)
            background = rounded(colors.header, 12)
        }
        returnRow?.background = rounded(colors.notice, 12)
        returnLabel?.setTextColor(colors.title)
        inputText?.apply {
            setTextColor(colors.title)
            setHintTextColor(colors.muted)
            background = rounded(colors.field, 14, colors.bubbleRing)
        }
        themedButtons.forEach { restyleButton(it, colors) }
        errorBadge?.apply {
            setTextColor(colors.onError)
            background = rounded(colors.error, 9, colors.panel)
        }
        bubbleView?.let { styleBubble(it) }
    }

    /** 只有展开态才让面板底色可见；折叠/进度态下 root 必须是透明的（否则会挡住下面应用）。 */
    private fun applyPanelBackground() {
        val root = content as? LinearLayout ?: return
        if (!expanded) return
        root.background = panelBackground
        root.clipToOutline = true
    }

    private fun restyleButton(button: Button, colors: FloatingThemeColors) {
        button.setTextColor(colors.title)
        button.background = pressable(colors.buttonFill, 12, colors.ripple)
    }

    private fun pressDown(view: View) {
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(120).start()
    }

    private fun pressUp(view: View) {
        view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
    }

    private fun markError(message: String) {
        if (message == errorText) return
        errorText = message
        errorBadge?.visibility = View.VISIBLE
        errorBadge?.contentDescription = "出错：${message.take(40)}。点开悬浮球查看详情"
    }

    private fun clearError() {
        if (errorText == null && errorBadge?.visibility == View.GONE) return
        errorText = null
        errorBadge?.visibility = View.GONE
        errorBadge?.contentDescription = "出错了，点开悬浮球查看详情"
    }

    /** 折叠态才看得见气泡；错误文案只能落在角标上。 */
    private fun updateErrorBadge(state: FloatingAssistantSnapshot) {
        if (state.busy || state.result.isBlank()) return
        if (looksLikeError(state.result)) {
            if (state.result != acknowledgedResult) markError(state.result)
        } else if (errorText != null) {
            acknowledgedResult = state.result
            clearError()
        }
    }

    private fun looksLikeError(value: String): Boolean = ERROR_HINTS.any { value.contains(it) }

    private fun refreshAppearance() {
        appearance = FloatingAssistantPreferences.load(this)
        if (applyBubbleSize() != null) bubbleView?.let { styleBubble(it) }
        if (!expanded) collapse()
    }

    /**
     * 折叠态的气泡层级是 root(LinearLayout) → wrap(FrameLayout) → bubble(ImageView) + errorBadge。
     * 尺寸必须两层一起改，且各自按父容器的 LayoutParams 类型取参数——气泡的 lp 是 FrameLayout 的，
     * 拿它去强转 LinearLayout.LayoutParams 会抛 ClassCastException 直接打死进程。
     * @return 尺寸更新后的气泡；气泡尚未创建时返回 null。
     */
    private fun applyBubbleSize(): ImageView? {
        val bubble = bubbleView ?: return null
        val size = dp(appearance.sizeDp)
        val wrap = bubble.parent as? View
        if (wrap != null) {
            val wrapLp = wrap.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(size, size)
            wrapLp.width = size
            wrapLp.height = size
            wrap.layoutParams = wrapLp
        }
        val bubbleLp = bubble.layoutParams as? FrameLayout.LayoutParams
            ?: FrameLayout.LayoutParams(size, size)
        bubbleLp.width = size
        bubbleLp.height = size
        bubble.layoutParams = bubbleLp
        return bubble
    }

    private fun screenHeightPx(): Int = resources.displayMetrics.heightPixels

    /**
     * 气泡可移动区间 = [statusBar 下沿, 导航栏上沿]，即系统栏之间的内容区（全屏自由拖动）。
     * overlay 窗口不带 FLAG_LAYOUT_IN_SCREEN，y 原点就是内容区顶（statusBar 下沿），
     * 所以区间下界为 0、上界为「内容区高 - 气泡高」；insets 取不到时退化为整屏高。
     */
    private fun contentAreaHeight(): Int {
        val screen = screenHeightPx()
        val available = screen - statusBarOffset() - navigationBarHeight()
        return available.coerceAtLeast(dp(120))
    }

    /**
     * overlay 窗口的 y 原点就是 statusBar 下沿（系统把窗口 fit 在系统栏之间），所以顶偏移要按
     * 「视图实际落点 - params.y」反推：rootWindowInsets 对 overlay 窗口报 0（insets 已被父 frame 吃掉）。
     */
    private fun statusBarOffset(): Int {
        val root = content ?: return 0
        val window = params ?: return 0
        return runCatching {
            val loc = IntArray(2)
            root.getLocationOnScreen(loc)
            (loc[1] - window.y).coerceIn(0, dp(160))
        }.getOrDefault(0)
    }

    /** 导航栏高度：insets 报了真实值就用它，否则读系统维度资源（overlay 上 insets 常为 0）。 */
    private fun navigationBarHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bottom = runCatching {
                content?.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
            }.getOrDefault(0)
            if (bottom > 0) return bottom
        }
        return runCatching {
            val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (id > 0) resources.getDimensionPixelSize(id) else 0
        }.getOrDefault(0)
    }

    private fun bubbleRange(): IntRange {
        val size = dp(appearance.sizeDp)
        return 0..(contentAreaHeight() - size).coerceAtLeast(0)
    }

    /** 默认停靠点：内容区 62% 处（气泡垂直居中于该点）。只在用户没拖过时使用。 */
    private fun defaultBubbleY(): Int {
        val height = contentAreaHeight()
        val size = dp(appearance.sizeDp)
        return ((height * DEFAULT_DOCK_RATIO).toInt() - size / 2).coerceIn(bubbleRange())
    }

    private fun clampBubbleY(value: Int): Int = value.coerceIn(bubbleRange())

    /**
     * 键盘弹出时给气泡挑一侧让位：键盘上沿之上 或 屏幕顶侧，取离用户位置更近的一侧。
     * 只改窗口的临时 y，不写回 bubbleY —— 键盘收起后气泡回到用户原来的位置。
     */
    private fun imeAvoidY(desired: Int): Int {
        val keyboard = imeHeight
        val height = contentAreaHeight()
        if (keyboard <= 0 || keyboard >= height) return desired
        val size = dp(appearance.sizeDp)
        val aboveKeyboard = (height - keyboard - size - dp(8)).coerceIn(bubbleRange())
        val topSide = bubbleRange().first
        return if (abs(desired - aboveKeyboard) <= abs(desired - topSide)) aboveKeyboard else topSide
    }

    private fun restoreBubblePosition() {
        val prefs = getSharedPreferences(POSITION_PREFS, Context.MODE_PRIVATE)
        bubbleMoved = prefs.getBoolean(KEY_BUBBLE_MOVED, false)
        val saved = prefs.getInt(KEY_BUBBLE_Y, -1)
        bubbleY = if (bubbleMoved && saved >= 0) clampBubbleY(saved) else defaultBubbleY()
    }

    private fun persistBubblePosition(y: Int) {
        getSharedPreferences(POSITION_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BUBBLE_MOVED, true)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    /** 折叠态下把气泡放到 [用户位置 › 键盘避让] 算出的窗口 y；展开/进度态不管（窗口尺寸另有语义）。 */
    private fun applyBubblePosition(force: Boolean = false) {
        if (expanded || compactForTask) return
        val window = params ?: return
        val root = content ?: return
        bubbleY = clampBubbleY(if (bubbleY >= 0) bubbleY else defaultBubbleY())
        val next = imeAvoidY(bubbleY)
        if (force) android.util.Log.i("JarvisFloating", "bubble y=" + next + " user=" + bubbleY + " range=" + bubbleRange() + " ime=" + imeHeight)
        if (!force && window.y == next) return
        window.y = next
        runCatching { windowManager.updateViewLayout(root, window) }
    }

    private fun performGesture(action: FloatingGestureAction, task: String) {
        when (action) {
            FloatingGestureAction.None -> Unit
            FloatingGestureAction.Hide -> {
                content?.visibility = View.GONE
                hiddenUntil = System.currentTimeMillis() + appearance.hideSeconds * 1000L
                mainHandler.postDelayed({
                    if (System.currentTimeMillis() >= hiddenUntil) content?.visibility = View.VISIBLE
                }, appearance.hideSeconds * 1000L)
            }
            FloatingGestureAction.Rotate -> bubbleView?.animate()?.rotationBy(360f)?.setDuration(550)?.start()
            FloatingGestureAction.Task -> if (task.isNotBlank()) {
                expand()
                inputText?.setText(task)
                submitPrompt()
            } else FloatingAssistantController.reportLaunchFailure("请先在设置中填写该手势的任务内容")
        }
    }

    private fun stageVoiceResult(transcript: String) {
        attachWindow()
        collapse()
        pendingVoiceSubmit?.let(mainHandler::removeCallbacks)
        pendingVoiceText = transcript.take(4000)
        bubbleView?.contentDescription = "识别到：${transcript.take(80)}。单点修改，1 秒后自动执行"
        bubbleView?.animate()?.scaleX(1.12f)?.scaleY(1.12f)?.setDuration(180)?.start()
        val submit = Runnable {
            val text = pendingVoiceText ?: return@Runnable
            pendingVoiceText = null
            bubbleView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(180)?.start()
            bubbleView?.contentDescription = "Jarvis 悬浮助手。单点打开，长按语音输入"
            expand()
            inputText?.setText(text)
            submitPrompt()
        }
        pendingVoiceSubmit = submit
        mainHandler.postDelayed(submit, 1000)
    }

    private fun editPendingVoice() {
        pendingVoiceSubmit?.let(mainHandler::removeCallbacks)
        pendingVoiceSubmit = null
        val text = pendingVoiceText.orEmpty()
        pendingVoiceText = null
        bubbleView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(180)?.start()
        expand()
        inputText?.setText(text)
        inputText?.setSelection(text.length)
        focusChatInput()
    }

    private fun submitPrompt() {
        if (taskRunning) {
            statusText?.text = "上一条消息仍在执行，请稍后再发送。"
            markError("上一条消息仍在执行")
            return
        }
        val prompt = inputText?.text?.toString()?.trim().orEmpty()
        if (prompt.isBlank()) {
            statusText?.text = "请输入消息后再发送。"
            markError("请输入消息后再发送")
            return
        }
        if (prompt.length > 4000) {
            statusText?.text = "任务太长，请控制在 4000 字以内。"
            markError("任务太长，未发送")
            return
        }
        if (FloatingAssistantController.snapshot.busy) {
            statusText?.text = "正在执行其他任务，请稍后再发送。"
            markError("正在执行其他任务")
            return
        }
        inputText?.setText("")
        hideKeyboard()
        inputText?.clearFocus()
        clearError()
        taskRunning = true
        android.util.Log.i("JarvisFloating", "small-window message accepted (${prompt.length} chars)")
        voiceButton?.isEnabled = false
        chatButton?.isEnabled = false
        sendButton?.isEnabled = false
        appendHistory(true, prompt)
        resultText?.text = historyText()
        scope.launch {
            val response = runCatching {
                val task = RemoteTask(
                    id = UUID.randomUUID().toString(),
                    sessionId = "floating-local",
                    prompt = buildPromptWithHistory(prompt),
                    state = RemoteTaskState.RUNNING,
                    result = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                DeviceTaskCoordinator.run("悬浮窗对话") {
                    RemoteTaskAgent(this@FloatingAssistantService).run(task, allowDeviceControl = true, source = TaskSource.FLOATING)
                }
            }.getOrElse { error -> "任务失败：${error.message ?: error.javaClass.simpleName}" }
            mainHandler.post {
                appendHistory(false, response)
                taskRunning = false
                voiceButton?.isEnabled = true
                chatButton?.isEnabled = true
                sendButton?.isEnabled = true
                resultText?.text = historyText()
                statusText?.text = "本次结果已显示；可继续提问。"
                content?.let { root ->
                    val scroll = findScroll(root)
                    scroll?.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
    }

    private fun buildPromptWithHistory(prompt: String): String = buildString {
        if (history.size > 1) {
            appendLine("小窗对话最近消息：")
            history.dropLast(1).takeLast(10).forEach { (user, text) ->
                appendLine("${if (user) "用户" else "Jarvis"}：${text.take(1200)}")
            }
            appendLine("请延续上下文回答，并优先使用可用的 Jarvis 工具完成明确任务。")
        }
        append("用户当前请求：").append(prompt)
    }

    private fun appendHistory(user: Boolean, value: String) {
        history += user to value.take(1800)
        while (history.size > 12) history.removeAt(0)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, JSONArray().apply {
            history.forEach { (isUser, text) -> put(JSONObject().put("user", isUser).put("text", text)) }
        }.toString()).apply()
    }

    private fun loadHistory() {
        val json = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, "[]") ?: "[]"
        runCatching { JSONArray(json) }.getOrNull()?.let { items ->
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val text = item.optString("text").take(1800)
                if (text.isNotBlank()) history += item.optBoolean("user") to text
            }
        }
        while (history.size > 12) history.removeAt(0)
    }

    private fun historyText(): String = history.joinToString("\n\n") { (user, text) ->
        "${if (user) "你" else "Jarvis"}：$text"
    }

    private fun render(state: FloatingAssistantSnapshot) {
        syncTheme()
        // 主题切换/重组也会走 render：只有用户没拖过才回默认位，拖过的仅做区间 clamp，位置不回弹。
        if (!bubbleMoved) bubbleY = defaultBubbleY()
        applyBubblePosition()
        updateErrorBadge(state)
        if (state.busy && (state.hadAccessibilityOperation || state.operation.startsWith("accessibility.") || state.operation == "app.launch")) {
            compactProgress(state)
            return
        }
        if (state.taskLabel == "本机对话") {
            if (compactForTask || expanded) collapse()
            return
        }
        if (state.busy && state.taskLabel != "悬浮窗对话") {
            if (compactForTask || expanded) collapse()
            return
        }
        if (compactForTask) {
            collapse()
            if (state.taskLabel == "悬浮窗对话" && !state.busy) expand()
        }
        statusText?.text = when {
            state.busy -> "正在执行：${state.taskLabel}"
            state.result.isNotBlank() -> "任务已结束"
            else -> "就绪 · 语音 / 小窗对话 / 任务进度"
        }
        progressText?.text = when {
            state.busy && state.operation.isNotBlank() -> "● ${operationLabel(state.operation)}  ·  已完成 ${state.completedOperations}/${state.attemptedOperations} 项工具操作"
            state.busy -> "◌ 正在连接 Jarvis 智能体…"
            else -> ""
        }
        if (!state.busy && state.result.isNotBlank()) {
            resultText?.text = if (state.taskLabel == "悬浮窗对话") historyText()
                else listOf(historyText(), "Jarvis：${state.result}").filter { it.isNotBlank() }.joinToString("\n\n")
        }
        returnRow?.visibility = if (state.showReturnChoice) View.VISIBLE else View.GONE
        val returnPackage = state.returnPackage
        returnButton?.isEnabled = !returnPackage.isNullOrBlank()
        if (state.showReturnChoice && returnPackage.isNullOrBlank()) {
            returnButton?.text = "未找到原应用"
        } else returnButton?.text = "切回原应用"
        voiceButton?.isEnabled = !state.busy
        chatButton?.isEnabled = !state.busy
        sendButton?.isEnabled = !state.busy
        if (state.busy && state.taskLabel == "悬浮窗对话" && !expanded) expand()
    }

    private fun returnToPreviousApp() {
        val packageName = FloatingAssistantController.snapshot.returnPackage
        if (packageName.isNullOrBlank()) {
            FloatingAssistantController.reportLaunchFailure("没有记录到可返回的原应用")
            return
        }
        FloatingAssistantController.dismissReturnChoice()
        if (ForegroundAppTracker.launch(this, packageName)) {
            // Keep the assistant available for the next task, but collapse it so
            // the app the user chose to return to is unobstructed.
            collapse()
        } else {
            FloatingAssistantController.reportLaunchFailure("无法切回原应用；它可能已卸载或没有可恢复的入口")
        }
    }

    private fun openMainActivity(action: FloatingLaunchAction) {
        val target = Intent(this, com.jarvis.app.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(com.jarvis.app.MainActivity.EXTRA_FLOATING_ACTION, action.name)
        }
        runCatching { startActivity(target) }.onFailure {
            FloatingAssistantController.reportLaunchFailure("无法打开 Jarvis：${it.message ?: "系统拒绝"}")
        }
    }

    private fun openVoiceCapture() {
        FloatingAssistantController.rememberVoiceOrigin()
        val target = Intent(this, FloatingVoiceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(target) }.onFailure {
            FloatingAssistantController.clearVoiceOrigin()
            FloatingAssistantController.reportLaunchFailure("无法打开语音识别：${it.message ?: "系统拒绝"}")
        }
    }

    private fun focusChatInput() {
        val field = inputText ?: return
        setWindowFocusable(true)
        field.requestFocus()
        field.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val view = inputText ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setWindowFocusable(focusable: Boolean) {
        val root = content ?: return
        val window = params ?: return
        window.flags = if (focusable) {
            window.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            window.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { windowManager.updateViewLayout(root, window) }
    }

    private fun findScroll(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is android.view.ViewGroup) for (index in 0 until view.childCount) {
            findScroll(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun removeWindow() {
        content?.let { view -> runCatching { windowManager.removeView(view) } }
        content = null
        params = null
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        val colors = colors()
        this.text = text
        isAllCaps = false
        textSize = 11f
        setTextColor(colors.title)
        background = pressable(colors.buttonFill, 12, colors.ripple)
        setPadding(dp(4), 0, dp(4), 0)
        themedButtons += this
        setOnClickListener { action() }
    }

    /** 面板：1dp 描边代替 elevation 方影；折叠/进度态 root 会被换成透明 drawable，这里只管展开态。 */
    private fun panelDrawable(colors: FloatingThemeColors): GradientDrawable =
        rounded(colors.panel, 24, colors.bubbleRing)

    /** 气泡描边环：与气泡同尺寸、跟随 shape，垫在圆形裁切的头像下面，只露出外缘约 1dp。 */
    private fun bubbleRingDrawable(colors: FloatingThemeColors): GradientDrawable = GradientDrawable().apply {
        when (appearance.shape) {
            FloatingShape.Circle -> shape = GradientDrawable.OVAL
            FloatingShape.Rounded -> cornerRadius = dp((appearance.sizeDp * .28f).toInt()).toFloat()
            FloatingShape.Square -> cornerRadius = dp(5).toFloat()
        }
        setColor(0x00000000)
        setStroke(dp(2), colors.bubbleRing)
    }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

    /** 圆角 + 涟漪：RippleDrawable 的 mask 与内容同圆角，涟漪不会溢出到圆角外的直角区域。 */
    private fun pressable(fill: Int, radiusDp: Int, ripple: Int, strokeColor: Int? = null): Drawable =
        RippleDrawable(
            ColorStateList.valueOf(ripple),
            rounded(fill, radiusDp, strokeColor),
            rounded(0xffffffff.toInt(), radiusDp)
        )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun operationLabel(value: String): String = when (value) {
        "accessibility.inspect" -> "读取屏幕"
        "accessibility.click" -> "点击屏幕"
        "accessibility.input" -> "输入文字"
        "accessibility.global" -> "执行系统操作"
        "accessibility.perform" -> "执行无障碍操作"
        "accessibility.swipe" -> "滑动屏幕"
        "app.launch" -> "打开应用"
        "camera.capture" -> "调用相机"
        "ocr.read" -> "识别屏幕文字"
        "notification.read" -> "读取通知"
        "root.exec" -> "运行命令"
        "runtime.tools" -> "使用运行时工具"
        else -> value
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Jarvis 悬浮助手", NotificationManager.IMPORTANCE_LOW))
        val intent = Intent(this, com.jarvis.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 18792, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Jarvis 悬浮助手运行中")
            .setContentText("点按悬浮球语音下达任务，或打开小窗对话")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW = "com.jarvis.app.action.SHOW_FLOATING"
        const val ACTION_HIDE = "com.jarvis.app.action.HIDE_FLOATING"
        const val ACTION_OPEN_CHAT = "com.jarvis.app.action.OPEN_FLOATING_CHAT"
        const val ACTION_VOICE = "com.jarvis.app.action.FLOATING_VOICE"
        const val ACTION_MAIN_CHAT = "com.jarvis.app.action.FLOATING_MAIN_CHAT"
        const val ACTION_QUICK_TASK = "com.jarvis.app.action.FLOATING_QUICK_TASK"
        const val ACTION_VOICE_RESULT = "com.jarvis.app.action.FLOATING_VOICE_RESULT"
        const val ACTION_REFRESH = "com.jarvis.app.action.FLOATING_REFRESH"
        const val EXTRA_QUICK_TASK = "com.jarvis.app.extra.FLOATING_QUICK_TASK"
        private const val EXTRA_TASK_ID = "com.jarvis.app.extra.FLOATING_TASK_ID"
        private const val CHANNEL = "jarvis_floating"
        private const val NOTIFICATION_ID = 18792
        private const val PREFS = "jarvis_floating_chat"
        /** 主界面装配层写主题的 prefs 名与键（与 FloatingThemePalette 保持一致）。 */
        private const val CONFIG_PREFS = "jarvis_config"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_MODE_OVERRIDE = "mode_override"
        private const val KEY_HISTORY = "messages"
        /** 气泡停靠位置单独存，避免被设置页保存外观时覆盖。 */
        private const val POSITION_PREFS = "jarvis_floating_position"
        private const val KEY_BUBBLE_MOVED = "bubbleMoved"
        private const val KEY_BUBBLE_Y = "bubbleY"
        private const val DEFAULT_DOCK_RATIO = 0.62f
        /** 快照没有 error 位（FloatingAssistantController 不在本阶段改动范围内），只能按文案判定失败。 */
        private val ERROR_HINTS = listOf("失败", "错误", "无法", "未开启", "未找到", "超时", "拒绝", "异常", "请先", "error", "failed")

        fun show(context: Context) = start(context, ACTION_SHOW)
        fun refresh(context: Context) = start(context, ACTION_REFRESH)
        fun stageVoiceResult(context: Context, transcript: String) {
            val intent = Intent(context, FloatingAssistantService::class.java)
                .setAction(ACTION_VOICE_RESULT)
                .putExtra(EXTRA_QUICK_TASK, transcript)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { FloatingAssistantController.reportLaunchFailure("无法暂存语音识别结果：${it.message ?: it.javaClass.simpleName}") }
        }
        fun submitTask(context: Context, prompt: String) {
            val intent = Intent(context, FloatingAssistantService::class.java)
                .setAction(ACTION_QUICK_TASK)
                .putExtra(EXTRA_QUICK_TASK, prompt)
                .putExtra(EXTRA_TASK_ID, UUID.randomUUID().toString())
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { FloatingAssistantController.reportLaunchFailure("无法提交语音任务：${it.message ?: it.javaClass.simpleName}") }
        }
        fun hide(context: Context) {
            context.stopService(Intent(context, FloatingAssistantService::class.java))
        }

        private fun start(context: Context, action: String) {
            val intent = Intent(context, FloatingAssistantService::class.java).setAction(action)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { FloatingAssistantController.reportLaunchFailure("悬浮助手启动失败：${it.message ?: it.javaClass.simpleName}") }
        }
    }
}
