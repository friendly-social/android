package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import friendly.android.FriendlyNavGraph.Home
import friendly.android.FriendlyNavGraph.Registration
import friendly.android.FriendlyNavGraph.Welcome
import friendly.sdk.Authorization
import kotlinx.serialization.Serializable

object FriendlyNavGraph {
    @Serializable
    data object Registration

    @Serializable
    data object Welcome

    @Serializable
    open class Home {
        @Serializable
        data object Feed : Home()

        @Serializable
        data object Network : Home()

        @Serializable
        data object HomeProfile : Home()
    }
}

@Composable
fun FriendlyNavGraph(
    navController: NavHostController,
    viewModelFactory: FriendlyViewModelFactory,
    authorization: Authorization?,
    modifier: Modifier = Modifier,
) {
    val firstDestination = when (authorization) {
        null -> Welcome
        else -> Home()
    }

    NavHost(
        navController = navController,
        startDestination = firstDestination,
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
                    navController.navigate(
                        Home.HomeProfile
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

            composable<Home.HomeProfile> { backStackEntry ->
                ProfileScreen(
                    source = ProfileScreenSource.SelfProfile,
                    vm = viewModel<ProfileScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    modifier = Modifier,
                )
            }
        }
    }
}
