package friendly.android

import android.content.Context
import friendly.sdk.LocaleCode

class LocaleRepository(private val applicationContext: Context) {
    fun obtain(): LocaleCode {
        val locale = applicationContext.resources.configuration.locales[0]
        val codes = mapOf(
            "ru" to LocaleCode.Ru,
            "en" to LocaleCode.En,
        )
        return codes[locale.language] ?: LocaleCode.En
    }
}
