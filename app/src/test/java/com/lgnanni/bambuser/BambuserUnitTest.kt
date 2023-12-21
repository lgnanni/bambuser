import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.lgnanni.bambuser.MainActivity
import com.lgnanni.bambuser.data.Movie
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private lateinit var context: Context
    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activity = spy(MainActivity())
    }

    @Test
    fun saveToStorage_savesImageToFile() {
        val bitmap = mock<Bitmap>()

        // Assuming Looper is needed and you're using Robolectric
        Looper.prepare()

        // Mock contentResolver and other necessary dependencies
        val contentResolver = mock<ContentResolver>()
        val fileOutputStream = mock<FileOutputStream>()

        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)).thenReturn(mock<File>())
        `when`(contentResolver.openOutputStream(any())).thenReturn(fileOutputStream)
        `when`(activity.contentResolver).thenReturn(contentResolver)

        runBlocking {
            activity.saveToStorage(activity.mainViewModel, bitmap)
        }
    }

    @Composable
    @Test
    fun textSearchBar_updatesViewModel() {
        val modifier = mock<Modifier>()
        val label = "Filter title"

        val observer: (String) -> Unit = mock()

        `when`(activity.mainViewModel.searchText).thenReturn(MutableLiveData(""))
        // Use CompositionLocalProvider to provide the context
        CompositionLocalProvider(LocalContext provides context) {
            activity.TextSearchBar(viewModel = activity.mainViewModel, modifier = modifier, label = label)
        }

        // Verify that the observer is invoked
        verify(observer).invoke(anyString())
    }

    @Composable
    @Test
    fun movieList_displaysMovies() {
        `when`(activity.mainViewModel.darkTheme).thenReturn(MutableLiveData(false))
        `when`(activity.mainViewModel.filterMovies).thenReturn(MutableLiveData(emptyList()))
        `when`(activity.mainViewModel.selectedMovie).thenReturn(MutableLiveData(Movie()))
        `when`(activity.mainViewModel.loading).thenReturn(MutableLiveData(false))

        // Use CompositionLocalProvider to provide the context
        CompositionLocalProvider(LocalContext provides context) {
            activity.MovieList(viewModel = activity.mainViewModel, movieList = emptyList(), movie = Movie(), loading = false)
        }
    }

}
