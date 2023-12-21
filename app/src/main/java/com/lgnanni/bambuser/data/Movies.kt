package com.lgnanni.bambuser.data
import com.google.gson.annotations.SerializedName

data class Movies(
    val page: Double,
    val results: List<Movie>,
    @SerializedName("total_pages")
    val totalPages: Double,
    @SerializedName("total_results")
    val totalResults: Double)