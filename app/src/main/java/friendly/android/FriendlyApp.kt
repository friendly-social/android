package friendly.android

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import friendly.sdk.Authorization

data class HomeNavigationItem(
    val titleResource: Int,
    val iconResource: Int = R.drawable.ic_photo_camera,
    val destination: FriendlyNavGraph.Home,
)

val homeNavigationItems = listOf(
    HomeNavigationItem(
        titleResource = R.string.feed,
        iconResource = R.drawable.ic_cards_star,
        destination = FriendlyNavGraph.Home.Feed,
    ),
    HomeNavigationItem(
        titleResource = R.string.network,
        iconResource = R.drawable.ic_group,
        destination = FriendlyNavGraph.Home.Network,
    ),
    HomeNavigationItem(
        titleResource = R.string.profile,
        iconResource = R.drawable.ic_person,
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
            modifier = modifier
                .fillMaxSize(),
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
            modifier = modifier,
        ) {
            for (item in navigationItems) {
                val selected = currentDestinationHierarchy.any { destination ->
                    destination.hasRoute(item.destination::class)
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { navController.navigate(item.destination) },
                    label = { Text(stringResource(item.titleResource)) },
                    icon = {
                        Icon(
                            painter = painterResource(item.iconResource),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier,
                )
            }
        }
    }
}
