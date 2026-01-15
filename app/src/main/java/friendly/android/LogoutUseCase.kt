package friendly.android

class LogoutUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
) {
    operator fun invoke() {
        authStorage.clear()
        selfProfileStorage.clear()
    }
}
