@file:Suppress("LanguageDetectionInspection")

package com.eriknie.memorymate

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.provider.CalendarContract
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Calendar
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.nativeCanvas
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.text.SimpleDateFormat
import kotlin.math.max
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import android.content.ContentUris
import androidx.compose.material3.Slider
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds


const val SHOW_VERSION = true
val COLOR_BACKGROUND = Color(0xff050535) // main screen background
val COLOR_BACKGROUND_DIGITAL = Color(0x88000000) // main screen background

val COLOR_SECONDS = Color(0xFFFF1108)
val COLOR_MINUTES = Color(0xFFFFCF09)
val COLOR_HOURS = Color(0xFF60C93F)
val COLOR_BACK = Color(0xFF252555)
val COLOR_TITLE = Color(0xFF45457F)
val COLOR_HOUR_TICKS = Color(0xFFFFFFFF)
val COLOR_MINUTE_TICKS = Color(0xFFAAAAAA)
val COLOR_SHADOW = Color(0x50000000)
const val EVENT_AMOUNT_MIN = 3
const val EVENT_AMOUNT_MAX = 10
const val EVENT_AMOUNT_DEFAULT = 5

// DataStore extensie
val Context.dataStore by preferencesDataStore(name = "settings")
val CALENDAR_NAME_KEY = stringPreferencesKey("calendar_name")

const val doubleClickThreshold = 250 // Time threshold for double-click in milliseconds

var globalBuildInfo = ""

class MainActivity : ComponentActivity() {

    //private lateinit var wakeLock: PowerManager.WakeLock


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verkrijg een PowerManager instantie

//        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
//        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyApp::WakeLockTag")

        // Registreer BroadcastReceiver om batterijstatus te volgen
//        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
//        registerReceiver(batteryReceiver, filter)

        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
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
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }


    //BroadcastReceiver om batterijstatus te detecteren
//    private val batteryReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
//
//            if (status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                status == BatteryManager.BATTERY_STATUS_FULL) {
//                // Het apparaat is aan de oplader
//                if (!wakeLock.isHeld) {
//                    wakeLock.acquire(10 * 60 * 1000L /*10 minuten*/)
//                }
//            } else {
//                // Het apparaat is niet aan de oplader
//                if (wakeLock.isHeld) {
//                    wakeLock.release()
//                }
//            }
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // Stop de receiver en release de WakeLock
//        unregisterReceiver(batteryReceiver)
//        if (wakeLock.isHeld) {
//            wakeLock.release()
//        }
//    }



}



