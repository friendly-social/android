package friendly.android

import androidx.compose.foundation.text.input.TextFieldState

val TextFieldState.string
    get() = this.text.toString()
