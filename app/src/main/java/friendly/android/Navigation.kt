package friendly.android

import android.net.Uri
import android.view.View
import android.view.Window
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import friendly.android.FriendlyNavGraph.AddFriendByToken
import friendly.android.FriendlyNavGraph.Home
import friendly.android.FriendlyNavGraph.Home.EditProfile
import friendly.android.FriendlyNavGraph.NavDestination
import friendly.android.FriendlyNavGraph.NoFriendsBlockingScreen
import friendly.android.FriendlyNavGraph.Registration
import friendly.android.FriendlyNavGraph.SignIn
import friendly.android.FriendlyNavGraph.Welcome
import friendly.android.NotificationDestinationExtra.Activity
import friendly.android.NotificationDestinationExtra.Network
import friendly.sdk.Authorization
import friendly.sdk.CommunityPostDescriptorSerializable
import friendly.sdk.EmailSerializable
import friendly.sdk.FriendToken
import friendly.sdk.InterestSerializable
import friendly.sdk.Nickname
import friendly.sdk.NicknameSerializable
import friendly.sdk.SocialLinkSerializable
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescriptionSerializable
import friendly.sdk.UserId
import friendly.sdk.UserIdSerializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// TODO: plans for this god file
//  - dsl for navigation to specific destinations
//  - make navigation operations live in the same file per each destination

private val addFriendDeepLink = navDeepLink {
    uriPattern = "friendly://add/{userId}/{friendToken}"
}

class JsonNavType<T>(
    private val serializer: KSerializer<T>,
    isNullableAllowed: Boolean,
) : NavType<T>(isNullableAllowed) {
    override fun put(bundle: SavedState, key: String, value: T) =
        bundle.putString(key, Json.encodeToString(serializer, value))

    override fun get(bundle: SavedState, key: String): T? =
        bundle.getString(key)?.let { Json.decodeFromString(serializer, it) }

    override fun parseValue(value: String): T =
        Json.decodeFromString(serializer, value)

    override fun serializeAsValue(value: T): String =
        Uri.encode(Json.encodeToString(serializer, value))
}

inline fun <reified T> navTypeOf(): Pair<KType, NavType<T>> =
    typeOf<T>() to JsonNavType(serializer<T>(), isNullableAllowed = null is T)

object FriendlyNavGraph {
    sealed interface NavDestination

    @Serializable
    data object SignIn : NavDestination

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
        data object Community : Home()

        @Serializable
        data class CommunityPost(
            val descriptor: CommunityPostDescriptorSerializable,
        ) : Home()

        @Serializable
        data object Chat : Home()

        @Serializable
        data object Activity : Home()

        @Serializable
        data object SelfProfile : Home()

        @Serializable
        data class EditProfile(
            val nickname: NicknameSerializable,
            val description: UserDescriptionSerializable,
            val interests: List<InterestSerializable>,
            val socialLink: SocialLinkSerializable?,
            val email: EmailSerializable?,
            val userId: UserIdSerializable,
            val avatarUri: String?,
        ) : Home()

        @Serializable
        data object ShareProfileSheet : Home()

        @Serializable
        data class CodeConfirmationSheet(val email: EmailSerializable) : Home()

        @Serializable
        data class CodeVerificationSheet(val email: EmailSerializable) : Home()

        @Serializable
        data class Profile(
            val userId: Long,
            val accessHash: String,
            val nickname: String,
            val avatarUri: String?,
        ) : Home()

        @Serializable
        data class PictureViewerDialog(val uri: String) : Home()

        @Serializable
        data class CreatePostDialog(
            val replyTo: CommunityPostDescriptorSerializable?,
        ) : Home()

        @Serializable
        data class EditInterestsDialog(val interests: List<String>) : Home()

        // todo: do they really should belong to the Home?
    }

    @Serializable
    data object AddFriendByToken : NavDestination

    @Serializable
    data object NoFriendsBlockingScreen : NavDestination
}

val EditProfileTypeMap = mapOf(
    navTypeOf<NicknameSerializable>(),
    navTypeOf<UserDescriptionSerializable>(),
    navTypeOf<InterestSerializable>(),
    navTypeOf<SocialLinkSerializable?>(),
    navTypeOf<EmailSerializable?>(),
    navTypeOf<UserIdSerializable>(),
    navTypeOf<List<InterestSerializable>>(),
)

