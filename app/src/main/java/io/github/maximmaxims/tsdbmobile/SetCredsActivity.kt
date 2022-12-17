package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import java.io.IOException

class SetCredsActivity : AppCompatActivity() {
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

        val usernameField = findViewById<EditText>(R.id.editTextTextUsername)
        val passwordField = findViewById<EditText>(R.id.editTextTextPassword)

        usernameField.setText(encryptedSharedPreferences.getString("username", ""))
        passwordField.setText(encryptedSharedPreferences.getString("password", ""))

        val button = findViewById<Button>(R.id.saveCredsButton)
        button.setOnClickListener {
            var url = PreferenceManager.getDefaultSharedPreferences(this).getString("api_address", "")
            if (url == "" || url == null) {
                // Show snackbar
                Snackbar.make(it, "Please set API address in settings", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
            button.isEnabled = false
            progressBar.visibility = View.VISIBLE

            if (!url.endsWith("/")) {
                url += "/"
            }

            url += "login"


            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username == "" || password == "") {
                // Show snackbar
                Snackbar.make(it, "Please enter username and password", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val client = OkHttpClient()

            val creds = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", creds)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Show snackbar
                    button.post {
                        Snackbar.make(button, "Failed to connect to API", Snackbar.LENGTH_LONG).show()
                        button.isEnabled = true
                    }
                    progressBar.post {
                        progressBar.visibility = View.INVISIBLE
                    }
                }

                override fun onResponse(call: Call, response: Response) {

                    button.post {
                        button.isEnabled = true
                    }
                    progressBar.post {
                        progressBar.visibility = View.INVISIBLE
                    }
                    when (response.code) {
                        200 -> {
                            val editor = encryptedSharedPreferences.edit()
                            editor.putString("username", username)
                            editor.putString("password", password)
                            editor.apply()
                            finish()
                        }

                        401 -> {
                            // Show snackbar
                            Snackbar.make(it, "Invalid username or password", Snackbar.LENGTH_LONG).show()
                        }

                        else -> {
                            // Show snackbar
                            Snackbar.make(it, "Unknown error " + response.code, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            })

        }
    }
}