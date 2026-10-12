package friendly.android

class LogoutUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val db: FriendlyDatabase,
) {
    suspend operator fun invoke(): Boolean {
        if (!FirebaseKit.onLogout()) return false
        authStorage.clear()
        selfProfileStorage.clear()
        db.queryItemDao().clearAll()
        db.pagingCacheDao().clearAll()
        return true
    }
}
