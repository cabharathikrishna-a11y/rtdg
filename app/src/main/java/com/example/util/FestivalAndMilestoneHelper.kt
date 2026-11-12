package com.example.util

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class SpecialOccasionGreeting(
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val showIndianFlag: Boolean = false,
    val accentHex: Long = 0xFFF59E0B
)

object FestivalAndMilestoneHelper {
    private const val PREFS_NAME = "weekly_finance_prefs"
    private const val KEY_LAST_SHOWN_DATE = "last_special_wish_shown_date"
    private const val KEY_CACHED_FESTIVALS_JSON = "cached_internet_hindu_festivals_json"

    /**
     * Checks if today's date has a special milestone, national day, or Hindu/Tamil festival
     * and whether it should be shown (once per calendar day).
     */
    fun getTodayGreetingIfFirstOpenOfDay(context: Context, forceCheckOnly: Boolean = false): SpecialOccasionGreeting? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastShown = prefs.getString(KEY_LAST_SHOWN_DATE, "") ?: ""

        val greeting = resolveOccasionForDate(context, Calendar.getInstance()) ?: return null

        if (!forceCheckOnly && lastShown == todayKey) {
            return null
        }
        return greeting
    }

    fun markTodayGreetingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        prefs.edit().putString(KEY_LAST_SHOWN_DATE, todayKey).apply()
    }

    fun resolveOccasionForDate(context: Context, cal: Calendar): SpecialOccasionGreeting? {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mmDd = String.format(Locale.US, "%02d-%02d", month, day)
        val yyyyMmDd = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

        // 1. Personal Milestones & Fixed National / Cultural Days for Muneeswaran
        when (mmDd) {
            "01-01" -> return SpecialOccasionGreeting(
                title = "Happy New Year Muneeswaran",
                subtitle = "Wishing you a prosperous, high-growth year ahead!",
                badgeLabel = "NEW YEAR WISHES",
                showIndianFlag = false,
                accentHex = 0xFF38BDF8
            )
            "01-26" -> return SpecialOccasionGreeting(
                title = "Happy Republic Day Muneeswaran",
                subtitle = "Saluting the spirit of a sovereign, united India — Jai Hind!",
                badgeLabel = "26 JANUARY • REPUBLIC DAY",
                showIndianFlag = true,
                accentHex = 0xFFFF9933
            )
            "04-14" -> return SpecialOccasionGreeting(
                title = "Happy Tamil New Year Muneeswaran",
                subtitle = "இனிய தமிழ் புத்தாண்டு நல்வாழ்த்துக்கள் முனீஸ்வரன்!",
                badgeLabel = "PUTHANDU VAZHTHUKKAL",
                showIndianFlag = false,
                accentHex = 0xFFF59E0B
            )
            "06-05" -> return SpecialOccasionGreeting(
                title = "Happy Birthday Muneeswaran",
                subtitle = "Wishing you abundant health, happiness, and continued success!",
                badgeLabel = "5 JUNE • SPECIAL BIRTHDAY WISH",
                showIndianFlag = false,
                accentHex = 0xFFEC4899
            )
            "08-15" -> return SpecialOccasionGreeting(
                title = "Happy Independence Day Muneeswaran",
                subtitle = "Celebrating freedom, unity, and national pride — Jai Hind!",
                badgeLabel = "15 AUGUST • INDEPENDENCE DAY",
                showIndianFlag = true,
                accentHex = 0xFFFF9933
            )
            "09-28" -> return SpecialOccasionGreeting(
                title = "Happy Marriage Anniversary Muneeswaran",
                subtitle = "Wishing you both a lifetime of love, harmony, and togetherness!",
                badgeLabel = "28 SEPTEMBER • HAPPY ANNIVERSARY",
                showIndianFlag = false,
                accentHex = 0xFFF43F5E
            )
            "12-25" -> return SpecialOccasionGreeting(
                title = "Happy Christmas Muneeswaran",
                subtitle = "Wishing you peace, joy, and festive blessings this season!",
                badgeLabel = "25 DECEMBER • MERRY CHRISTMAS",
                showIndianFlag = false,
                accentHex = 0xFF10B981
            )
        }

        // 2. Fixed Solar Hindu / Tamil Harvest Festivals (Pongal: Jan 14-16)
        when (mmDd) {
            "01-14", "01-15" -> return SpecialOccasionGreeting(
                title = "Happy Pongal Muneeswaran",
                subtitle = "இனிய தை பொங்கல் நல்வாழ்த்துக்கள் முனீஸ்வரன்!",
                badgeLabel = "THAI PONGAL FESTIVAL",
                showIndianFlag = false,
                accentHex = 0xFFF59E0B
            )
            "01-16" -> return SpecialOccasionGreeting(
                title = "Happy Mattu Pongal Muneeswaran",
                subtitle = "Celebrating prosperity and gratitude!",
                badgeLabel = "MATTU PONGAL",
                showIndianFlag = false,
                accentHex = 0xFFF59E0B
            )
            "08-03" -> return SpecialOccasionGreeting(
                title = "Happy Aadi Perukku Muneeswaran",
                subtitle = "Wishing overflowing prosperity and abundance!",
                badgeLabel = "AADI PERUKKU",
                showIndianFlag = false,
                accentHex = 0xFF10B981
            )
        }

        // 3. Check Cached Internet Calendar Override first
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_FESTIVALS_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(cachedJson)
                if (obj.has(yyyyMmDd)) {
                    val festName = obj.getString(yyyyMmDd)
                    return SpecialOccasionGreeting(
                        title = "Happy $festName Muneeswaran",
                        subtitle = "Wishing you auspicious blessings and prosperity on $festName!",
                        badgeLabel = festName.uppercase(Locale.US),
                        showIndianFlag = false,
                        accentHex = 0xFFF59E0B
                    )
                }
            } catch (_: Exception) {}
        }

        // 4. Built-in Multi-Year Panchangam Lookup for Key Hindu & Tamil Lunar Festivals (2025-2030)
        val hinduLunarFestivals = mapOf(
            // 2025
            "2025-02-11" to "Thai Poosam",
            "2025-02-26" to "Maha Shivaratri",
            "2025-03-14" to "Holi",
            "2025-03-30" to "Ugadi",
            "2025-04-06" to "Rama Navami",
            "2025-04-12" to "Hanuman Jayanti",
            "2025-04-30" to "Akshaya Tritiya",
            "2025-08-16" to "Krishna Janmashtami",
            "2025-08-27" to "Vinayagar Chaturthi",
            "2025-09-05" to "Onam",
            "2025-10-01" to "Ayudha Pooja",
            "2025-10-02" to "Vijayadashami",
            "2025-10-20" to "Diwali",
            "2025-10-21" to "Diwali",
            "2025-12-03" to "Karthigai Deepam",

            // 2026
            "2026-02-01" to "Thai Poosam",
            "2026-02-15" to "Maha Shivaratri",
            "2026-03-03" to "Holi",
            "2026-03-19" to "Ugadi",
            "2026-03-26" to "Rama Navami",
            "2026-04-02" to "Hanuman Jayanti",
            "2026-04-19" to "Akshaya Tritiya",
            "2026-09-04" to "Krishna Janmashtami",
            "2026-09-14" to "Vinayagar Chaturthi",
            "2026-08-26" to "Onam",
            "2026-10-20" to "Ayudha Pooja",
            "2026-10-21" to "Vijayadashami",
            "2026-11-08" to "Diwali",
            "2026-11-09" to "Diwali",
            "2026-11-24" to "Karthigai Deepam",

            // 2027
            "2027-01-22" to "Thai Poosam",
            "2027-03-06" to "Maha Shivaratri",
            "2027-03-22" to "Holi",
            "2027-04-07" to "Ugadi",
            "2027-04-15" to "Rama Navami",
            "2027-04-20" to "Hanuman Jayanti",
            "2027-05-09" to "Akshaya Tritiya",
            "2027-08-25" to "Krishna Janmashtami",
            "2027-09-04" to "Vinayagar Chaturthi",
            "2027-09-12" to "Onam",
            "2027-10-09" to "Ayudha Pooja",
            "2027-10-10" to "Vijayadashami",
            "2027-10-29" to "Diwali",
            "2027-12-13" to "Karthigai Deepam",

            // 2028
            "2028-02-09" to "Thai Poosam",
            "2028-02-23" to "Maha Shivaratri",
            "2028-03-11" to "Holi",
            "2028-03-27" to "Ugadi",
            "2028-04-03" to "Rama Navami",
            "2028-04-27" to "Akshaya Tritiya",
            "2028-08-13" to "Krishna Janmashtami",
            "2028-08-23" to "Vinayagar Chaturthi",
            "2028-09-01" to "Onam",
            "2028-09-27" to "Ayudha Pooja",
            "2028-10-17" to "Diwali",
            "2028-12-01" to "Karthigai Deepam"
        )

        val matchedFestival = hinduLunarFestivals[yyyyMmDd]
        if (matchedFestival != null) {
            return SpecialOccasionGreeting(
                title = "Happy $matchedFestival Muneeswaran",
                subtitle = "Wishing you divine blessings, joy, and prosperity on $matchedFestival!",
                badgeLabel = matchedFestival.uppercase(Locale.US),
                showIndianFlag = false,
                accentHex = 0xFFF59E0B
            )
        }

        return null
    }

    /**
     * Asynchronously refreshes public Indian / Hindu holiday dates from the Nager/Google public calendar endpoint
     * and caches them locally so movable festival dates stay up to date across years.
     */
    suspend fun refreshFestivalCalendarFromInternet(context: Context) = withContext(Dispatchers.IO) {
        try {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val url = URL("https://date.nager.at/api/v3/PublicHolidays/$year/IN")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = org.json.JSONArray(body)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val existingJson = prefs.getString(KEY_CACHED_FESTIVALS_JSON, "{}") ?: "{}"
                val mapObj = JSONObject(existingJson)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val dateStr = item.optString("date")
                    val localName = item.optString("localName").ifBlank { item.optString("name") }
                    if (dateStr.isNotBlank() && localName.isNotBlank()) {
                        mapObj.put(dateStr, localName)
                    }
                }
                prefs.edit().putString(KEY_CACHED_FESTIVALS_JSON, mapObj.toString()).apply()
            }
        } catch (_: Exception) {
            // Built-in multi-year panchangam lookup seamlessly covers offline/fallback
        }
    }
}

