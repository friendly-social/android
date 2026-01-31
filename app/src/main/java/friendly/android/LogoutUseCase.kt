package friendly.android

class LogoutUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
) {
    suspend operator fun invoke(): Boolean {
        if (!FirebaseKit.onLogout()) return false
        authStorage.clear()
        selfProfileStorage.clear()
        return true
    }
}