@Preview(showBackground = true)
@Composable
fun MainScreen() {
    // The layout is positioned absolutely (calendar icon top-left, battery top-right),
    // so keep it left-to-right in RTL languages; text itself still follows its own script.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = Modifier.fillMaxSize(), // Zorgt ervoor dat de surface de hele ruimte opvult
            color = COLOR_BACKGROUND  // Achtergrondkleur instellen
        ) {
            ClockDisplay()
            DisplayCalendar()
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MemoryMateTheme {
//        Greeting("Android")
//    }
//}

//
//@Composable
//fun KeepScreenOnWhenCharging() {
//    val context = LocalContext.current
//    val window = (context as? Activity)?.window
//
//    // Check oplaadstatus bij opstarten
//    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
//    val isInitiallyCharging = batteryManager.isCharging
//
//    // Houd de huidige power status bij
//    val isCharging = remember { mutableStateOf(isInitiallyCharging) }
//
//    LaunchedEffect(isCharging.value) {
//        if (isCharging.value) {
//            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//            println("KeepScreenOnWhenCharging --> ON (Startup)")
//        } else {
//            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//            println("KeepScreenOnWhenCharging --> OFF (Startup)")
//        }
//    }
//
//    DisposableEffect(context) {
//        val receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                when (intent?.action) {
//                    Intent.ACTION_POWER_CONNECTED -> {
//                        isCharging.value = true
//                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//                        println("KeepScreenOnWhenCharging --> ON")
//                    }
//                    Intent.ACTION_POWER_DISCONNECTED -> {
//                        isCharging.value = false
//                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//                        println("KeepScreenOnWhenCharging --> OFF")
//                    }
//                }
//            }
//        }
//
//        val filter = IntentFilter().apply {
//            addAction(Intent.ACTION_POWER_CONNECTED)
//            addAction(Intent.ACTION_POWER_DISCONNECTED)
//        }
//
//        context.registerReceiver(receiver, filter)
//
//        onDispose {
//            context.unregisterReceiver(receiver)
//        }
//    }
//}

// ✅ **Helper functie om te checken of de telefoon al aan het opladen is**
//fun BatteryManager.isCharging(): Boolean {
//    val status = this.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
//    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
//}


///////////////////////////////////////////////////////////////////////////////////////////////////
// CLOCK STUFF
///////////////////////////////////////////////////////////////////////////////////////////////////
@Composable
fun BuildInfoText() {
    val context = LocalContext.current

   // KeepScreenOnWhenCharging() // seems not to work stand-alone, but might help an certain devices

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    val versionName = packageInfo.versionName

    val buildTimeMillis = BuildConfig.BUILD_TIME.toLongOrNull() ?: 0L
    val formattedBuildDate = Instant.ofEpochMilli(buildTimeMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    globalBuildInfo = "v$versionName ($formattedBuildDate)"

    /* do not display
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(200.dp))
        Text(text = formattedBuildTime, fontSize = 16.sp, color = Color(0x70FFFFFF))
        Text(text = "Version: $versionName ($versionCode)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0x70FFFFFF))
    }
    */

}
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ClockDisplay() {
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    val is24HourFormat = DateFormat.is24HourFormat(context)
    //println(is24HourFormat)
    var currentTime12 by remember { mutableStateOf(getCurrentTime(is24hourFormat = false)) }
    var currentTime24 by remember { mutableStateOf(getCurrentTime(is24hourFormat = true)) }
    var parsableTime by remember { mutableStateOf(getCurrentTimeParsable()) }

    var batteryPct by remember { mutableIntStateOf(0) }
    var chargingStatus by remember { mutableStateOf(false) }
    var displayMode by remember { mutableIntStateOf(0) }

    // if charging: keep on
    if (chargingStatus) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    displayMode = sharedPreferences.getInt("displayMode", 0)

    DisposableEffect(context) {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                // Bereken batterijpercentage
                if (level != -1 && scale != -1) {
                    batteryPct = (level * 100) / scale
                }

                // Bepaal oplaadstatus
                chargingStatus = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> true //"Opladen"
                    BatteryManager.BATTERY_STATUS_FULL -> true //"Volledig opgeladen"
                    //BatteryManager.BATTERY_STATUS_DISCHARGING -> false //"Ontladen"
                    //BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false //"Niet aan het opladen"
                    else -> false //"Onbekend"
                }
            }
        }
        context.registerReceiver(batteryReceiver, intentFilter)

        onDispose {
            context.unregisterReceiver(batteryReceiver)
        }
    }

    val currentTime = if (is24HourFormat) currentTime24 else currentTime12
    // parse from an ASCII-digit string; localized digits (e.g. Arabic) would crash toInt()
    val (hour, minute, sec) = getTimeFromString(parsableTime)
    val configuration = LocalConfiguration.current
    val isPortrait = (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds) // Wacht 1 seconde
            currentTime12 = getCurrentTime(is24hourFormat = false) // Update de tijd
            currentTime24 = getCurrentTime(is24hourFormat = true) // Update de tijd
            parsableTime = getCurrentTimeParsable()
        }
    }

    // displayMode ---> 0:both, 1:analog 2:digital
    val paddings = floatArrayOf(0.2f, 0f, 0f)
    val bottomPaddingFactor = paddings[displayMode]

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var lastClickTime by remember { mutableLongStateOf(0L) }
        BoxWithConstraints(
            modifier = Modifier
                //.background(COLOR_BACKGROUND)
                .clickable {
                    val nowTime = System.currentTimeMillis()
                    if (nowTime - lastClickTime <= doubleClickThreshold) {
                        displayMode = if  (displayMode < 2) displayMode+1 else 0
                        editor.putInt("displayMode", displayMode)
                        editor.apply() // Opslaan
                    }
                    lastClickTime = nowTime
                }
                .then(
                    if (isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f) // Bovenste helft in portret
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f) // Linker helft in landschap
                    }
                ),
            contentAlignment = Alignment.Center // Tekst in het midden
            ) {
                val bottomPadding = maxHeight * bottomPaddingFactor
                Box(
                    modifier = Modifier
                        //.background(Color.Red)
                        .fillMaxWidth()
                        .padding(bottom = bottomPadding)

                ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (displayMode<2) {
                        drawClockFace()
                        // Ticks
                        for (i in 1..60) {
                            drawClockHand(angle = i*(360f/60f),startLength = 0.97f,handLength = 1f,strokeWidth = 0.005f,color = COLOR_MINUTE_TICKS,cap = StrokeCap.Square
                            )
                        }
                        for (i in 1..12) {
                            drawClockHand(angle = i*(360f/12f),startLength = 0.96f,handLength = 0.99f,strokeWidth = 0.015f,color = COLOR_HOUR_TICKS,cap = StrokeCap.Square
                            )
                        }
                        drawHours()
                        drawText(text="MemoryMate",x=0.5f,y=0.33f,textSizePerc=0.07f, textColor= COLOR_TITLE)

                        drawClockHand(angle = (hour % 12 + minute / 60f) * 30f-90f,startLength = -0.1f,handLength = 0.6f,strokeWidth = 0.02f,color = COLOR_MINUTES)
                        drawClockHand(angle = (minute + sec / 60f) * 6f-90f,startLength = -0.1f,handLength = 0.9f,strokeWidth = 0.02f,color = COLOR_HOURS)
                        drawClockHand(angle = sec * 6f-90f, startLength = -0.2f,handLength = 0.8f,strokeWidth = 0.005f,color = COLOR_SECONDS)
                        drawClockDot()
                    }
                    if (displayMode==0)  {
                        drawText(text=currentTime,x=0.5f,y=1.04f,textSizePerc=0.110f, textColor=Color.White)
                        drawText(text=getDateString(),x=0.5f,y=1.15f,textSizePerc=0.07f, textColor=Color.White)
                    }
                    if (displayMode==2)  {
                        drawText(text=currentTime,x=0.5f,y=0.42f,textSizePerc=0.13f, textColor=Color.White, backgroundColor = COLOR_BACKGROUND_DIGITAL)
                        drawText(text=getDateString(),x=0.5f,y=0.58f,textSizePerc=0.065f, textColor=Color.White)
                    }
                    drawBattery("$batteryPct%",if (chargingStatus) Color.White else Color.Red)

                }
               Text(text = "",color = Color.White,fontSize = 24.sp,fontWeight = FontWeight.Bold )
            }
        }
    }

    if (SHOW_VERSION) {
        BuildInfoText()
    }

}


