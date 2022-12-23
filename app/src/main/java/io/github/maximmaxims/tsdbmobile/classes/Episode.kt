package io.github.maximmaxims.tsdbmobile.classes

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class Episode private constructor(
    val title: String,
    val premiere: Instant,
    val id: UInt,
    val season: UInt,
    val episode: UInt,
    val directedBy: String,
    val writtenBy: String,
    val plot: String,
    val watched: Boolean
) {
    companion object {
        fun create(
            title: String,
            premiere: String,
            id: UInt,
            season: UInt,
            episode: UInt,
            directedBy: String,
            writtenBy: String,
            plot: String,
            watched: Boolean
        ): Episode? {
            val date = Instant.parse(premiere)
            val zoned = ZonedDateTime.ofInstant(date, ZoneId.of("UTC"))
            // Check if time is 00:00:00
            if (zoned.hour != 0 || zoned.minute != 0 || zoned.second != 0) {
                Log.e("Episode", "Premiere time is not 00:00:00 ($id)")
                return null
            }

            return Episode(title, date, id, season, episode, directedBy, writtenBy, plot, watched)
        }
    }
}
