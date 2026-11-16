package com.eriknie.memorymate

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val Context.dataStore by preferencesDataStore(name = "settings")
val CALENDAR_NAME_KEY = stringPreferencesKey("calendar_name")

const val HOLD_MILLIS = 2000L

/** LCD-style font of the digital clock (same font as the iOS app). */
var digitalTypeface: android.graphics.Typeface? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        digitalTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.technology_bold)
        enableEdgeToEdge()
        setContent { MainScreen() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Calendar access
// ---------------------------------------------------------------------------------------------

data class CalendarEvent(val id: Long, val title: String, val startTime: Long, val endTime: Long, val allDay: Boolean)

suspend fun saveCalendarName(context: Context, name: String) {
    context.dataStore.edit { it[CALENDAR_NAME_KEY] = name }
}

suspend fun getSavedCalendarName(context: Context): String? = context.dataStore.data.first()[CALENDAR_NAME_KEY]

fun loadCalendars(context: Context): List<Pair<Int, String>> {
    val calendars = mutableListOf<Pair<Int, String>>()
    val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
    val cursor = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, CalendarContract.Calendars.VISIBLE + " = 1", null, null
    )
    cursor?.use {
        while (it.moveToNext()) calendars.add(it.getInt(0) to (it.getString(1) ?: ""))
    }
    return calendars
}

fun loadEvents(context: Context, calendarId: Int): List<CalendarEvent> {
    val now = System.currentTimeMillis()
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, now + 365L * 24 * 60 * 60 * 1000)
    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY
    )
    val events = mutableListOf<CalendarEvent>()
    try {
        context.contentResolver.query(
            builder.build(), projection, "${CalendarContract.Instances.CALENDAR_ID} = ?", arrayOf(calendarId.toString()),
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use {
            while (it.moveToNext() && events.size < AppSettings.EVENTS_RANGE.last) {
                events.add(CalendarEvent(it.getLong(0), it.getString(1) ?: "", it.getLong(2), it.getLong(3), it.getInt(4) == 1))
            }
        }
    } catch (_: SecurityException) {
    }
    return events
}

// ---------------------------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------------------------

private fun clockTime(date: Date, seconds: Boolean, is24: Boolean): String {
    val pattern = (if (is24) "HH:mm" else "h:mm") + (if (seconds) ":ss" else "") + (if (is24) "" else " a")
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

private fun weekdayName(date: Date): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(date).replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun mediumDate(date: Date): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(date)
private fun fullDate(date: Date): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(date)

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

@Composable
fun MainScreen() {
    val theme = AppSettings.colorStyle.theme
    // The layout is positioned absolutely, so keep it left-to-right in RTL languages.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .fillMaxSize()
                .background(theme.background)
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout)
        ) {
            MemoryMateContent(theme)
        }
    }
}