fun DrawScope.drawHours(textColor: Color = Color.White) {
    for(h in 1..12){
        val radius = 9 * size.minDimension / 20
        val textSizePx = 0.11f * size.minDimension
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radians = Math.toRadians((0f+h*360f/12f-90f).toDouble())
        val setX =  centerX + 0.8f * radius * cos(radians).toFloat()
        val setY =  centerY + 0.8f * radius * sin(radians).toFloat()
        //println("X $X, Y $Y, h $h, radius $radius")
        val paint = Paint().apply {
            color = textColor.toArgb() // Set text color
            textSize = textSizePx // Use already converted text size in pixels
            typeface = Typeface.DEFAULT_BOLD // Bold text
            isAntiAlias = true // Smooth edges
        }
        // Measure text width and height
        val text = h.toString()
        val textWidth = paint.measureText(text)
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height()
        // Adjust position so (centerX, centerY) is the actual center of the text
        val x = setX - (textWidth / 2)
        val y = setY + (textHeight / 2)
        drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)

    }
}
fun DrawScope.drawText(text: String, x: Float, y: Float, textSizePerc: Float, textColor: Color = Color.White, backgroundColor: Color = Color.Transparent) {
    val textSizePx = textSizePerc * size.minDimension
    val centerX = x * size.width
    val centerY = y * size.height

    val paint = Paint().apply {
        color = textColor.toArgb()
        textSize = textSizePx // Use already converted text size in pixels
        typeface = Typeface.DEFAULT_BOLD // Bold text
        isAntiAlias = true // Smooth edges
    }
    // Measure text width and height
    val textWidth = paint.measureText(text)
    val textBounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, textBounds)
    val textHeight = textBounds.height()
    // Adjust position so (centerX, centerY) is the actual center of the text
    val textX = centerX - (textWidth / 2)
    val textY = centerY + (textHeight / 2)

    // Bepaal de rechthoek rond de tekst
    val padding = textSizePx * 0.3f // Extra ruimte rond de tekst
    val rectLeft = centerX - (textWidth / 2) - padding
    val rectTop = centerY - (textHeight / 2) - 0.5f*padding
    val rectRight = centerX + (textWidth / 2) + padding
    val rectBottom = centerY + (textHeight / 2) + 1.5f*padding

    // Teken de achtergrond rechthoek
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(rectLeft, rectTop),
        size = Size(rectRight - rectLeft, rectBottom - rectTop),
        cornerRadius = CornerRadius(50f,50f)
    )

    drawContext.canvas.nativeCanvas.drawText(text, textX, textY, paint)

}
fun DrawScope.drawClockFace() {
    val radius = 9 * size.minDimension / 20
    val centerX = size.width / 2
    val centerY = size.height / 2
    drawCircle(color = COLOR_BACK,radius = radius,center = Offset(centerX, centerY))
}
fun DrawScope.drawClockDot() {
    val radius = 1 * size.minDimension / 60
    val centerX = size.width / 2
    val centerY = size.height / 2
    drawCircle(color = COLOR_SECONDS,radius = radius,center = Offset(centerX, centerY))
}
fun DrawScope.drawBattery(text: String, textColor: Color) {
    val radius = 0.07f * size.minDimension
    val centerX = 0.88f * size.width
    val centerY = 0.1f * size.height
    drawCircle(color = COLOR_BACK,radius = radius,center = Offset(centerX, centerY))
    drawText(text = text,x = 0.88f,y = 0.1f,textSizePerc = 0.045f,textColor = textColor)
}

