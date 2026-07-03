package friendly.android

import friendly.sdk.FriendlyNetworkClient
import friendly.sdk.NetworkDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NetworkRepository(
    private val dao: NetworkDao,
    private val networkClient: FriendlyNetworkClient,
    private val authStorage: AuthStorage,
    private val scope: CoroutineScope,
) {
    private val _friends = MutableStateFlow<NetworkDetails?>(null)

    val friends: StateFlow<NetworkDetails?> = _friends.stateIn(
        scope = scope,
        started = Eagerly,
        initialValue = null,
    )

    fun sync(details: NetworkDetails) {
        TODO("Not implemented yet.")
    }
}
