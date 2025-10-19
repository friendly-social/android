package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import friendly.android.ProfileScreenModel.Data.UserDetails
import friendly.android.ProfileScreenModel.Loading
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import friendly.sdk.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

sealed interface ProfileScreenModel {
    data class Data(
        val userDetails: UserDetails,
    ) : ProfileScreenModel {
        data class UserDetails(
            val nickname: Nickname,
            val description: UserDescription,
            val interests: List<Interest>,
        )
    }

    data object Loading : ProfileScreenModel
}

@Composable
fun ProfileScreenModel(
    userId: UserId,
): ProfileScreenModel {
    var data: ProfileScreenModel by remember { mutableStateOf(Loading) }

    LaunchedEffect(Unit) {
        println("user id: ${userId.long}")

        delay(1500L)

        data = ProfileScreenModel.Data(
            UserDetails(
                nickname = Nickname.orThrow("y9kap"),
                description = UserDescription.orThrow("mikael fridman big friend"),
                interests = buildList {
                    add(Interest.orThrow("cooking"))
                    add(Interest.orThrow("singing"))
                    add(Interest.orThrow("macroelectricity"))
                    add(Interest.orThrow("sleep"))
                    add(Interest.orThrow("birds"))
                },
            ),
        )
    }

    return data
}

@Composable
fun ProfileScreen(
    userId: UserId,
    modifier: Modifier = Modifier,
) {
    val state = ProfileScreenModel(userId)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize(),
    ) {
        when (val state = state) {
            is ProfileScreenModel.Data -> {
                Text(
                    text = "data: ${state.userDetails}",
                )
                Text(
                    text = "explore your profile lol $userId",
                )
            }
            is Loading -> {
                CircularProgressIndicator()
            }
        }
    }
}
