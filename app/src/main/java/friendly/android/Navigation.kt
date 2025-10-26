package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import friendly.android.FriendlyGraph.Home
import friendly.android.FriendlyGraph.Registration
import friendly.android.FriendlyGraph.Welcome
import friendly.sdk.UserId
import kotlinx.serialization.Serializable

object FriendlyGraph {
    @Serializable
    data object Registration

    @Serializable
    data object Welcome

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
fun FriendlyNavGraph(
    viewModelFactory: FriendlyViewModelFactory,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Welcome,
        modifier = modifier,
    ) {
        composable<Welcome> {
            WelcomeScreen(
                onSignUp = { navController.navigate(Registration) },
                modifier = Modifier,
            )
        }

        composable<Registration> {
            val vm = viewModel<RegisterScreenViewModel>(
                factory = viewModelFactory,
            )
            RegisterScreen(
                vm = vm,
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
                    vm = viewModel<ProfileScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    modifier = Modifier,
                )
            }
        }
    }
}
