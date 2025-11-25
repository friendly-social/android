package friendly.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import friendly.android.FriendlyNavGraph.Home
import friendly.android.FriendlyNavGraph.Registration
import friendly.android.FriendlyNavGraph.Welcome
import friendly.sdk.Authorization
import friendly.sdk.FriendToken
import friendly.sdk.UserId
import kotlinx.serialization.Serializable

private val addFriendDeepLink = navDeepLink {
    uriPattern = "friendly://add/{userId}/{friendToken}"
}

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

        @Serializable
        data object ShareProfile : Home()
    }

    @Serializable
    data object AddFriendByToken : Home()
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
                        Home.HomeProfile,
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
                    onShare = {
                        navController.navigate(Home.ShareProfile)
                    },
                    vm = viewModel<ProfileScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    modifier = Modifier,
                )
            }

            composable<Home.ShareProfile> { backStackEntry ->
                ShareProfileScreen(
                    vm = viewModel<ShareProfileScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    onHome = navController::popBackStack,
                    modifier = Modifier,
                )
            }
        }

        composable<FriendlyNavGraph.AddFriendByToken>(
            deepLinks = listOf(addFriendDeepLink),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments
                ?.getString("userId") ?: error("no userId")
            val friendToken = backStackEntry.arguments
                ?.getString("friendToken") ?: error("no friendToken")

            AddFriendByTokenScreen(
                goToSignUp = { navController.navigate(Welcome) },
                goHome = { navController.navigate(Home()) },
                friendToken = FriendToken.orThrow(friendToken),
                userId = UserId(userId.toLong()),
                vm = viewModel<AddFriendByTokenScreenViewModel>(
                    factory = viewModelFactory,
                ),
                modifier = Modifier,
            )
        }
    }
}
