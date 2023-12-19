package com.lgnanni.bambuser.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gcorp.retrofithelper.Response
import com.gcorp.retrofithelper.ResponseHandler
import com.gcorp.retrofithelper.RetrofitClient
import com.lgnanni.bambuser.data.Photo
import com.lgnanni.bambuser.data.PhotosWrapper
import com.lgnanni.bambuser.util.MainViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class MainViewModel : ViewModel() {

    private var _photos = MutableLiveData(emptyList<Photo>())
    val photos: LiveData<List<Photo>> = _photos

    private var _selectedPhoto = MutableLiveData(Photo())
    val selectedPhoto: LiveData<Photo> = _selectedPhoto

    private var _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var _searchText = MutableLiveData("")
    val searchText: LiveData<String> = _searchText

    private var _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private var _darkTheme = MutableLiveData(false)
    val darkTheme: LiveData<Boolean> = _darkTheme

    companion object {
        lateinit var retrofitClient: RetrofitClient
    }

    private val API_BASE = "https://api.flickr.com/services/rest/"
    private val API_KEY = "171f377e4b52f2cd6740dc0ce789b8e0"

    fun loadPhotos(context: Context, tag: String = "bambuser") {
        _loading.value = true
        retrofitClient = RetrofitClient.instance
            .setBaseUrl(API_BASE)
            .setConnectionTimeout(4)
            .setReadingTimeout(15)
            .enableCaching(context)
            .caching(true, context)
            //add Headers
            .addHeader("Content-Type", "application/json")
            .addHeader("client", "android")
            .addHeader("language", Locale.getDefault().language)
            .addHeader("os", android.os.Build.VERSION.RELEASE)

        retrofitClient.Get<PhotosWrapper>()
            .setPath("?method=flickr.photos.search" +
                    "&api_key=$API_KEY" +
                    "&tags=$tag" +
                    "&media=photo" +
                    "&per_page=21" +
                    "&page=1" +
                    "&format=json" +
                    "&nojsoncallback=1")
            .setResponseHandler(PhotosWrapper::class.java,
                object : ResponseHandler<PhotosWrapper>() {
                    override fun onSuccess(response: Response<PhotosWrapper>) {
                        super.onSuccess(response)
                        _loading.value = false
                        _photos.value = response.body.photos.photo
                    }

                    override fun onError(response: Response<PhotosWrapper>?) {
                        super.onError(response)
                        _loading.value = false
                        Toast.makeText(context, response.toString(), Toast.LENGTH_LONG).show()
                    }

                    override fun onFailed(e: Throwable?) {
                        super.onFailed(e)
                        _loading.value = false
                        Toast.makeText(context, e?.message, Toast.LENGTH_LONG).show()
                    }
                }).run(context)

    }

    fun setSelectedPhoto(photo: Photo) {
        _selectedPhoto.value = photo
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun setIsConnected(connected: Boolean) {
        _isConnected.value = connected
    }
    fun refresh(context: Context) {
        val tag = searchText.value!!.ifEmpty { "bambuser" }
        loadPhotos(
            context = context,
            tag = tag)
    }

    fun setDarkTheme(dark: Boolean) {
        _darkTheme.value = dark
    }
}