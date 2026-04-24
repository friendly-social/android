package friendly.android

import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.AddFriendByToken
import friendly.android.FriendlyNavGraph.Home
import friendly.android.FriendlyNavGraph.Home.EditProfile
import friendly.android.FriendlyNavGraph.NavDestination
import friendly.android.FriendlyNavGraph.Registration
import friendly.android.FriendlyNavGraph.Welcome
import friendly.sdk.Authorization
import friendly.sdk.FriendToken
import friendly.sdk.InterestSerializable
import friendly.sdk.Nickname
import friendly.sdk.NicknameSerializable
import friendly.sdk.SocialLinkSerializable
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescriptionSerializable
import friendly.sdk.UserId
import friendly.sdk.UserIdSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

private val addFriendDeepLink = navDeepLink {
    uriPattern = "friendly://add/{userId}/{friendToken}"
}

val NicknameSerializableNavType =
    object : NavType<NicknameSerializable>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): NicknameSerializable? =
            bundle.getString(key)?.let(::NicknameSerializable)

        override fun parseValue(value: String): NicknameSerializable =
            NicknameSerializable(value)

        override fun serializeAsValue(value: NicknameSerializable): String =
            Uri.encode(value.string)

        override fun put(
            bundle: Bundle,
            key: String,
            value: NicknameSerializable,
        ) = bundle.putString(key, value.string)
    }

val UserDescriptionSerializableNavType =
    object : NavType<UserDescriptionSerializable>(isNullableAllowed = false) {
        override fun get(
            bundle: Bundle,
            key: String,
        ): UserDescriptionSerializable? =
            bundle.getString(key)?.let(::UserDescriptionSerializable)

        override fun parseValue(value: String): UserDescriptionSerializable =
            UserDescriptionSerializable(value)

        override fun serializeAsValue(
            value: UserDescriptionSerializable,
        ): String = Uri.encode(value.string)

        override fun put(
            bundle: Bundle,
            key: String,
            value: UserDescriptionSerializable,
        ) = bundle.putString(key, value.string)
    }

val InterestSerializableNavType =
    object : NavType<InterestSerializable>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): InterestSerializable? =
            bundle.getString(key)?.let(::InterestSerializable)

        override fun parseValue(value: String): InterestSerializable =
            InterestSerializable(value)

        override fun serializeAsValue(value: InterestSerializable): String =
            Uri.encode(value.string)

        override fun put(
            bundle: Bundle,
            key: String,
            value: InterestSerializable,
        ) = bundle.putString(key, value.string)
    }

val SocialLinkSerializableNavType =
    object : NavType<SocialLinkSerializable>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): SocialLinkSerializable? =
            bundle.getString(key)?.let(::SocialLinkSerializable)

        override fun parseValue(value: String): SocialLinkSerializable =
            SocialLinkSerializable(value)

        override fun serializeAsValue(value: SocialLinkSerializable): String =
            Uri.encode(value.string)

        override fun put(
            bundle: Bundle,
            key: String,
            value: SocialLinkSerializable,
        ) = bundle.putString(key, value.string)
    }

val UserIdSerializableNavType =
    object : NavType<UserIdSerializable>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): UserIdSerializable? =
            bundle.getLong(key).let(::UserIdSerializable)

        override fun parseValue(value: String): UserIdSerializable =
            UserIdSerializable(value.toLong())

        override fun serializeAsValue(value: UserIdSerializable): String =
            value.long.toString()

        override fun put(
            bundle: Bundle,
            key: String,
            value: UserIdSerializable,
        ) = bundle.putLong(key, value.long)
    }

val InterestSerializableListNavType =
    object : NavType<List<InterestSerializable>>(isNullableAllowed = false) {
        override fun get(
            bundle: Bundle,
            key: String,
        ): List<InterestSerializable>? = bundle.getString(key)?.let { json ->
            Json.decodeFromString<List<String>>(json)
                .map(::InterestSerializable)
        }

        override fun parseValue(value: String): List<InterestSerializable> =
            Json.decodeFromString<List<String>>(Uri.decode(value))
                .map(::InterestSerializable)

        override fun serializeAsValue(
            value: List<InterestSerializable>,
        ): String = Uri.encode(Json.encodeToString(value.map { it.string }))

        override fun put(
            bundle: Bundle,
            key: String,
            value: List<InterestSerializable>,
        ) = bundle.putString(key, Json.encodeToString(value.map { it.string }))
    }

object FriendlyNavGraph {
    sealed interface NavDestination

    @Serializable
    data object Registration : NavDestination

    @Serializable
    data object Welcome : NavDestination

    @Serializable
    open class Home : NavDestination {
        @Serializable
        data object Feed : Home()

        @Serializable
        data object Network : Home()

        @Serializable
        data object SelfProfile : Home()

        @Serializable
        data class EditProfile(
            val nickname: NicknameSerializable,
            val description: UserDescriptionSerializable,
            val interests: List<InterestSerializable>,
            val socialLink: SocialLinkSerializable?,
            val userId: UserIdSerializable,
            val avatarUri: String?,
        ) : Home()

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

val EditProfileTypeMap = mapOf(
    typeOf<NicknameSerializable>() to NicknameSerializableNavType,
    typeOf<UserDescriptionSerializable>() to UserDescriptionSerializableNavType,
    typeOf<InterestSerializable>() to InterestSerializableNavType,
    typeOf<SocialLinkSerializable?>() to SocialLinkSerializableNavType,
    typeOf<UserIdSerializable>() to UserIdSerializableNavType,
    typeOf<List<InterestSerializable>>() to InterestSerializableListNavType,
)

@Composable
fun FriendlyNavGraph(
    contentPadding: (NavDestination) -> PaddingValues,
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
                    contentPadding = contentPadding(Welcome),
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
                        navController.navigate(Home.SelfProfile)
                    },
                    contentPadding = contentPadding(Registration),
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
                        contentPadding = contentPadding(Home.Feed),
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
                        contentPadding = contentPadding(Home.Network),
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

                composable<Home.SelfProfile> {
                    SelfProfileScreen(
                        vm = viewModel<SelfProfileScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onSignOut = {
                            navController.navigate(Welcome) { popUpTo(0) }
                        },
                        onEditProfileClick = { route ->
                            navController.navigate(route)
                        },
                        contentPadding = contentPadding(Home.SelfProfile),
                        modifier = Modifier,
                    )
                }

                composable<EditProfile>(
                    typeMap = EditProfileTypeMap,
                ) { backStackEntry ->
                    val route: EditProfile = backStackEntry.toRoute()
                    EditProfileScreen(
                        vm = viewModel<EditProfileScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onBack = { navController.popBackStack() },
                        contentPadding = contentPadding(route),
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
                        contentPadding = contentPadding(route),
                        modifier = Modifier,
                    )
                }
            }

            composable<AddFriendByToken>(
                deepLinks = listOf(addFriendDeepLink),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments
                    ?.getString("userId") ?: error("no userId")
                val friendToken = backStackEntry.arguments
                    ?.getString("friendToken") ?: error("no friendToken")

                AddFriendByTokenScreen(
                    goToSignUp = { navController.navigate(Welcome) },
                    onGoBack = { navController.popBackStack() },
                    onNetworkScreen = {
                        navController.navigate(Home.Network)
                    },
                    friendToken = FriendToken.orThrow(friendToken),
                    userId = UserId(userId.toLong()),
                    vm = viewModel<AddFriendByTokenScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    contentPadding = contentPadding(AddFriendByToken),
                    modifier = Modifier,
                )
            }
        }
    }
}
