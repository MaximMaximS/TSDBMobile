package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.maximmaxims.tsdbmobile.classes.Episode
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EpisodeActivity : AppCompatActivity() {

    private var episode: Episode? = null

    companion object {
        const val EPISODE_ID = "io.github.maximmaxims.tsdbmobile.EPISODE_ID"
    }

    private lateinit var watchedSwitch: SwitchMaterial
    private lateinit var detailsButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var progressBar: LinearProgressIndicator

    private lateinit var titleTextView: TextView
    private lateinit var premiereTextView: TextView
    private lateinit var directedByTextView: TextView
    private lateinit var writtenByTextView: TextView
    private lateinit var episodeIdTextView: TextView
    private fun loading(state: Boolean) {
        runOnUiThread {
            watchedSwitch.isEnabled = !state
            detailsButton.isEnabled = !state
            prevButton.isEnabled = !state
            nextButton.isEnabled = !state

            progressBar.visibility = if (state) LinearProgressIndicator.VISIBLE else LinearProgressIndicator.INVISIBLE
        }
    }

    private fun updateView(newId: UInt) {
        val view: View = findViewById(android.R.id.content)
        val api = TSDBAPI.getInstance(view) ?: return
        loading(true)
        api.getEpisode(newId, view, always = {
            loading(false)
        }, onSuccess = { episode ->
            this.episode = episode
            val instant = episode.premiere
            runOnUiThread {
                titleTextView.text = episode.title
                premiereTextView.text =
                    instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                directedByTextView.text = episode.directedBy
                writtenByTextView.text = episode.writtenBy
                watchedSwitch.isChecked = episode.watched
                episodeIdTextView.text = episode.id.toString()
            }

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        watchedSwitch = findViewById(R.id.watchedSwitch)
        detailsButton = findViewById(R.id.detailsButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        progressBar = findViewById(R.id.progressBar)

        titleTextView = findViewById(R.id.titleTextView)
        premiereTextView = findViewById(R.id.premiereTextView)
        directedByTextView = findViewById(R.id.directedByTextView)
        writtenByTextView = findViewById(R.id.writtenByTextView)
        episodeIdTextView = findViewById(R.id.episodeIdTextView)

        updateView(intent.getIntExtra(EPISODE_ID, 0).toUInt())
    }
}