@Composable
private fun MemoryMateContent(theme: Theme) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val is24 = DateFormat.is24HourFormat(context)

    // Time and battery, refreshed every second
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPct by remember { mutableIntStateOf(-1) }
    var plugged by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (battery != null) {
                val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else -1
                plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            }
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }
    // Keep the screen on only while charging
    LaunchedEffect(plugged) {
        if (plugged) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Calendar permission and selection
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var calendars by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) calendars = loadCalendars(context)
    }
    LaunchedEffect(Unit) { if (!permissionGranted) launcher.launch(Manifest.permission.READ_CALENDAR) }
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            calendars = loadCalendars(context)
            val saved = getSavedCalendarName(context)
            val match = calendars.find { it.second == saved }
            selectedId = match?.first
            selectedName = match?.second
        }
    }

    // Events, reloaded every 30 seconds and whenever the calendar changes
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    LaunchedEffect(permissionGranted, selectedId) {
        val id = selectedId
        while (permissionGranted && id != null) {
            events = loadEvents(context, id)
            delay(30_000)
        }
        if (id == null) events = emptyList()
    }

    // Hint shown after a short tap on the calendar button
    var hintVisible by remember { mutableStateOf(false) }
    var hintCounter by remember { mutableIntStateOf(0) }
    LaunchedEffect(hintCounter) {
        if (hintCounter > 0) {
            hintVisible = true
            delay(3000)
            hintVisible = false
        }
    }
    val hintAlpha by animateFloatAsState(if (hintVisible) 1f else 0f, label = "hint")

    val mode = AppSettings.clockMode
    val clockStyle = AppSettings.clockStyle
    val analogSeconds = AppSettings.analogSeconds
    val digitalSeconds = AppSettings.digitalSeconds
    val eventCount = AppSettings.displayedEvents
    val textSize = AppSettings.textSize

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val portrait = hPx > wPx
        val margin = (min(wPx, hPx) * 0.02f).roundToInt().toFloat()

        // Clock side: top half (portrait) or left half (landscape), inset by the margin
        val cx0 = margin; val cy0 = margin
        val cw = (if (portrait) wPx else wPx / 2) - 2 * margin
        val ch = (if (portrait) hPx / 2 else hPx) - 2 * margin
        // Events side
        val ex0 = (if (portrait) 0f else wPx / 2) + margin / 2
        val ey0 = if (portrait) hPx / 2 else 0f
        val ew = (if (portrait) wPx else wPx / 2) - margin
        val eh = if (portrait) hPx / 2 else hPx

        val icon = max(40f * density.density, min(cw, ch) * 0.1f)

        // --- Clock side ---------------------------------------------------------------------
        Canvas(Modifier.fillMaxSize()) {
            val date = Date(now)
            val fonts = ClockText(drawContext.canvas.nativeCanvas)
            drawIntoCanvas { canvas ->
                val c = canvas.nativeCanvas
                // Clock + digital time + date
                when (mode) {
                    ClockMode.ANALOG ->
                        ClockFace.draw(c, cx0, cy0, cw, ch, clockStyle, theme, analogSeconds)
                    ClockMode.BOTH -> {
                        val clockH = ch * 0.66f
                        ClockFace.draw(c, cx0, cy0, cw, clockH, clockStyle, theme, analogSeconds)
                        val labelW = min(cw * 0.75f, clockH * 1.1f)
                        placeTexts(fonts, theme, date, digitalSeconds, is24, cx0, cw, cy0 + clockH + margin / 2,
                            ch * 0.14f, labelW, ch * 0.18f, cy0 + ch)
                    }
                    ClockMode.DIGITAL ->
                        placeTexts(fonts, theme, date, digitalSeconds, is24, cx0, cw, cy0 + ch * 0.22f,
                            ch * 0.36f, cw * 0.9f, ch * 0.22f, cy0 + ch)
                }
                // Calendar (settings) button, top left
                drawCalendarIcon(c, cx0, cy0, icon, theme)
                // Battery, top right
                val bx = cx0 + cw - icon / 2; val by = cy0 + icon / 2
                fonts.fillCircle(bx, by, icon / 2, theme.block.toArgb(), theme.border.toArgb(), 1f * density.density)
                fonts.text(if (batteryPct < 0) "--" else "$batteryPct%", bx, by, icon * 0.3f,
                    theme.battery.toArgb(), bold = true, maxWidth = icon * 0.86f)
            }
        }

        // Hint pill
        val hintW = 190.dp; val hintH = 56.dp
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(cx0.roundToInt(), (cy0 + icon + margin / 2).roundToInt()) }
                .size(hintW, hintH)
                .alpha(hintAlpha)
                .background(theme.block, RoundedCornerShape(12.dp))
                .border(1.dp, theme.accent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Hold for 2 seconds\nto open settings", color = theme.text, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        // Invisible touch target on top of the calendar icon: tap shows the hint, a 2 second hold opens settings
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(cx0.roundToInt(), cy0.roundToInt()) }
                .size(with(density) { icon.toDp() })
                .semantics { contentDescription = "Settings (hold for 2 seconds)" }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val result = withTimeoutOrNull(HOLD_MILLIS) { tryAwaitRelease() }
                        when (result) {
                            null -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hintVisible = false
                                calendars = if (permissionGranted) loadCalendars(context) else calendars
                                showSettings = true
                            }
                            true -> hintCounter++
                            else -> {}
                        }
                    })
                }
        )

        // --- Events -------------------------------------------------------------------------
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                drawEventBlocks(canvas.nativeCanvas, events, eventCount, textSize, theme, is24, ex0, ey0, ew, eh, density.density)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            theme = theme,
            onChooseCalendar = {
                showSettings = false
                if (permissionGranted) {
                    calendars = loadCalendars(context)
                    showChooser = true
                } else launcher.launch(Manifest.permission.READ_CALENDAR)
            },
            onDone = { showSettings = false }
        )
    }

    if (showChooser) {
        CalendarChooser(theme, calendars, selectedId,
            onSelect = { id, name ->
                selectedId = id; selectedName = name
                scope.launch { saveCalendarName(context, name) }
                showChooser = false
            },
            onCancel = { showChooser = false })
    }
}

