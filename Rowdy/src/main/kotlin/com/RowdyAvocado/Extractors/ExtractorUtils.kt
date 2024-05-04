package com.RowdyAvocado

import android.util.Base64
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import java.net.URI
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

enum class ServerName {
    MyCloud,
    Mp4upload,
    Streamtape,
    Vidplay,
    Filemoon,
    Custom,
    NONE
}

object RowdyExtractorUtil {

    val mediaPrdList =
            listOf(
                    "CineZone" to "https://cinezone.to",
                    "VidsrcNet" to "https://vidsrc.net",
                    "VidsrcTo" to "https://vidsrc.to",
                    "Moflix" to "https://moflix-stream.xyz",
                    "Ridomovies" to "https://ridomovies.tv",
                    "ZShow" to "https://tv.idlixofficial.co",
            )

    val animePrdList =
            listOf(
                    "Aniwave" to "https://aniwave.to",
                    "Anitaku" to "https://www.anitaku.com",
                    "HiAnime" to "https://www.hianime.com",
            )
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
object CommonUtils {
    fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }

    suspend fun haveDub(url: String, referer: String): Boolean {
        return app.get(url, referer = referer).text.contains("TYPE=AUDIO")
    }

    fun createSlug(data: String?): String? {
        return data
                ?.filter { it.isWhitespace() || it.isLetterOrDigit() }
                ?.trim()
                ?.replace("\\s+".toRegex(), "-")
                ?.lowercase()
    }
}

object AniwaveUtils {
    fun vrfEncrypt(input: String): String {
        val rc4Key = SecretKeySpec("tGn6kIpVXBEUmqjD".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var vrf = cipher.doFinal(input.toByteArray())
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = Base64.encode(vrf, Base64.DEFAULT or Base64.NO_WRAP)
        vrf = vrfShift(vrf)
        // vrf = rot13(vrf)
        vrf = vrf.reversed().toByteArray()
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        val stringVrf = vrf.toString(Charsets.UTF_8)
        val final = java.net.URLEncoder.encode(stringVrf, "utf-8")
        return final
    }

    fun vrfDecrypt(input: String): String {
        var vrf = input.toByteArray()
        vrf = Base64.decode(vrf, Base64.URL_SAFE)

        val rc4Key = SecretKeySpec("LUyDrL4qIxtIxOGs".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return URLDecoder.decode(vrf.toString(Charsets.UTF_8), "utf-8")
    }

    @kotlin.ExperimentalStdlibApi
    private fun rot13(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val byte = vrf[i]
            if (byte in 'A'.code..'Z'.code) {
                vrf[i] = ((byte - 'A'.code + 13) % 26 + 'A'.code).toByte()
            } else if (byte in 'a'.code..'z'.code) {
                vrf[i] = ((byte - 'a'.code + 13) % 26 + 'a'.code).toByte()
            }
        }
        return vrf
    }

    private fun vrfShift(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val shift = arrayOf(-2, -4, -5, 6, 2, -3, 3, 6)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
    }

    fun serverName(id: String): ServerName {
        when (id) {
            "41" -> return ServerName.Vidplay
            "28" -> return ServerName.MyCloud
            "44" -> return ServerName.Filemoon
            "35" -> return ServerName.Mp4upload
        }
        return ServerName.NONE
    }
}

object CineZoneUtils {
    fun vrfEncrypt(input: String): String {
        val rc4Key = SecretKeySpec("Ij4aiaQXgluXQRs6".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var vrf = cipher.doFinal(input.toByteArray())
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = vrf.reversed().toByteArray()
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = vrfShift(vrf)
        val stringVrf = vrf.toString(Charsets.UTF_8)
        return stringVrf
    }

    fun vrfDecrypt(input: String): String {
        var vrf = input.toByteArray()
        vrf = Base64.decode(vrf, Base64.URL_SAFE)

        val rc4Key = SecretKeySpec("8z5Ag5wgagfsOuhz".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return URLDecoder.decode(vrf.toString(Charsets.UTF_8), "utf-8")
    }

    @kotlin.ExperimentalStdlibApi
    private fun rot13(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val byte = vrf[i]
            if (byte in 'A'.code..'Z'.code) {
                vrf[i] = ((byte - 'A'.code + 13) % 26 + 'A'.code).toByte()
            } else if (byte in 'a'.code..'z'.code) {
                vrf[i] = ((byte - 'a'.code + 13) % 26 + 'a'.code).toByte()
            }
        }
        return vrf
    }

    private fun vrfShift(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val shift = arrayOf(4, 3, -2, 5, 2, -4, -4, 2)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
    }

    fun serverName(id: String): ServerName {
        when (id) {
            "28" -> return ServerName.MyCloud
            "35" -> return ServerName.Mp4upload
            "40" -> return ServerName.Streamtape
            "41" -> return ServerName.Vidplay
            "45" -> return ServerName.Filemoon
        }
        return ServerName.NONE
    }
}

object VidsrcToUtils {
    fun serverName(id: String): ServerName {
        when (id) {
            "Vidplay" -> return ServerName.Vidplay
            "Filemoon" -> return ServerName.Filemoon
        }
        return ServerName.NONE
    }
}

object WpUtils {
    fun generateWpKey(r: String, m: String): String {
        val rList = r.split("\\x").toTypedArray()
        var n = ""
        val decodedM = String(base64Decode(m.split("").reversed().joinToString("")).toCharArray())
        for (s in decodedM.split("|")) {
            n += "\\x" + rList[Integer.parseInt(s) + 1]
        }
        return n
    }
}

data class ApiResponseHTML(
        @JsonProperty("status") val status: Int? = null,
        @JsonProperty("result") val html: String? = null
)

data class AniwaveResponseServer(
        @JsonProperty("status") val status: Int? = null,
        @JsonProperty("result") val result: Url? = null
)

data class Url(@JsonProperty("url") val url: String? = null)
