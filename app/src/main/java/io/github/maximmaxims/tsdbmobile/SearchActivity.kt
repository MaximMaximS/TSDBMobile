package io.github.maximmaxims.tsdbmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.maximmaxims.tsdbmobile.classes.SearchedEpisode
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
import io.github.maximmaxims.tsdbmobile.utils.ErrorType
import io.github.maximmaxims.tsdbmobile.utils.ErrorUtil

class SearchActivity : AppCompatActivity() {
    private lateinit var seasonEditText: EditText
    private lateinit var episodeEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var searchBySEButton: Button
    private lateinit var searchByTitleButton: Button
    private lateinit var episodeSpinner: Spinner
    private lateinit var openBySEButton: Button
    private lateinit var progressBar: LinearProgressIndicator

    private var currentList: Array<SearchedEpisode> = arrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        seasonEditText = findViewById(R.id.seasonEditText)
        episodeEditText = findViewById(R.id.episodeEditText)
        titleEditText = findViewById(R.id.titleEditText)
        searchBySEButton = findViewById(R.id.searchBySEButton)
        searchByTitleButton = findViewById(R.id.searchByTitleButton)
        episodeSpinner = findViewById(R.id.episodeSpinner)
        openBySEButton = findViewById(R.id.openBySEButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loading(state: Boolean) {
        runOnUiThread {
            seasonEditText.isEnabled = !state
            episodeEditText.isEnabled = !state
            searchBySEButton.isEnabled = !state
            titleEditText.isEnabled = !state
            searchByTitleButton.isEnabled = !state
            episodeSpinner.isEnabled = !state
            openBySEButton.isEnabled = !state
            progressBar.visibility = if (state) View.VISIBLE else View.INVISIBLE
            if (state) {
                episodeSpinner.adapter = null
                episodeSpinner.visibility = View.INVISIBLE
                openBySEButton.visibility = View.INVISIBLE
            }
        }
    }

    fun searchEpisodeBySE(view: View) {
        val season = seasonEditText.text.toString().toUIntOrNull()
        val episode = episodeEditText.text.toString().toUIntOrNull()
        if (season == null || episode == null) {
            ErrorUtil.showSnackbar(view, ErrorType.EMPTY_SE)
            return
        }
        val api = TSDBAPI.getInstance(view) ?: return
        loading(true)
        api.searchBySE(season, episode, view, always = {
            loading(false)
        }, onSuccess = { id ->
            val intent = Intent(this, EpisodeActivity::class.java)
            intent.putExtra(EpisodeActivity.EPISODE_ID, id.toInt())
            startActivity(intent)
        })
    }

    fun searchEpisodeByTitle(view: View) {
        val title = titleEditText.text.toString()
        if (title == "") {
            ErrorUtil.showSnackbar(view, ErrorType.EMPTY_TITLE)
            return
        }
        val api = TSDBAPI.getInstance(view) ?: return
        loading(true)
        api.searchByTitle(title, view, always = {
            loading(false)
        }, onSuccess = { episodes ->
            val names = episodes.map { it.title }
            currentList = episodes
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            runOnUiThread {
                episodeSpinner.adapter = adapter
                episodeSpinner.visibility = View.VISIBLE
                openBySEButton.visibility = View.VISIBLE
            }
        })
    }

    fun openEpisode(view: View) {
        val episode = currentList[episodeSpinner.selectedItemPosition]
        val intent = Intent(this, EpisodeActivity::class.java)
        intent.putExtra(EpisodeActivity.EPISODE_ID, episode.id.toInt())
        startActivity(intent)
    }
}