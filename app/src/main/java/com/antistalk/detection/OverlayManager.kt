package com.antistalk.detection

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.antistalk.core.RoastBank
import com.antistalk.data.AntiStalkRepository
import com.antistalk.ui.intervention.InterventionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * System overlay built with classic Views (not Compose) so it works from a
 * Service without a LifecycleOwner. The same messages live in RoastBank and
 * the same look is mirrored by InterventionOverlay composable in-app.
 */
object OverlayManager {
    private var root: FrameLayout? = null
    private var wm: WindowManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoHideJob: Job? = null
    var current: InterventionState? = null
        private set

    /** Overlay never stays forever: a stuck card would block every later show(). */
    const val AUTO_HIDE_MS = 45_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    fun canDrawOverlays(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(ctx) else true

    fun isShowing(): Boolean = root != null

    /** Follows the same Sáng / Tối / Hệ thống setting as the in-app theme. */
    private fun isDarkOverlay(appCtx: Context): Boolean {
        return try {
            when (appCtx.getSharedPreferences("antistalk", Context.MODE_PRIVATE)
                .getString("theme_mode", "system")) {
                "dark" -> true
                "light" -> false
                else -> (appCtx.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        } catch (_: Exception) { true }
    }

    fun openOverlaySettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    @SuppressLint("SetTextI18n")
    fun show(
        ctx: Context, eventId: Long, personName: String, packageName: String,
        trigger: String, confidence: String, countToday: Int, isRepeatAttempt: Boolean = false
    ) {
        val appCtx = ctx.applicationContext
        // Real triggers arrive from Dispatchers.IO (see StalkAccessibilityService
        // .fireTrigger). WindowManager.addView must run on a Looper thread — on a
        // background thread it throws, which used to be swallowed by `catch`,
        // so a perfectly matched roast never appeared. The TEST button worked
        // only because it is clicked on the main thread.
        mainHandler.post {
            showNow(appCtx, eventId, personName, packageName, trigger, confidence, countToday, isRepeatAttempt)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showNow(
        appCtx: Context, eventId: Long, personName: String, packageName: String,
        trigger: String, confidence: String, countToday: Int, isRepeatAttempt: Boolean = false
    ) {
        if (!canDrawOverlays(appCtx)) {
            DetectionLog.add("overlay", "show", personName.take(24), "no-overlay-permission")
            return
        }
        // A previous card never got dismissed: replace it instead of
        // blocking every later roast forever (root != null used to return).
        if (root != null) hideNow()
        val vi = Locale.getDefault().language == "vi"
        current = InterventionState(eventId, personName, packageName, trigger, confidence, countToday)

        val prefs = appCtx.getSharedPreferences("antistalk", Context.MODE_PRIVATE)
        val level = prefs.getInt("roast_level", 2)
        val roast = RoastBank.pick(level, countToday.coerceAtLeast(1), personName, vi, isRepeatAttempt)
        val dark = isDarkOverlay(appCtx)
        val cardBg = if (dark) "#1A1628" else "#FFFFFF"
        val cardBorder = if (dark) "#3D3559" else "#E5DFD7"
        val titleFg = if (dark) "#F5F3FF" else "#2C2420"
        val bodyFg = if (dark) "#A89FC0" else "#6B5E56"
        val countFg = if (dark) "#DDD6FE" else "#5B8A6A"
        val hintFg = if (dark) "#6F6787" else "#9B8E85"
        val primaryBtnBg = if (dark) "#8B5CF6" else "#5B8A6A"

        val manager = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = manager

        val bg = FrameLayout(appCtx).apply {
            setBackgroundColor(Color.parseColor("#B30D0B14"))
            isClickable = true
        }
        // Play-safe: tap outside the card dismisses without a decision (same as
        // auto-hide). Blank person keeps only the 2s transition suppress so the
        // same screen doesn't instantly re-popup; cooldowns still apply.
        bg.setOnClickListener {
            try {
                StalkAccessibilityService.onUserDismiss("", "", isLeave = true)
            } catch (_: Exception) { }
            DetectionLog.add("overlay", "tap-outside", personName.take(24), "dismissed")
            hide()
        }

        val cardShape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 64f // ~24dp
            setColor(Color.parseColor(cardBg))
            setStroke(2, Color.parseColor(cardBorder))
        }

        val card = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            background = cardShape
            setPadding(64, 56, 64, 56)
            // Consume touches on the card so tap-outside (bg) doesn't fire
            // when the user taps body text (TextViews aren't clickable).
            isClickable = true
        }

        val topAccent = android.view.View(appCtx).apply {
            val h = (if (dark) 3 else 5) * appCtx.resources.displayMetrics.density.toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h).apply {
                bottomMargin = (14 * appCtx.resources.displayMetrics.density).toInt()
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 8f
                if (dark) {
                    setColor(Color.parseColor("#8B5CF6"))
                } else {
                    colors = intArrayOf(
                        Color.parseColor("#5B8A6A"),
                        Color.parseColor("#E8795A"),
                        Color.parseColor("#7B6FBF")
                    )
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                }
            }
        }
        card.addView(topAccent)

        val emoji = TextView(appCtx).apply {
            text = if (dark) "😏" else "🧠"
            textSize = 38f
        }
        val title = TextView(appCtx).apply {
            text = if (vi) {
                if (dark) "Lại tìm người ta à?" else "Khoan đã bạn ơi!"
            } else "Looking them up again?"
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(titleFg))
        }
        val body = TextView(appCtx).apply {
            text = roast
            textSize = 15f
            setTextColor(Color.parseColor(bodyFg))
            setLineSpacing(6f, 1.15f)
        }
        val count = TextView(appCtx).apply {
            text = if (vi) "Lần thứ ${countToday.coerceAtLeast(1)} hôm nay · $personName"
            else "#${countToday.coerceAtLeast(1)} today · $personName"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(countFg))
        }

        val btnOutShape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 48f
            setColor(Color.parseColor(primaryBtnBg))
        }
        val btnOut = Button(appCtx).apply {
            text = if (vi) {
                if (dark) "Thôi, tôi đi ra" else "🌿 Thôi, tôi đi ra!"
            } else "Nah, I'm out"
            background = btnOutShape
            setTextColor(Color.WHITE)
            textSize = 14f
            isAllCaps = false
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(32, 28, 32, 28)
        }

