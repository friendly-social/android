package friendly.android

import android.net.Uri
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.Home
import friendly.android.FriendlyNavGraph.Registration
import friendly.android.FriendlyNavGraph.Welcome
import friendly.sdk.Authorization
import friendly.sdk.FriendToken
import friendly.sdk.Nickname
import friendly.sdk.UserAccessHash
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

        @Serializable
        data class Profile(
            val userId: Long,
            val accessHash: String,
            val nickname: String,
            val avatarUri: String?,
        ) : Home()
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

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = firstDestination,
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
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
                        navController.navigate(Home.HomeProfile)
                    },
                    modifier = Modifier,
                )
            }

            navigation<Home>(startDestination = Home.Feed) {
                composable<Home.Feed> {
                    FeedScreen(
                        vm = viewModel<FeedScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        modifier = Modifier,
                    )
                }

                composable<Home.Network> {
                    NetworkScreen(
                        vm = viewModel<NetworkScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onProfile = { route ->
                            navController.navigate(route)
                        },
                        onShare = { navController.navigate(Home.ShareProfile) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        modifier = Modifier,
                    )
                }

                dialog<Home.ShareProfile> {
                    ModalBottomSheet(
                        sheetState = rememberModalBottomSheetState(
                            skipPartiallyExpanded = true,
                        ),
                        onDismissRequest = { navController.popBackStack() },
                        modifier = Modifier,
                    ) {
                        ShareProfileScreen(
                            onDone = { navController.popBackStack() },
                            vm = viewModel<ShareProfileScreenViewModel>(
                                factory = viewModelFactory,
                            ),
                        )
                    }
                }

                composable<Home.HomeProfile> {
                    SelfProfileScreen(
                        vm = viewModel<SelfProfileScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onSignOut = {
                            navController.navigate(Welcome) { popUpTo(0) }
                        },
                        modifier = Modifier,
                    )
                }

                composable<Home.Profile> { backStackEntry ->
                    val route: Home.Profile = backStackEntry.toRoute()
                    ProfileScreen(
                        source = ProfileScreenSource(
                            userId = UserId(route.userId),
                            accessHash = UserAccessHash
                                .orThrow(route.accessHash),
                            nickname = Nickname.orThrow(route.nickname),
                            avatar = route.avatarUri?.let(Uri::parse),
                        ),
                        onHome = {
                            navController.popBackStack()
                        },
                        vm = viewModel<ProfileScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
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
}
