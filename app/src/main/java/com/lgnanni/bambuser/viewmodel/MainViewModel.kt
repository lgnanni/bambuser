package com.lgnanni.bambuser.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gcorp.retrofithelper.Response
import com.gcorp.retrofithelper.ResponseHandler
import com.gcorp.retrofithelper.RetrofitClient
import com.lgnanni.bambuser.data.Movie
import com.lgnanni.bambuser.data.Movies

class MainViewModel : ViewModel() {

    private var _movies = MutableLiveData(emptyList<Movie>())

    private var _filterMovies = MutableLiveData(emptyList<Movie>())
    val filterMovies: LiveData<List<Movie>> = _filterMovies

    private var _selectedMovie = MutableLiveData(Movie())
    val selectedMovie: LiveData<Movie> = _selectedMovie

    private var _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var _searchText = MutableLiveData("")
    val searchText: LiveData<String> = _searchText

    private var _isConnected = MutableLiveData(false)

    private var _darkTheme = MutableLiveData(false)
    val darkTheme: LiveData<Boolean> = _darkTheme


    companion object {
        lateinit var retrofitClient: RetrofitClient
    }

    private val API_BASE = "https://api.themoviedb.org/3/movie/"
    private val API_HEADER_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1ZTk5N2VhODJiZmNkMTRmYTQyN2M1OTNhY2E1ZTUzZCIsInN1YiI6IjY1ODQwOTY3ODU4Njc4NTYzNWY2YWI2OSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.QbfUAnswzEFq2H4HOoLNt8vDgArbeVGpHvJGAD1kkj8"
    private val API_PATH = "popular?language=en-US&page="
    fun loadMovies(context: Context, page: Int = 1) {

        _loading.value = true
        retrofitClient = RetrofitClient.instance
            .setBaseUrl(API_BASE)
            .setConnectionTimeout(4)
            .setReadingTimeout(15)
            .enableCaching(context)
            .caching(true, context)
            //add Headers
            .addHeader("accept", "application/json")
            .addHeader("Authorization", API_HEADER_TOKEN)

        retrofitClient.Get<Movies>()
            .setPath(API_PATH + page)
            .setResponseHandler(
                Movies::class.java,
                object : ResponseHandler<Movies>() {
                    override fun onSuccess(response: Response<Movies>) {
                        super.onSuccess(response)
                        _movies.value = response.body.results
                        _filterMovies.value = _movies.value
                        _loading.value = false

                    }

                    override fun onError(response: Response<Movies>?) {
                        super.onError(response)
                        _loading.value = false
                        val text = if(!_isConnected.value!!) "Please connect to the internet to fetch data" else response.toString()
                        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    }

                    override fun onFailed(e: Throwable?) {
                        super.onFailed(e)
                        _loading.value = false
                        val text = if(!_isConnected.value!!) "Please connect to the internet to fetch data" else e?.message
                        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    }
                }).run(context)

    }

    fun filterMovies(search: String) {
        _filterMovies.value = _movies.value?.filter { it.title.contains(search, true) }
    }
    fun setSelectedPhoto(movie: Movie) {
        _selectedMovie.value = movie
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun setIsConnected(context: Context, connected: Boolean) {
        _isConnected.value = connected
        if(connected && _movies.value!!.isEmpty())
            loadMovies(context)
    }
    fun setDarkTheme(dark: Boolean) {
        _darkTheme.value = dark
    }
}