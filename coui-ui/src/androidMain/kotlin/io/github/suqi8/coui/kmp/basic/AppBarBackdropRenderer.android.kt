// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "COUIAppBarBlur"

/**
 * `AppBarBlurParams` defaults (`appBarBlurParamsDefault`), read from [AppBarGlassDefaults] so the
 * native and engine paths cannot drift apart: start radius 30px, end radius 0px, speed 3. These are
 * handed to the framework unchanged; it is the framework's own blur that consumes them, so they need
 * no tuning against COUI's engine.
 */
private val NATIVE_START_BLUR_RADIUS_PX: Float get() = AppBarGlassDefaults.GradientBlurRadiusPx
private val NATIVE_END_BLUR_RADIUS_PX: Float get() = AppBarGlassDefaults.GradientEndBlurRadiusPx
private val NATIVE_GRADIENT_BLUR_SPEED: Float get() = AppBarGlassDefaults.GradientBlurSpeed

/**
 * A way to put a gradient blur on the overlay view's RenderNode as a backdrop effect, so HWUI blurs
 * what is already drawn behind the view on the RenderThread in the same frame.
 */
private interface BackdropBlurApi {
    /** Whether the effect depends on the view's height and has to be set again when it changes. */
    val sizeDependent: Boolean

    fun apply(view: View)

    fun clear(view: View)
}

/**
 * The vendor gradient-blur APIs `AppBarBlurHelper.applyGradientBlurRenderEffect` calls, resolved once
 * and invoked reflectively.
 *
 * `COUIMaterialEffect.canApplyBackgroundRenderEffect` gates the call on the OplusView sub-SDK being
 * at least 33.34, which is checked here too: below it the backdrop node is not wired up.
 */
private class OplusGradientBlurApi : BackdropBlurApi {
    private val renderEffect = Class.forName("android.graphics.RenderEffect")
    private val create = Class.forName("com.oplus.graphics.OplusRenderEffect").getMethod(
        "createGradientBlurEffect",
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val set = Class.forName("com.oplus.view.OplusViewBackgroundRenderEffect")
        .getMethod("setBackgroundRenderEffect", renderEffect, View::class.java)

    init {
        val version = Class.forName("com.oplus.os.OplusBuild\$VERSION")
        val major = version.getField("SDK_VERSION").getInt(null)
        val sub = version.getField("SDK_SUB_VERSION").getInt(null)
        check(major > 33 || (major == 33 && sub >= 34)) { "OplusView sub-SDK $major.$sub below 33.34" }
    }

    override val sizeDependent: Boolean get() = false

    override fun apply(view: View) {
        val effect = create.invoke(null, NATIVE_START_BLUR_RADIUS_PX, NATIVE_END_BLUR_RADIUS_PX, true, NATIVE_GRADIENT_BLUR_SPEED)
        set.invoke(null, effect, view)
    }

    override fun clear(view: View) {
        set.invoke(null, null, view)
    }
}

/**
 * `AppBarBlurOverlayView`: an empty view whose background is the alpha ramp and whose RenderNode
 * carries the gradient-blur backdrop effect. The effect is set once, when the view is first
 * attached; afterwards only [setAlpha] and the visibility change.
 */
private class AppBarBlurOverlayView(context: Context, private val api: BackdropBlurApi) : View(context) {
    private var effectApplied = false
    private var rampColor = 0
    private var rampDark: Boolean? = null

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
        alpha = 0f
        visibility = INVISIBLE
    }

