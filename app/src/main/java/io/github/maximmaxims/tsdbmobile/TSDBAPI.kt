package io.github.maximmaxims.tsdbmobile

import android.content.Context
import android.view.View
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class TSDBAPI private constructor(private val baseUrl: HttpUrl, private val lang: String) {
    private val client = OkHttpClient()
    private var creds: String = ""

    companion object {
        private fun getInstance(context: Context): TSDBAPI? {
            val url = PreferenceManager.getDefaultSharedPreferences(context).getString("api_address", "")
            if (url.isNullOrEmpty()) {
                return null
            }
            val lang = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("api_lang", context.getString(R.string.api_default_locale))!!
            val parsedUrl = url.toHttpUrlOrNull() ?: return null
            return TSDBAPI(parsedUrl, lang)
        }

        fun getInstance(context: Context, view: View): TSDBAPI? {
            val api = getInstance(context)
            if (api == null) {
                // Show snackbar
                Snackbar.make(view, "Please set API address in settings", Snackbar.LENGTH_LONG).show()
                return null
            }
            return api
        }
    }

    private fun failureCallback(response: Response, view: View) {
        when (response.code) {
            400 -> Snackbar.make(view, "Bad request", Snackbar.LENGTH_LONG).show()
            401 -> Snackbar.make(view, "Invalid credentials", Snackbar.LENGTH_LONG).show()
            404 -> Snackbar.make(view, "API not found", Snackbar.LENGTH_LONG).show()
            500 -> Snackbar.make(view, "Internal server error", Snackbar.LENGTH_LONG).show()
            else -> Snackbar.make(view, "Unknown error (${response.code})", Snackbar.LENGTH_LONG).show()
        }
    }

    fun failureCallback(view: View) {
        Snackbar.make(view, "Failed to connect to API", Snackbar.LENGTH_LONG).show()
    }

    private fun callback(view: View, always: () -> Unit, onResponse: (Response) -> Unit) = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            always()
            failureCallback(view)
        }

        override fun onResponse(call: Call, response: Response) {
            always()
            onResponse(response)
        }
    }

    private fun parseIdJson(body: ResponseBody?, success: (UInt) -> Unit) {
        if (body == null) {
            return
        }
        val json = body.string()
        try {
            val obj = JSONObject(json)
            val id = obj.getInt("id").toUInt()
            success(id)
        } catch (e: JSONException) {
            return
        }
    }

    private fun parseEpisodeArrayJson(body: ResponseBody?, success: (List<Episode>) -> Unit) {
        if (body == null) {
            return
        }
        val json = body.string()
        try {
            val arr = JSONArray(json)
            val episodes = mutableListOf<Episode>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id").toUInt()
                val name = obj.getString("name")
                episodes.add(Episode(name, id))
            }
            success(episodes)
        } catch (e: JSONException) {
            return
        }
    }

    fun login(username: String, password: String, view: View, always: () -> Unit, onSuccess: () -> Unit) {
        creds = Credentials.basic(username, password)

        val request = Request.Builder().apply {
            url(baseUrl.newBuilder().addPathSegment("login").build())
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                onSuccess()
            } else {
                failureCallback(response, view)
            }
        })
    }

    fun searchBySE(season: UInt, episode: UInt, view: View, always: () -> Unit, onSuccess: (UInt) -> Unit) {
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("s").addPathSegment(season.toString())
                    .addPathSegment("e").addPathSegment(episode.toString()).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                parseIdJson(response.body) { id ->
                    onSuccess(id)
                }
            } else {
                failureCallback(response, view)
            }
        })
    }

    fun searchByTitle(title: String, view: View, always: () -> Unit, onSuccess: (List<Episode>) -> Unit) {
        println(
            baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search").addQueryParameter("name", title)
                .addQueryParameter("lang", lang).build()
        )
        val request = Request.Builder().apply {
            url(
                baseUrl.newBuilder().addPathSegment("episode").addPathSegment("search").addQueryParameter("name", title)
                    .addQueryParameter("lang", lang).build()
            )
            header("Authorization", creds)
        }.build()

        client.newCall(request).enqueue(callback(view, always) { response ->
            if (response.code == 200) {
                parseEpisodeArrayJson(response.body) { episodes ->
                    onSuccess(episodes)
                }
            } else {
                failureCallback(response, view)
            }
        })
    }
}