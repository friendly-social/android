package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import friendly.android.FriendlyGraph.Home
import friendly.android.FriendlyGraph.Registration
import friendly.sdk.UserId
import kotlinx.serialization.Serializable

object FriendlyGraph {
    @Serializable
    data object Registration

    @Serializable
    data object Home {
        @Serializable
        data object Feed

        @Serializable
        data object Network

        @Serializable
        data class Profile(val userId: Long)
    }
}

@Composable
fun FriendlyNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Registration,
        modifier = modifier,
    ) {
        composable<Registration> {
            RegisterScreen(
                onHome = {
                    val userId = UserId(123L).serializable()
                    navController.navigate(
                        Home.Profile(userId.long),
                    )
                },
                modifier = Modifier,
            )
        }

        navigation<Home>(startDestination = Home.Feed) {
            composable<Home.Feed> {
                FeedScreen(Modifier)
            }

            composable<Home.Network> {
                NetworkScreen(Modifier)
            }

            composable<Home.Profile> { backStackEntry ->
                val profile: Home.Profile = backStackEntry.toRoute()
                ProfileScreen(
                    userId = UserId(profile.userId),
                    modifier = Modifier,
                )
            }
        }
    }
}
