package com.lgnanni.bambuser

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.*
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lgnanni.bambuser.data.Movie
import com.lgnanni.bambuser.ui.theme.BambuserDemoTheme
import com.lgnanni.bambuser.util.ConnectionState
import com.lgnanni.bambuser.util.connectivityState
import com.lgnanni.bambuser.viewmodel.MainViewModel
import com.lgnanni.bambuserdemo.R
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {

   val mainViewModel: MainViewModel by viewModels()
    //Initialization of job
    private var job = Job()

    // Initialization of scope for the coroutine to run in
    private var scopeForSaving = CoroutineScope(job + Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContent {
            BambuserDemoTheme {
                MovieListLayout()
            }
        }


    }

    @OptIn(
        ExperimentalCoroutinesApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    fun MovieListLayout(viewModel: MainViewModel = mainViewModel) {

        viewModel.setDarkTheme(isSystemInDarkTheme())
        viewModel.loadMovies(this)
        val darkTheme = viewModel.darkTheme.observeAsState(false)

        // Instead of always setting enabled to true at the beginning,
        // you need to check the state ahead of time to know what the
        // initial enabled state should be
        val callback = object : OnBackPressedCallback(
            false
        ) {
            override fun handleOnBackPressed() {
                viewModel.setSelectedPhoto(Movie())
            }
        }

        ConnectivityStatus(context = LocalContext.current, viewModel = viewModel)

        callback.isEnabled = viewModel.selectedMovie.observeAsState().value!!.title.isNotEmpty()

        onBackPressedDispatcher.addCallback(this, callback)

        BambuserDemoTheme(darkTheme.value) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                content = { padding ->
                    Box(
                        Modifier
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        val moviesList = viewModel.filterMovies.observeAsState().value
                        val selectedMovie = viewModel.selectedMovie.observeAsState().value
                        val loading = viewModel.loading.observeAsState().value

                        //Loader indicator trying to get the api call for the photos
                        AnimatedVisibility(
                            visible = loading!!,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(1.0f)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxSize(0.5f)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        MovieList(
                            viewModel = viewModel,
                            movieList = moviesList!!,
                            movie = selectedMovie!!,
                            loading = loading
                        )

                        MovieSelected(viewModel = viewModel, movie = selectedMovie)
                    }
                },
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text(
                                getString(R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },

                        actions = {
                            val isDarkOn = darkTheme.value

                            IconButton(
                                onClick = {
                                    viewModel.setDarkTheme(!darkTheme.value)
                                }) {
                                val icon = if (isDarkOn) Filled.LightMode else Filled.DarkMode
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Light / Dark Switch"
                                )
                            }
                        })
                })
        }
    }

        /**
         * Main view with search bar and photos list
         * @param movieList = The list of Photo objects returned from Flickr photo.search
         * @param movie = The selected photo object, if any
         * @param searchText = The actual search text if any
         * @param loading = If we are retrieving Flick.photo.search
         */
        @Composable
        fun MovieList(
            viewModel: MainViewModel,
            movieList: List<Movie>,
            movie: Movie,
            loading: Boolean
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = !loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextSearchBar(
                        viewModel = viewModel,
                        modifier = Modifier.padding(8.dp, 0.dp),
                        label = "Filter title"
                    )
                }
                AnimatedVisibility(
                    visible = movieList.isNotEmpty() && movie.title.isBlank() && !loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(Modifier.height(2.dp))
                    MovieColumn(viewModel = viewModel, movies = movieList)
                }
                AnimatedVisibility(
                    visible = movieList.isEmpty() && !loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "No movie found with that title",
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        @ExperimentalCoroutinesApi
        @Composable
        fun ConnectivityStatus(context: Context, viewModel: MainViewModel) {
            // This will cause re-composition on every network state change
            val connection by connectivityState()
            val isConnected = connection === ConnectionState.Available
            viewModel.setIsConnected(context, isConnected)
        }

        /**
         * The expanded view on selecting one item from the photos grid
         * @param movie = The selected photo object, if any
         */
        @Composable
        fun MovieSelected(viewModel: MainViewModel, movie: Movie) {
            AnimatedVisibility(
                visible = movie.title.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = {
                    it / 2
                }),
                exit = slideOutVertically(targetOffsetY = {
                    it / 2
                })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(1.0f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        //Dismiss expanded image
                        .clickable { onBackPressedDispatcher.onBackPressed() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(1f))
                    //Bitmap prepared for if the user desires to save the image
                    var imageBitmap: Bitmap? = null
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.5f)
                        .background(MaterialTheme.colorScheme.outline)
                        .border(2.dp, MaterialTheme.colorScheme.primary)

                        //Dismiss expanded image
                        .clickable { onBackPressedDispatcher.onBackPressed() },
                        ) {
                        GlideImage(
                            modifier = Modifier
                                .align(Alignment.Center),
                            imageModel = movie.getMoviePoster(),
                            success = {
                                //Instead of just loading the image with Glide,
                                // we keep a reference for storage saving
                                imageBitmap = it.drawable!!.toBitmap()
                                Image(
                                    bitmap = imageBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(modifier = Modifier.fillMaxWidth(0.9f), text = "Movie: ${movie.title}")

                    Spacer(Modifier.height(4.dp))

                    Text(modifier = Modifier.fillMaxWidth(0.9f), text = "Description: ${movie.overview}")

                    Spacer(Modifier.weight(1f))

                    Button(modifier = Modifier
                        .fillMaxWidth(0.9f),
                        onClick = {
                            scopeForSaving.launch {
                                imageBitmap?.let { saveToStorage(viewModel = viewModel, it) }
                            }
                        }
                    ) {
                        Text("Save Image")
                    }

                    Spacer(Modifier.height(12.dp))

                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun MovieColumn(viewModel: MainViewModel, movies: List<Movie>) {
            LazyColumn(content =  {
                    items(movies.size, key = { movies[it].id }) { movie ->
                        MovieElement(viewModel = viewModel, movie = movies[movie], Modifier.animateItemPlacement())
                    }
            })
        }

        @Composable
        fun MovieElement(viewModel: MainViewModel, movie: Movie, modifier: Modifier) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .clickable { viewModel.setSelectedPhoto(movie) }
            ) {
                //CircularProgressIndicator()
                Text(
                    text = movie.title,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .align(Alignment.CenterVertically)
                        .padding(8.dp)
                )

                GlideImage(
                    imageModel = movie.getMoviePoster(),
                    requestOptions = {
                        //Make sure the image it's loaded in square shape and cached
                        RequestOptions()
                            .override(90, 90)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                    },
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterEnd
                )
            }
        }

        @Composable
        fun TextSearchBar(
            viewModel: MainViewModel,
            modifier: Modifier = Modifier,
            label: String,
        ) {
            val searchText = viewModel.searchText.observeAsState().value

            OutlinedTextField(
                modifier = modifier
                    .fillMaxWidth(1f),
                value = searchText!!,
                onValueChange = {
                    viewModel.setSearchText(it)
                    viewModel.filterMovies(it) },
                label = { Text(text = label) },
                textStyle = MaterialTheme.typography.labelMedium,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.setSearchText("") }) {
                        Icon(imageVector = Filled.Clear, contentDescription = "Clear")
                    }
                },
            )
        }

        suspend fun saveToStorage(viewModel: MainViewModel, bitmap: Bitmap) {
            withContext(Dispatchers.IO) {
                val filename = "Bambuser_Demo_${System.currentTimeMillis()}.jpg"
                var fos: OutputStream? = null
                if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                    contentResolver?.also { resolver ->
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES
                            )
                        }
                        val imageUri: Uri? =
                            resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        fos = imageUri?.let { resolver.openOutputStream(it) }
                    }
                } else {
                    val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = File(imagesDir, filename)
                    fos = FileOutputStream(image)
                }
                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    runOnUiThread {
                        viewModel.setSelectedPhoto(Movie())
                        Toast.makeText(
                            this@MainActivity,
                            "$filename saved to Photos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
