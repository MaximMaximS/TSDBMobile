package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar

class SetCredsActivity : AppCompatActivity() {

    private fun loading(state: Boolean) {
        runOnUiThread {
            findViewById<EditText>(R.id.usernameEditText).isEnabled = !state
            findViewById<EditText>(R.id.passwordEditText).isEnabled = !state
            findViewById<Button>(R.id.saveCredsButton).isEnabled = !state
            findViewById<LinearProgressIndicator>(R.id.progressBar).visibility =
                if (state) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_creds)

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedSharedPreferences = EncryptedSharedPreferences.create(
            "encrypted_creds",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val usernameField = findViewById<EditText>(R.id.usernameEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)

        usernameField.setText(encryptedSharedPreferences.getString("username", ""))
        passwordField.setText(encryptedSharedPreferences.getString("password", ""))

        val button = findViewById<Button>(R.id.saveCredsButton)
        button.setOnClickListener {
            val api = TSDBAPI.getInstance(this, it) ?: return@setOnClickListener

            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username == "" || password == "") {
                // Show snackbar
                Snackbar.make(it, "Please enter username and password", Snackbar.LENGTH_LONG).show()
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