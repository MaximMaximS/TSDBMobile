package io.github.maximmaxims.tsdbmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateButton()
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
        val button = findViewById<Button>(R.id.loginButton)
        if (username == "") {
            button.setText(R.string.login)
            button.setOnClickListener {
                val intent = Intent(this, SetCredsActivity::class.java)
                startActivity(intent)
            }
        } else {
            button.setText(R.string.logout)
            button.setOnClickListener {
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