fun DrawScope.drawClockHand(
    angle: Float,
    startLength: Float,
    handLength: Float,
    strokeWidth: Float,
    color: Color = Color.Black,
    cap: StrokeCap = StrokeCap.Round
) {
    val radius = 9 * size.minDimension / 20
    val centerX = size.width / 2
    val centerY = size.height / 2
    val lineWidth = strokeWidth * size.minDimension
    val radians = Math.toRadians(angle.toDouble())
    val startX = centerX + startLength * radius * cos(radians).toFloat()
    val startY = centerY + startLength * radius * sin(radians).toFloat()
    val endX = centerX + handLength * radius * cos(radians).toFloat()
    val endY = centerY + handLength * radius * sin(radians).toFloat()
    val shadowDelta = size.minDimension / 90.0
    if (startLength < 0.5) {
        drawLine(color = COLOR_SHADOW,start = Offset((startX + shadowDelta).toFloat(), (startY + shadowDelta).toFloat()),end = Offset((endX +shadowDelta).toFloat(), (endY+shadowDelta).toFloat()),strokeWidth = lineWidth,cap = cap)
    }
    drawLine(color = color,start = Offset(startX, startY),end = Offset(endX, endY),strokeWidth = lineWidth,cap = cap)
}
// Hulpfunctie om de huidige tijd te krijgen

fun getCurrentTime(is24hourFormat: Boolean = false): String {
    // Set the format pattern based on the system's setting
    val formatPattern = if (is24hourFormat) {
        "HH:mm:ss"  // 24-hour format
    } else {
        "hh:mm:ss a"  // 12-hour format with AM/PM
    }
    val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
    //println("format = $formatter.format(Date()), is24hourFormat = $is24hourFormat")
    return formatter.format(Date())
}
// Same time, but always with ASCII digits so it can be parsed safely (see getTimeFromString)
fun getCurrentTimeParsable(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}
fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}
fun getDateString(): String {
    val locale = Locale.getDefault()
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
    return dateFormat.format(Date())
}
fun getTimeFromString(timeString: String): Triple<Int, Int, Int> {
    val hour = timeString.substring(0, 2).toInt()
    val minute = timeString.substring(3, 5).toInt()
    val second = timeString.substring(6, 8).toInt()
    return Triple(hour, minute, second)
}



///////////////////////////////////////////////////////////////////////////////////////////////////
// CALENDERS STUFF
///////////////////////////////////////////////////////////////////////////////////////////////////
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean
)

suspend fun saveCalendarName(context: Context, name: String) {
    context.dataStore.edit { settings -> settings[CALENDAR_NAME_KEY] = name }
}

suspend fun getSavedCalendarName(context: Context): String? {
    return context.dataStore.data.first()[CALENDAR_NAME_KEY]
}

fun loadCalendars(context: Context): List<Pair<Int, String>> {
    val calendars = mutableListOf<Pair<Int, String>>()
    val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
    val selection = CalendarContract.Calendars.VISIBLE + " = 1" // Alleen zichtbare kalenders
    val cursor = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null
    )
    cursor?.use {
        val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
        val nameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        while (it.moveToNext()) {
            calendars.add(it.getInt(idIndex) to it.getString(nameIndex))
        }
    }
    return calendars
}

