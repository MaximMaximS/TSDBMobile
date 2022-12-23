package io.github.maximmaxims.tsdbmobile.utils

import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import io.github.maximmaxims.tsdbmobile.R
import okhttp3.Response

object ErrorUtil {
    fun showSnackbar(view: View, type: ErrorType, payload: String? = null) {
        val context = view.context
        val msg = when (type) {
            ErrorType.NO_CREDS -> context.getString(R.string.no_creds)
            ErrorType.API_FAIL -> context.getString(R.string.api_fail)
            ErrorType.INVALID_URL -> context.getString(R.string.invalid_url)
            ErrorType.FAILED_TO_PARSE -> context.getString(R.string.failed_to_parse)
            ErrorType.NO_EPISODES_FOUND -> context.getString(R.string.no_episodes_found)
            ErrorType.ID_MISMATCH -> context.getString(R.string.id_mismatch)
            ErrorType.EMPTY_CREDS -> context.getString(R.string.empty_creds)
            ErrorType.EMPTY_TITLE -> context.getString(R.string.empty_title)
            ErrorType.EMPTY_SE -> context.getString(R.string.empty_se)
        }
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
        if (type.value) {
            Log.e("TSDBMobile", "Error: ${type.name}, payload: ${payload ?: "empty"}")
        }
    }

    fun showSnackbar(view: View, response: Response) {
        val context = view.context
        val msg = when (response.code) {
            400 -> context.getString(R.string.code_400)
            401 -> context.getString(R.string.code_401)
            404 -> context.getString(R.string.code_404)
            429 -> context.getString(R.string.code_429)
            500 -> context.getString(R.string.code_500)
            else -> context.getString(R.string.code_unknown) + " ($response.code)"
        }
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
        if (response.code == 400 || response.code == 403 || response.code == 404 || response.code == 500) {
            Log.e("TSDBMobile", "Client failure: ${response}")
        }
    }
}