/**
 * Crisp Vector Canvas illustration of the Indian National Flag (Tiranga)
 * with Saffron, White, India Green bands and 24-spoke Navy Blue Ashoka Chakra.
 */
@Composable
fun IndianNationalFlagGraphic(
    width: Dp = 96.dp,
    height: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
    ) {
        val w = size.width
        val h = size.height
        val bandHeight = h / 3f

        // Top Band: India Saffron (#FF9933)
        drawRect(
            color = Color(0xFFFF9933),
            topLeft = Offset(0f, 0f),
            size = Size(w, bandHeight)
        )
        // Middle Band: Crisp White (#FFFFFF)
        drawRect(
            color = Color.White,
            topLeft = Offset(0f, bandHeight),
            size = Size(w, bandHeight)
        )
        // Bottom Band: India Green (#138808)
        drawRect(
            color = Color(0xFF138808),
            topLeft = Offset(0f, bandHeight * 2f),
            size = Size(w, bandHeight)
        )

        // Center: Navy Blue Ashoka Chakra (#000080)
        val center = Offset(w / 2f, h / 2f)
        val radius = (bandHeight * 0.42f)
        val navyBlue = Color(0xFF000080)

        drawCircle(
            color = navyBlue,
            radius = radius,
            center = center,
            style = Stroke(width = 2.2f)
        )
        drawCircle(
            color = navyBlue,
            radius = radius * 0.18f,
            center = center
        )

        // 24 spokes
        for (i in 0 until 24) {
            val angleRad = Math.toRadians((i * 15).toDouble())
            val endX = center.x + (radius * cos(angleRad)).toFloat()
            val endY = center.y + (radius * sin(angleRad)).toFloat()
            drawLine(
                color = navyBlue,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.2f
            )
        }
    }
}
