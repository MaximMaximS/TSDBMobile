package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.maximmaxims.tsdbmobile.classes.Episode
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
import io.github.maximmaxims.tsdbmobile.exceptions.TSDBException
import io.github.maximmaxims.tsdbmobile.exceptions.UserException
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import io.github.maximmaxims.tsdbmobile.utils.ErrorUtil
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EpisodeActivity : AppCompatActivity() {

    private var episode: Episode? = null

    companion object {
        const val EPISODE_ID = "io.github.maximmaxims.tsdbmobile.EPISODE_ID"
    }

    private lateinit var watchedSwitch: SwitchMaterial
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var progressBar: LinearProgressIndicator

    private lateinit var titleTextView: TextView
    private lateinit var premiereTextView: TextView
    private lateinit var episodeNumberTextView: TextView
    private lateinit var plotTextView: TextView
    private lateinit var episodeIdTextView: TextView
    private fun loading(state: Boolean) {
        val valid = episode != null && !state
        runOnUiThread {
            watchedSwitch.isEnabled = valid
            prevButton.isEnabled = valid
            nextButton.isEnabled = valid

            progressBar.visibility = if (state) LinearProgressIndicator.VISIBLE else LinearProgressIndicator.INVISIBLE
        }
    }

    private fun updateView(newId: UShort, view: View) {
        try {
            loading(true)
            val api = TSDBAPI.getInstance(this) ?: throw UserException(ErrorType.INVALID_URL)
            api.getEpisode(newId, onSuccess = { episode ->
                this.episode = episode
                loading(false)
                val instant = episode.premiere
                runOnUiThread {
                    titleTextView.text = episode.title
                    premiereTextView.text =
                        instant.atZone(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    watchedSwitch.isChecked = episode.watched
                    episodeNumberTextView.text = getString(
                        R.string.episode_se,
                        episode.season.toString().padStart(2, '0'),
                        episode.episode.toString().padStart(2, '0')
                    )
                    plotTextView.text = episode.plot
                    episodeIdTextView.text = episode.id.toString()
                    prevButton.isEnabled = episode.id != 1u.toUShort()
                }
            }, e = { e ->
                loading(false)
                ErrorUtil.showSnackbar(e, view)
            })
        } catch (e: TSDBException) {
            loading(false)
            ErrorUtil.showSnackbar(e, view)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        watchedSwitch = findViewById(R.id.watchedSwitch)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        progressBar = findViewById(R.id.progressBar)

        titleTextView = findViewById(R.id.titleTextView)
        premiereTextView = findViewById(R.id.premiereTextView)
        episodeNumberTextView = findViewById(R.id.episodeNumberTextView)
        plotTextView = findViewById(R.id.plotTextView)
        episodeIdTextView = findViewById(R.id.episodeIdTextView)

        updateView(intent.getIntExtra(EPISODE_ID, 0).toUShort(), progressBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun nextEpisode(view: View) {
        val id = episode?.id ?: return
        updateView((id + 1u).toUShort(), view)
    }

    fun prevEpisode(view: View) {
        val id = episode?.id ?: return
        if (id == 1u.toUShort()) return
        updateView((id - 1u).toUShort(), view)
    }

    fun markEpisode(view: View) {
        val episode = episode ?: return
        val value = watchedSwitch.isChecked
        if (episode.watched == value) return
        try {
            loading(true)
            val api = TSDBAPI.getInstance(this) ?: throw UserException(ErrorType.INVALID_URL)
            api.markEpisode(episode.id, value, onSuccess = {
                updateView(episode.id, view)
            }, e = { e ->
                runOnUiThread {
                    watchedSwitch.isChecked = episode.watched
                }
                loading(false)
                ErrorUtil.showSnackbar(e, view)
            })
        } catch (e: TSDBException) {
            watchedSwitch.isChecked = episode.watched
            loading(false)
            ErrorUtil.showSnackbar(e, view)
        }

    }
}