package friendly.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import friendly.android.ui.theme.FriendlyandroidTheme
import friendly.sdk.FriendlyClient
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription
import kotlinx.coroutines.launch

class MockDataActivity : ComponentActivity() {
    private val client = FriendlyClient.meetacy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isLoading by remember { mutableStateOf(false) }

            FriendlyandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    ) {
                        Text("Generate mock account")

                        Spacer(Modifier.height(16.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    generateAccount()
                                },
                            ) {
                                Text(text = "Generate")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateAccount() {
        lifecycleScope.launch {
            val randomNumber = (1..1_000_000).random()
            val nickname = Nickname.orThrow("random-person-$randomNumber")
            val description = UserDescription.orThrow("Phronology Evangelist")
            val interests = listOf(Interest.orThrow("phronology"))
            val socialLink = SocialLink.orThrow("https://github.com/demndevel")
            val authorization1 = client.auth.generate(
                nickname = nickname,
                description = description,
                interests = interests,
                avatar = null,
                socialLink = socialLink,
            ).orThrow()
            println("=== Authorization 1 ===")
            println(authorization1)
            println()
            val authorization2 = client.auth.generate(
                nickname = Nickname.orThrow("y9demn"),
                description = UserDescription.orThrow("Zed Enjoyer"),
                interests = listOf(Interest.orThrow("zed")),
                avatar = null,
                socialLink = SocialLink.orThrow("TODO()"),
            ).orThrow()
            println("=== Authorization 2 ===")
            println(authorization2)
            println()
            val authorization3 = client.auth.generate(
                nickname = Nickname.orThrow("y9kap"),
                description = UserDescription.orThrow(
                    "Senior Python Developer",
                ),
                interests = listOf(Interest.orThrow("python3+")),
                avatar = null,
                socialLink = SocialLink.orThrow("TODO()"),
            ).orThrow()
            println("=== Authorization 3 ===")
            println(authorization3)
            println()
            val authorization4 = client.auth.generate(
                nickname = Nickname.orThrow("otomir23"),
                description = UserDescription.orThrow("Webring Master"),
                interests = listOf(Interest.orThrow("webring")),
                avatar = null,
                socialLink = SocialLink.orThrow("TODO()"),
            ).orThrow()
            println("=== Authorization 4 ===")
            println(authorization4)
            println()
            val friend1Token = client.friends.generate(authorization1).orThrow()
            println("=== Friend 1 Token ===")
            println(friend1Token)
            println()
            val add1ResultSuccess = client.friends
                .add(authorization2, friend1Token, authorization1.id)
                .orThrow()
            println("=== Add Friend 1 Success ===")
            println(add1ResultSuccess)
            println()
            val friend2Token = client.friends.generate(authorization2).orThrow()
            println("=== Friend 2 Token ===")
            println(friend1Token)
            println()
            val add2ResultSuccess = client.friends
                .add(authorization3, friend2Token, authorization2.id)
                .orThrow()
            println("=== Add Friend 2 Success ===")
            println(add2ResultSuccess)
            println()
            val friend3Token = client.friends.generate(authorization3).orThrow()
            println("=== Friend 3 Token ===")
            println(friend2Token)
            println()
            val add3ResultSuccess = client.friends
                .add(authorization4, friend3Token, authorization3.id)
                .orThrow()
            println("=== Add Friend 3 Success ===")
            println(add3ResultSuccess)
            println()
            val network = client.feed.queue(authorization1).orThrow()
            println("=== Feed ===")
            println(network.entries)
            println()

            val selfProfileStorage = SelfProfileStorage(this@MockDataActivity)
            val authStorage = AuthStorage(this@MockDataActivity)
            authStorage.clear()
            selfProfileStorage.clear()
            selfProfileStorage.store(
                nickname = nickname,
                description = description,
                avatar = null,
                interests = interests,
                userId = authorization1.id,
            )
            authStorage.store(authorization1)

            val intent = Intent(this@MockDataActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
