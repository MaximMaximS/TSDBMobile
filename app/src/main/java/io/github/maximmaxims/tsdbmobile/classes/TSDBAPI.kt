package io.github.maximmaxims.tsdbmobile.classes

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.maximmaxims.tsdbmobile.R
import io.github.maximmaxims.tsdbmobile.exceptions.InvalidResponseException
import io.github.maximmaxims.tsdbmobile.exceptions.ResponseException
import io.github.maximmaxims.tsdbmobile.exceptions.TSDBException
import io.github.maximmaxims.tsdbmobile.exceptions.UserException
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException


class TSDBAPI private constructor(private val baseUrl: HttpUrl, private val lang: String, private val creds: String?) {
    private val client = OkHttpClient()

    companion object {
        /**
         * Create a new instance of TSDBAPI.
         *
         * @return A new instance of TSDBAPI.
         */
        fun getInstance(context: Context): TSDBAPI? {
            val url = PreferenceManager.getDefaultSharedPreferences(context).getString("api_address", "")
            if (url.isNullOrEmpty()) {
                return null
            }
            val lang = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("api_language", context.getString(R.string.api_default_language)) ?: "en"

            val parsedUrl = url.toHttpUrlOrNull() ?: return null

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedSharedPreferences = EncryptedSharedPreferences.create(
                "encrypted_creds",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val username = encryptedSharedPreferences.getString("username", "") ?: ""
            val password = encryptedSharedPreferences.getString("password", "") ?: ""

            return TSDBAPI(parsedUrl, lang, Credentials.basic(username, password))
        }
    }

    /**
     * Callback builder
     *
     * @param e Error callback
     * @param onSuccess The function to run if the request is successful.
     * @return A new Callback object.
     */
    private fun callback(e: (TSDBException) -> Unit, onSuccess: (Response) -> Unit) = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e(UserException(ErrorType.API_FAIL))
        }

        override fun onResponse(call: Call, response: Response) {
            onSuccess(response)
        }
    }

    /**
     * Parse JSON of single episode id.
     *
     * @param response The response to parse.
     * @return The episode id.
     * @throws InvalidResponseException If the response is invalid.
     */
    private fun parseIdJson(response: Response): UInt {
        val json = response.body?.string() ?: throw InvalidResponseException(response, false)
        return try {
            val jsonObject = Gson().fromJson(json, JsonObject::class.java)
            jsonObject.get("id").asInt.toUInt()
        } catch (e: Exception) {
            throw InvalidResponseException(response, false)
        }
    }

    /**
     * Parse JSON of multiple episode titles and ids.
     *
     * @param response The response to parse.
     * @return An array of episode titles and ids.
     * @throws InvalidResponseException If the response is invalid.
     */
    private fun parseSearchedEpisodeArrayJson(response: Response): Array<SearchedEpisode> {
        val json = response.body?.string() ?: throw InvalidResponseException(response, false)

        try {
            return Gson().fromJson(json, Array<SearchedEpisode>::class.java)
        } catch (e: Exception) {
            throw InvalidResponseException(response, false)
        }
    }

    /**
     * Parse JSON of single episode.
     *
     * @param response The response to parse.
     * @return The episode.
     * @throws InvalidResponseException If the response is invalid.
     */
    private fun parseEpisodeJson(response: Response): Episode {
        val json = response.body?.string() ?: throw InvalidResponseException(response, false)
        Log.d("TSDBAPI", json)

        try {
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
            return Episode.create(title, premiere, id, s, e, directedBy, writtenBy, plot, watched)
                ?: throw InvalidResponseException(response, false)
        } catch (e: Exception) {
            throw InvalidResponseException(response, false)
        }
    }

    /**
     * Log in to the API.
     *
     * @param username The username to log in with.
     * @param password The password to log in with.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun login(username: String, password: String, onSuccess: () -> Unit, e: (TSDBException) -> Unit) {
        val newCreds = Credentials.basic(username, password)
        val request = Request.Builder().apply {
            url(baseUrl.newBuilder().addPathSegment("login").build())

            header("Authorization", newCreds)
        }.build()
        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code == 200) {
                    onSuccess()
                } else {
                    throw ResponseException(response)
                }
            } catch (e: TSDBException) {
                e(e)
            }
        })
    }

    /**
     * Search episode by SE.
     *
     * @param season The season number.
     * @param episode The episode number.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun searchBySE(season: UInt, episode: UInt, onSuccess: (UInt) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("s").addPathSegment(season.toString())
                    .addPathSegment("e").addPathSegment(episode.toString()).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code == 200) {
                    val result = parseIdJson(response)
                    onSuccess(result)
                } else {
                    throw ResponseException(response)
                }
            } catch (e: TSDBException) {
                e(e)
            }
        })
    }

    /**
     * Search episode by title.
     *
     * @param title The title to search for.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun searchByTitle(title: String, onSuccess: (Array<SearchedEpisode>) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search")
                    .addQueryParameter("title", title).addQueryParameter("lang", lang).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code == 200) {
                    val result = parseSearchedEpisodeArrayJson(response)
                    if (result.isEmpty()) {
                        throw UserException(ErrorType.NO_EPISODES_FOUND)
                    } else {
                        onSuccess(result)
                    }
                } else {
                    throw ResponseException(response)
                }
            } catch (e: TSDBException) {
                e(e)
            }
        })
    }

    /**
     * Get episode by id.
     *
     * @param id The id of the episode.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun getEpisode(id: UInt, onSuccess: (Episode) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("id").addPathSegment(id.toString())
                    .addQueryParameter("lang", lang).build()

            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code == 200) {
                    val result = parseEpisodeJson(response)
                    if (result.id != id) {
                        throw InvalidResponseException(response, true)
                    } else {
                        onSuccess(result)
                    }
                } else {
                    throw ResponseException(response)
                }
            } catch (e: TSDBException) {
                e(e)
            }
        })
    }

    /**
     * Mark episode as watched or unwatched.
     *
     * @param id The id of the episode.
     * @param watched Whether the episode should be marked as watched or unwatched.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun markEpisode(id: UInt, watched: Boolean, onSuccess: () -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("id").addPathSegment(id.toString())
                    .addPathSegment("watch").build()
            ).post(
                FormBody.Builder().add("state", if (watched) "true" else "false").build()
            ).header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code == 204) {
                    onSuccess()
                } else {
                    throw ResponseException(response)
                }
            } catch (e: TSDBException) {
                e(e)
            }
        })
    }
}