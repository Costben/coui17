// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package i18n

/**
 * Every Chinese entry the gallery ships, assembled from the part each surface owns.
 *
 * The parts are disjoint: the shell, the five pages and the component demos never share a key, so
 * a later part can never quietly shadow an earlier one.
 */
internal val zhStrings: Map<String, String> = zhChrome + zhPages + zhComponents
