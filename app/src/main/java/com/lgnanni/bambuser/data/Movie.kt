package com.lgnanni.bambuser.data

import com.google.gson.annotations.SerializedName

data class Movie (
    val adult: Boolean,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    @SerializedName("genre_ids")
    val genreIds: List<Int>,
    val id: Long,
    @SerializedName("original_language")
    val originalLanguage: String,
    @SerializedName("original_title")
    val originalTitle: String,
    val overview: String,
    val popularity: Double,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("release_date")
    val releaseDate: String,
    val title: String,
    val video: Boolean,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int
    ) {
    constructor() : this(
        false,
        "",
        emptyList(),
        -1,
        "",
        "",
        "",
        -1.0,
        "",
        "",
        "",
        false,
        -1.0,
        -1)

    constructor(title: String) : this(
        false,
        "",
        emptyList(),
        -1,
        "",
        "",
        "",
        -1.0,
        "",
        "",
        title,
        false,
        -1.0,
        -1)

    fun getMoviePoster() : String {
        val POSTER_BASE = "https://image.tmdb.org/t/p/original/"
        return POSTER_BASE + posterPath
    }
}