// ---------------------------------------------------------------------------------------------
// Drawing helpers
// ---------------------------------------------------------------------------------------------

/** Small helper for centered, auto-fitting text and circles on a native canvas. */
private class ClockText(private val canvas: android.graphics.Canvas) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    fun text(s: String, cx: Float, cy: Float, size: Float, color: Int, bold: Boolean, maxWidth: Float = Float.MAX_VALUE,
             mono: Boolean = false) {
        paint.reset(); paint.isAntiAlias = true
        paint.typeface = if (mono && digitalTypeface != null) digitalTypeface else android.graphics.Typeface.create(
            if (mono) android.graphics.Typeface.MONOSPACE else android.graphics.Typeface.DEFAULT,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        paint.textSize = size
        val w = paint.measureText(s)
        if (w > maxWidth) paint.textSize = size * maxWidth / w
        paint.color = color
        paint.textAlign = android.graphics.Paint.Align.CENTER
        val fm = paint.fontMetrics
        canvas.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2, paint)
    }

    fun fillCircle(cx: Float, cy: Float, r: Float, fill: Int, stroke: Int, strokeWidth: Float) {
        paint.reset(); paint.isAntiAlias = true
        paint.color = fill
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = stroke
        canvas.drawCircle(cx, cy, r - strokeWidth / 2, paint)
    }

    fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, fill: Int, stroke: Int, strokeWidth: Float) {
        paint.reset(); paint.isAntiAlias = true
        paint.color = fill
        canvas.drawRoundRect(l, t, r, b, radius, radius, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = stroke
        val h = strokeWidth / 2
        canvas.drawRoundRect(l + h, t + h, r - h, b - h, radius, radius, paint)
    }
}

/** Digital time in a rounded block plus the date underneath. */
private fun placeTexts(
    f: ClockText, theme: Theme, date: Date, seconds: Boolean, is24: Boolean,
    areaX: Float, areaW: Float, timeY: Float, timeH: Float, width: Float, dateH: Float, bottom: Float
) {
    val midX = areaX + areaW / 2
    f.roundRect(midX - width / 2, timeY, midX + width / 2, timeY + timeH, 12f * 2.2f.coerceAtMost(timeH / 8), theme.block.toArgb(),
        theme.border.toArgb(), 2f)
    f.text(clockTime(date, seconds, is24), midX, timeY + timeH / 2, timeH * 0.78f, theme.text.toArgb(), bold = true,
        maxWidth = width * 0.94f, mono = true)
    val dateTop = timeY + timeH + 4f
    val h = min(dateH, bottom - dateTop)
    val size = dateH * 0.36f
    f.text(weekdayName(date), midX, dateTop + h * 0.27f, size, theme.text.toArgb(), bold = true, maxWidth = areaW)
    f.text(mediumDate(date), midX, dateTop + h * 0.73f, size, theme.text.toArgb(), bold = true, maxWidth = areaW)
}