@SuppressLint("Range")
fun getNext10CalendarEvents(context: Context, calendarId: Int): List<CalendarEvent> {
    val resolver: ContentResolver = context.contentResolver
    val now = System.currentTimeMillis()
    val threeMonthsFromNow = now + 90L * 24 * 60 * 60 * 1000 // 90 dagen vooruit

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, threeMonthsFromNow)
    val eventsUri = builder.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY
    )

    val selection = "${CalendarContract.Instances.CALENDAR_ID} = ?"
    val selectionArgs = arrayOf(calendarId.toString())

    val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

    val events = mutableListOf<CalendarEvent>()
    val cursor = resolver.query(eventsUri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
        var count = 0
        while (it.moveToNext() && count < 10) {
            val event = CalendarEvent(
                id = it.getLong(it.getColumnIndex(CalendarContract.Instances.EVENT_ID)),
                title = it.getString(it.getColumnIndex(CalendarContract.Instances.TITLE)) ?: "----",
                startTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN)),
                endTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.END)),
                allDay = it.getInt(it.getColumnIndex(CalendarContract.Instances.ALL_DAY)) == 1
            )
            events.add(event)
            count++
        }
    }

    return events
}


fun formatEventDate(startTime: Long): String {
    val locale = Locale.getDefault()
    val language = locale.language  // De taalcode, bijvoorbeeld "nl" voor Nederlands, "en" voor Engels

    val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val dateFormat = SimpleDateFormat("EEEE, d MMMM", locale)

    val vandaag = if(language == "nl") "Vandaag" else "Today"
    val morgen = if(language == "nl") "Morgen" else "Tomorrow"
    return when {
        isSameDay(calendar, today) -> vandaag
        isSameDay(calendar, tomorrow) -> morgen
        else -> dateFormat.format(calendar.time)
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun getCurrentTimeEvent(time: Long, is24hourFormat: Boolean): String {
    // Set the format pattern based on the system's setting
    //println("is24hourFormat = $is24hourFormat")
    val formatPattern = if (is24hourFormat) {
        "HH:mm"  // 24-hour format
    } else {
        "hh:mm a"  // 12-hour format with AM/PM
    }
    val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
    return formatter.format(time)
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DisplayCalendar() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    val coroutineScope = rememberCoroutineScope()
    var selectedCalendarId by remember { mutableStateOf<Int?>(null) }
    var selectedCalendarName by remember { mutableStateOf<String?>(null) }
    var calendars by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var showInfo by remember { mutableStateOf(false) }
    var showSelectCalendar by remember { mutableStateOf(false) }
    var textScale by remember { mutableIntStateOf(100) }
    var calendarPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }

    // Laad opgeslagen kalender-ID
    LaunchedEffect(calendarPermissionGranted) {
        if (calendarPermissionGranted) {
            val savedName = getSavedCalendarName(context)
            val calendarsList = loadCalendars(context)
            val matchingCalendar = calendarsList.find { it.second == savedName }
            selectedCalendarId = matchingCalendar?.first
            selectedCalendarName = matchingCalendar?.second
        }
    }
    // Permission launcher
    var openInfoAfterGrant by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        calendarPermissionGranted = isGranted
        if (isGranted && openInfoAfterGrant) {
            calendars = loadCalendars(context)
            showInfo = true
        }
        openInfoAfterGrant = false
    }

    // Ask for the permission once at startup (not on every recomposition)
    LaunchedEffect(Unit) {
        if (!calendarPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    Column(modifier = Modifier.padding(10.dp)) {
        Text(
            text = "\uD83D\uDDD3", // calendar emoji
            fontSize = 25.sp,
            modifier = Modifier
                .clickable {

                    if (calendarPermissionGranted) {
                        calendars = loadCalendars(context)
                        showInfo = true
                    } else {
                        openInfoAfterGrant = true
                        permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                }
                .padding(12.dp)
        )
    }

    @Composable
    fun BulletItem(text: String) { // make a bullit list with correct spacing when lines are wrapped
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text( text = "●",  color = Color.Gray,  modifier = Modifier.padding(top = 1.dp, start = 6.dp) )
            Spacer(modifier = Modifier.width(8.dp)) // Space between bullet and text
            Column(  modifier = Modifier.padding(start = 2.dp) ) {
                Text(  text = text, modifier = Modifier.fillMaxWidth(), color = Color.Gray )
            }
        }
    }

    // Display starting point for settings and help
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("MemoryMate") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(  text = "Version: ", color = Color.DarkGray, modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold  )
                            Text(  text = globalBuildInfo,  color = Color.Gray )
                        } // row
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text( text = "Calender: ", color = Color.DarkGray, modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold )
                            Text(
                                text = selectedCalendarName ?: "", // selected calendar
                                color = Color.Gray,
                            )
                        } // row

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                showSelectCalendar = true
                            }) { Text(text = "Change Calendar") }
                        }
                        //HorizontalDivider( modifier = Modifier.padding(vertical = 15.dp),  thickness = 2.dp, color = Color.Gray )
                    } // item

                    // textScaling
                    item {
                        var localScale by remember { mutableIntStateOf(textScale) } // werkt in % (50..150)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Text Scaling:",
                                    color = Color.DarkGray, modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold
                                )
                                Text("$localScale%")
                            }

                            Slider(
                                value = localScale.toFloat(),
                                onValueChange = { v ->
                                    // Snap naar dichtstbijzijnde 5%
                                    val snapped = ((v / 10f).roundToInt() * 10)
                                        .coerceIn(50, 200)
                                    localScale = snapped
                                },
                                valueRange = 50f..200f,
                                // 10%-stappen: 50..150 => 21 waardes => steps = 21 - 2 = 19
                                steps = 15,
                                onValueChangeFinished = {
                                    textScale = localScale
                                    editor.putInt("textScale", textScale)
                                    editor.apply()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Text( text = "How to use:", fontWeight = FontWeight.Bold, color = Color.DarkGray )
                        BulletItem( "Select the appropriate Calendar," )
                        BulletItem( "Double click Clock to select different display modes," )
                        BulletItem( "Double click Calendar to select different amount of events.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInfo = false }) {
                    Text("Close")
                }
            }
        )
    }



    // select calendar
    if (showSelectCalendar) {
        AlertDialog(
            onDismissRequest = { showSelectCalendar = false },
            title = { Text("Select Calendar") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .background(Color.White)
                            .height(400.dp)
                    ) {
                        items(calendars) { (id, name) ->
                            Text(
                                name, modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (id == selectedCalendarId) Color(0xFF9999EE) else Color(
                                        0xFFFFFFFF
                                    )
                                ) // Geselecteerde kleur
                                .clickable {
                                    //println("-----------> Selected calendar ID: $id, Name: $name")
                                    selectedCalendarId = id
                                    selectedCalendarName = name
                                    coroutineScope.launch { saveCalendarName(context, name) }
                                    showSelectCalendar = false
                                }
                                .padding(3.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectCalendar = false }) {
                    Text("Close")
                }
            }
        )
    }


    val is24HourFormat = DateFormat.is24HourFormat(context)

    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var updater by remember { mutableIntStateOf(0) }

    LaunchedEffect(updater) {
        while (true) {
            delay(10.seconds) // Wacht 10 seconde
            currentTime = getCurrentDate() // Update de tijd
            updater++
        }
    }
    if (calendarPermissionGranted) {
        if (selectedCalendarId != null) {
            val events = getNext10CalendarEvents(context, calendarId = selectedCalendarId ?: 0)
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            var lastClickTime by remember { mutableLongStateOf(0L) } // Store the last click time
            var eventAmount by remember { mutableIntStateOf(EVENT_AMOUNT_DEFAULT) } // Store the last click time

            eventAmount = sharedPreferences.getInt("eventAmount", EVENT_AMOUNT_DEFAULT)
            textScale = sharedPreferences.getInt("textScale", 100)

            Box(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        //.background(COLOR_BACKGROUND)
                        .clickable {
                            val nowTime = System.currentTimeMillis()
                            if (nowTime - lastClickTime <= doubleClickThreshold) {
                                eventAmount =
                                    if (eventAmount < EVENT_AMOUNT_MAX) eventAmount + 1 else EVENT_AMOUNT_MIN
                                editor.putInt("eventAmount", eventAmount)
                                editor.apply() // Opslaan
                            }
                            lastClickTime = nowTime

                        }
                        .padding(10.dp)
                        .then(
                            if (isPortrait) {
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f) // Bovenste helft in portret
                                    .align(Alignment.BottomEnd)
                            } else {
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f) // Linker helft in landschap
                                    .align(Alignment.CenterEnd)
                            }
                        ),
                    //contentAlignment = Alignment.Center // Tekst in het midden
                ) {
                    //val maxHeight = maxHeight
                    //val fontSize = calculateFontSizeToFit("l", (maxHeight/eventAmount))
                    //            val dpList: List<TextUnit> = listOf(5.sp,5.sp,5.sp,25.sp,22.sp,20.sp,21.sp,17.sp,15.sp,12.sp,9.sp,8.sp,7.sp,6.sp,5.sp,4.sp,3.sp,2.sp,1.sp)
                    //            var fontSize = dpList[eventAmount]

                    // calc
//                    val displayMetrics = Resources.getSystem().displayMetrics
//                    val screenHeightPx =
//                        if (isPortrait) displayMetrics.heightPixels / 2 else displayMetrics.heightPixels
//                    val density = displayMetrics.density
//                    val screenHeightDp = screenHeightPx / density
//                    val lineHeightDp = screenHeightDp / (max(5, eventAmount) * 11)
//                    val fontSize = (textScale * lineHeightDp * density/100).sp


//                    val displayMetrics = Resources.getSystem().displayMetrics
//                    val density = displayMetrics.density
//                    val widthDp = displayMetrics.widthPixels / density
//                    val heightDp = displayMetrics.heightPixels / density
//                    val smallestWidthDp = min(widthDp, heightDp)
//
//// Bepaal schaalfactor, bv. baseline = 360dp
//                    val scale = smallestWidthDp / 360f
//
//                    val lineHeightDp = heightDp / (max(5, eventAmount) * 15)
//                    val fontSize = (textScale * lineHeightDp * scale/100).sp
//

                    val displayMetrics = Resources.getSystem().displayMetrics
                    val density = displayMetrics.density
                    val widthDp = displayMetrics.widthPixels / density
                    val heightDp = displayMetrics.heightPixels / density
                    val smallestWidthDp = min(widthDp, heightDp)

                    // Niet-lineaire schaal: map 360dp→~1.15x en 600dp→~1.60x, met clamping
                    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
                    val t = ((smallestWidthDp - 360f) / (600f - 360f)).coerceIn(0f, 1f)
                    val scale = lerp(2.55f, 1.99f, t) // telefoons krijgen al ~+15%, tablets groeien door tot ~+60%

// Je bestaande regelhoogte (eventAmount iets ruimer gezet met 15)
                    val lineHeightDp = heightDp / (max(5, eventAmount) * 15)

// Bereken sp en zet een ondergrens (bv. 12sp)
                    val computedSp = (lineHeightDp * scale).coerceAtLeast(10f)
                    val fontSize = (textScale * computedSp / 100).sp

                    //println("fontSize: $fontSize, maxHeight: ${maxHeight/eventAmount}")
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly, // Distribute evenly
                        horizontalAlignment = Alignment.Start // Center horizontally
                    ) {
                        repeat(eventAmount) { index ->
                            // format event
                            var textLeft = "---"
                            var textRight = ""
                            if (index < events.size) {
                                val date = formatEventDate(events[index].startTime)
                                val starttime =
                                    getCurrentTimeEvent(events[index].startTime, is24HourFormat)
                                val endtime =
                                    getCurrentTimeEvent(events[index].endTime, is24HourFormat)
                                val allDay = events[index].allDay
                                val title = events[index].title
                                textLeft = "$date\n$title"
                                textRight = if (allDay) "" else "$starttime\n$endtime"
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Verdeelt de hoogte gelijk over aantal Boxen
                                    .padding(2.dp)
                                    .background(
                                        COLOR_BACK,
                                        shape = RoundedCornerShape(10.dp)
                                    ), // Unieke achtergrondkleur

                                contentAlignment = Alignment.Center
                            ) {
                                val paddingValLong = maxWidth * 0.5f
                                val paddingValShort = maxWidth * 0.3f
                                Text(
                                    text = textLeft,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                        .padding(start = 15.dp, end = paddingValShort),
                                    color = Color.White,
                                    fontSize = fontSize, //20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = textRight,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                        .padding(start = paddingValLong, end = 15.dp),
                                    color = Color.White,
                                    fontSize = fontSize, //20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                } // box with constraints
            } // box main
        }
    }
}





