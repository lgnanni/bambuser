package com.lgnanni.bambuser.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class Photos (
    val page: Int,
    val pages: Int,
    val perPage: Int,
    val total: Int,
    val photo: List<Photo>) :Parcelable