        val btnStayShape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 48f
            setColor(Color.TRANSPARENT)
            setStroke(2, Color.parseColor(if (dark) "#4A4268" else "#D5CEC7"))
        }
        val btnStay = Button(appCtx).apply {
            text = if (vi) "Tôi vẫn muốn xem" else "I still want to see"
            background = btnStayShape
            setTextColor(Color.parseColor(if (dark) "#A89FC0" else "#6B5E56"))
            textSize = 13f
            isAllCaps = false
            setPadding(32, 22, 32, 22)
        }

        val hint = TextView(appCtx).apply {
            text = if (vi) "Không cấm bạn. Chỉ bắt bạn nghĩ một lần." else "Not blocking you. Just one second of pause."
            textSize = 12f
            setTextColor(Color.parseColor(hintFg))
            gravity = Gravity.CENTER
        }

        btnOut.setOnClickListener {
            val s = current
            val pName = s?.personName ?: personName
            val pPkg = s?.packageName ?: packageName
            scope.launch {
                try { AntiStalkRepository(appCtx).setDecision(eventId, "STOPPED") } catch (_: Exception) { }
            }
            StalkAccessibilityService.onUserDismiss(pName, pPkg, isLeave = true)
            hide()
            // Pull user out of the social app by returning to Android Home Screen
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appCtx.startActivity(homeIntent)
            } catch (_: Exception) { }
        }
        btnStay.setOnClickListener {
            val s = current
            val pName = s?.personName ?: personName
            val pPkg = s?.packageName ?: packageName
            scope.launch {
                try { AntiStalkRepository(appCtx).setDecision(eventId, "CONTINUED") } catch (_: Exception) { }
            }
            StalkAccessibilityService.onUserDismiss(pName, pPkg, isLeave = false)
            hide()
        }

        fun marginLp(top: Int, bottom: Int) = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = top; bottomMargin = bottom }

        card.addView(emoji, marginLp(0, 12))
        card.addView(title, marginLp(0, 12))
        card.addView(body, marginLp(0, 10))
        card.addView(count, marginLp(0, 24))
        card.addView(btnOut, marginLp(0, 10))
        card.addView(btnStay, marginLp(0, 16))
        card.addView(hint, marginLp(0, 0))

        val lpCard = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = 52; rightMargin = 52; gravity = Gravity.CENTER }
        bg.addView(card, lpCard)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try {
            manager.addView(bg, params)
            root = bg
            autoHideJob?.cancel()
            autoHideJob = scope.launch {
                delay(AUTO_HIDE_MS)
                hide()
            }
        } catch (e: Exception) {
            current = null; wm = null
            DetectionLog.add("overlay", "show", personName.take(24), "addView-failed:${e.javaClass.simpleName}")
        }
    }

    /** Thread-safe: hops to the main thread if needed (WM views live there). */
    fun hide() {
        if (Looper.myLooper() == Looper.getMainLooper()) hideNow()
        else mainHandler.post { hideNow() }
    }

    private fun hideNow() {
        autoHideJob?.cancel()
        autoHideJob = null
        try { root?.let { wm?.removeView(it) } } catch (_: Exception) { }
        root = null; wm = null; current = null
    }
}
