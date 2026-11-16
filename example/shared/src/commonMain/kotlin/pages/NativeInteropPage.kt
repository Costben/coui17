// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import i18n.str
import interop.LocalNativeHostBackend
import interop.LocalNativeViewControllerSlot
import interop.LocalNativeViewSlot
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.ButtonSize
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Demonstrates bidirectional Swift / UIKit interoperability:
 * - Calling an injected [interop.NativeHostBackend] from COUI controls.
 * - Hosting native `UIView` and `UIViewController` slots inside Compose via [LocalNativeViewSlot]
 *   and [LocalNativeViewControllerSlot], with COUI placeholders on non-iOS targets.
 */
@Composable
fun NativeInteropPage(
    padding: PaddingValues,
) {
    val backend = LocalNativeHostBackend.current
    val nativeViewSlot = LocalNativeViewSlot.current
    val nativeViewControllerSlot = LocalNativeViewControllerSlot.current
    val focusManager = LocalFocusManager.current

    var topic by remember { mutableStateOf("ColorOS 17") }
    var backendOutput by remember(backend) {
        mutableStateOf(backend.fetchHostGreeting("ColorOS 17"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            SmallTitle(text = str("Native Host Backend"))
            Card(
                cornerRadius = CardDefaults.GroupCornerRadius,
                weight = CardDefaults.GroupWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                BasicComponent(
                    title = str("Host Platform"),
                    summary = backend.hostPlatformName,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = str("Interop topic"),
                        backgroundMode = TextFieldMode.Line,
                        useLabelAsPlaceholder = true,
                        showClearButton = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                backendOutput = backend.fetchHostGreeting(topic)
                            },
                            modifier = Modifier.weight(1f),
                            size = ButtonSize.Small,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text(text = str("Fetch Greeting"))
                        }
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                backendOutput = backend.dispatchHostAction(topic.ifBlank { "ping" })
                            },
                            modifier = Modifier.weight(1f),
                            size = ButtonSize.Small,
                        ) {
                            Text(text = str("Dispatch Action"))
                        }
                    }
                    Text(
                        text = backendOutput,
                        style = COUITheme.textStyles.body2,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = str("UIKitView Slot"))
            Card(
                cornerRadius = CardDefaults.GroupCornerRadius,
                weight = CardDefaults.GroupWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                BasicComponent(
                    title = str("Embedded Native UIView"),
                    summary = if (nativeViewSlot != null) {
                        str("Live UIKit UIView hosted inside Compose")
                    } else {
                        str("No native UIView slot provided on this platform")
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    if (nativeViewSlot != null) {
                        nativeViewSlot(
                            Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    } else {
                        NativeSlotPlaceholder(
                            message = str("Placeholder: UIKitView slot renders when hosted by iOS Swift."),
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = str("UIKitViewController Slot"))
            Card(
                cornerRadius = CardDefaults.GroupCornerRadius,
                weight = CardDefaults.GroupWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                BasicComponent(
                    title = str("Embedded Native UIViewController"),
                    summary = if (nativeViewControllerSlot != null) {
                        str("Live UIKit UIViewController hosted inside Compose")
                    } else {
                        str("No native UIViewController slot provided on this platform")
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    if (nativeViewControllerSlot != null) {
                        nativeViewControllerSlot(
                            Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    } else {
                        NativeSlotPlaceholder(
                            message = str("Placeholder: UIKitViewController slot renders when hosted by iOS Swift."),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeSlotPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(COUITheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            style = COUITheme.textStyles.body2,
            color = COUITheme.colorScheme.onSecondaryContainer,
        )
    }
}
