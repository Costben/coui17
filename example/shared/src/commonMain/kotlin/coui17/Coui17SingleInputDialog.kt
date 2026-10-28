// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package coui17

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The single-input dialog as the gallery shows it, with neutral placeholder copy. The "Dialog"
 * entry opens it from its own row, next to the other dialogs, so every dialog in the gallery is
 * the same [OverlayDialog] reached the same way.
 *
 * @param show Whether the dialog is shown.
 * @param onDismissRequest Called when the dialog closes.
 * @param onDismissFinished Called once the exit animation has finished.
 */
@Composable
fun SingleInputDialogDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val initial = str("Sample text")
    var value by remember { mutableStateOf(initial) }
    SingleInputDialog(
        show = show,
        title = str("Enter a name"),
        message = str("A sample message showing how the dialog body is laid out"),
        value = value,
        onValueChange = { value = it },
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        onConfirm = onDismissRequest,
        confirmText = str("Save"),
        cancelText = str("Cancel"),
    )
}

/**
 * The ColorOS 17 single-input dialog template: a centred panel with a title, a message, one
 * 50dp input row carrying a byte counter and a clear button, and a two-button bar.
 *
 * Geometry is taken from the live dialog, measured at 3.875 px/dp:
 * panel 1148x873 px (296x225 dp), title 24dp from the top, message 6dp under the title, the input
 * row flush with the 24dp side inset, and the button bar 22dp under the row.
 *
 * @param show Whether the dialog is shown.
 * @param title The panel title.
 * @param message The panel body copy.
 * @param value The current text in the field.
 * @param onValueChange Called with the new text, already limited to [maxCount] UTF-8 bytes.
 * @param onDismissRequest Called when the dialog is dismissed by the negative button, an outside
 *   tap or back.
 * @param onConfirm Called when the positive button is tapped.
 * @param modifier The modifier applied to the dialog.
 * @param onDismissFinished Called once the exit animation has finished.
 * @param maxCount The byte budget of the field; the counter shows `used/maxCount` in UTF-8 bytes,
 *   mirroring `Utf8ByteLengthFilter`.
 * @param confirmText The positive button label.
 * @param cancelText The negative button label.
 */
@Composable
fun SingleInputDialog(
    show: Boolean,
    title: String,
    message: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissFinished: (() -> Unit)? = null,
    maxCount: Int = 32,
    confirmText: String = "Save",
    cancelText: String = "Cancel",
) {
    val used = remember(value) { value.encodeToByteArray().size }
    val canSave = value.isNotBlank()
    val focusRequester = remember { FocusRequester() }

    // The same OverlayDialog every other gallery dialog uses: its title, message, panel material
    // and button bar are the library's, and only the input row is this template's own.
    OverlayDialog(
        show = show,
        modifier = modifier,
        title = title,
        summary = message,
        // The field opens focused, so the keyboard comes up with the dialog; the IME padding keeps
        // the panel above it, as the native dialog rises to sit 16dp over the keyboard.
        defaultWindowInsetsPadding = true,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SingleInputField(
                value = value,
                onValueChange = { onValueChange(it.limitUtf8Bytes(maxCount)) },
                used = used,
                maxCount = maxCount,
                focusRequester = focusRequester,
                autoFocus = show,
                // 24dp side inset like the title; 18dp below plus the bar's own 4dp top padding
                // keeps the measured 22dp between the row and the buttons.
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 18.dp),
            )
            DialogButtonBar(
                negative = DialogButtonBarAction(text = cancelText, onClick = onDismissRequest),
                positive = DialogButtonBarAction(text = confirmText, enabled = canSave, onClick = onConfirm),
            )
        }
    }
}

/**
 * The `COUIInputView` row: 50dp tall, a borderless 15sp field, the `n/max` counter and the
 * `ic_edit_text_delete` clear button, all vertically centred and flush with the 24dp inset.
 *
 * The row also carries the field's focus underline. `COUIEditText.onDraw` draws it whenever
 * `mBoxBackgroundMode == 1` and the field has focus: a `mStrokeWidthFocused`-tall (1dp) rectangle
 * across the full row width in `mFocusedStrokeColor`, which defaults to `couiColorPrimary`
 * (`#1A8CFF` in night mode). The unfocused stroke colour defaults to transparent, so nothing is
 * painted while the field is idle.
 */
