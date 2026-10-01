// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Banner
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.LockPatternDisplayMode
import io.github.suqi8.coui.kmp.basic.LockPatternView
import io.github.suqi8.coui.kmp.basic.LockScreenPwdInput
import io.github.suqi8.coui.kmp.basic.NumericKeyboard
import io.github.suqi8.coui.kmp.basic.NumericKeyboardDefaults
import io.github.suqi8.coui.kmp.basic.SimpleLockType
import io.github.suqi8.coui.kmp.basic.SimpleLockView
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.squircle.squircleSurface

fun LazyListScope.lockAndBannerSection() {
    item(key = "lockAndBanner") {
        SmallTitle(text = "LockPatternView / NumericKeyboard / SimpleLockView / LockScreenPwdInput / Banner")
        LockAndBannerDemo()
    }
}

/** Renders the lock pattern, numeric keyboard, simple lock, password input and banner demos inside one card. */
@Composable
fun LockAndBannerDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        LockPatternDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        NumericKeyboardDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SimpleLockDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        LockScreenPwdInputDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        BannerDemo()
    }
}

@Composable
private fun LockPatternDemo() {
    var mode by remember { mutableStateOf(LockPatternDisplayMode.Correct) }
    var pattern by remember { mutableStateOf(listOf(0, 4, 8)) }
    val drawAPatternStatus = str("Draw a pattern")
    var status by remember { mutableStateOf(drawAPatternStatus) }
    val patternPrefix = str("Pattern: ")
    val correctStatus = str("Pattern: 0-1-2-5-8 (correct)")
    val wrongStatus = str("Pattern: 0-1-2-5-8 (wrong)")
    val replayStatus = str("Replaying 0-3-6-7-8")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LockPatternView(
            onPatternComplete = { drawn ->
                pattern = drawn
                status = patternPrefix + drawn.joinToString("-")
            },
            mode = mode,
            pattern = pattern,
        )
        Text(
            text = status,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = str("Correct"),
                onClick = {
                    pattern = listOf(0, 1, 2, 5, 8)
                    mode = LockPatternDisplayMode.Correct
                    status = correctStatus
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = str("Wrong"),
                onClick = {
                    pattern = listOf(0, 1, 2, 5, 8)
                    mode = LockPatternDisplayMode.Wrong
                    status = wrongStatus
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = str("Replay"),
                onClick = {
                    pattern = listOf(0, 3, 6, 7, 8)
                    mode = LockPatternDisplayMode.Animate
                    status = replayStatus
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumericKeyboardDemo() {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = if (pin.isEmpty()) str("Enter a PIN") else pin)
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .squircleSurface(color = Color(0xFF1B1B1B), cornerRadius = 24.dp),
        ) {
            NumericKeyboard(
                onNumberClick = { digit ->
                    if (pin.length < 6) pin += digit.toString()
                },
                leftStyle = NumericKeyboardDefaults.deleteSideStyle(COUIIcons.Delete),
                rightStyle = NumericKeyboardDefaults.finishSideStyle(),
                onLeftClick = { pin = pin.dropLast(1) },
                onRightClick = { pin = "" },
            )
        }
    }
}

@Composable
private fun SimpleLockDemo() {
    var count by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(SimpleLockType.Six) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SimpleLockView(
            count = count,
            type = type,
            failed = failed,
        )
        Text(
            text = str("Filled ") + count.toString() + str(" of ") + type.dotCount.toString(),
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = str("Add"),
                onClick = {
                    failed = false
                    count = (count + 1).coerceAtMost(type.dotCount)
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = str("Remove"),
                onClick = {
                    failed = false
                    count = (count - 1).coerceAtLeast(0)
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = str("Fail"),
                onClick = { failed = !failed },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = str("4 dots"),
                onClick = {
                    type = SimpleLockType.Four
                    count = count.coerceAtMost(type.dotCount)
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = str("6 dots"),
                onClick = {
                    type = SimpleLockType.Six
                    count = count.coerceAtMost(type.dotCount)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LockScreenPwdInputDemo() {
    var password by remember { mutableStateOf("") }
    val typeAPasswordStatus = str("Type a password")
    var status by remember { mutableStateOf(typeAPasswordStatus) }
    val nextPrefix = str("Next: ")
    val charactersSuffix = str(" characters")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LockScreenPwdInput(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = str("Password"),
            minCount = 4,
            onNextClick = { status = nextPrefix + it.length.toString() + charactersSuffix },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Text(
            text = status,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun BannerDemo() {
    val cardOne = str("Card one")
    val cardTwo = str("Card two")
    val cardThree = str("Card three")
    val cardFour = str("Card four")
    val pages = remember(cardOne, cardTwo, cardThree, cardFour) {
        listOf(
            Color(0xFF3A6EA5) to cardOne,
            Color(0xFF7A4FA3) to cardTwo,
            Color(0xFF2E8B6E) to cardThree,
            Color(0xFFB4553C) to cardFour,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Banner(
            pageCount = pages.size,
            autoScrollIntervalMillis = 3000L,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            itemColor = pages.first().first,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pages[page].first),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = pages[page].second, color = Color.White)
            }
        }
    }
}
