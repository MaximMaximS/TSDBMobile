package io.github.maximmaxims.tsdbmobile.classes

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import io.github.maximmaxims.tsdbmobile.exceptions.*
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class TSDBAPI private constructor(private val baseUrl: HttpUrl, private val creds: String?) {
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

            val parsedUrl = url.toHttpUrlOrNull() ?: return null

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

            val auth = if (username.isNullOrEmpty() || password.isNullOrEmpty()) null else Credentials.basic(
                username,
                password
            )

            return TSDBAPI(parsedUrl, auth)
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
            e(RequestFailedException(e))
        }

        override fun onResponse(call: Call, response: Response) {
            onSuccess(response)
        }
    }

    private fun extractJson(response: Response): String {
        return response.body?.string() ?: throw InvalidResponseException(response, false)
    }

    private fun <T> wrapper(response: Response, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            throw InvalidResponseException(response, false)
        }
    }

    private fun parseWatchedJson(response: Response): Boolean {
        val json = extractJson(response)
        return wrapper(response) {
            Gson().fromJson(json, Boolean::class.java)
        }
    }

    /**
     * Parse JSON of single episode id.
     *
     * @param response The response to parse.
     * @return The episode id.
     * @throws InvalidResponseException If the response is invalid.
     */
    private fun parseIdJson(response: Response): UShort {
        val json = extractJson(response)
        return try {
            json.toUShort()
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

        return try {
            Gson().fromJson(json, Array<SearchedEpisode>::class.java)
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
    private fun parseEpisodeJson(response: Response): EpisodeInfo {
        val json = response.body?.string() ?: throw InvalidResponseException(response, false)
        Log.d("TSDBAPI", json)

        return try {
            Gson().fromJson(json, EpisodeInfo::class.java)
            // Episode.create(r.title, r.premiere, r.id, r.season, r.episode, r.plot)
            //   ?: throw InvalidResponseException(response, false)


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
            url(baseUrl.newBuilder().addPathSegment("auth").addPathSegment("login").build())

            header("Authorization", newCreds)
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

    /**
     * Search episode by SE.
     *
     * @param season The season number.
     * @param episode The episode number.
     * @param onSuccess The function to run if the request is successful.
     * @param e Error callback.
     */
    fun searchBySE(season: UInt, episode: UInt, onSuccess: (UShort) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search").addPathSegment("number")
                    .addQueryParameter("s", season.toString()).addQueryParameter("e", episode.toString()).build()
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
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search").addPathSegment("title")
                    .addQueryParameter("q", title).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code != 200) {
                    throw ResponseException(response)
                }
                val result = parseSearchedEpisodeArrayJson(response)
                if (result.isEmpty()) {
                    throw UserException(ErrorType.NO_EPISODES_FOUND)
                }
                onSuccess(result)
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
    fun getEpisode(id: UShort, onSuccess: (Episode) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("info").addPathSegment(id.toString())
                    .build()

            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->
            try {
                if (response.code != 200) {
                    throw ResponseException(response)
                }
                val result = parseEpisodeJson(response)
                if (result.id != id) {
                    throw InvalidResponseException(response, true)
                }

                val watchedRequest = Request.Builder().apply {
                    url(
                        baseUrl.newBuilder().addPathSegment("episode").addPathSegment("watched")
                            .addPathSegment(id.toString()).build()
                    )
                    header("Authorization", creds)
                }.build()

                client.newCall(watchedRequest).enqueue(callback(e) { res ->
                    if (res.code != 200) {
                        throw ResponseException(res)
                    }
                    val watched = parseWatchedJson(res)
                    val episode = Episode.create(result, watched) ?: throw InvalidResponseException(res, false)
                    onSuccess(episode)
                })
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
    fun markEpisode(id: UShort, watched: Boolean, onSuccess: (Boolean) -> Unit, e: (TSDBException) -> Unit) {
        if (creds == null) {
            e(UserException(ErrorType.NOT_LOGGED_IN))
            return
        }

        val json = if (watched) "true" else "false"

        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("watched").addPathSegment(id.toString())
                    .build()
            ).post(
                json.toRequestBody("application/json".toMediaType())
            ).header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(e) { response ->


            try {
                if (response.code != 200) {
                    throw ResponseException(response)
                }
                onSuccess(parseWatchedJson(response))

            } catch (e: TSDBException) {
                e(e)
            }
        })
    }
}