@Composable
private fun SingleInputField(
    value: String,
    onValueChange: (String) -> Unit,
    used: Int,
    maxCount: Int,
    focusRequester: FocusRequester,
    autoFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val accent = COUITheme.colorScheme.primary
    // The dialog opens with the field already focused (the native dialog calls
    // `inputView.requestFocus()` in `onShow`), which is also what makes the focus underline appear.
    // Requesting focus from here rather than from the dialog body keeps the requester and its
    // modifier in the same composition, so the node is attached by the time the effect runs.
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .drawWithContent {
                drawContent()
                if (focused) {
                    val stroke = 1.dp.toPx()
                    drawRect(
                        color = accent,
                        topLeft = Offset(0f, size.height - stroke),
                        size = Size(size.width, stroke),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = COUITheme.colorScheme.onBackground,
            ),
            singleLine = true,
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions.Default,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    innerTextField()
                }
            },
        )
        // The counter is a TextView centred by the row's `gravity=center_vertical`, so its glyphs
        // straddle the row centre. A 22sp line box would park the baseline at its top and lift the
        // digits ~4dp; trimming the line box to the font's own ascent/descent keeps them centred.
        Text(
            text = "$used/$maxCount",
            style = trimmedLineHeight,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            color = COUITheme.colorScheme.onSurfaceVariantActions,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ClearTextButton(
            enabled = value.isNotEmpty(),
            onClick = { onValueChange("") },
        )
    }
}

/**
 * The `R.id.delete_button` ImageButton: `ic_edit_text_delete`, 22dp, tinted
 * `couiColorLabelTertiary` (`#42000000` / `#4DFFFFFF`).
 */
@Composable
private fun ClearTextButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = COUITheme.colorScheme.onSurfaceVariantActions
    Box(
        modifier = modifier
            .size(22.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(EditTextDeleteIcon),
            contentDescription = str("Clear text"),
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

/**
 * `res/drawable/ic_edit_text_delete.xml` — a 22×22dp disc with a knocked-out cross.
 */
private val EditTextDeleteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "EditTextDelete",
        defaultWidth = 22.dp,
        defaultHeight = 22.dp,
        viewportWidth = 22f,
        viewportHeight = 22f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M11,20C15.971,20 20,15.971 20,11C20,6.029 15.971,2 11,2C6.029,2 2,6.029 2,11C2,15.971 " +
                "6.029,20 11,20ZM8.596,7.464L11,9.868L13.404,7.465C13.716,7.152 14.223,7.152 " +
                "14.535,7.465C14.848,7.777 14.848,8.283 14.535,8.596L12.132,11L14.536,13.404C14.848," +
                "13.716 14.848,14.223 14.536,14.535C14.224,14.848 13.717,14.848 13.405,14.535L11," +
                "12.131L8.595,14.536C8.283,14.848 7.777,14.848 7.464,14.536C7.152,14.223 7.152," +
                "13.717 7.464,13.404L9.869,11L7.465,8.595C7.152,8.283 7.152,7.777 7.465,7.464C7.777," +
                "7.152 8.284,7.152 8.596,7.464Z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/**
 * Truncates the string so that its UTF-8 encoding fits in [maxBytes] bytes, never splitting a
 * surrogate pair — the Compose counterpart of `android.text.InputFilter.Utf8ByteLengthFilter`.
 */
private fun String.limitUtf8Bytes(maxBytes: Int): String {
    if (encodeToByteArray().size <= maxBytes) return this
    var bytes = 0
    var end = 0
    while (end < length) {
        val c = this[end]
        val paired = c.isHighSurrogate() && end + 1 < length && this[end + 1].isLowSurrogate()
        val size = when {
            paired -> 4
            c.code < 0x80 -> 1
            c.code < 0x800 -> 2
            else -> 3
        }
        if (bytes + size > maxBytes) break
        bytes += size
        end += if (paired) 2 else 1
    }
    return substring(0, end)
}

/**
 * `COUILineHeightStyle` keeps the full font box (`Trim.None`, top-aligned), which renders 1-2px
 * taller than the native `COUIDialogTitle` / `COUIAlertDialogMessageView` line boxes. Both text
 * slots are pinned to their measured heights, so the trimmed style keeps the glyphs centred
 * inside them.
 */
private val trimmedLineHeight: TextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
