package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EpisodeActivity : AppCompatActivity() {

    private var episodeId = 0u
    companion object {
        const val EPISODE_ID = "io.github.maximmaxims.tsdbmobile.EPISODE_ID"
    }

    private fun updateView() {
        findViewById<TextView>(R.id.episodeIdTextView).text = episodeId.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        episodeId = intent.getIntExtra(EPISODE_ID, 0).toUInt()

        updateView()
    }
}