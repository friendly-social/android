package friendly.android

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import friendly.android.FriendlyNavGraph.Home

fun NavHostController.switchHomeTab(tab: Home) {
    val navController = this
    navController.navigate(tab) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
