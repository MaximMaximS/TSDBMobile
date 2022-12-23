package io.github.maximmaxims.tsdbmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton = findViewById(R.id.loginButton)

        updateButton()

        // TODO: Remove before release
        val intent = Intent(this, EpisodeActivity::class.java)
        intent.putExtra(EpisodeActivity.EPISODE_ID, 1)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateButton()
    }

    private fun updateButton() {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedSharedPreferences = EncryptedSharedPreferences.create(
            "encrypted_creds",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val username = encryptedSharedPreferences.getString("username", "")


        if (username == "") {
            loginButton.setText(R.string.login)
            loginButton.setOnClickListener {
                val intent = Intent(this, SetCredsActivity::class.java)
                startActivity(intent)
            }
        } else {
            loginButton.setText(R.string.logout)
            loginButton.setOnClickListener {
                val editor = encryptedSharedPreferences.edit()
                editor.putString("username", "")
                editor.putString("password", "")
                editor.apply()
                updateButton()
            }
        }
    }

    fun openPreferences(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun openSearch(view: View) {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }
}