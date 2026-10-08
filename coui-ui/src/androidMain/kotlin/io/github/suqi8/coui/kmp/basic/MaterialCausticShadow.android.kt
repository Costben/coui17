// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** The vendor material caustic-shadow APIs, resolved once and invoked reflectively. */
private class CausticApi {
    private val util = Class.forName("com.oplus.view.material.OplusMaterialUtil")
    private val oplusView = Class.forName("com.oplus.view.OplusView")
    private val wrapper = oplusView.getConstructor(View::class.java)
    private val geometry = oplusView.getMethod(
        "setOverrideLightSourceGeometry",
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val color = util.getMethod("setOutlineCausticShadowColor", View::class.java, Int::class.javaPrimitiveType)
    private val readColor = util.getMethod("getOutlineCausticShadowColor", View::class.java)
    private val layout = util.getMethod(
        "setOutlineCausticShadowLayout",
        View::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
    )
    private val params = util.getMethod(
        "setOutlineCausticShadowParams",
        View::class.java,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )

    fun update(view: View, profile: CausticShadowProfile, density: Float, light: Color) {
        val upper = profile == CausticShadowProfile.Toolbar
        // Upper / down caustic shadow. Geometry enables hwui's shadow draw path.
        val fade = (light.alpha / (if (upper) 51f / 255f else 66f / 255f)).coerceIn(0f, 1f)
        val shadow = android.graphics.Color.argb(((if (upper) 30 else 45) * fade).roundToInt(), 0, 0, 0)
        // outlineAmbientShadowColor / outlineSpotShadowColor were added in API 28. The native
        // caustic API this renderer talks to is vendor-specific in practice, but the module declares
        // minSdk 23, so the assignment has to be guarded rather than left to throw NoSuchMethodError.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.outlineAmbientShadowColor = shadow
            view.outlineSpotShadowColor = shadow
        }
        geometry.invoke(
            wrapper.newInstance(view),
            -1f,
            ((if (upper) -160f else -320f) * density).roundToInt().toFloat(),
            0f,
            ((if (upper) 800f else 1600f) * density + 0.5f).toInt().toFloat(),
            0f,
        )
        color.invoke(null, view, light.toArgb())
        val bottom = profile.pixels(profile.bottomOffset.value * density).toInt()
        val width = profile.pixels(profile.sdfWidth.value * density)
        layout.invoke(null, view, 0, 0, 0, bottom)
        params.invoke(null, view, width, 0.6f, 0.6f, 18f, 0.45f, 0.6f)
        check(readColor.invoke(null, view) == light.toArgb()) { "Native caustic color did not persist" }
        // Enable shadow casting only after geometry and material parameters are ready.
        view.elevation = ((if (upper) 16f else 24f) * density + 0.5f).toInt().toFloat()
    }

    fun clear(view: View) {
        color.invoke(null, view, 0)
        view.elevation = 0f
    }
}

private class CausticOutlineView(context: Context) : View(context) {
    var radius = 0f
    init {
        // Keep a display list even with no body: the body is drawn by Compose above this sibling.
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (view.width <= 0 || view.height <= 0) {
                    outline.setEmpty()
                } else {
                    outline.setRoundRect(0, 0, view.width, view.height, radius.coerceIn(0f, minOf(view.width, view.height) / 2f))
                    outline.alpha = 1f
                }
            }
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}

private class CausticHost(context: Context) : FrameLayout(context) {
    init {
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
    var reported = false
}

@Composable
internal actual fun rememberPlatformCausticShadowRenderer(): CausticShadowRenderer? = remember {
    runCatching { AndroidCausticRenderer(CausticApi()) }.onFailure {
        Log.d("COUICausticShadow", "Native caustic API unavailable", it)
    }.getOrNull()
}

private class AndroidCausticRenderer(val api: CausticApi) : CausticShadowRenderer {
    @Composable
    override fun Draw(particles: List<CausticShadowParticle>, color: Color, modifier: Modifier, fallback: @Composable () -> Unit) {
        var failed by remember { mutableStateOf(false) }
        if (failed) {
            fallback()
            return
        }
        val density = LocalDensity.current
        AndroidView(
            modifier = modifier,
            factory = { CausticHost(it) },
            onRelease = { host ->
                for (i in 0 until host.childCount) runCatching { api.clear(host.getChildAt(i)) }
            },
            update = { host ->
                runCatching {
                    while (host.childCount > particles.size) {
                        val view = host.getChildAt(host.childCount - 1)
                        api.clear(view)
                        host.removeView(view)
                    }
                    particles.forEachIndexed { i, (particle, profile) ->
                        val view = if (i < host.childCount) {
                            host.getChildAt(i) as CausticOutlineView
                        } else {
                            CausticOutlineView(host.context).also { host.addView(it) }
                        }
                        val width = with(density) { particle.width.roundToPx() }.coerceAtLeast(1)
                        val height = with(density) { particle.height.roundToPx() }.coerceAtLeast(1)
                        if (view.layoutParams.width != width || view.layoutParams.height != height) {
                            view.layoutParams = FrameLayout.LayoutParams(width, height)
                        }
                        view.radius = with(density) { particle.cornerRadius.toPx() }
                        // LayoutParams alone leaves new views at 0x0 until the next traversal.
                        // Synchronize the native caster before enabling caustics, including resizes.
                        if (view.width != width || view.height != height) {
                            view.layout(0, 0, width, height)
                        }
                        view.invalidateOutline()
                        view.pivotX = width / 2f
                        view.pivotY = height / 2f
                        view.translationX = with(density) { particle.centerX.toPx() } - width / 2f
                        view.translationY = with(density) { particle.centerY.toPx() } - height / 2f
                        view.scaleX = particle.scaleX
                        view.scaleY = particle.scaleY
                        // Fade the native light color rather than View.alpha: the system reads this
                        // color in its destination-dependent shader, avoiding an offscreen group clip.
                        api.update(view, profile, density.density, color.copy(alpha = color.alpha * particle.alpha))
                    }
                    if (!host.reported && particles.isNotEmpty()) {
                        Log.d("COUICausticShadow", "native color readback verified; particles=${particles.size}; factors=0.6,0.6,18,0.45,0.6")
                        host.reported = true
                    }
                }.onFailure {
                    for (i in 0 until host.childCount) runCatching { api.clear(host.getChildAt(i)) }
                    Log.w("COUICausticShadow", "Native caustic initialization failed", it)
                    failed = true
                }
            },
        )
    }
}
