package com.lgnanni.bambuser

import android.content.ContentValues
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContent {
            BambuserDemoTheme {
                viewModel.setDarkTheme(isSystemInDarkTheme())
                ImageGridLayout()
            }
        }

        viewModel.loadPhotos(this)
    }

    @OptIn(
        ExperimentalCoroutinesApi::class,
        ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class
    )
    @Composable
    fun ImageGridLayout() {
        val isRefreshing by remember { mutableStateOf(false) }
        val pullRefreshState =
            rememberPullRefreshState(isRefreshing, { viewModel.refresh(this@MainActivity) })
        val darkTheme = viewModel.darkTheme.observeAsState(false)

        // Instead of always setting enabled to true at the beginning,
        // you need to check the state ahead of time to know what the
        // initial enabled state should be
        val callback = object : OnBackPressedCallback(
            false
        ) {
            override fun handleOnBackPressed() {
                viewModel.setSelectedPhoto(Photo())
            }
        }

        ConnectivityStatus(viewModel = viewModel)

        callback.isEnabled = viewModel.selectedPhoto.observeAsState().value!!.title.isNotEmpty()

        onBackPressedDispatcher.addCallback(this, callback)

        BambuserDemoTheme(darkTheme.value) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                content = { padding ->
                    Box(
                        Modifier
                            .pullRefresh(pullRefreshState)
                            .padding(padding)
                            //Need to make it scrollable to be able to swipe to refresh
                            .scrollable(
                                state = ScrollableState { 1f },
                                orientation = Orientation.Vertical
                            ),
                        contentAlignment = Alignment.Center
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

                        ImageGallery(
                            photosList = photosList!!,
                            photo = selectedPhoto!!,
                            loading = loading
                        )

                        ImageSelected(photo = selectedPhoto)

                        PullRefreshIndicator(
                            isRefreshing, pullRefreshState, Modifier.align(
                                Alignment.TopCenter
                            )
                        )

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
                                    contentDescription = "Localized description"
                                )
                            }
                        })
                })
        }
    }

        /**
         * Main view with search bar and photos list
         * @param photosList = The list of Photo objects returned from Flickr photo.search
         * @param photo = The selected photo object, if any
         * @param searchText = The actual search text if any
         * @param loading = If we are retrieving Flick.photo.search
         */
        @Composable
        fun ImageGallery(
            photosList: List<Photo>,
            photo: Photo,
            loading: Boolean
        ) {
            AnimatedVisibility(
                visible = photosList.isNotEmpty() && photo.id.isBlank() && !loading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextSearchBar(
                        modifier = Modifier.padding(8.dp, 0.dp),
                        label = "Search",
                        onDoneActionClick = {
                            if (!viewModel.isConnected.value!!) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.no_connection),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                viewModel.loadPhotos(this@MainActivity, it)
                            }

                        }
                    )
                    Spacer(Modifier.height(2.dp))
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
         * @param photo = The selected photo object, if any
         */
        @Composable
        fun ImageSelected(photo: Photo) {
            AnimatedVisibility(
                visible = photo.id.isNotEmpty(),
                enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
                exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
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
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.5f)
                        .background(MaterialTheme.colorScheme.outline)
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        //Dismiss expanded image
                        .clickable { onBackPressedDispatcher.onBackPressed() }) {
                        GlideImage(
                            modifier = Modifier
                                .align(Alignment.Center),
                            imageModel = photo.getUrl(),
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

                    Text(modifier = Modifier.fillMaxWidth(0.9f), text = "Photo title: ${photo.title}")

                    Spacer(Modifier.height(4.dp))

                    Text(modifier = Modifier.fillMaxWidth(0.9f), text = "Photo owner: ${photo.owner}")

                    Spacer(Modifier.weight(1f))

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

                    Spacer(Modifier.height(12.dp))

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
                        PhotoElement(photo = photos[photo])
                    }
                })
        }

        @Composable
        fun PhotoElement(photo: Photo) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .padding(8.dp), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                GlideImage(
                    imageModel = photo.getUrl(),
                    modifier = Modifier
                        .border(1.dp, Color.White)
                        .clickable { viewModel.setSelectedPhoto(photo) },
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
        fun TextSearchBar(
            modifier: Modifier = Modifier,
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
                onValueChange = { viewModel.setSearchText(it) },
                label = { Text(text = label) },
                textStyle = MaterialTheme.typography.labelMedium,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.setSearchText("") }) {
                        Icon(imageVector = Filled.Clear, contentDescription = "Clear")
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
                        viewModel.setSelectedPhoto(Photo())
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