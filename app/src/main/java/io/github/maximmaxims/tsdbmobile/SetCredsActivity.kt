package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
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
            val api = TSDBAPI.getInstance(it) ?: return@setOnClickListener

            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username == "" || password == "") {
                // Show snackbar
                ErrorUtil.showSnackbar(it, ErrorType.EMPTY_CREDS)
                return@setOnClickListener
            }

            loading(true)

            api.login(username, password, it, always = {
                loading(false)
            }, onSuccess = {
                val editor = encryptedSharedPreferences.edit()
                editor.putString("username", username)
                editor.putString("password", password)
                editor.apply()
                finish()
            })

        }
    }
}