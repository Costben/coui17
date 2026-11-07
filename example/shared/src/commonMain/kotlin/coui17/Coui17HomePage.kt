// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appstate.LocalNavigator
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import navigation3.Route

/**
 * The home page: the component list, one titled card per category in [Coui17Catalog].
 *
 * Each row only names a component and opens its page, the way COUI-Kit's demo and the system
 * Settings list do: the demos themselves live on [Coui17ComponentPage], so the home page stays one
 * screen of short lists instead of every demo stacked into a single scroll.
 *
 * Every heading and row resolves through [str], so the page follows the language switch: the
 * catalog's titles are English source keys, never a second copy of the same name.
 */
@Composable
fun Coui17HomePage(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        Coui17Catalog.forEach { category ->
            item(key = "title:${category.title}") { Coui17GroupTitle(category.title) }
            item(key = "card:${category.title}") {
                Card(
                    cornerRadius = CardDefaults.GroupCornerRadius,
                    weight = CardDefaults.GroupWeight,
                    modifier = Modifier
                        .padding(horizontal = Coui17Dimens.ListCardInset)
                        .padding(bottom = 16.dp),
                ) {
                    val last = category.components.lastIndex
                    category.components.forEachIndexed { index, component ->
                        ArrowPreference(
                            title = str(component.title),
                            onClick = { navigator.push(Route.Component(component.title)) },
                            cardListPosition = when {
                                last == 0 -> CardListPosition.Full
                                index == 0 -> CardListPosition.Head
                                index == last -> CardListPosition.Tail
                                else -> CardListPosition.Middle
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A component's page: its demos in catalog order. A component with several demos titles each one;
 * a single demo goes untitled, because the bar above already names it.
 *
 * Shared demos own their 16dp card inset and bottom spacing, so this list adds neither again. Each
 * demo sits in a [Column], not a box: several emit more than one root — `ButtonDemo` alone emits
 * four rows — and a box would stack them on the same origin.
 */
@Composable
internal fun Coui17ComponentPage(
    component: Coui17Component,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val titled = component.demos.size > 1
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(component.demos, key = { _, demo -> demo.title }) { _, demo ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (titled) {
                    Coui17GroupTitle(demo.title)
                } else {
                    Spacer(modifier = Modifier.height(Coui17Dimens.ListGroupTitlePadding))
                }
                demo.content(snackbarHostState)
            }
        }
    }
}

/**
 * A compact Settings group title: `SmallTitle` at 12sp in `onBackgroundVariant`, aligned with the
 * text inside its card.
 *
 * The row reproduces `Widget.COUI.List.Category.Title` — 8dp of padding above and below a 16dp line
 * box, no gap after the row — and the 16dp between it and the card above comes from that card's own
 * bottom spacing, so a titled group reads like the Settings list.
 *
 * [title] is the English source string; the heading resolves it here, so the entry list above stays
 * a plain list of keys and the language can change under it.
 */
@Composable
private fun Coui17GroupTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Coui17Dimens.ListCardInset),
    ) {
        SmallTitle(
            text = str(title),
            insideMargin = PaddingValues(
                horizontal = Coui17Dimens.ListTextIndent,
                vertical = Coui17Dimens.ListGroupTitlePadding,
            ),
        )
    }
}
