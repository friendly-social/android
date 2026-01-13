package friendly.android

import friendly.sdk.FriendlyFeedClient

class LoadFeedQueueUseCase(
    private val feedClient: FriendlyFeedClient,
    private val authStorage: AuthStorage,
) {
    suspend operator fun invoke(): FriendlyFeedClient.QueueResult {
        val queueResult = feedClient.queue(
            authorization = authStorage.getAuth(),
        )
        return queueResult
    }
}
