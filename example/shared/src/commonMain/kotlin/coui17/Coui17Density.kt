// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

/**
 * Density facts of the reference device (OnePlus PLK110, 1272x2772 px).
 *
 * The screen reports 560 dpi, so a third-party app lays out at 3.5 px/dp, while the system forces
 * 620 dpi for its own apps, which puts Settings at 3.875 px/dp. Every screen in this gallery copies
 * a system screen, so the gallery is laid out at [SYSTEM_APP] to keep the replicas pixel-identical to
 * the screens they were measured from.
 */
internal object Coui17Density {
    /** 620 / 160. */
    const val SYSTEM_APP = 3.875f
}
