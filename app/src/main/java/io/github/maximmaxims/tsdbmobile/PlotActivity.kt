package io.github.maximmaxims.tsdbmobile

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlotActivity : AppCompatActivity() {
    companion object {
        const val PLOT = "io.github.maximmaxims.tsdbmobile.PLOT"
        const val TITLE = "io.github.maximmaxims.tsdbmobile.TITLE"
    }

    private lateinit var plotTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plot)

        plotTextView = findViewById(R.id.plotTextView)

        val plot = intent.getStringExtra(PLOT)
        val title = intent.getStringExtra(TITLE)

        plotTextView.text = plot
        plotTextView.movementMethod = ScrollingMovementMethod()
        setTitle(title)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}