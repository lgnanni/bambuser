package com.lgnanni.bambuser.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class Photo (
    val id: String,
    val owner: String,
    val secret: String,
    val server: Int,
    val title: String,
    val isPublic: Boolean,
    val isFriend: Boolean,
    val isFamily: Boolean): Parcelable {
    constructor() : this("", "", "", -1, "", false, false, false)
    fun getUrl() : String {
        return StringBuilder()
            .append("https://live.staticflickr.com/")
            .append(server)
            .append('/')
            .append(id)
            .append('_')
            .append(secret)
            .append('_')
            .append('b')
            .append(".jpg").toString()
    }
}

