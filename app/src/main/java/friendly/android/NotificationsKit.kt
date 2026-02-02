package friendly.android

import java.util.concurrent.atomic.AtomicInteger

object NotificationsKit {
    private val id = AtomicInteger(0)

    fun getNextId(): Int = id.getAndIncrement()
}
