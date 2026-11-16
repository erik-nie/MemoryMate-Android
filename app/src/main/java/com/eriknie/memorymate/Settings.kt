package com.eriknie.memorymate

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Colors for one color style (same values as the iOS app). */
class Theme(
    bg: Long, block: Long, border: Long,
    text: Long, secondary: Long, accent: Long,
    second: Long, warning: Long,
    hours: Long? = null, minutes: Long? = null,
    val isDark: Boolean
) {
    val background = Color(0xFF000000 or bg)
    val block = Color(0xFF000000 or block)
    val border = Color(0xFF000000 or border)
    val text = Color(0xFF000000 or text)
    val secondary = Color(0xFF000000 or secondary)
    val accent = Color(0xFF000000 or accent)
    val secondHand = Color(0xFF000000 or second)
    val warning = Color(0xFF000000 or warning)
    val hourHand = Color(0xFF000000 or (hours ?: text))
    val minuteHand = Color(0xFF000000 or (minutes ?: accent))
    val tick = this.secondary
    /** Battery indicator: always red (a brighter red on dark backgrounds, a darker one on light backgrounds). */
    val battery = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)

    companion object {
        val midnight = Theme(0x050535, 0x202050, 0x3A3A7A, 0xFFFFFF, 0xC4C4E8, 0xFFCF09, 0xFF5555, 0xFFB020,
            hours = 0x60C93F, minutes = 0xFFCF09, isDark = true)
        val daylight = Theme(0xEEF1F6, 0xFFFFFF, 0xC5CCD8, 0x111827, 0x4B5563, 0x1D4ED8, 0xDC2626, 0xB45309, isDark = false)
        val forest = Theme(0x0B1F17, 0x153A2B, 0x2A6048, 0xFFFFFF, 0xB7D8C6, 0x7BE0A4, 0xFF6B5E, 0xFFB020, isDark = true)
        val amber = Theme(0x1A1208, 0x33240F, 0x5C4220, 0xFFF4E0, 0xE8C99A, 0xFFB020, 0xFF5A3C, 0xFF8A3D, isDark = true)
        val contrast = Theme(0x000000, 0x141414, 0xFFFFFF, 0xFFFFFF, 0xE0E0E0, 0xFFEB3B, 0xFF5252, 0xFF9800, isDark = true)
    }
}

enum class ColorStyle(val title: String, val theme: Theme) {
    MIDNIGHT("Midnight", Theme.midnight),
    DAYLIGHT("Daylight", Theme.daylight),
    FOREST("Forest", Theme.forest),
    AMBER("Amber", Theme.amber),
    CONTRAST("Contrast", Theme.contrast)
}

enum class ClockMode(val title: String) { BOTH("Both"), ANALOG("Analog"), DIGITAL("Digital") }

enum class ClockStyle(val title: String) {
    CLASSIC("Classic"), SLIM("Slim"), RAILWAY("Railway"), ROMANTIC("Romantic"), MINIMAL("Minimal"), MODERN("Modern")
}

/** All user settings. Values are observable by Compose and persisted in SharedPreferences. */
object AppSettings {
    val EVENTS_RANGE = 3..10
    val TEXT_SIZE_RANGE = 50..140
    const val DEFAULT_EVENTS = 5
    const val DEFAULT_TEXT_SIZE = 120

    private lateinit var prefs: SharedPreferences

    var colorStyle by mutableStateOf(ColorStyle.MIDNIGHT); private set
    var clockMode by mutableStateOf(ClockMode.BOTH); private set
    var clockStyle by mutableStateOf(ClockStyle.CLASSIC); private set
    var analogSeconds by mutableStateOf(true); private set
    var digitalSeconds by mutableStateOf(true); private set
    var displayedEvents by mutableIntStateOf(DEFAULT_EVENTS); private set
    var textSize by mutableIntStateOf(DEFAULT_TEXT_SIZE); private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        colorStyle = enumOf(prefs.getString("colorStyle", null), ColorStyle.MIDNIGHT)
        clockStyle = enumOf(prefs.getString("clockStyle", null), ClockStyle.CLASSIC)
        // displayMode: 0 = both, 1 = analog, 2 = digital (same as earlier versions)
        clockMode = ClockMode.entries[prefs.getInt("displayMode", 0).coerceIn(0, 2)]
        analogSeconds = prefs.getBoolean("analogSeconds", true)
        digitalSeconds = prefs.getBoolean("digitalSeconds", true)
        displayedEvents = prefs.getInt("eventAmount", DEFAULT_EVENTS).coerceIn(EVENTS_RANGE)
        textSize = prefs.getInt("textSizePercent", DEFAULT_TEXT_SIZE).coerceIn(TEXT_SIZE_RANGE)
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    fun updateColorStyle(v: ColorStyle) { colorStyle = v; prefs.edit().putString("colorStyle", v.name).apply() }
    fun updateClockStyle(v: ClockStyle) { clockStyle = v; prefs.edit().putString("clockStyle", v.name).apply() }
    fun updateClockMode(v: ClockMode) { clockMode = v; prefs.edit().putInt("displayMode", v.ordinal).apply() }
    fun updateAnalogSeconds(v: Boolean) { analogSeconds = v; prefs.edit().putBoolean("analogSeconds", v).apply() }
    fun updateDigitalSeconds(v: Boolean) { digitalSeconds = v; prefs.edit().putBoolean("digitalSeconds", v).apply() }
    fun updateDisplayedEvents(v: Int) {
        displayedEvents = v.coerceIn(EVENTS_RANGE); prefs.edit().putInt("eventAmount", displayedEvents).apply()
    }
    fun updateTextSize(v: Int) {
        textSize = v.coerceIn(TEXT_SIZE_RANGE); prefs.edit().putInt("textSizePercent", textSize).apply()
    }
}
