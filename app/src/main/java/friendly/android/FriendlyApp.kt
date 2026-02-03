package friendly.android

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import friendly.sdk.Authorization

data class HomeNavigationItem(
    val titleResource: Int,
    val selectedIconResource: Int = R.drawable.ic_photo_camera,
    val unselectedIconResource: Int = R.drawable.ic_photo_camera,
    val destination: FriendlyNavGraph.Home,
)

val homeNavigationItems = listOf(
    HomeNavigationItem(
        titleResource = R.string.feed,
        selectedIconResource = R.drawable.ic_cards_star_filled,
        unselectedIconResource = R.drawable.ic_cards_star_unfilled,
        destination = FriendlyNavGraph.Home.Feed,
    ),
    HomeNavigationItem(
        titleResource = R.string.network,
        selectedIconResource = R.drawable.ic_group_filled,
        unselectedIconResource = R.drawable.ic_group_unfilled,
        destination = FriendlyNavGraph.Home.Network,
    ),
    HomeNavigationItem(
        titleResource = R.string.profile,
        selectedIconResource = R.drawable.ic_person_filled,
        unselectedIconResource = R.drawable.ic_person_unfilled,
        destination = FriendlyNavGraph.Home.HomeProfile,
    ),
)

@Composable
fun FriendlyApp(
    viewModelFactory: FriendlyViewModelFactory,
    authorization: Authorization?,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    FriendlyTheme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    navController = navController,
                    navigationItems = homeNavigationItems,

                )
            },
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.navigationBars,
        ) { innerPadding ->
            FriendlyNavGraph(
                navController = navController,
                viewModelFactory = viewModelFactory,
                authorization = authorization,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    navigationItems: List<HomeNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentDestinationHierarchy = currentDestination?.hierarchy

    val isHome = currentDestinationHierarchy?.any { destination ->
        homeNavigationItems.any { item ->
            destination.hasRoute(item.destination::class)
        }
    } ?: false

    if (isHome) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier,
        ) {
            for (item in navigationItems) {
                val selected = currentDestinationHierarchy.any { destination ->
                    destination.hasRoute(item.destination::class)
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.destination) {
                            popUpTo(
                                navController.graph.findStartDestination().id,
                            ) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(item.titleResource),
                            fontWeight = if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    icon = {
                        Icon(
                            painter = if (selected) {
                                painterResource(item.selectedIconResource)
                            } else {
                                painterResource(item.unselectedIconResource)
                            },
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier,
                )
            }
        }
    }
}
