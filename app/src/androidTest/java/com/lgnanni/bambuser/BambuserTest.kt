
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lgnanni.bambuser.MainActivity
import com.lgnanni.bambuser.data.Movie
import com.lgnanni.bambuser.viewmodel.MainViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @JvmField
    @Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        // Initialize your ViewModel
        viewModel = MainViewModel()
    }

    @Test
    fun movieListLayout_displaysMovieList() {
        // Given a list of movies
        val movies = listOf(
            Movie(title = "Movie 1"),
            Movie(title = "Movie 2"),
            Movie(title = "Movie 3")
        )

        // When the MovieListLayout is displayed
        composeTestRule.setContent {
            Scaffold {
                Surface {
                    MainActivity().MovieListLayout(it)
                }
            }
        }

        // Then each movie title should be displayed
        movies.forEach { movie ->
            composeTestRule
                .onNodeWithText(movie.title)
                .assertIsDisplayed()
        }
    }

    // Add more test cases as needed...
}
