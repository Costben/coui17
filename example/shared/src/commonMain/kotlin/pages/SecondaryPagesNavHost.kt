// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package pages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.savedstate.serialization.SavedStateConfiguration
import appstate.LocalAppState
import appstate.LocalNavigator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import navigation3.Navigator
import navigation3.Route

/**
 * Hosts the secondary pages behind a caller-supplied home screen.
 *
 * The ColorOS 17 gallery draws its own chrome and its own home, so it needs the pages without any
 * home of their own. This composable is the navigation half alone: it builds the back stack,
 * publishes the [Navigator] into [appstate.LocalNavigator], and renders [mainContent] as the
 * `Route.Main` entry so every `navigator.push` inside it lands somewhere. The pages it hosts are
 * listed in its `entryProvider`. Without a navigator and a `Route.Main` entry the pushes would fall
 * on a stack nothing draws, and the arrows in the "Other" section would be inert.
 *
 * @param padding The outer padding the host already applies to its own content. Each page adds it
 *   to the insets of the scaffold it draws, so this is normally zero for a full-window host.
 * @param mainContent The host's home screen: the `Route.Main` entry.
 */
@Composable
fun SecondaryPagesNavHost(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    val appState = LocalAppState.current

    val serializersModule = remember {
        SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Route.Main::class)
                subclass(Route.PullToRefresh::class)
                subclass(Route.FullPagePullToRefresh::class)
                subclass(Route.NotificationSwipe::class)
                subclass(Route.About::class)
                subclass(Route.License::class)
                subclass(Route.Navigation::class)
                subclass(Route.MultiScaffold::class)
            }
        }
    }

    val savedStateConfig = remember(serializersModule) {
        SavedStateConfiguration {
            this.serializersModule = serializersModule
        }
    }

    val backStack = rememberNavBackStack(
        configuration = savedStateConfig,
        Route.Main,
    )
    val navigator = remember { Navigator(backStack) }

    val entryProvider = remember(backStack) {
        entryProvider<NavKey> {
            entry<Route.Main> { mainContent() }
            entry<Route.PullToRefresh> {
                PullToRefreshPage(padding = padding)
            }
            entry<Route.FullPagePullToRefresh> {
                FullPagePullToRefreshPage(padding = padding)
            }
            entry<Route.NotificationSwipe> {
                NotificationSwipePage(padding = padding)
            }
            entry<Route.About> {
                AboutPage(padding = padding)
            }
            entry<Route.License> {
                LicensePage(padding = padding)
            }
            entry<Route.Navigation> { route ->
                val index = backStack.filterIsInstance<Route.Navigation>().indexOf(route) + 1
                NavTestPage(
                    index = index,
                    padding = padding,
                )
            }
            entry<Route.MultiScaffold> {
                MultiScaffoldTestPage(padding = padding)
            }
        }
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    val transitionEffects = remember(
        appState.enableCornerClip,
        appState.enableDim,
        appState.blockInputDuringTransition,
        appState.popDirectionFollowsSwipeEdge,
    ) {
        NavDisplayTransitionEffects(
            enableCornerClip = appState.enableCornerClip,
            dimAmount = if (appState.enableDim) 0.5f else 0f,
            blockInputDuringTransition = appState.blockInputDuringTransition,
            popDirectionFollowsSwipeEdge = appState.popDirectionFollowsSwipeEdge,
        )
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            entries = entries,
            modifier = modifier,
            onBack = { navigator.pop() },
            transitionEffects = transitionEffects,
        )
    }
}
