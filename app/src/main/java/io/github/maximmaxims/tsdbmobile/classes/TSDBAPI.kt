package io.github.maximmaxims.tsdbmobile.classes

import android.util.Log
import android.view.View
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.maximmaxims.tsdbmobile.R
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import io.github.maximmaxims.tsdbmobile.utils.ErrorUtil
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException


class TSDBAPI private constructor(private val baseUrl: HttpUrl, private val lang: String, private val creds: String?) {
    private val client = OkHttpClient()

    companion object {
        fun getInstance(view: View): TSDBAPI? {
            val context = view.context
            val url = PreferenceManager.getDefaultSharedPreferences(context).getString("api_address", "")
            if (url.isNullOrEmpty()) {
                ErrorUtil.showSnackbar(view, ErrorType.API_FAIL)
                return null
            }
            val lang = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("api_lang", context.getString(R.string.api_default_locale))!!

            val parsedUrl = url.toHttpUrlOrNull()
            if (parsedUrl == null) {
                ErrorUtil.showSnackbar(view, ErrorType.INVALID_URL)
                return null
            }

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedSharedPreferences = EncryptedSharedPreferences.create(
                "encrypted_creds",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val username = encryptedSharedPreferences.getString("username", "")
            val password = encryptedSharedPreferences.getString("password", "")

            val creds = if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                null
            } else {
                Credentials.basic(username, password)
            }

            return TSDBAPI(parsedUrl, lang, creds)
        }
    }

    private fun callback(view: View, always: () -> Unit, onSuccess: (Response) -> Unit) = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            always()
            ErrorUtil.showSnackbar(view, ErrorType.API_FAIL)
        }

        override fun onResponse(call: Call, response: Response) {
            always()
            onSuccess(response)
        }
    }

    private fun parseIdJson(response: Response): UInt? {
        val json = response.peekBody(1024).string()
        return try {
            val jsonObject = Gson().fromJson(json, JsonObject::class.java)
            jsonObject.get("id").asInt.toUInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSearchedEpisodeArrayJson(response: Response): Array<SearchedEpisode>? {
        val json = response.peekBody(4096).string()

        return try {
            Gson().fromJson(json, Array<SearchedEpisode>::class.java)
        } catch (e: Exception) {
            Log.e("TSDBAPI", "Error parsing JSON: $e")
            null
        }
    }

    private fun parseEpisodeJson(response: Response): Episode? {
        val json = response.peekBody(1024).string()

        return try {
            val episode = Gson().fromJson(json, JsonObject::class.java)
            val watched = episode.get("watched").asBoolean
            val title = episode.get("title").asString
            val id = episode.get("id").asInt.toUInt()
            val s = episode.get("season").asInt.toUInt()
            val e = episode.get("episode").asInt.toUInt()
            val premiere = episode.get("premiere").asString
            val directedBy = episode.get("directedBy").asString
            val writtenBy = episode.get("writtenBy").asString
            val plot = episode.get("plot").asString
            Episode.create(title, premiere, id, s, e, directedBy, writtenBy, plot, watched)
        } catch (e: Exception) {
            null
        }

    }

    fun login(username: String, password: String, view: View, always: () -> Unit, onSuccess: () -> Unit) {
        val newCreds = Credentials.basic(username, password)
        val request = Request.Builder().apply {
            url(baseUrl.newBuilder().addPathSegment("login").build())

            header("Authorization", newCreds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                onSuccess()
            } else {
                ErrorUtil.showSnackbar(view, response)
            }
        })
    }

    fun searchBySE(season: UInt, episode: UInt, view: View, always: () -> Unit, onSuccess: (UInt) -> Unit) {
        if (creds == null) {
            ErrorUtil.showSnackbar(view, ErrorType.NO_CREDS)
            always()
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("s").addPathSegment(season.toString())
                    .addPathSegment("e").addPathSegment(episode.toString()).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                val result = parseIdJson(response)
                if (result == null) {
                    ErrorUtil.showSnackbar(view, ErrorType.FAILED_TO_PARSE, response.body?.string())
                } else {
                    onSuccess(result)
                }
            } else {
                ErrorUtil.showSnackbar(view, response)
            }
        })
    }

    fun searchByTitle(title: String, view: View, always: () -> Unit, onSuccess: (Array<SearchedEpisode>) -> Unit) {
        if (creds == null) {
            ErrorUtil.showSnackbar(view, ErrorType.NO_CREDS)
            always()
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search")
                    .addQueryParameter("title", title).addQueryParameter("lang", lang).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                val result = parseSearchedEpisodeArrayJson(response)
                if (result == null) {
                    ErrorUtil.showSnackbar(view, ErrorType.FAILED_TO_PARSE, response.body?.string())
                } else if (result.isEmpty()) {
                    ErrorUtil.showSnackbar(view, ErrorType.NO_EPISODES_FOUND, response.body?.string())
                } else {
                    onSuccess(result)
                }
            } else {
                ErrorUtil.showSnackbar(view, response)
            }
        })
    }

    fun getEpisode(id: UInt, view: View, always: () -> Unit, onSuccess: (Episode) -> Unit) {
        if (creds == null) {
            ErrorUtil.showSnackbar(view, ErrorType.NO_CREDS)
            always()
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("id").addPathSegment(id.toString())
                    .addQueryParameter("lang", lang).build()

            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                val result = parseEpisodeJson(response)
                if (result == null) {
                    ErrorUtil.showSnackbar(view, ErrorType.FAILED_TO_PARSE, response.body?.string())
                } else if (result.id != id) {
                    ErrorUtil.showSnackbar(view, ErrorType.ID_MISMATCH, response.body?.string())
                } else {
                    onSuccess(result)
                }
            } else {
                ErrorUtil.showSnackbar(view, response)
            }
        })
    }
}