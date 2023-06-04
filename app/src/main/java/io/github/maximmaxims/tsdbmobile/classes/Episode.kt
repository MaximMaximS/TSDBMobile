package io.github.maximmaxims.tsdbmobile.classes

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class Episode private constructor(
    val title: String,
    val premiere: Instant,
    val id: UShort,
    val season: UShort,
    val episode: UShort,
    val plot: String,
    val watched: Boolean
) {
    companion object {
        private fun create(
            title: String,
            premiere: UInt,
            id: UShort,
            season: UShort,
            episode: UShort,
            plot: String,
            watched: Boolean
        ): Episode? {
            // Parse JS timestamp
            val date = Instant.ofEpochSecond(premiere.toLong())
            val zoned = ZonedDateTime.ofInstant(date, ZoneId.of("UTC"))
            // Check if time is 00:00:00
            if (zoned.hour != 0 || zoned.minute != 0 || zoned.second != 0) {
                Log.e("Episode", "Premiere time is not 00:00:00 ($id)")
                return null
            }

            return Episode(title, date, id, season, episode, plot, watched)
        }

        fun create(i: EpisodeInfo, w: Boolean): Episode? {
            return create(i.title, i.premiere, i.id, i.season, i.episode, i.plot, w)
        }
    }
}
