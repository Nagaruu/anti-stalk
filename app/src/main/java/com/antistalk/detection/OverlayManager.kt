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

    /** Follows the same Sáng/Tối/Theo hệ thống setting as the in-app theme. */
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
        } catch (_: Exception) { false }
    }

    fun openOverlaySettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    @SuppressLint("SetTextI18n")
    fun show(
        ctx: Context, eventId: Long, personName: String, packageName: String,
        trigger: String, confidence: String, countToday: Int
    ) {
        val appCtx = ctx.applicationContext
        // Real triggers arrive from Dispatchers.IO (see StalkAccessibilityService
        // .fireTrigger). WindowManager.addView must run on a Looper thread — on a
        // background thread it throws, which used to be swallowed by `catch`,
        // so a perfectly matched roast never appeared. The TEST button worked
        // only because it is clicked on the main thread.
        mainHandler.post {
            showNow(appCtx, eventId, personName, packageName, trigger, confidence, countToday)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showNow(
        appCtx: Context, eventId: Long, personName: String, packageName: String,
        trigger: String, confidence: String, countToday: Int
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
        val roast = RoastBank.pick(level, countToday.coerceAtLeast(1), personName, vi)
        val dark = isDarkOverlay(appCtx)
        val cardBg = if (dark) "#1E1B30" else "#FFFFFF"
        val titleFg = if (dark) "#F2EFFA" else "#14101F"
        val bodyFg = if (dark) "#B9B3CC" else "#4A4458"

        val manager = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = manager

        val bg = FrameLayout(appCtx).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            isClickable = true
        }
        val card = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(cardBg))
            setPadding(56, 56, 56, 56)
        }
        val emoji = TextView(appCtx).apply { text = "😏"; textSize = 40f }
        val title = TextView(appCtx).apply {
            text = if (vi) "Lại tìm người ta à?" else "Looking them up again?"
            textSize = 24f
            setTextColor(Color.parseColor(titleFg))
        }
        val body = TextView(appCtx).apply {
            text = roast + "\n" + if (vi) "Lần thứ ${countToday.coerceAtLeast(1)} hôm nay · $personName"
            else "#${countToday.coerceAtLeast(1)} today · $personName"
            textSize = 15f
            setTextColor(Color.parseColor(bodyFg))
        }
        val btnOut = Button(appCtx).apply { text = if (vi) "THÔI, TÔI ĐI RA" else "NAH, I'M OUT" }
        val btnStay = Button(appCtx).apply { text = if (vi) "TÔI VẪN MUỐN XEM" else "I STILL WANT TO SEE" }

        btnOut.setOnClickListener {
            scope.launch {
                try { AntiStalkRepository(appCtx).setDecision(eventId, "STOPPED") } catch (_: Exception) { }
            }
            hide()
        }
        btnStay.setOnClickListener {
            scope.launch {
                try { AntiStalkRepository(appCtx).setDecision(eventId, "CONTINUED") } catch (_: Exception) { }
            }
            hide()
        }
        card.addView(emoji); card.addView(title); card.addView(body)
        card.addView(btnOut); card.addView(btnStay)
        val lpCard = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = 48; rightMargin = 48; gravity = Gravity.CENTER }
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
