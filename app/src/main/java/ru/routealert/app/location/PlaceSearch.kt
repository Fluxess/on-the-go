package ru.routealert.app.location

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.routealert.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

data class PlaceHit(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

object PlaceSearch {

    private const val TAG = "PlaceSearch"
    private const val USER_AGENT = "on-the-go/1.0 (Android; route alert app)"

    private val coordRegex = Regex(
        """^\s*([+-]?\d{1,3}(?:\.\d+)?)\s*[,;\s]\s*([+-]?\d{1,3}(?:\.\d+)?)\s*$"""
    )

    fun parseCoordinates(query: String): PlaceHit? {
        val match = coordRegex.matchEntire(query.trim()) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return PlaceHit(
            title = "Координаты",
            subtitle = String.format(Locale.US, "%.5f, %.5f", lat, lon),
            latitude = lat,
            longitude = lon
        )
    }

    suspend fun search(context: Context, query: String, limit: Int = 8): List<PlaceHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()

            parseCoordinates(q)?.let { return@withContext listOf(it) }

            val variants = queryVariants(q)
            coroutineScope {
                val yandex = async { searchYandex(variants, limit) }
                val nominatim = async { searchNominatim(variants, limit) }
                val android = async { searchAndroidGeocoder(context, variants, limit) }
                mergeHits(yandex.await() + nominatim.await() + android.await(), limit)
            }
        }

    private fun queryVariants(q: String): List<String> {
        val cleaned = q
            .replace(Regex("""\s+в\s+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return listOf(
            q,
            cleaned,
            "$cleaned, Россия",
            "$q, Россия"
        ).map { it.trim() }.filter { it.length >= 2 }.distinct()
    }

    private fun searchYandex(variants: List<String>, limit: Int): List<PlaceHit> {
        val key = BuildConfig.YANDEX_MAPKIT_API_KEY
        if (key.isBlank() || key.startsWith("YOUR_")) return emptyList()
        val hits = mutableListOf<PlaceHit>()
        for (variant in variants) {
            if (hits.size >= limit) break
            val url = Uri.parse("https://geocode-maps.yandex.ru/1.x/").buildUpon()
                .appendQueryParameter("apikey", key)
                .appendQueryParameter("geocode", variant)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("lang", "ru_RU")
                .appendQueryParameter("results", limit.toString())
                .build()
                .toString()
            val body = httpGet(url) ?: continue
            runCatching {
                val members = JSONObject(body)
                    .optJSONObject("response")
                    ?.optJSONObject("GeoObjectCollection")
                    ?.optJSONArray("featureMember")
                    ?: return@runCatching
                for (i in 0 until members.length()) {
                    val geo = members.getJSONObject(i).optJSONObject("GeoObject") ?: continue
                    val pos = geo.optJSONObject("Point")?.optString("pos").orEmpty()
                    val parts = pos.split(Regex("""\s+"""))
                    if (parts.size < 2) continue
                    val lon = parts[0].toDoubleOrNull() ?: continue
                    val lat = parts[1].toDoubleOrNull() ?: continue
                    val title = geo.optString("name").ifBlank { "Место" }
                    val subtitle = geo.optString("description").ifBlank {
                        String.format(Locale.US, "%.5f, %.5f", lat, lon)
                    }
                    hits += PlaceHit(title, subtitle, lat, lon)
                }
            }.onFailure { Log.w(TAG, "Yandex parse failed for $variant", it) }
        }
        return hits
    }

    private fun searchNominatim(variants: List<String>, limit: Int): List<PlaceHit> {
        val hits = mutableListOf<PlaceHit>()
        for (variant in variants) {
            if (hits.size >= limit) break
            val url = Uri.parse("https://nominatim.openstreetmap.org/search").buildUpon()
                .appendQueryParameter("q", variant)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("addressdetails", "1")
                .appendQueryParameter("limit", limit.toString())
                .appendQueryParameter("accept-language", "ru")
                .appendQueryParameter("countrycodes", "ru")
                .build()
                .toString()
            val body = httpGet(url, userAgent = USER_AGENT) ?: continue
            runCatching {
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                    val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                    val display = obj.optString("display_name")
                    val name = obj.optString("name").ifBlank {
                        display.substringBefore(',').ifBlank { "Место" }
                    }
                    val subtitle = when {
                        display.contains(',') -> display.substringAfter(',').trim()
                        display.isNotBlank() -> display
                        else -> String.format(Locale.US, "%.5f, %.5f", lat, lon)
                    }
                    hits += PlaceHit(name, subtitle, lat, lon)
                }
            }.onFailure { Log.w(TAG, "Nominatim parse failed for $variant", it) }
        }
        return hits
    }

    private fun searchAndroidGeocoder(
        context: Context,
        variants: List<String>,
        limit: Int
    ): List<PlaceHit> {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(context, Locale("ru", "RU"))
        val hits = mutableListOf<PlaceHit>()
        for (variant in variants) {
            if (hits.size >= limit) break
            val addresses = runCatching {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(variant, limit).orEmpty()
            }.getOrDefault(emptyList())
            addresses.forEach { addr ->
                val lat = addr.latitude
                val lon = addr.longitude
                val title = addr.featureName
                    ?.takeIf { it.isNotBlank() && !it.matches(Regex("""^\d+$""")) }
                    ?: addr.thoroughfare
                    ?: addr.locality
                    ?: "Место"
                val subtitle = buildList {
                    listOfNotNull(addr.thoroughfare, addr.subThoroughfare)
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                        ?.let(::add)
                    addr.locality?.let(::add)
                    addr.adminArea?.let(::add)
                }.distinct().joinToString(", ").ifBlank {
                    String.format(Locale.US, "%.5f, %.5f", lat, lon)
                }
                hits += PlaceHit(title = title, subtitle = subtitle, latitude = lat, longitude = lon)
            }
        }
        return hits
    }

    private fun mergeHits(hits: List<PlaceHit>, limit: Int): List<PlaceHit> {
        val out = ArrayList<PlaceHit>(limit)
        for (hit in hits) {
            val dup = out.any {
                abs(it.latitude - hit.latitude) < 0.0008 &&
                    abs(it.longitude - hit.longitude) < 0.0008
            }
            if (!dup) out += hit
            if (out.size >= limit) break
        }
        return out
    }

    private fun httpGet(url: String, userAgent: String? = null): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (userAgent != null) setRequestProperty("User-Agent", userAgent)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code for $url")
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "HTTP failed: $url", t)
            null
        } finally {
            conn?.disconnect()
        }
    }
}
