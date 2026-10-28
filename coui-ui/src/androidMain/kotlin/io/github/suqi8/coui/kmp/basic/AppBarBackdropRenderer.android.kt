// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.math.abs
import kotlin.math.floor

private const val TAG = "COUIAppBarBlur"

/**
 * `AppBarBlurParams` defaults (`appBarBlurParamsDefault`): start radius 30px, end radius 0px,
 * speed 3. These are handed to the framework unchanged; it is the framework's own blur that
 * consumes them, so they need no tuning against COUI's engine.
 */
private const val NATIVE_START_BLUR_RADIUS_PX = 30f
private const val NATIVE_END_BLUR_RADIUS_PX = 0f
private const val NATIVE_GRADIENT_BLUR_SPEED = 3f

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
 * AOSP's own backdrop hook, `View.setBackdropRenderEffect` (Android 16), which is the same
 * `RenderNode` backdrop the vendor call ends in. AOSP has no gradient blur, so the vendor effect is
 * rebuilt from public RenderEffects: [PLATFORM_GRADIENT_BLUR_LEVELS] full-resolution Gaussians, each
 * weighted by a tent around its radius on the native radius curve `start × (1 − (y / h)^speed)`, and
 * summed. Where the weights add up to less than one the sharp content below shows through, which is
 * the radius-0 end of the curve.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private class PlatformGradientBlurApi : BackdropBlurApi {
    // The setter is @hide and on the hidden-API blocklist, so plain reflection cannot see it.
    // HiddenApiBypass looks up this one method without lifting the restriction for the process.
    private val set = HiddenApiBypass.getDeclaredMethod(View::class.java, "setBackdropRenderEffect", RenderEffect::class.java)

    private val step = NATIVE_START_BLUR_RADIUS_PX / PLATFORM_GRADIENT_BLUR_LEVELS

    // Compiled once; a resize only updates the height uniform and re-wraps the effect.
    private val weights = Array(PLATFORM_GRADIENT_BLUR_LEVELS) { i ->
        RuntimeShader(PLATFORM_GRADIENT_BLUR_WEIGHT_SHADER).apply {
            setFloatUniform("level", step * (i + 1))
            setFloatUniform("spacing", step)
            setFloatUniform("startRadius", NATIVE_START_BLUR_RADIUS_PX)
            setFloatUniform("speed", NATIVE_GRADIENT_BLUR_SPEED)
        }
    }
    private val blurs = Array(PLATFORM_GRADIENT_BLUR_LEVELS) { i ->
        val radius = step * (i + 1)
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    }

    override val sizeDependent: Boolean get() = true

    override fun apply(view: View) {
        val height = view.height
        if (height <= 0) return
        var effect: RenderEffect? = null
        for (i in 0 until PLATFORM_GRADIENT_BLUR_LEVELS) {
            weights[i].setFloatUniform("height", height.toFloat())
            val level = RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(weights[i], "content"), blurs[i])
            effect = effect?.let { RenderEffect.createBlendModeEffect(it, level, BlendMode.PLUS) } ?: level
        }
        set.invoke(view, effect)
    }

    override fun clear(view: View) {
        set.invoke(view, null)
    }
}

private const val PLATFORM_GRADIENT_BLUR_LEVELS = 3

private const val PLATFORM_GRADIENT_BLUR_WEIGHT_SHADER = """
uniform shader content;
uniform float height;
uniform float level;
uniform float spacing;
uniform float startRadius;
uniform float speed;

half4 main(float2 coord) {
    float t = clamp(coord.y / height, 0.0, 1.0);
    float radius = startRadius * (1.0 - pow(t, speed));
    float weight = max(0.0, 1.0 - abs(radius - level) / spacing);
    return content.eval(coord) * half(weight);
}
"""

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
 * `GradientBackgroundDrawable`: the overlay's alpha ramp, measured from the bottom edge up, with a
 * monotone cubic between the stops and a 16-column ordered dither so the long, shallow fade does not
 * band. The ramp is baked into a 16px-wide bitmap per height and tiled horizontally.
 */
