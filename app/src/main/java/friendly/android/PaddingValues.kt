package friendly.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

infix fun PaddingValues.plusAll(other: PaddingValues) = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        this@plusAll.calculateLeftPadding(layoutDirection) +
            other.calculateLeftPadding(layoutDirection)
    override fun calculateTopPadding(): Dp =
        this@plusAll.calculateTopPadding() + other.calculateTopPadding()
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        this@plusAll.calculateRightPadding(layoutDirection) +
            other.calculateRightPadding(layoutDirection)
    override fun calculateBottomPadding(): Dp =
        this@plusAll.calculateBottomPadding() + other.calculateBottomPadding()
}

fun PaddingValues.plusHorizontal(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateLeftPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plusHorizontal.calculateLeftPadding(layoutDirection) +
            other.calculateLeftPadding(layoutDirection)
        override fun calculateRightPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plusHorizontal.calculateRightPadding(layoutDirection) +
            other.calculateRightPadding(layoutDirection)
    }

fun PaddingValues.plusVertical(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateTopPadding(): Dp =
            this@plusVertical.calculateTopPadding() +
                other.calculateTopPadding()
        override fun calculateBottomPadding(): Dp =
            this@plusVertical.calculateBottomPadding() +
                other.calculateBottomPadding()
    }

fun PaddingValues.plusLeft(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateLeftPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plusLeft.calculateLeftPadding(layoutDirection) +
            other.calculateLeftPadding(layoutDirection)
    }

fun PaddingValues.plusRight(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateRightPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plusRight.calculateRightPadding(layoutDirection) +
            other.calculateRightPadding(layoutDirection)
    }

fun PaddingValues.plusTop(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateTopPadding(): Dp =
            this@plusTop.calculateTopPadding() + other.calculateTopPadding()
    }

fun PaddingValues.plusBottom(other: PaddingValues) =
    object : PaddingValues by this {
        override fun calculateBottomPadding(): Dp =
            this@plusBottom.calculateBottomPadding() +
                other.calculateBottomPadding()
    }

fun PaddingValues.plus(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) =
    object : PaddingValues {
        override fun calculateLeftPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plus.calculateLeftPadding(layoutDirection) + horizontal
        override fun calculateTopPadding(): Dp =
            this@plus.calculateTopPadding() + vertical
        override fun calculateRightPadding(
            layoutDirection: LayoutDirection,
        ): Dp = this@plus.calculateRightPadding(layoutDirection) + horizontal
        override fun calculateBottomPadding(): Dp =
            this@plus.calculateBottomPadding() + vertical
    }

fun PaddingValues.flipHorizontal() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        this@flipHorizontal.calculateRightPadding(layoutDirection)
    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        this@flipHorizontal.calculateLeftPadding(layoutDirection)
}

fun PaddingValues.flipVertical() = object : PaddingValues by this {
    override fun calculateTopPadding() =
        this@flipVertical.calculateBottomPadding()
    override fun calculateBottomPadding() =
        this@flipVertical.calculateTopPadding()
}

fun PaddingValues.takeVertical() = dropHorizontal()
fun PaddingValues.takeHorizontal() = dropVertical()

fun PaddingValues.dropStart() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) {
            0.dp
        } else {
            this@dropStart.calculateLeftPadding(layoutDirection)
        }
    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Rtl) {
            0.dp
        } else {
            this@dropStart.calculateLeftPadding(layoutDirection)
        }
}

fun PaddingValues.dropEnd() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Rtl) {
            0.dp
        } else {
            this@dropEnd.calculateLeftPadding(layoutDirection)
        }
    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) {
            0.dp
        } else {
            this@dropEnd.calculateLeftPadding(layoutDirection)
        }
}

fun PaddingValues.dropTop() = object : PaddingValues by this {
    override fun calculateTopPadding() = 0.dp
}

fun PaddingValues.dropBottom() = object : PaddingValues by this {
    override fun calculateBottomPadding() = 0.dp
}

fun PaddingValues.dropHorizontal() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
}

fun PaddingValues.dropVertical() = object : PaddingValues by this {
    override fun calculateTopPadding() = 0.dp
    override fun calculateBottomPadding() = 0.dp
}

fun PaddingValues.takeTop() = object : PaddingValues by this {
    override fun calculateBottomPadding() = 0.dp
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
}

fun PaddingValues.takeBottom() = object : PaddingValues by this {
    override fun calculateTopPadding() = 0.dp
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
}

fun PaddingValues.takeStart() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection ==
            LayoutDirection.Ltr
        ) {
            this@takeStart.calculateLeftPadding(layoutDirection)
        } else {
            0.dp
        }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) {
            0.dp
        } else {
            this@takeStart.calculateLeftPadding(layoutDirection)
        }
}

fun PaddingValues.takeEnd() = object : PaddingValues by this {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) {
            0.dp
        } else {
            this@takeEnd.calculateLeftPadding(layoutDirection)
        }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection ==
            LayoutDirection.Ltr
        ) {
            this@takeEnd.calculateLeftPadding(layoutDirection)
        } else {
            0.dp
        }
}
