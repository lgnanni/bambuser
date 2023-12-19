package com.lgnanni.bambuser

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.*
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.lgnanni.bambuser.data.Photo
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

    private val viewModel: MainViewModel by viewModels()

    //Initialization of job
    private var job = Job()

    // Initialization of scope for the coroutine to run in
    private var scopeForSaving = CoroutineScope(job + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImageGridLayout()
        }

        viewModel.loadPhotos(this)
    }
    @OptIn(ExperimentalMaterialApi::class, ExperimentalCoroutinesApi::class)
    @Composable
    fun ImageGridLayout() {
        val isRefreshing by remember { mutableStateOf(false) }
        val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.refresh(this@MainActivity) })

        // Instead of always setting enabled to true at the beginning,
        // you need to check the state ahead of time to know what the
        // initial enabled state should be
        val callback = object : OnBackPressedCallback(
            false
        ) {
            override fun handleOnBackPressed() {
                viewModel.setSelectedPhoto("")
            }
        }

        ConnectivityStatus(viewModel = viewModel)

        callback.isEnabled = viewModel.selectedPhoto.observeAsState().value!!.isNotEmpty()

        onBackPressedDispatcher.addCallback(this, callback)

        BambuserDemoTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                content = { padding ->
                    Box(
                        Modifier.pullRefresh(pullRefreshState).
                        padding(padding)
                            //Need to make it scrollable to be able to swipe to refresh
                            .scrollable(
                                state = ScrollableState { 1f },
                                orientation = Orientation.Vertical
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                            val photosList = viewModel.photos.observeAsState().value
                            val selectedPhoto = viewModel.selectedPhoto.observeAsState().value
                            val loading = viewModel.loading.observeAsState().value

                            //Loader indicator trying to get the api call for the photos
                            AnimatedVisibility(
                                visible = loading!!,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(modifier = Modifier
                                    .fillMaxSize(1.0f)) {
                                    CircularProgressIndicator(modifier = Modifier
                                        .fillMaxSize(0.5f)
                                        .align(Alignment.Center))
                                }
                            }

                            ImageGallery(
                                photosList = photosList!!,
                                photoUrl = selectedPhoto!!,
                                loading = loading)

                            ImageSelected(photoUrl = selectedPhoto)

                        PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(
                            Alignment.TopCenter))

                    }
                },
                topBar = {
                    TopAppBar(title = {Text(getString(R.string.app_name))})
                })

        }
    }

    /**
     * Main view with search bar and photos list
     * @param photosList = The list of Photo objects returned from Flickr photo.search
     * @param photoUrl = The URL for the selected photo, if any
     * @param searchText = The actual search text if any
     * @param loading = If we are retrieving Flick.photo.search
     */
    @Composable
    fun ImageGallery(
        photosList: List<Photo>,
        photoUrl: String,
        loading: Boolean) {
        AnimatedVisibility(
            visible = photosList.isNotEmpty() && photoUrl.isBlank() && !loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextSearchBar(
                    modifier = Modifier.padding(8.dp),
                    label = "Search",
                    onDoneActionClick = {
                        if(!viewModel.isConnected.value!!) {
                            Toast.makeText(this@MainActivity, getString(R.string.no_connection), Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.loadPhotos(this@MainActivity, it)
                        }

                    }
                )
                PhotoGrid(photosList)
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Composable
    fun ConnectivityStatus(viewModel: MainViewModel) {
        // This will cause re-composition on every network state change
        val connection by connectivityState()
        val isConnected = connection === ConnectionState.Available
        viewModel.setIsConnected(isConnected)
    }

    /**
     * The expanded view on selecting one item from the photos grid
     * @param photoUrl = The URL for the selected photo, if any
     */
    @Composable
    fun ImageSelected(photoUrl: String) {
        AnimatedVisibility(
            visible = photoUrl.isNotEmpty(),
            enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(1.0f)
                    .background(Color.DarkGray)
                    //Dismiss expanded image
                    .clickable { onBackPressedDispatcher.onBackPressed() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                //Bitmap prepared for if the user desires to save the image
                var imageBitmap: Bitmap? = null
                Box(modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.5f)
                    .background(Color.LightGray)
                    .border(2.dp, Color.Blue)
                    //Dismiss expanded image
                    .clickable { onBackPressedDispatcher.onBackPressed() }) {
                    GlideImage(
                        modifier = Modifier
                            .align(Alignment.Center),
                        imageModel = photoUrl,
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

                Button(modifier = Modifier
                    .fillMaxWidth(0.9f),
                    onClick = {
                        scopeForSaving.launch {
                            imageBitmap?.let { saveToStorage(it) }
                        }
                    }
                ) {
                    Text("Save Image")
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PhotoGrid(photos: List<Photo>) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            content = {
                    items(photos.size) { photo ->
                    PhotoElement(photoUrl = photos[photo].getUrl())
                }
            })
    }

    @Composable
    fun PhotoElement(photoUrl: String) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(8.dp), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
            GlideImage(
                imageModel = photoUrl,
                modifier = Modifier
                    .border(1.dp, Color.White)
                    .clickable { viewModel.setSelectedPhoto(photoUrl) },
                requestOptions = {
                    //Make sure the image it's laoded in square shape and cached
                    RequestOptions()
                        .override(90, 90)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                },
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        }
    }

    @Composable
    fun TextSearchBar(modifier: Modifier = Modifier,
                      label: String,
                      onDoneActionClick: (String) -> Unit = {},
                      onFocusChanged: (FocusState) -> Unit = {},
    ) {
        val searchText = viewModel.searchText.observeAsState().value

        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth(1f)
                .onFocusChanged { onFocusChanged(it) },
            value = searchText!!,
            onValueChange = { viewModel.setSearchText(it)},
            label = { Text(text = label) },
            textStyle = MaterialTheme.typography.subtitle1,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.setSearchText("") }) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                }
            },
            keyboardActions = KeyboardActions(onSearch = { onDoneActionClick(searchText) }),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text
            )
        )
    }

   suspend fun saveToStorage(bitmap: Bitmap) {
        withContext(Dispatchers.IO){
            val filename = "Bambuser_Demo_${System.currentTimeMillis()}.jpg"
            var fos: OutputStream? = null
            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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
                    viewModel.setSelectedPhoto("")
                    Toast.makeText(this@MainActivity, "$filename saved to Photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}