private class GradientBackgroundDrawable(
    private val baseColor: Int,
    private val positions: FloatArray,
    private val alphas: FloatArray,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private val slopes = monotoneSlopes()
    private var gradient: BitmapShader? = null
    private var gradientHeight = 0

    private fun monotoneSlopes(): FloatArray {
        if (positions.size <= 1 || positions.size != alphas.size) return FloatArray(alphas.size)
        val n = positions.size - 1
        val intervals = FloatArray(n) { positions[it + 1] - positions[it] }
        val secants = FloatArray(n) { if (intervals[it] > 0f) (alphas[it + 1] - alphas[it]) / intervals[it] else 0f }
        if (positions.size == 2) return floatArrayOf(secants[0], secants[0])
        val result = FloatArray(positions.size)
        result[0] = endpointSlope(intervals[0], intervals[1], secants[0], secants[1])
        for (i in 1 until positions.lastIndex) {
            result[i] = interiorSlope(intervals[i - 1], intervals[i], secants[i - 1], secants[i])
        }
        result[result.lastIndex] = endpointSlope(intervals[n - 1], intervals[n - 2], secants[n - 1], secants[n - 2])
        return result
    }

    private fun endpointSlope(interval: Float, adjacent: Float, secant: Float, adjacentSecant: Float): Float {
        val slope = ((2f * interval + adjacent) * secant - interval * adjacentSecant) / (interval + adjacent)
        if (slope * secant <= 0f) return 0f
        if (adjacentSecant * secant < 0f && abs(slope) > abs(3f * secant)) return 3f * secant
        return slope
    }

    private fun interiorSlope(previous: Float, current: Float, previousSecant: Float, currentSecant: Float): Float {
        if (previousSecant * currentSecant <= 0f) return 0f
        val w1 = 2f * current + previous
        val w2 = 2f * previous + current
        return (w1 + w2) / (w2 / currentSecant + w1 / previousSecant)
    }

    private fun alphaAt(position: Float): Float {
        if (position <= positions.first()) return alphas.first().coerceIn(0f, 1f)
        for (i in 1 until positions.size) {
            if (position <= positions[i]) {
                val range = positions[i] - positions[i - 1]
                if (range <= 0f) return alphas[i].coerceIn(0f, 1f)
                val t = (position - positions[i - 1]) / range
                val t2 = t * t
                val t3 = t2 * t
                val value = (2f * t3 - 3f * t2 + 1f) * alphas[i - 1] +
                    (t3 - 2f * t2 + t) * range * slopes[i - 1] +
                    (-2f * t3 + 3f * t2) * alphas[i] +
                    (t3 - t2) * range * slopes[i]
                return value.coerceIn(0f, 1f)
            }
        }
        return alphas.last().coerceIn(0f, 1f)
    }

    private fun updateGradient(bounds: Rect) {
        val height = bounds.height()
        if (gradient == null || gradientHeight != height) {
            val pixels = IntArray(height * 16)
            for (y in 0 until height) {
                val level = alphaAt(1f - (y + 0.5f) / height) * 255f / 16f
                val floorLevel = floor(level)
                val low = floorLevel.toInt() * 16
                val fraction = level - floorLevel
                for (x in 0 until 16) {
                    val threshold = (((x * 5) xor (y * 3)) and 15) + 0.5f
                    val a = if (fraction > threshold / 16f) minOf(low + 16, 255) else low
                    pixels[y * 16 + x] = (baseColor and 0xFFFFFF) or (a.coerceIn(0, 255) shl 24)
                }
            }
            val shader = BitmapShader(
                Bitmap.createBitmap(pixels, 16, height, Bitmap.Config.ARGB_8888),
                Shader.TileMode.REPEAT,
                Shader.TileMode.CLAMP,
            )
            gradient = shader
            gradientHeight = height
            paint.shader = shader
        }
        shaderMatrix.setTranslate(bounds.left.toFloat(), bounds.top.toFloat())
        gradient?.setLocalMatrix(shaderMatrix)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (!bounds.isEmpty) updateGradient(bounds)
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        if (gradient == null) updateGradient(bounds)
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
            .recoverCatching {
                check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) { "No backdrop RenderEffect before Android 16" }
                PlatformGradientBlurApi()
            }
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
