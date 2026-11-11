// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package coui17

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The five dual-tone tints ColorOS 17 paints its Settings icons with, in day and night values.
 *
 * Each tint is one hue at two weights: the glyph's main shape is the full colour, its accent layer
 * is the same colour at [Coui17CategoryIcons.ACCENT_ALPHA], so a row reads as one coloured mark
 * rather than two. [light] is the day value and [dark] the night one — the night set is lifted
 * slightly, because the same hue on a dark card otherwise loses contrast.
 */
internal enum class Coui17CategoryTint(val light: Color, val dark: Color) {
    Green(Color(0xFF00BD13), Color(0xFF24B232)),
    Blue(Color(0xFF0080FF), Color(0xFF1A8CFF)),
    Orange(Color(0xFFFF7700), Color(0xFFF08222)),
    Yellow(Color(0xFFFFB200), Color(0xFFE5A100)),
    Red(Color(0xFFEB3B2F), Color(0xFFEB493D)),
    ;

    /** The value for a dark background when [dark] is true, the day value otherwise. */
    fun resolve(dark: Boolean): Color = if (dark) this.dark else light
}

/**
 * The home page's category glyphs: one 24dp dual-tone mark per [Coui17Category].
 *
 * Every icon is built from two filled layers — a main shape and an accent — that the row tints with
 * one [Coui17CategoryTint]. The paths come from the ColorOS 17 icon set (`coui_grid_filled`,
 * `coui_bell_filled`, `coui_book_open_filled`, `coui_foldable_filled`, `coui_grid_sliders_filled`,
 * `coui_lab_filled`) and, for the three the set has no glyph for, are drawn here in the same
 * 24dp geometry; keeping them as [ImageVector]s rather than platform drawables is what lets the
 * Desktop and Android builds share one set.
 */
internal object Coui17CategoryIcons {

    /** The accent layer's weight: the second colour step of a ColorOS settings icon. */
    const val ACCENT_ALPHA = 0.4f

