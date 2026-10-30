package friendly.android

import android.content.Context
import androidx.core.content.edit

private const val PostDraftPreferences = "post_draft_preferences"
private const val PostDraftPreference = "post_draft_preferences"

class PostDraftStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        PostDraftPreferences,
        Context.MODE_PRIVATE,
    )

    fun store(string: String) {
        preferences.edit {
            putString(PostDraftPreference, string)
            commit()
        }
    }

    fun clear() {
        preferences.edit {
            putString(PostDraftPreference, null)
            commit()
        }
    }

    fun get(): String? = preferences.getString(PostDraftPreference, null)
}