/** Calendar icon: page with a header band and a grid of days, drawn in the theme colors. */
private fun drawCalendarIcon(c: android.graphics.Canvas, x: Float, y: Float, s: Float, theme: Theme) {
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val rad = s * 0.16f
    p.color = theme.block.toArgb()
    c.drawRoundRect(x, y, x + s, y + s, rad, rad, p)
    p.style = android.graphics.Paint.Style.STROKE
    p.strokeWidth = max(1f, s * 0.03f)
    p.color = theme.border.toArgb()
    c.drawRoundRect(x + p.strokeWidth / 2, y + p.strokeWidth / 2, x + s - p.strokeWidth / 2, y + s - p.strokeWidth / 2, rad, rad, p)
    p.style = android.graphics.Paint.Style.FILL
    p.color = theme.accent.toArgb()
    val save = c.save()
    c.clipRect(x, y, x + s, y + s * 0.3f)
    c.drawRoundRect(x, y, x + s, y + s * 0.5f, rad, rad, p)
    c.restoreToCount(save)
    p.color = theme.text.toArgb()
    val cell = s * 0.13f
    for (row in 0..2) for (col in 0..3) {
        val gx = x + s * 0.17f + col * s * 0.18f
        val gy = y + s * 0.42f + row * s * 0.17f
        c.drawRoundRect(gx, gy, gx + cell, gy + cell, cell * 0.25f, cell * 0.25f, p)
    }
}

/** The event blocks: date (accent), time (secondary), title (primary). Text that does not fit is scaled down to 60%. */
private fun drawEventBlocks(
    c: android.graphics.Canvas, events: List<CalendarEvent>, count: Int, textSizePct: Int, theme: Theme,
    is24: Boolean, x: Float, y: Float, w: Float, h: Float, dp: Float
) {
    val gap = max(4f * dp, h * 0.01f)
    val rowH = (h - gap) / count - gap
    val base = rowH * 0.2f * textSizePct / 100f
    val insetX = 16f * dp; val insetY = 8f * dp
    val f = ClockText(c)
    val corner = 12f * dp

    for (i in 0 until count) {
        val top = y + i * rowH + (i + 1) * gap
        f.roundRect(x, top, x + w, top + rowH, corner, theme.block.toArgb(), theme.border.toArgb(), dp)
        val event = events.getOrNull(i) ?: continue
        val availW = (w - 2 * insetX).roundToInt()
        val availH = rowH - 2 * insetY
        if (availW <= 0) continue

        val start = Date(event.startTime)
        val times = if (event.allDay) "All day" else
            clockTime(start, false, is24) + " - " + clockTime(Date(event.endTime), false, is24)
        var scale = 1f
        var layout = buildEventLayout(fullDate(start), times, event.title, base, scale, theme, availW)
        while (scale > 0.6f && layout.height > availH) {
            scale -= 0.05f
            layout = buildEventLayout(fullDate(start), times, event.title, base, scale, theme, availW)
        }
        val save = c.save()
        c.clipRect(x, top, x + w, top + rowH)
        c.translate(x + insetX, top + insetY)
        layout.draw(c)
        c.restoreToCount(save)
    }
}

private fun buildEventLayout(
    date: String, times: String, title: String, base: Float, scale: Float, theme: Theme, width: Int
): android.text.StaticLayout {
    val sb = android.text.SpannableStringBuilder()
    fun line(s: String, size: Float, bold: Boolean, color: Color) {
        val start = sb.length
        sb.append(s)
        sb.setSpan(android.text.style.AbsoluteSizeSpan(max(1f, size * scale).roundToInt(), false), start, sb.length, 0)
        sb.setSpan(android.text.style.ForegroundColorSpan(color.toArgb()), start, sb.length, 0)
        sb.setSpan(android.text.style.StyleSpan(if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL),
            start, sb.length, 0)
    }
    line(date + "\n", base * 0.85f, true, theme.accent)
    line(times + "\n", base * 0.85f, false, theme.secondary)
    line(title, base * 1.15f, true, theme.text)
    return android.text.StaticLayout.Builder.obtain(sb, 0, sb.length, android.text.TextPaint(), width)
        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
        .build()
}

