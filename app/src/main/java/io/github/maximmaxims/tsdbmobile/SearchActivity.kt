package io.github.maximmaxims.tsdbmobile

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.maximmaxims.tsdbmobile.classes.SearchedEpisode
import io.github.maximmaxims.tsdbmobile.classes.TSDBAPI
import io.github.maximmaxims.tsdbmobile.exceptions.TSDBException
import io.github.maximmaxims.tsdbmobile.exceptions.UserException
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
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
        try {
            loading(true)
            if (season == null || episode == null) {
                throw UserException(ErrorType.EMPTY_SE)
            }
            val api = TSDBAPI.getInstance(this) ?: throw UserException(ErrorType.INVALID_URL)
            api.searchBySE(season, episode, onSuccess = { id ->
                loading(false)
                val intent = Intent(this, EpisodeActivity::class.java)
                intent.putExtra(EpisodeActivity.EPISODE_ID, id.toInt())
                startActivity(intent)
            }, e = { e ->
                loading(false)
                ErrorUtil.showSnackbar(e, view)
            })
        } catch (e: TSDBException) {
            loading(false)
            ErrorUtil.showSnackbar(e, view)
        }
    }

    fun searchEpisodeByTitle(view: View) {
        val title = titleEditText.text.toString()
        try {
            loading(true)
            if (title == "") {
                throw UserException(ErrorType.EMPTY_TITLE)
            }
            val api = TSDBAPI.getInstance(this) ?: throw UserException(ErrorType.INVALID_URL)
            api.searchByTitle(title, onSuccess = { episodes ->
                loading(false)
                val names = episodes.map { it.title }
                currentList = episodes
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                runOnUiThread {
                    episodeSpinner.adapter = adapter
                    episodeSpinner.visibility = View.VISIBLE
                    openBySEButton.visibility = View.VISIBLE
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

    fun openEpisode(@Suppress("UNUSED_PARAMETER") view: View) {
        val episode = currentList[episodeSpinner.selectedItemPosition]
        val intent = Intent(this, EpisodeActivity::class.java)
        intent.putExtra(EpisodeActivity.EPISODE_ID, episode.id.toInt())
        startActivity(intent)
    }
}