    fun setRamp(color: Int, dark: Boolean) {
        if (color == rampColor && dark == rampDark) return
        rampColor = color
        rampDark = dark
        background = if (dark) {
            GradientBackgroundDrawable(
                color,
                floatArrayOf(AppBarGlassDefaults.GradientPos0, AppBarGlassDefaults.GradientPos2),
                floatArrayOf(AppBarGlassDefaults.GradientAlpha0, AppBarGlassDefaults.GradientAlphaDark),
            )
        } else {
            GradientBackgroundDrawable(
                color,
                floatArrayOf(AppBarGlassDefaults.GradientPos0, AppBarGlassDefaults.GradientPos1, AppBarGlassDefaults.GradientPos2),
                floatArrayOf(AppBarGlassDefaults.GradientAlpha0, AppBarGlassDefaults.GradientAlpha1, AppBarGlassDefaults.GradientAlpha2),
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!effectApplied) applyEffect()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (api.sizeDependent && h != oldh) applyEffect()
    }

    private fun applyEffect() {
        runCatching { api.apply(this) }
            .onSuccess { effectApplied = true }
            .onFailure { Log.w(TAG, "Native gradient blur could not be applied", it) }
    }

    fun release() {
        if (effectApplied) runCatching { api.clear(this) }
        effectApplied = false
    }
}

/**
 * `GradientBackgroundDrawable` (`com.coui.appcompat.toolbar.GradientBackgroundDrawable`): the
 * overlay's alpha ramp, measured from the bottom edge up.
 *
 * The native class builds one `LinearGradient(0, height, 0, 0, colors, positions, CLAMP)` and fills
 * the bounds with it, so the alpha is **linear** between the stops. A monotone cubic through the
 * same stops — the earlier port of this class — overshoots the middle of the ramp: on the reference
 * device the native ramp at 40% up from the bottom edge is 0.70 while a cubic through
 * (0, 0) / (0.4, 0.7) / (1, 1) reads 0.85, and the device measurement fits the linear model to
 * within 0.02 everywhere. The native class also carries no dither, so neither does this one.
 */
private class GradientBackgroundDrawable(
    private val baseColor: Int,
    private val positions: FloatArray,
    private val alphas: FloatArray,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradient: LinearGradient? = null
    private var gradientHeight = 0

    private fun applyAlphaToColor(alpha: Float): Int = (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255) shl 24 or (baseColor and 0xFFFFFF)

    private fun updateGradient(height: Int) {
        val colors = IntArray(alphas.size) { applyAlphaToColor(alphas[it]) }
        val shader = LinearGradient(
            0f,
            height.toFloat(),
            0f,
            0f,
            colors,
            positions,
            Shader.TileMode.CLAMP,
        )
        gradient = shader
        gradientHeight = height
        paint.shader = shader
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (!bounds.isEmpty) updateGradient(bounds.height())
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        if (gradient == null || gradientHeight != bounds.height()) updateGradient(bounds.height())
        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        val value = alpha.coerceIn(0, 255)
        if (paint.alpha == value) return
        paint.alpha = value
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

@Composable
internal actual fun rememberPlatformAppBarBackdropRenderer(): AppBarBackdropRenderer? = remember {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        null
    } else {
        val api = runCatching<BackdropBlurApi> { OplusGradientBlurApi() }
            .onFailure { Log.d(TAG, "Native gradient blur unavailable", it) }
            .getOrNull()
        api?.let(::AndroidAppBarBackdropRenderer)
    }
}

private class AndroidAppBarBackdropRenderer(private val api: BackdropBlurApi) : AppBarBackdropRenderer {
    @Composable
    override fun Material(containerColor: Color, dark: Boolean, alpha: () -> Float, modifier: Modifier) {
        val color = containerColor.toArgb()
        // The overlay is decorative and reaches past the bar over the list, so it must not take the
        // touches that start in that band.
        TouchTransparentInterop(modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { AppBarBlurOverlayView(it, api) },
                onRelease = { it.release() },
                update = { view ->
                    view.setRamp(color, dark)
                    // `AppBarBlurOverlayBehavior.applyOverlayBlurProperties`: only the alpha moves,
                    // and the view goes INVISIBLE at exactly 0. The update block re-runs when the
                    // state read here changes, without recomposing the bar.
                    val value = alpha().coerceIn(0f, 1f)
                    view.alpha = value
                    view.visibility = if (value > 0f) View.VISIBLE else View.INVISIBLE
                },
            )
        }
    }
}
