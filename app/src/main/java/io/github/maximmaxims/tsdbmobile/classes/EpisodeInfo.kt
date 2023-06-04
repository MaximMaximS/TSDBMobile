package io.github.maximmaxims.tsdbmobile.classes

data class EpisodeInfo(
    val id: UShort,
    val season: UShort,
    val episode: UShort,
    val title: String,
    val premiere: UInt,
    val plot: String
)