// ---------------------------------------------------------------------------------------------
// Settings dialog
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(items: List<T>, selected: T, theme: Theme, title: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (item in items) {
            val on = item == selected
            Text(
                title(item),
                color = if (on) theme.background else theme.text,
                fontSize = 15.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) theme.accent else theme.block)
                    .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(10.dp))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun Section(name: String, theme: Theme, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, color = theme.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, theme: Theme, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.background, checkedTrackColor = theme.accent,
                uncheckedThumbColor = theme.secondary, uncheckedTrackColor = theme.block, uncheckedBorderColor = theme.border
            )
        )
    }
}

@Composable
private fun StepButton(label: String, theme: Theme, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, color = if (enabled) theme.text else theme.border, fontSize = 22.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(theme.block)
            .border(1.dp, theme.border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun SettingsDialog(theme: Theme, onChooseCalendar: () -> Unit, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .heightIn(max = 820.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.background)
                .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Settings", color = theme.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Choose calendar", color = theme.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onChooseCalendar() }.padding(vertical = 4.dp)
            )
            Section("Color style", theme) {
                Choice(ColorStyle.entries, AppSettings.colorStyle, theme, { it.title }) { AppSettings.updateColorStyle(it) }
            }
            Section("Clock", theme) {
                Choice(ClockMode.entries, AppSettings.clockMode, theme, { it.title }) { AppSettings.updateClockMode(it) }
            }
            Section("Analog clock style", theme) {
                Choice(ClockStyle.entries, AppSettings.clockStyle, theme, { it.title }) { AppSettings.updateClockStyle(it) }
            }
            Section("Show seconds", theme) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRow("Analog clock", AppSettings.analogSeconds, theme) { AppSettings.updateAnalogSeconds(it) }
                    SwitchRow("Digital clock", AppSettings.digitalSeconds, theme) { AppSettings.updateDigitalSeconds(it) }
                }
            }
            Section("Displayed events", theme) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${AppSettings.displayedEvents}", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 60.dp))
                    StepButton("−", theme, AppSettings.displayedEvents > AppSettings.EVENTS_RANGE.first) {
                        AppSettings.updateDisplayedEvents(AppSettings.displayedEvents - 1)
                    }
                    StepButton("+", theme, AppSettings.displayedEvents < AppSettings.EVENTS_RANGE.last) {
                        AppSettings.updateDisplayedEvents(AppSettings.displayedEvents + 1)
                    }
                }
            }
            Section("Event text size", theme) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${AppSettings.textSize}%", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 60.dp))
                    Slider(
                        value = AppSettings.textSize.toFloat(),
                        onValueChange = { AppSettings.updateTextSize((it / 10).roundToInt() * 10) },
                        valueRange = AppSettings.TEXT_SIZE_RANGE.first.toFloat()..AppSettings.TEXT_SIZE_RANGE.last.toFloat(),
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accent, activeTrackColor = theme.accent,
                            inactiveTrackColor = theme.border, activeTickColor = theme.background,
                            inactiveTickColor = theme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Done", color = theme.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onDone() }.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CalendarChooser(
    theme: Theme, calendars: List<Pair<Int, String>>, selectedId: Int?,
    onSelect: (Int, String) -> Unit, onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.background)
                .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Choose calendar", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (calendars.isEmpty()) {
                    Text("No calendars found. Allow calendar access for MemoryMate in the system settings.",
                        color = theme.secondary, fontSize = 16.sp)
                }
                for ((id, name) in calendars) {
                    val on = id == selectedId
                    Text(
                        name, color = if (on) theme.background else theme.text, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) theme.accent else theme.block)
                            .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(10.dp))
                            .clickable { onSelect(id, name) }
                            .padding(14.dp)
                    )
                }
            }
            Text(
                "Cancel", color = theme.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onCancel() }.padding(vertical = 8.dp)
            )
        }
    }
}
