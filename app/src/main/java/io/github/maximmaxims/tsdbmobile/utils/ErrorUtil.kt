package io.github.maximmaxims.tsdbmobile.utils

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import io.github.maximmaxims.tsdbmobile.R
import io.github.maximmaxims.tsdbmobile.exceptions.InvalidResponseException
import io.github.maximmaxims.tsdbmobile.exceptions.ResponseException
import io.github.maximmaxims.tsdbmobile.exceptions.TSDBException
import io.github.maximmaxims.tsdbmobile.exceptions.UserException

object ErrorUtil {
    /**
     * Show a snackbar with the error message.
     *
     * @param context The context to use. Usually your Activity object.
     * @param type The type of error.
     */
    private fun getMessage(context: Context, type: ErrorType): String {
        return when (type) {
            ErrorType.NOT_LOGGED_IN -> context.getString(R.string.not_logged_in)
            ErrorType.API_FAIL -> context.getString(R.string.api_fail)
            ErrorType.INVALID_URL -> context.getString(R.string.invalid_url)
            ErrorType.NO_EPISODES_FOUND -> context.getString(R.string.no_episodes_found)
            ErrorType.EMPTY_CREDS -> context.getString(R.string.empty_creds)
            ErrorType.EMPTY_TITLE -> context.getString(R.string.empty_title)
            ErrorType.EMPTY_SE -> context.getString(R.string.empty_se)
            ErrorType.NO_PLOT -> context.getString(R.string.no_plot)
        }
    }

    /**
     * Show a snackbar with the error message.
     *
     * @param e The exception to use.
     * @param view The view to use. Usually your Activity's content view.
     */
    fun showSnackbar(e: TSDBException, view: View) {
        val context = view.context
        val snackMsg = when (e) {
            is ResponseException -> {
                val response = e.response
                val msg = when (response.code) {
                    400 -> context.getString(R.string.code_400)
                    401 -> context.getString(R.string.code_401)
                    404 -> context.getString(R.string.code_404)
                    429 -> context.getString(R.string.code_429)
                    500 -> context.getString(R.string.code_500)
                    else -> context.getString(R.string.code_unknown) + " (${response.code})"
                }
                msg
            }

            is InvalidResponseException -> {
                Log.e("ErrorUtil", "Failed to parse response", e)
                Log.e("ErrorUtil", "Response: ${e.response}")
                if (e.isId) {
                    context.getString(R.string.id_mismatch)
                } else {
                    context.getString(R.string.failed_to_parse)
                }
            }

            is UserException -> {
                getMessage(context, e.type)
            }

            else -> {
                Log.e("ErrorUtil", "Unknown error", e)
                context.getString(R.string.unknown_error)
            }
        }
        Snackbar.make(view, snackMsg, Snackbar.LENGTH_LONG).show()

    }
}