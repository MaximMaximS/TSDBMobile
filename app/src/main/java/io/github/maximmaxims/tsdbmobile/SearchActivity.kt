package io.github.maximmaxims.tsdbmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar

class SearchActivity : AppCompatActivity() {
    var currentList: List<Episode> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
    }

    private fun loading(state: Boolean) {
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val openButton = findViewById<Button>(R.id.openBySEButton)
        runOnUiThread {
            findViewById<EditText>(R.id.seasonEditText).isEnabled = !state
            findViewById<EditText>(R.id.episodeEditText).isEnabled = !state
            findViewById<Button>(R.id.searchBySEButton).isEnabled = !state
            findViewById<EditText>(R.id.titleEditText).isEnabled = !state
            findViewById<Button>(R.id.searchByTitleButton).isEnabled = !state
            spinner.isEnabled = !state
            openButton.isEnabled = !state
            findViewById<LinearProgressIndicator>(R.id.progressBar).visibility =
                if (state) View.VISIBLE else View.INVISIBLE
            if (state) {
                spinner.adapter = null
                spinner.visibility = View.INVISIBLE
                openButton.visibility = View.INVISIBLE
            }
        }
    }

    fun searchEpisodeBySE(view: View) {
        val seasonView = findViewById<EditText>(R.id.seasonEditText)
        val episodeView = findViewById<EditText>(R.id.episodeEditText)
        val season = seasonView.text.toString().toUIntOrNull()
        val episode = episodeView.text.toString().toUIntOrNull()
        if (season == null || episode == null) {
            Snackbar.make(view, "Please enter season and episode numbers", Snackbar.LENGTH_LONG).show()
            return
        }
        val api = TSDBAPI.getInstance(this, view) ?: return
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
        val titleView = findViewById<EditText>(R.id.titleEditText)
        val title = titleView.text.toString()
        if (title == "") {
            Snackbar.make(view, "Please enter episode title", Snackbar.LENGTH_LONG).show()
            return
        }
        val api = TSDBAPI.getInstance(this, view) ?: return
        loading(true)
        api.searchByTitle(title, view, always = {
            loading(false)
        }, onSuccess = { episodes ->
            val names = episodes.map { it.name }
            if (names.isEmpty()) {
                Snackbar.make(view, "No episodes found", Snackbar.LENGTH_LONG).show()
                return@searchByTitle
            }
            currentList = episodes
            val spinner = findViewById<Spinner>(R.id.episodeSpinner)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            runOnUiThread {
                spinner.adapter = adapter
                spinner.visibility = View.VISIBLE
                findViewById<View>(R.id.openBySEButton).visibility = View.VISIBLE
            }
        })
    }

    fun openEpisode(view: View) {
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val episode = currentList[spinner.selectedItemPosition]
        val intent = Intent(this, EpisodeActivity::class.java)
        intent.putExtra(EpisodeActivity.EPISODE_ID, episode.id.toInt())
        startActivity(intent)
    }
}