val EmailSheetTypeMap = mapOf(navTypeOf<EmailSerializable>())

val CommunityPostTypeMap = mapOf(
    navTypeOf<CommunityPostDescriptorSerializable>(),
)

val CreatePostDialogTypeMap = mapOf(
    navTypeOf<CommunityPostDescriptorSerializable?>(),
)

private const val VerificationCodeStateKey = "verification_code_state"
private const val PickedInterestsStateKey = "picked_interests_state"

@Composable
fun FriendlyNavGraph(
    contentPadding: (NavDestination) -> PaddingValues,
    navController: NavHostController,
    viewModelFactory: FriendlyViewModelFactory,
    authorization: Authorization?,
    hasFirstFriend: Boolean,
    notificationDestination: NotificationDestinationExtra?,
    modifier: Modifier = Modifier,
) {
    val firstDestination = remember {
        determineInitialDestination(
            authorization,
            hasFirstFriend,
        )
    }
    val startHomeDestination = remember {
        determineInitialHomeDestination(notificationDestination)
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = firstDestination,
            enterTransition = {
                fadeIn(animationSpec = tween(150))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150))
            },
            predictivePopEnterTransition = {
                fadeIn(animationSpec = tween(150))
            },
            predictivePopExitTransition = {
                fadeOut(animationSpec = tween(150))
            },
            modifier = modifier,
        ) {
            composable<Welcome> {
                WelcomeScreen(
                    onSignIn = { navController.navigate(SignIn) },
                    onSignUp = { navController.navigate(Registration) },
                    contentPadding = contentPadding(Welcome),
                    modifier = Modifier,
                )
            }

            composable<SignIn> { backStackEntry ->
                val vm = viewModel<SignInScreenViewModel>(
                    factory = viewModelFactory,
                )
                val emailCodeSubmissionState by backStackEntry
                    .savedStateHandle
                    .getStateFlow<EmailCodeLoginState?>(
                        key = VerificationCodeStateKey,
                        initialValue = null,
                    )
                    .collectAsStateWithLifecycle()
                LaunchedEffect(emailCodeSubmissionState) {
                    emailCodeSubmissionState?.let(vm::onCodeConfirmationResult)
                }
                SignInScreen(
                    vm = vm,
                    onHome = {
                        navController.navigate(Home.SelfProfile) {
                            popUpTo(navController.graph.id)
                        }
                    },
                    onConfirm = { email ->
                        vm.sendConfirmationCode()
                        navController.navigate(
                            Home.CodeVerificationSheet(
                                email = email.serializable(),
                            ),
                        )
                    },
                    onBack = { navController.popBackStack() },
                    contentPadding = contentPadding(Registration),
                    modifier = Modifier,
                )
            }

            composable<Registration> { backStackEntry ->
                val vm = viewModel<RegisterScreenViewModel>(
                    factory = viewModelFactory,
                )

                val pickedInterestsState by backStackEntry
                    .savedStateHandle
                    .getStateFlow<List<String>?>(
                        key = PickedInterestsStateKey,
                        initialValue = null,
                    )
                    .collectAsStateWithLifecycle()

                LaunchedEffect(pickedInterestsState) {
                    pickedInterestsState?.let(vm::onPickedInterests)
                }

                RegisterScreen(
                    vm = vm,
                    onRegistration = {
                        navController.navigate(NoFriendsBlockingScreen) {
                            popUpTo(navController.graph.id)
                        }
                    },
                    onEditInterests = { initialInterests ->
                        navController.navigate(
                            Home.EditInterestsDialog(initialInterests),
                        )
                    },
                    contentPadding = contentPadding(Registration),
                    modifier = Modifier,
                )
            }

            navigation<Home>(startDestination = startHomeDestination) {
                composable<Home.Feed> {
                    FeedScreen(
                        vm = viewModel<FeedScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onProfilePictureClick = { uri ->
                            navController.navigate(
                                route = Home.PictureViewerDialog(
                                    uri.toString(),
                                ),
                            )
                        },
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
                        onFeed = { navController.navigate(Home.Feed) },
                        onShare = {
                            navController.navigate(Home.ShareProfileSheet)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        contentPadding = contentPadding(Home.Network),
                        modifier = Modifier,
                    )
                }

                composable<Home.Community> {
                    CommunityScreen(
                        vm = viewModel<CommunityScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        contentPadding = contentPadding(Home.Community),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        onPostClick = { descriptor ->
                            navController.navigate(
                                Home.CommunityPost(descriptor.serializable()),
                            )
                        },
                        onImageClick = { uri: String ->
                            navController.navigate(
                                route = Home.PictureViewerDialog(uri),
                            )
                        },
                        onCreatePostClick = {
                            navController.navigate(
                                route = Home.CreatePostDialog(null),
                            )
                        },
                        modifier = Modifier,
                    )
                }

                composable<Home.CommunityPost>(
                    typeMap = CommunityPostTypeMap,
                ) { backStackEntry ->
                    val route: Home.CommunityPost = backStackEntry.toRoute()
                    CommunityPostScreen(
                        vm = viewModel<CommunityPostScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        contentPadding = contentPadding(route),
                        onImageClick = { uri: String ->
                            navController.navigate(
                                route = Home.PictureViewerDialog(uri),
                            )
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        onPostClick = { descriptor ->
                            val descriptorSerializable =
                                descriptor.serializable()
                            navController.navigate(
                                Home.CommunityPost(descriptorSerializable),
                            ) {
                                popUpTo(
                                    Home.CommunityPost(descriptorSerializable),
                                ) {
                                    inclusive = true
                                    saveState = true
                                }
                            }
                        },
                        onMaximizeReplyFieldClick = { descriptor ->
                            val route = Home.CreatePostDialog(
                                descriptor.serializable(),
                            )
                            navController.navigate(route)
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Home.Activity> {
                    ActivityScreen(
                        vm = viewModel<ActivityScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onActivityClick = { descriptor ->
                            navController.navigate(
                                Home.CommunityPost(descriptor.serializable()),
                            )
                        },
                        contentPadding = contentPadding(Home.Activity),
                    )
                }

                composable<Home.Chat> {
                    ChatScreen(
                        contentPadding = contentPadding(Home.Chat),
                        modifier = Modifier,
                    )
                }

                dialog<Home.ShareProfileSheet> {
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

                dialog<Home.CodeVerificationSheet>(
                    typeMap = EmailSheetTypeMap,
                ) {
                    VerifyEmailAuthCodeSheet(
                        vm = viewModel<VerifyEmailAuthCodeSheetViewModel>(
                            factory = viewModelFactory,
                        ),
                        onDismiss = { navController.popBackStack() },
                        onVerification = { verificationCodeState ->
                            val entry = navController.previousBackStackEntry
                            entry?.savedStateHandle?.set(
                                key = VerificationCodeStateKey,
                                value = verificationCodeState,
                            )
                            navController.popBackStack()
                        },
                        modifier = Modifier,
                    )
                }

                dialog<Home.CodeConfirmationSheet>(
                    typeMap = EmailSheetTypeMap,
                ) {
                    ConfirmEmailCodeSheet(
                        vm = viewModel<ConfirmEmailCodeSheetViewModel>(
                            factory = viewModelFactory,
                        ),
                        onDismiss = { navController.popBackStack() },
                        onVerification = { verificationCodeState ->
                            val entry = navController.previousBackStackEntry
                            entry?.savedStateHandle?.set(
                                key = VerificationCodeStateKey,
                                value = verificationCodeState,
                            )
                            navController.popBackStack()
                        },
                        modifier = Modifier,
                    )
                }

                composable<Home.SelfProfile> {
                    SelfProfileScreen(
                        vm = viewModel<SelfProfileScreenViewModel>(
                            factory = viewModelFactory,
                        ),
                        onSignOut = {
                            navController.navigate(Welcome) {
                                popUpTo(navController.graph.id)
                            }
                        },
                        onEditProfileClick = { route ->
                            navController.navigate(route)
                        },
                        onProfilePictureClick = { uri ->
                            navController.navigate(
                                route = Home.PictureViewerDialog(uri),
                            )
                        },
                        contentPadding = contentPadding(Home.SelfProfile),
                        modifier = Modifier,
                    )
                }

                composable<EditProfile>(
                    typeMap = EditProfileTypeMap,
                ) { backStackEntry ->
                    val route: EditProfile = backStackEntry.toRoute()

                    val vm = viewModel<EditProfileScreenViewModel>(
                        factory = viewModelFactory,
                    )

                    val emailCodeSubmissionState by backStackEntry
                        .savedStateHandle
                        .getStateFlow<EmailCodeSubmissionState?>(
                            key = VerificationCodeStateKey,
                            initialValue = null,
                        )
                        .collectAsStateWithLifecycle()

                    LaunchedEffect(emailCodeSubmissionState) {
                        emailCodeSubmissionState?.let { state ->
                            if (state.successful) {
                                vm.onSuccessfulVerificationCodeState(state)
                            }
                        }
                    }

                    val pickedInterestsState by backStackEntry
                        .savedStateHandle
                        .getStateFlow<List<String>?>(
                            key = PickedInterestsStateKey,
                            initialValue = null,
                        )
                        .collectAsStateWithLifecycle()

                    LaunchedEffect(pickedInterestsState) {
                        pickedInterestsState?.let(vm::onPickedInterests)
                    }

                    EditProfileScreen(
                        onBack = { navController.popBackStack() },
                        onSendEmailCode = { email ->
                            navController.navigate(
                                Home.CodeConfirmationSheet(
                                    email = email.serializable(),
                                ),
                            )
                        },
                        vm = vm,
                        contentPadding = contentPadding(route),
                        onEditInterests = { initialInterests ->
                            navController.navigate(
                                Home.EditInterestsDialog(initialInterests),
                            )
                        },
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
                        contentPadding = contentPadding(route),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        onProfilePictureClick = { uri ->
                            navController.navigate(
                                route = Home.PictureViewerDialog(
                                    uri.toString(),
                                ),
                            )
                        },
                    )
                }

                dialog<Home.PictureViewerDialog>(
                    dialogProperties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ) { backStackEntry ->
                    val route: Home.PictureViewerDialog = backStackEntry
                        .toRoute()

                    val localView = LocalView.current
                    SideEffect {
                        localView.parentWindow?.setWindowAnimations(-1)
                    }

                    PictureViewerDialog(
                        onDismiss = { navController.popBackStack() },
                        uri = route.uri.toUri(),
                        contentPadding = contentPadding(route),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable<Home.CreatePostDialog>(
                    typeMap = CreatePostDialogTypeMap,
                ) { backStackEntry ->
                    val route: Home.CreatePostDialog = backStackEntry
                        .toRoute()
                    CreatePostDialog(
                        vm = viewModel<CreatePostDialogViewModel>(
                            factory = viewModelFactory,
                        ),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable,
                        contentPadding = contentPadding(route),
                        onGoBack = { navController.popBackStack() },
                    )
                }

                composable<Home.EditInterestsDialog> { backStackEntry ->
                    val route: Home.EditInterestsDialog = backStackEntry
                        .toRoute()
                    EditInterestsDialog(
                        onDismiss = { navController.popBackStack() },
                        contentPadding = contentPadding(route),
                        modifier = Modifier.fillMaxSize(),
                        onConfirm = { newPickedInterests ->
                            val entry = navController.previousBackStackEntry
                            entry?.savedStateHandle?.set(
                                key = PickedInterestsStateKey,
                                value = newPickedInterests,
                            )
                            navController.popBackStack()
                        },
                        vm = viewModel<EditInterestsDialogViewModel>(
                            factory = viewModelFactory,
                        ),
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
                        navController.navigate(Home.Network) {
                            popUpTo(navController.graph.id)
                        }
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

            composable<NoFriendsBlockingScreen> {
                NoFriendsBlockingScreen(
                    onAddFriendSuccess = {
                        navController.navigate(Home.Feed) {
                            popUpTo(navController.graph.id)
                        }
                    },
                    contentPadding = contentPadding(NoFriendsBlockingScreen),
                    vm = viewModel<NoFriendsBlockingScreenViewModel>(
                        factory = viewModelFactory,
                    ),
                    modifier = Modifier,
                )
            }
        }
    }
}

private val View.parentWindow: Window?
    get() = (this.parent as? DialogWindowProvider)?.window

private fun determineInitialHomeDestination(
    notificationDestination: NotificationDestinationExtra?,
): Home = if (notificationDestination != null) {
    when (notificationDestination) {
        is Activity -> Home.Community
        is Network -> Home.Network
        is Feed -> Home.Feed
    }
} else {
    Home.Community
}

private fun determineInitialDestination(
    authorization: Authorization?,
    hasFirstFriend: Boolean,
): NavDestination = when (authorization) {
    null -> Welcome
    else -> {
        if (hasFirstFriend) {
            Home()
        } else {
            NoFriendsBlockingScreen
        }
    }
}
