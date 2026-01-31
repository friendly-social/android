package friendly.android

class FirebaseMessagingService :
    com.google.firebase.messaging.FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseKit.onNewToken()
    }
}
