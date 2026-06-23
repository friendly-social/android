package friendly.android

data class ValidatableField<T>(val value: T, val isValid: Boolean = true)

val ValidatableField<String>.invalidAndNotBlank: Boolean
    get() = this.value.isNotBlank() && !this.isValid
