package ru.routealert.app.share

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import ru.routealert.app.data.AlertType
import ru.routealert.app.data.RoutePoint
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class SharedPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val alertType: AlertType
)

object PointShare {
    const val SCHEME = "onthego"
    const val HOST = "import"
    const val HTTPS_HOST = "onthego.app"
    const val HTTPS_PATH = "/i"
    private const val PARAM = "d"

    /** Regex: native deep link or https share / App Link. */
    private val linkInText = Regex(
        """(?:onthego://import\?[^\s<>\]]+|https?://[^\s<>\]]+)""",
        RegexOption.IGNORE_CASE
    )

    fun toDeepLink(points: List<RoutePoint>): Uri {
        val payload = encodePayload(points.toShared())
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendQueryParameter(PARAM, payload)
            .build()
    }

    /**
     * HTTPS App Link — кликабельна в Telegram/WhatsApp.
     * Открывается приложением через intent-filter (и через лендинг, если зальёте docs/i).
     */
    fun toHttpsDeepLink(points: List<RoutePoint>): Uri {
        val payload = encodePayload(points.toShared())
        return Uri.Builder()
            .scheme("https")
            .authority(HTTPS_HOST)
            .path(HTTPS_PATH)
            .appendQueryParameter(PARAM, payload)
            .build()
    }

    /**
     * Ссылка для мессенджеров: https (кликабельно) → 302 на onthego:// → открывается приложение.
     * Работает в in-app браузере Telegram без своего домена.
     */
    fun toMessengerLink(points: List<RoutePoint>): String {
        val deep = toDeepLink(points).toString()
        return "https://httpbin.org/redirect-to?url=" + Uri.encode(deep)
    }

    fun toShareText(points: List<RoutePoint>): String {
        val names = points.joinToString(", ") { it.name }
        val link = toMessengerLink(points)
        return "Точки on-the-go ($names):\n$link"
    }

    fun parse(uri: Uri): List<SharedPoint> {
        unwrapToAppUri(uri)?.let { return decodePayload(it.getQueryParameter(PARAM) ?: return emptyList()) }
        return emptyList()
    }

    fun parse(text: String): List<SharedPoint> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Whole string is a URI
        parse(Uri.parse(trimmed)).takeIf { it.isNotEmpty() }?.let { return it }

        // Link embedded in a longer message (Telegram share text, etc.)
        linkInText.findAll(trimmed).forEach { match ->
            val candidate = match.value.trimEnd(')', '.', ',', ';', '"', '\'')
            val hits = parse(Uri.parse(candidate))
            if (hits.isNotEmpty()) return hits
        }
        return emptyList()
    }

    fun qrBitmap(content: String, sizePx: Int = 720): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    /** Normalize messenger redirects / https App Links to onthego://import?d=… */
    private fun unwrapToAppUri(uri: Uri): Uri? {
        val scheme = uri.scheme?.lowercase() ?: return null

        if (scheme == SCHEME && uri.host.equals(HOST, ignoreCase = true)) {
            return uri
        }

        if (scheme == "https" || scheme == "http") {
            val host = uri.host?.lowercase().orEmpty()

            // App Link: https://onthego.app/i?d=…
            if (host == HTTPS_HOST || host == "www.$HTTPS_HOST") {
                val d = uri.getQueryParameter(PARAM) ?: return null
                return Uri.Builder()
                    .scheme(SCHEME)
                    .authority(HOST)
                    .appendQueryParameter(PARAM, d)
                    .build()
            }

            // Messenger wrapper: https://httpbin.org/redirect-to?url=onthego://…
            if (host == "httpbin.org" && uri.path?.contains("redirect-to") == true) {
                val target = uri.getQueryParameter("url") ?: return null
                return unwrapToAppUri(Uri.parse(target))
            }
        }
        return null
    }

    private fun List<RoutePoint>.toShared() = map {
        SharedPoint(it.name, it.latitude, it.longitude, it.radiusMeters, it.alertType)
    }

    private fun encodePayload(points: List<SharedPoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(
                JSONObject()
                    .put("n", p.name)
                    .put("lat", p.latitude)
                    .put("lon", p.longitude)
                    .put("r", p.radiusMeters.toDouble())
                    .put("t", p.alertType.name)
            )
        }
        val bytes = arr.toString().toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun decodePayload(raw: String): List<SharedPoint> {
        return runCatching {
            val decoded = try {
                String(Base64.decode(raw, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
            }
            val arr = JSONArray(decoded)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SharedPoint(
                            name = o.optString("n", "Точка"),
                            latitude = o.getDouble("lat"),
                            longitude = o.getDouble("lon"),
                            radiusMeters = o.optDouble("r", 200.0).toFloat(),
                            alertType = runCatching {
                                AlertType.valueOf(o.optString("t", AlertType.BOTH.name))
                            }.getOrDefault(AlertType.BOTH)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
