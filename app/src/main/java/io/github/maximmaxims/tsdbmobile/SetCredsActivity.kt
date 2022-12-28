package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
import io.github.maximmaxims.tsdbmobile.exceptions.TSDBException
import io.github.maximmaxims.tsdbmobile.exceptions.UserException
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import io.github.maximmaxims.tsdbmobile.utils.ErrorUtil

class SetCredsActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveCredsButton: Button
    private lateinit var progressBar: LinearProgressIndicator

    private fun loading(state: Boolean) {
        runOnUiThread {
            usernameEditText.isEnabled = !state
            passwordEditText.isEnabled = !state
            saveCredsButton.isEnabled = !state
            progressBar.visibility = if (state) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_creds)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        saveCredsButton = findViewById(R.id.saveCredsButton)
        progressBar = findViewById(R.id.progressBar)

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedSharedPreferences = EncryptedSharedPreferences.create(
            "encrypted_creds",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        usernameEditText.setText(encryptedSharedPreferences.getString("username", ""))
        passwordEditText.setText(encryptedSharedPreferences.getString("password", ""))

        saveCredsButton.setOnClickListener {
            try {
                loading(true)
                val api = TSDBAPI.getInstance(this) ?: throw UserException(ErrorType.INVALID_URL)

                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (username == "" || password == "") {
                    // Show snackbar
                    throw UserException(ErrorType.EMPTY_CREDS)
                }

                api.login(username, password, onSuccess = {
                    loading(false)
                    val editor = encryptedSharedPreferences.edit()
                    editor.putString("username", username)
                    editor.putString("password", password)
                    editor.apply()
                    finish()
                }, e = { e ->
                    loading(false)
                    ErrorUtil.showSnackbar(e, it)
                })
            } catch (e: TSDBException) {
                loading(false)
                ErrorUtil.showSnackbar(e, it)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}