    /** `Basic`: four rounded tiles, the top-right one in the accent layer. */
    val Basic: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Basic",
            main = "M14.8,21H19.2C20.194,21 21,20.194 21,19.2V14.8C21,13.806 20.194,13 19.2,13H14.8" +
                "C13.806,13 13,13.806 13,14.8V19.2C13,20.194 13.806,21 14.8,21Z" +
                "M4.8,21H9.2C10.194,21 11,20.194 11,19.2V14.8C11,13.806 10.194,13 9.2,13H4.8" +
                "C3.806,13 3,13.806 3,14.8L3,19.2C3,20.194 3.806,21 4.8,21Z" +
                "M4.8,11H9.2C10.194,11 11,10.194 11,9.2V4.8C11,3.806 10.194,3 9.2,3H4.8" +
                "C3.806,3 3,3.806 3,4.8L3,9.2C3,10.194 3.806,11 4.8,11Z",
            accent = "M14.8,11H20.2C21.194,11 22,10.194 22,9.2V3.8C22,2.806 21.194,2 20.2,2H14.8" +
                "C13.806,2 13,2.806 13,3.8V9.2C13,10.194 13.806,11 14.8,11Z",
        )
    }

    /** `Buttons & actions`: a bell whose notification dot is the accent layer. */
    val Actions: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Actions",
            main = "M14.207,19.934C14.33,19.934 14.425,20.045 14.394,20.164C14.366,20.275 " +
                "14.331,20.384 14.287,20.49C14.161,20.793 13.977,21.069 13.745,21.301" +
                "C13.513,21.533 13.238,21.717 12.934,21.843C12.631,21.968 12.306,22.033 " +
                "11.977,22.033C11.649,22.033 11.324,21.968 11.02,21.843C10.717,21.717 " +
                "10.442,21.533 10.21,21.301C9.978,21.069 9.793,20.793 9.668,20.49" +
                "C9.624,20.384 9.589,20.275 9.56,20.164C9.53,20.045 9.625,19.934 9.748,19.934H14.207Z" +
                "M11.976,1.967C12.423,1.967 12.851,2.144 13.167,2.46C13.402,2.695 13.559,2.993 " +
                "13.625,3.313C13.747,3.346 13.866,3.383 13.985,3.423C13.784,3.92 13.673,4.464 " +
                "13.673,5.033C13.673,7.408 15.598,9.333 17.972,9.333C18.088,9.333 18.202,9.326 " +
                "18.315,9.317C18.316,9.347 18.318,9.376 18.318,9.405L18.473,12.596" +
                "C18.518,13.497 18.866,14.357 19.461,15.036L20.576,16.31C21.368,17.215 " +
                "20.726,18.632 19.523,18.633H4.426C3.223,18.632 2.58,17.216 3.373,16.31L4.49,15.034" +
                "C5.083,14.356 5.431,13.498 5.475,12.599L5.635,9.404C5.65,6.488 7.634,4.038 " +
                "10.326,3.314C10.392,2.993 10.551,2.695 10.786,2.46C11.102,2.144 11.53,1.967 " +
                "11.976,1.967Z",
            accent = "M17.977,5.033m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0",
        )
    }

    /** `Text input`: an open book whose edit bubble is the accent layer. */
    val TextInput: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.TextInput",
            main = "M2.003,7.9C2.003,7.473 2.379,7.149 2.802,7.206C4.041,7.371 5.463,7.649 " +
                "7.003,8.111C8.757,8.638 10.248,9.28 11.453,9.891V22.891C10.248,22.28 " +
                "8.757,21.638 7.003,21.111C5.386,20.625 3.9,20.344 2.618,20.182" +
                "C2.266,20.137 2.003,19.843 2.003,19.488V7.9ZM21.997,7.9V19.488" +
                "C21.997,19.843 21.734,20.137 21.382,20.182C20.1,20.344 18.614,20.625 " +
                "16.997,21.111C15.243,21.638 13.752,22.28 12.547,22.891V9.891C13.752,9.28 " +
                "15.243,8.638 16.997,8.111C18.537,7.649 19.959,7.371 21.198,7.206" +
                "C21.621,7.149 21.997,7.473 21.997,7.9ZM20.341,14.647C20.29,14.346 19.997,14.141 " +
                "19.707,14.198C18.726,14.366 17.736,14.6 16.766,14.893C16.158,15.073 " +
                "15.515,15.294 14.803,15.57C14.52,15.679 14.38,15.998 14.49,16.282" +
                "C14.574,16.5 14.782,16.632 15.002,16.632C15.069,16.632 15.136,16.621 " +
                "15.202,16.595C15.884,16.331 16.498,16.119 17.081,15.947C18.011,15.667 " +
                "18.957,15.443 19.892,15.282C20.192,15.23 20.393,14.946 20.341,14.647Z" +
                "M20.341,11.308C20.29,11.008 19.997,10.802 19.707,10.859C18.726,11.027 " +
                "17.736,11.262 16.765,11.554C16.148,11.738 15.506,11.959 14.803,12.231" +
                "C14.52,12.341 14.38,12.659 14.49,12.943C14.574,13.161 14.782,13.294 " +
                "15.002,13.294C15.069,13.294 15.136,13.282 15.202,13.257C15.875,12.996 " +
                "16.489,12.784 17.081,12.608C18.011,12.328 18.957,12.104 19.892,11.943" +
                "C20.192,11.891 20.393,11.607 20.341,11.308Z",
            accent = "M14.146,1.109H10.03C8.405,1.109 6.949,2.326 6.864,3.948" +
                "C6.774,5.678 8.15,7.109 9.86,7.109H10.359L11.616,8.639C11.816,8.883 " +
                "12.189,8.883 12.389,8.639L13.647,7.109H13.976C15.6,7.109 17.056,5.893 " +
                "17.141,4.271C17.232,2.541 15.856,1.109 14.146,1.109Z",
        )
    }

    /**
     * `Pickers`: a clock face in the accent layer, a solid rim and two hands on top of it.
     *
     * The face is one circle, the rim a ring knocked out of the same circle by a second, smaller
     * one, and the hands are stroked lines — the only glyph here that needs a stroke to stay thin
     * at the centre, where a filled bar would blob.
     */
    val Pickers: ImageVector by lazy {
        val face = "M12,12m-8.4,0a8.4,8.4 0,1 1,16.8 0a8.4,8.4 0,1 1,-16.8 0"
        val bore = "M12,12m-6.2,0a6.2,6.2 0,1 1,12.4 0a6.2,6.2 0,1 1,-12.4 0"
        builder("Coui17.Category.Pickers").apply {
            addPath(
                pathData = path(face),
                fill = SolidColor(Color.Black),
                fillAlpha = ACCENT_ALPHA,
            )
            addPath(
                pathData = path(face + bore),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black),
            )
            addPath(
                pathData = path("M12,7.5V12.2"),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            )
            addPath(
                pathData = path("M12.4,12H16.2"),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            )
        }.build()
    }

    /** `Overlays & menus`: two stacked panels, the one behind in the accent layer. */
    val Overlays: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Overlays",
            main = "M5.8,8.6H13.6C15.4,8.6 16.8,10 16.8,11.8V18.2C16.8,20 15.4,21.4 13.6,21.4H5.8" +
                "C4,21.4 2.6,20 2.6,18.2V11.8C2.6,10 4,8.6 5.8,8.6Z",
            accent = "M10.4,2.6H18.2C20,2.6 21.4,4 21.4,5.8V10.2C21.4,12 20,13.4 18.2,13.4H10.4" +
                "C8.6,13.4 7.2,12 7.2,10.2V5.8C7.2,4 8.6,2.6 10.4,2.6Z",
        )
    }

    /** `Navigation & layout`: a folded page whose near face is the accent layer. */
    val Navigation: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Navigation",
            main = "M19.728,2.359L12,4.543L4.272,2.359C3.634,2.179 3,2.658 3,3.321V19.789" +
                "C3,20.505 3.476,21.134 4.165,21.329L11.184,23.312C11.717,23.463 12.282,23.463 " +
                "12.816,23.312L19.835,21.329C20.524,21.134 21,20.505 21,19.789V3.321" +
                "C21,2.658 20.366,2.179 19.728,2.359ZM10.317,21.509L5.01,20.009" +
                "C4.708,19.924 4.5,19.649 4.5,19.335V4.908C4.5,4.444 4.944,4.108 5.39,4.234" +
                "L10.698,5.733C10.999,5.819 11.207,6.094 11.207,6.407V20.835" +
                "C11.207,21.299 10.764,21.635 10.317,21.509Z",
            accent = "M5.6,19.033V5.437L10.107,6.71V20.306L5.6,19.033Z",
        )
    }

    /** `Preferences`: three tiles and two sliders, the slider knobs in the accent layer. */
    val Preferences: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Preferences",
            main = "M8.274,13C9.78,13 11,14.22 11,15.726V19.274C11,20.78 9.78,22 8.274,22H4.726" +
                "C3.22,22 2,20.78 2,19.274V15.726C2,14.22 3.22,13 4.726,13H8.274ZM8.274,2" +
                "C9.78,2 11,3.22 11,4.726V8.274C11,9.78 9.78,11 8.274,11H4.726C3.22,11 2,9.78 " +
                "2,8.274V4.726C2,3.22 3.22,2 4.726,2H8.274ZM19.274,2C20.78,2 22,3.22 22,4.726V8.274" +
                "C22,9.78 20.78,11 19.274,11H15.726C14.22,11 13,9.78 13,8.274V4.726" +
                "C13,3.22 14.22,2 15.726,2H19.274Z",
            accent = "M14.996,13.5C15.383,13.5 15.696,13.814 15.696,14.2V17.873" +
                "C16.172,18.125 16.496,18.625 16.496,19.2C16.496,19.775 16.172,20.274 " +
                "15.696,20.525V21.2C15.696,21.587 15.383,21.9 14.996,21.9C14.61,21.9 " +
                "14.296,21.587 14.296,21.2V20.525C13.821,20.274 13.496,19.775 13.496,19.2" +
                "C13.496,18.625 13.82,18.125 14.296,17.873V14.2C14.296,13.814 14.609,13.5 " +
                "14.996,13.5ZM19.996,13.5C20.383,13.5 20.696,13.814 20.696,14.2V14.873" +
                "C21.172,15.125 21.496,15.625 21.496,16.2C21.496,16.776 21.172,17.275 " +
                "20.696,17.526V21.2C20.696,21.587 20.383,21.9 19.996,21.9C19.61,21.9 " +
                "19.296,21.587 19.296,21.2V17.526C18.82,17.275 18.496,16.776 18.496,16.2" +
                "C18.496,15.625 18.82,15.125 19.296,14.873V14.2C19.296,13.814 19.61,13.5 " +
                "19.996,13.5Z",
        )
    }

    /** `Visual effects`: a large four-point sparkle with a small one in the accent layer. */
    val VisualEffects: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.VisualEffects",
            main = "M9.6,6.4C10.11,10.1 13.5,13.49 17.2,14C13.5,14.51 10.11,17.9 9.6,21.6" +
                "C9.09,17.9 5.7,14.51 2,14C5.7,13.49 9.09,10.1 9.6,6.4Z",
            accent = "M18.4,2C18.64,3.76 20.24,5.36 22,5.6C20.24,5.84 18.64,7.44 18.4,9.2" +
                "C18.16,7.44 16.56,5.84 14.8,5.6C16.56,5.36 18.16,3.76 18.4,2Z",
        )
    }

    /** `Misc`: a lab flask whose sparkle is the accent layer. */
    val Misc: ImageVector by lazy {
        dualTone(
            name = "Coui17.Category.Misc",
            main = "M21.263,19.989L15.552,13.531C15.199,13.192 15,12.724 15,12.234V10H16" +
                "C16.552,10 17,9.552 17,9C17,8.448 16.552,8 16,8H8C7.448,8 7,8.448 7,9" +
                "C7,9.552 7.448,10 8,10H9V12.234C9,12.724 8.801,13.192 8.448,13.531L2.737,19.989" +
                "C1.686,21.145 2.506,23 4.069,23H19.931C21.493,23 22.314,21.145 21.263,19.989Z" +
                "M17.276,21H6.723C6.187,21 5.902,20.288 6.257,19.836L8,17.62" +
                "C10.222,15 12.204,20.37 15.653,17.18L17.742,19.836C18.098,20.288 17.813,21 " +
                "17.276,21Z",
            accent = "M12.159,0.111L12.347,0.619C12.782,1.793 13.707,2.718 14.881,3.153L15.389,3.341" +
                "C15.537,3.395 15.537,3.604 15.389,3.659L14.881,3.847C13.707,4.282 12.782,5.207 " +
                "12.347,6.381L12.159,6.889C12.104,7.037 11.896,7.037 11.841,6.889L11.653,6.381" +
                "C11.218,5.207 10.293,4.282 9.119,3.847L8.611,3.659C8.463,3.604 8.463,3.395 " +
                "8.611,3.341L9.119,3.153C10.293,2.718 11.218,1.793 11.653,0.619L11.841,0.111" +
                "C11.896,-0.037 12.104,-0.037 12.159,0.111Z",
        )
    }

    /** A glyph of two filled layers: [main] at full strength, [accent] at [ACCENT_ALPHA]. */
    private fun dualTone(name: String, main: String, accent: String): ImageVector = builder(name).apply {
        addPath(
            pathData = path(main),
            fill = SolidColor(Color.Black),
        )
        addPath(
            pathData = path(accent),
            fill = SolidColor(Color.Black),
            fillAlpha = ACCENT_ALPHA,
        )
    }.build()

    private fun path(pathData: String) = PathParser().parsePathString(pathData).toNodes()

    private fun builder(name: String): ImageVector.Builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24f.dp,
        defaultHeight = 24f.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
}
