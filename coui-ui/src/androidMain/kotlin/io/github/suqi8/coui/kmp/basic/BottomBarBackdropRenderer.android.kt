// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.database.ContentObserver
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.ArraySet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.math.roundToInt

private const val TAG = "COUIBottomBarBlur"

/**
 * Native bottom-bar material params (from COUIMaterialBlurEffect.TYPE_FRAMEWORK_BOTTOM_BAR):
 * 25px blur sigma.
 * Light theme:
 *   Layer 1: #B3666666 with BlendMode.COLOR_DODGE
 *   Layer 2: #99F5F5F5 with BlendMode.LUMINOSITY
 * Dark theme:
 *   Layer 1: #80666666 with BlendMode.OVERLAY
 *   Layer 2: #B3262626 with BlendMode.LUMINOSITY
 */
private const val NATIVE_BLUR_RADIUS_PX = 25f
private const val LIGHT_COLOR_1 = 0xB3666666.toInt()
private val LIGHT_MODE_1 = BlendMode.COLOR_DODGE
private const val LIGHT_COLOR_2 = 0x99F5F5F5.toInt()
private val LIGHT_MODE_2 = BlendMode.LUMINOSITY

private const val DARK_COLOR_1 = 0x80666666.toInt()
private val DARK_MODE_1 = BlendMode.OVERLAY
private const val DARK_COLOR_2 = 0xB3262626.toInt()
private val DARK_MODE_2 = BlendMode.LUMINOSITY

/**
 * `COUIMaterialStrokeEffect.FRAMEWORK_CAPSULE_PARAMS_3_LIGHT` / `_DARK`:
 * `strokeType=1, edgeAlpha=0.25/0.8, edgeWidthDp=2.5, edgeAngle=0,
 * shadowFadeIn=0.1/0.03, shadowFadeOut=1, shadowFadeScale=1`.
 *
 * Both the menu capsule (`BaseBottomFloatingMenuView:578`) and the accessory ball
 * (`COUIBottomFloatingAccessoryView:692`) request `TYPE_FRAMEWORK_CAPSULE_3`, so both silhouettes
 * carry this stroke type. The `2` stroke type belongs to the `FRAMEWORK_CIRCLE_*` family, which
 * neither view uses.
 */
private const val STROKE_TYPE_CAPSULE = 1
private const val EDGE_WIDTH_DP = 2.5f
private const val EDGE_ALPHA_LIGHT = 0.25f
private const val EDGE_ALPHA_DARK = 0.8f
private const val SHADOW_FADE_IN_LIGHT = 0.1f
private const val SHADOW_FADE_IN_DARK = 0.03f
private const val SHADOW_FADE_OUT = 1.0f
private const val SHADOW_FADE_SCALE = 1.0f

/** One `COUIMaterialStrokeEffect.Companion.StrokeEffectParams` row, resolved for a theme. */
private class StrokeParams(
    val type: Int,
    val edgeAlpha: Float,
    val edgeWidthDp: Float,
    val edgeAngle: Float,
    val fadeIn: Float,
    val fadeOut: Float,
    val fadeScale: Float,
)

/**
 * `COUIMaterialStrokeEffect.NO_EFFECT`: what `updateStrokeEffect` applies when the stroke switch is
 * off. The `_DISABLED_` rows of the same family are never selected by either bottom bar — they exist
 * only in the enum table — so "off" really means no edge and no fade band.
 */
private val NoStroke = StrokeParams(type = 0, 0f, 0f, 0f, 0f, 0f, 0f)

/** `FRAMEWORK_CAPSULE_PARAMS_3_LIGHT` / `_DARK` as one row. */
private fun strokeParams(isDark: Boolean) = StrokeParams(
    type = STROKE_TYPE_CAPSULE,
    edgeAlpha = if (isDark) EDGE_ALPHA_DARK else EDGE_ALPHA_LIGHT,
    edgeWidthDp = EDGE_WIDTH_DP,
    edgeAngle = 0f,
    fadeIn = if (isDark) SHADOW_FADE_IN_DARK else SHADOW_FADE_IN_LIGHT,
    fadeOut = SHADOW_FADE_OUT,
    fadeScale = SHADOW_FADE_SCALE,
)

/** `R.color.coui_caustic_color_2` (`#42FFFFFF`), fed to the outline caustic shader. */
private const val CAUSTIC_COLOR = 0x42FFFFFF.toInt()
private const val CAUSTIC_ELEVATION_DP = 24f
private const val CAUSTIC_LIGHT_Y_DP = -320f
private const val CAUSTIC_LIGHT_Z_DP = 0f
private const val CAUSTIC_LIGHT_R_DP = 1600f
private const val CAUSTIC_LIGHT_BLUR_DP = 0f
private const val CAUSTIC_ALPHA = 45

/**
 * `coui_caustic_layout_*_level4` (menu capsule) and `level5` (accessory ball):
 * bottom offset 4.9dp / 11.2dp, SDF width 10.4dp / 20dp, left/top/right all zero.
 */
private const val CAUSTIC_BOTTOM_MENU_DP = 4.9f
private const val CAUSTIC_SDF_MENU_DP = 10.4f
private const val CAUSTIC_BOTTOM_BALL_DP = 11.2f
private const val CAUSTIC_SDF_BALL_DP = 20f
private const val CAUSTIC_FACTOR_0 = 0.6f
private const val CAUSTIC_FACTOR_1 = 0.6f
private const val CAUSTIC_SPEED = 18f
private const val CAUSTIC_LUMINANCE_THRESHOLD = 0.45f
private const val CAUSTIC_LUMINANCE_INTENSITY = 0.6f

/**
 * The non-caustic outer shadow: elevation **level index 2**, which `ShadowUtils.java:72-118` maps to
 * the `coui_shadow_*_level3b` resources — not to `coui_shadow_*_level2`, which the index `1` branch
 * reads. `ShadowUtils.setElevationToView(this, 2, ...)` takes an index, so the names below follow
 * the resource set the index resolves to.
 *
 * `coui_shadow_elevation_level3b` = 350dp, `coui_shadow_color_lv3b` = alpha 40,
 * `coui_shadow_light_y_level3b` = -160dp, `coui_shadow_light_z_level3b` = 10000dp,
 * `coui_shadow_light_r_level3b` = 2000dp, `coui_shadow_blur_r_level3b` = 0dp.
 */
private const val SHADOW_ELEVATION_LEVEL3B_DP = 350f
private const val SHADOW_ALPHA_LEVEL3B = 40
private const val SHADOW_LIGHT_Y_LEVEL3B_DP = -160f
private const val SHADOW_LIGHT_Z_LEVEL3B_DP = 10000f
private const val SHADOW_LIGHT_R_LEVEL3B_DP = 2000f
private const val SHADOW_LIGHT_BLUR_LEVEL3B_DP = 0f

/** `OplusMaterialMetaBallParams` factors from `COUIBottomFloatingAccessoryNavigationBar`. */
private const val MELT_BALL_FACTOR_X = 1.5f
private const val MELT_BALL_FACTOR_Y = 2.0f

private enum class BottomBarShape { Capsule, Circle }

private interface BottomBarBackdropApi {
    val drawsOwnStroke: Boolean
    val drawsOwnShadow: Boolean

    /** `stroke` is `COUIMaterialStateManager.isStrokeEffectEnable()` at the moment of the call. */
    fun apply(view: View, isDark: Boolean, shape: BottomBarShape, stroke: Boolean)
    fun clear(view: View)
}

/**
 * The Oplus material APIs `COUIMaterialEffect` / `COUIMaterialStrokeEffect` invoke: continuous
 * corner clipping, the `TYPE_FRAMEWORK_CAPSULE_3` edge and shadow params, the melt-ball chain and
 * the outline caustic shadow.
 *
 * Every member is resolved in the constructor so a device missing one of them degrades to the
 * AOSP path instead of half-applying the material.
 */
private class OplusMaterialApi {
    private val util = Class.forName("com.oplus.view.material.OplusMaterialUtil")
    private val cornerClass = Class.forName("com.oplus.view.material.OplusMaterialCornerParams")
    private val edgeClass = Class.forName("com.oplus.view.material.OplusMaterialEdgeParams")
    private val shadowClass = Class.forName("com.oplus.view.material.OplusMaterialShadowParams")
    private val metaBallClass = Class.forName("com.oplus.view.material.OplusMaterialMetaBallParams")

    private val cornerCtor = cornerClass.getConstructor(
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val edgeCtor = edgeClass.getConstructor(
        Int::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val shadowCtor = shadowClass.getConstructor(
        Int::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val metaBallCtor = metaBallClass.getConstructor(
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )

    private val setCorner = util.getMethod("setCornerParams", View::class.java, cornerClass)
    private val setEdge = util.getMethod("setEdgeParams", View::class.java, edgeClass)
    private val setShadow = util.getMethod("setShadowParams", View::class.java, shadowClass)
    private val setMetaBall = util.getMethod("setMetaBallParams", View::class.java, metaBallClass)
    private val setMetaBallChain = util.getMethod("setMetaBallChain", Set::class.java)

    private val causticColor =
        util.getMethod("setOutlineCausticShadowColor", View::class.java, Int::class.javaPrimitiveType)
    private val causticLayout = util.getMethod(
        "setOutlineCausticShadowLayout",
        View::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
    )
    private val causticParams = util.getMethod(
        "setOutlineCausticShadowParams",
        View::class.java,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )

    private val oplusViewClass = Class.forName("com.oplus.view.OplusView")
    private val oplusViewCtor = oplusViewClass.getConstructor(View::class.java)
    private val lightGeometry = oplusViewClass.getMethod(
        "setOverrideLightSourceGeometry",
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val setClipExpandSize = oplusViewClass.getMethod(
        "setClipExpandSize",
        View::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
    )

    /**
     * `COUIEffectVersionUtil.isCausticShadowVersion()`: the outline caustic needs `OplusView`
     * sub-SDK 40.12, and `updateOuterShadow` falls back to the level3b spot shadow without it.
     */
    val causticShadowAvailable: Boolean = runCatching {
        val version = Class.forName("com.oplus.os.OplusBuild\$VERSION")
        val major = version.getField("SDK_VERSION").getInt(null)
        val sub = version.getField("SDK_SUB_VERSION").getInt(null)
        major > CAUSTIC_SDK || (major == CAUSTIC_SDK && sub >= CAUSTIC_SUB_SDK)
    }.getOrDefault(false)

    /** Continuous-corner clip, `TYPE_FRAMEWORK_CAPSULE_3` edge, and its fade band. */
    fun applyShape(view: View, isDark: Boolean, shape: BottomBarShape, density: Float, stroke: Boolean) {
        val radius = view.height / 2f
        // `BaseBottomFloatingMenuView` uses the 1.0 weight, `COUIBottomFloatingAccessoryView` the
        // 2.0 smooth-corner factor for its round body.
        val smooth = if (shape == BottomBarShape.Circle) 2.0f else 1.0f
        setCorner.invoke(null, view, cornerCtor.newInstance(radius, smooth))
        val params = if (stroke) strokeParams(isDark) else NoStroke
        setEdge.invoke(
            null,
            view,
            edgeCtor.newInstance(
                params.type,
                params.edgeWidthDp * density,
                params.edgeAlpha,
                params.edgeAngle,
            ),
        )
        setShadow.invoke(
            null,
            view,
            shadowCtor.newInstance(params.type, params.fadeScale, params.fadeIn, params.fadeOut),
        )
        view.clipToOutline = false
    }

    /** `ShadowUtils.setElevationToView(..., 7)` plus the level 4/5 caustic layout. */
    fun applyCausticShadow(view: View, shape: BottomBarShape, density: Float) {
        val alpha = CAUSTIC_ALPHA
        val shadowColor = Color.argb(alpha, 0, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.outlineAmbientShadowColor = shadowColor
            view.outlineSpotShadowColor = shadowColor
        }
        lightGeometry.invoke(
            oplusViewCtor.newInstance(view),
            -1f,
            CAUSTIC_LIGHT_Y_DP * density,
            CAUSTIC_LIGHT_Z_DP * density,
            CAUSTIC_LIGHT_R_DP * density,
            CAUSTIC_LIGHT_BLUR_DP * density,
        )
        causticColor.invoke(null, view, CAUSTIC_COLOR)
        causticLayout.invoke(
            null,
            view,
            0,
            0,
            0,
            ((if (shape == BottomBarShape.Circle) CAUSTIC_BOTTOM_BALL_DP else CAUSTIC_BOTTOM_MENU_DP) * density)
                .roundToInt(),
        )
        causticParams.invoke(
            null,
            view,
            (if (shape == BottomBarShape.Circle) CAUSTIC_SDF_BALL_DP else CAUSTIC_SDF_MENU_DP) * density,
            CAUSTIC_FACTOR_0,
            CAUSTIC_FACTOR_1,
            CAUSTIC_SPEED,
            CAUSTIC_LUMINANCE_THRESHOLD,
            CAUSTIC_LUMINANCE_INTENSITY,
        )
        view.elevation = CAUSTIC_ELEVATION_DP * density
    }

    /**
     * The non-caustic `SHADOW_LV3_B` path: elevation index 2 (the `level3b` resource set) with the
     * system light geometry.
     */
    fun applySpotShadow(view: View, density: Float) {
        val shadowColor = Color.argb(SHADOW_ALPHA_LEVEL3B, 0, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.outlineAmbientShadowColor = shadowColor
            view.outlineSpotShadowColor = shadowColor
        }
        lightGeometry.invoke(
            oplusViewCtor.newInstance(view),
            -1f,
            SHADOW_LIGHT_Y_LEVEL3B_DP * density,
            SHADOW_LIGHT_Z_LEVEL3B_DP * density,
            SHADOW_LIGHT_R_LEVEL3B_DP * density,
            SHADOW_LIGHT_BLUR_LEVEL3B_DP * density,
        )
        view.elevation = SHADOW_ELEVATION_LEVEL3B_DP * density
    }

    fun setMetaBallParams(view: View, kind: Int, extensionPx: Int): Boolean = setMetaBall.invoke(
        null,
        view,
        metaBallCtor.newInstance(kind, 0, extensionPx, MELT_BALL_FACTOR_X, MELT_BALL_FACTOR_Y),
    ) as Boolean

    fun clearMetaBallParams(view: View) {
        setMetaBall.invoke(null, view, null)
    }

    fun setChain(views: Set<View>): Boolean = setMetaBallChain.invoke(null, views) as Boolean

    fun expandClip(host: View) {
        // `UIUtil.setViewClipExpandSize(this, 0, 120, 0, 0)` in the native ViewGroup.
        setClipExpandSize.invoke(null, host, 0, 120, 0, 0)
    }
}

private class OplusBottomBarBackdropApi : BottomBarBackdropApi {
    private val renderEffect = Class.forName("android.graphics.RenderEffect")
    private val setBackgroundRenderEffect = Class.forName("com.oplus.view.OplusViewBackgroundRenderEffect")
        .getMethod("setBackgroundRenderEffect", renderEffect, View::class.java)
    private val material = OplusMaterialApi()

    init {
        val version = Class.forName("com.oplus.os.OplusBuild\$VERSION")
        val major = version.getField("SDK_VERSION").getInt(null)
        val sub = version.getField("SDK_SUB_VERSION").getInt(null)
        check(major > 33 || (major == 33 && sub >= 34)) { "OplusView sub-SDK $major.$sub below 33.34" }
    }

    override val drawsOwnStroke: Boolean get() = true
    override val drawsOwnShadow: Boolean get() = true

    override fun apply(view: View, isDark: Boolean, shape: BottomBarShape, stroke: Boolean) {
        val color1 = if (isDark) DARK_COLOR_1 else LIGHT_COLOR_1
        val mode1 = if (isDark) DARK_MODE_1 else LIGHT_MODE_1
        val color2 = if (isDark) DARK_COLOR_2 else LIGHT_COLOR_2
        val mode2 = if (isDark) DARK_MODE_2 else LIGHT_MODE_2

        val blur = RenderEffect.createBlurEffect(
            NATIVE_BLUR_RADIUS_PX,
            NATIVE_BLUR_RADIUS_PX,
            Shader.TileMode.MIRROR,
        )
        val filter1 = RenderEffect.createColorFilterEffect(BlendModeColorFilter(color1, mode1), blur)
        val filter2 = RenderEffect.createColorFilterEffect(BlendModeColorFilter(color2, mode2), filter1)

        setBackgroundRenderEffect.invoke(null, filter2, view)

        val density = view.resources.displayMetrics.density
        val applied = runCatching { material.applyShape(view, isDark, shape, density, stroke) }
            .onFailure { Log.w(TAG, "Oplus material shape setup failed", it) }
            .isSuccess
        if (!applied) {
            // Keep the capsule clipped the way the pre-material AOSP path does.
            view.clipToOutline = true
        }
        // `updateOuterShadow` picks the caustic only while the stroke is on and the build has the
        // caustic shader; every other case is the level3b spot shadow.
        if (stroke && material.causticShadowAvailable) {
            runCatching { material.applyCausticShadow(view, shape, density) }
                .onFailure {
                    Log.w(TAG, "Oplus caustic shadow failed, using the level3b spot shadow", it)
                    runCatching { material.applySpotShadow(view, density) }
                        .onFailure { error -> Log.w(TAG, "Oplus level3b shadow failed", error) }
                }
        } else {
            runCatching { material.applySpotShadow(view, density) }
                .onFailure { error -> Log.w(TAG, "Oplus level3b shadow failed", error) }
        }
    }

    override fun clear(view: View) {
        setBackgroundRenderEffect.invoke(null, null, view)
        view.elevation = 0f
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private class PlatformBottomBarBackdropApi : BottomBarBackdropApi {
    private val setBackdropRenderEffect = HiddenApiBypass.getDeclaredMethod(
        View::class.java,
        "setBackdropRenderEffect",
        RenderEffect::class.java,
    )

    /** No vendor stroke: the Compose rim stays in place on an AOSP build. */
    override val drawsOwnStroke: Boolean get() = false

    /** No vendor shadow: the Compose drop shadow stays in place. */
    override val drawsOwnShadow: Boolean get() = false

    override fun apply(view: View, isDark: Boolean, shape: BottomBarShape, stroke: Boolean) {
        val color1 = if (isDark) DARK_COLOR_1 else LIGHT_COLOR_1
        val mode1 = if (isDark) DARK_MODE_1 else LIGHT_MODE_1
        val color2 = if (isDark) DARK_COLOR_2 else LIGHT_COLOR_2
        val mode2 = if (isDark) DARK_MODE_2 else LIGHT_MODE_2

        val blur = RenderEffect.createBlurEffect(
            NATIVE_BLUR_RADIUS_PX,
            NATIVE_BLUR_RADIUS_PX,
            Shader.TileMode.CLAMP,
        )
        val filter1 = RenderEffect.createColorFilterEffect(BlendModeColorFilter(color1, mode1), blur)
        val filter2 = RenderEffect.createColorFilterEffect(BlendModeColorFilter(color2, mode2), filter1)

        view.clipToOutline = true
        setBackdropRenderEffect.invoke(view, filter2)
    }

    override fun clear(view: View) {
        setBackdropRenderEffect.invoke(view, null)
    }
}

private val EmptyOutline = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setEmpty()
    }
}

private class BottomBarMaterialView(
    context: Context,
    private val api: BottomBarBackdropApi,
    private val shape: BottomBarShape,
) : View(context) {
    private var isDark: Boolean? = null
    private var stroke: Boolean = true
    private var effectApplied = false

    /** Melt-ball type: 0 when the view is not chained, 1 for the capsule, 2 for an accessory. */
    var meltKind: Int = 0

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
        background = ColorDrawable(0)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (view.width <= 0 || view.height <= 0) {
                    outline.setEmpty()
                    return
                }
                if (shape == BottomBarShape.Circle) {
                    outline.setOval(0, 0, view.width, view.height)
                } else {
                    outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
                }
            }
        }
    }

    fun setMode(dark: Boolean, strokeEnabled: Boolean) {
        if (isDark == dark && stroke == strokeEnabled && effectApplied) return
        isDark = dark
        stroke = strokeEnabled
        if (isAttachedToWindow && width > 0 && height > 0) {
            applyEffect()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!effectApplied && width > 0 && height > 0) {
            applyEffect()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // The corner radius follows the measured height, exactly like the native views.
            effectApplied = false
            applyEffect()
        }
    }

    private fun applyEffect() {
        val dark = isDark ?: return
        runCatching { api.apply(this, dark, shape, stroke) }
            .onSuccess { effectApplied = true }
            .onFailure { Log.w(TAG, "Native bottom bar material could not be applied", it) }
    }

    fun release() {
        if (effectApplied) runCatching { api.clear(this) }
        effectApplied = false
    }
}

/**
 * The three sibling views `COUIBottomFloatingAccessoryNavigationBar` keeps under one parent
 * (`clipChildren = false`, `clipToPadding = false`, a 120px top clip expansion) so that the
 * material can melt their silhouettes together.
 */
private class MeltBallHostView(
    context: Context,
    private val api: BottomBarBackdropApi,
    private val material: OplusMaterialApi?,
) : FrameLayout(context) {
    val startView = BottomBarMaterialView(context, api, BottomBarShape.Circle).apply { meltKind = 2 }
    val endView = BottomBarMaterialView(context, api, BottomBarShape.Circle).apply { meltKind = 2 }
    val menuView = BottomBarMaterialView(context, api, BottomBarShape.Capsule).apply { meltKind = 1 }

    private var chainState: Boolean? = null
    private var chainKey: List<Int>? = null
    private var extensionPx = 0

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        outlineProvider = EmptyOutline
        addView(startView)
        addView(endView)
        addView(menuView)
        runCatching { material?.expandClip(this) }
            .onFailure { Log.d(TAG, "Oplus clip expansion unavailable", it) }
    }

    fun update(geometry: BottomBarMeltBallGeometry, isDark: Boolean, stroke: Boolean) {
        place(startView, geometry.start)
        place(endView, geometry.end)
        place(menuView, geometry.menu)
        startView.setMode(isDark, stroke)
        endView.setMode(isDark, stroke)
        menuView.setMode(isDark, stroke)
        updateChain(geometry)
    }

    private fun place(view: BottomBarMaterialView, child: BottomBarMeltBallChild?) {
        if (child == null || !child.visible || child.widthPx < 1f || child.heightPx < 1f) {
            view.visibility = GONE
            return
        }
        view.visibility = VISIBLE
        val width = child.widthPx.roundToInt().coerceAtLeast(1)
        val height = child.heightPx.roundToInt().coerceAtLeast(1)
        val params = view.layoutParams
        if (params == null || params.width != width || params.height != height) {
            view.layoutParams = FrameLayout.LayoutParams(width, height)
        }
        if (view.width != width || view.height != height) {
            // LayoutParams alone leaves a new view at 0x0 until the next traversal.
            view.layout(0, 0, width, height)
        }
        view.x = child.leftPx
        view.y = child.topPx
        view.pivotX = width / 2f
        view.pivotY = height / 2f
        view.scaleX = child.scaleX
        view.scaleY = child.scaleY
    }

    private fun updateChain(geometry: BottomBarMeltBallGeometry) {
        val material = material ?: return
        val visible = buildList {
            if (startView.visibility == VISIBLE) add(startView)
            if (endView.visibility == VISIBLE) add(endView)
            if (menuView.visibility == VISIBLE) add(menuView)
        }
        val chain = geometry.chainActive && visible.size >= 2
        val key = visible.map { System.identityHashCode(it) }
        if (chainState == chain && extensionPx == geometry.extensionPx && chainKey == key) return
        chainState = chain
        chainKey = key
        extensionPx = geometry.extensionPx
        runCatching {
            if (chain) {
                if (menuView.visibility == VISIBLE) material.setMetaBallParams(menuView, 1, geometry.extensionPx)
                if (startView.visibility == VISIBLE) material.setMetaBallParams(startView, 2, geometry.extensionPx)
                if (endView.visibility == VISIBLE) material.setMetaBallParams(endView, 2, geometry.extensionPx)
                val set = ArraySet<View>(visible.size)
                visible.forEach { set.add(it) }
                material.setChain(set)
            } else {
                material.clearMetaBallParams(startView)
                material.clearMetaBallParams(endView)
                material.clearMetaBallParams(menuView)
            }
        }.onFailure { Log.w(TAG, "Melt ball chain update failed", it) }
    }

    fun release() {
        runCatching {
            material?.let {
                it.clearMetaBallParams(startView)
                it.clearMetaBallParams(endView)
                it.clearMetaBallParams(menuView)
            }
        }
        startView.release()
        endView.release()
        menuView.release()
    }
}

/** `COUIMaterialStateManager.MATERIAL_STROKE`, a `Settings.System` int. */
private const val MATERIAL_STROKE = "system_material_stroke_enable"

/** `COUIMaterialStateManager.SUPER_POWER_SAVE_MODE`, a `Settings.System` int. */
private const val SUPER_POWER_SAVE_MODE = "super_powersave_mode_state"

/** `COUIMaterialStateManager.LOW_POWER_MODE`, a `Settings.Global` int. */
private const val LOW_POWER_MODE = "low_power"

/** `COUIMaterialStateManager.getAnimLevelVersion`: the animation-quality system property. */
private const val ANIM_LEVEL_PROPERTY = "persist.sys.oplus.anim_level"

/** `COUIMaterialStateManager.checkThemeEnable`: the two app booleans the theme gate reads. */
private const val BLUR_ENABLE_BOOL = "coui_blur_enable"
private const val IMMERSIVE_THEME_BOOL = "is_immersive_theme"

/** `COUIEffectVersionUtil.CAUSTIC_SHADOW_MAJOR_VERSION` / `CAUSTIC_SHADOW_SUB_VERSION`. */
private const val CAUSTIC_SDK = 40
private const val CAUSTIC_SUB_SDK = 12

/**
 * The part of `COUIMaterialStateManager` that decides whether the bar may paint the
 * `TYPE_FRAMEWORK_CAPSULE_3` stroke, and whether it casts the caustic shadow or the level3b spot
 * shadow.
 *
 * `updateState` (`COUIMaterialStateManager.java:710-738`) disables the whole material before it
 * reads any switch, then `updateStrokeState` (`:354-396`) reads the stroke switch:
 *
 * 1. `isLowAnimationLevel()` (`:580-587`) — `persist.sys.oplus.anim_level` (`:181-210`) must parse
 *    to 1 or 2. A missing property, a parse failure or any other level disables the material.
 * 2. `checkThemeEnable()` (`:483-488`) — `coui_blur_enable` on and `is_immersive_theme` off.
 * 3. `isLowPowerMode()` (`:589-591`) — `Settings.Global low_power` or
 *    `Settings.System super_powersave_mode_state`.
 * 4. `Settings.System system_material_stroke_enable == 1`.
 *
 * `isCausticShadowEffectEnable` (`:572-574`) additionally needs
 * `COUIEffectVersionUtil.isCausticShadowVersion()` (`:59`, `OplusView` sub-SDK 40.12).
 *
 * The blur switch and both power-saving modes are already read by `coui-blur`'s
 * `SystemBlurState.android.kt`, which is what `isBlurEnabled()` consults, so only the gates that
 * module does not cover are repeated here.
 *
 * `isFeatureEnabled()` — the confidential `com.oplus.appcompat.texture_effect.Enable` app feature —
 * is not consulted: the material APIs resolving at all is taken as the same answer.
 */
private class BottomBarMaterialGate(private val context: Context) {

    /** `isLowAnimationLevel()`: only the `1` and `2` animation levels keep the material. */
    private val lowAnimationLevel: Boolean = runCatching {
        val level = Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, ANIM_LEVEL_PROPERTY) as String
        val parsed = level.trim().toInt()
        parsed < 1 || parsed >= 3
    }.getOrDefault(true)

    /** `checkThemeEnable()`: the app must not have asked for the flat, non-blurring theme. */
    private val themeEnabled: Boolean = runCatching {
        boolResource(BLUR_ENABLE_BOOL, true) && !boolResource(IMMERSIVE_THEME_BOOL, false)
    }.getOrDefault(true)

    /** `isLowPowerMode()`. */
    private val lowPowerMode: Boolean = runCatching {
        val resolver = context.contentResolver
        Settings.Global.getInt(resolver, LOW_POWER_MODE, 0) == 1 ||
            Settings.System.getInt(resolver, SUPER_POWER_SAVE_MODE, 0) == 1
    }.getOrDefault(false)

    /** `updateState`'s `forceDisableEffect`. */
    fun materialUsable(): Boolean = !lowAnimationLevel && themeEnabled && !lowPowerMode

    /** `isStrokeEffectEnable()`: the switch, unless one of the gates above disabled everything. */
    fun strokeEnabled(): Boolean = materialUsable() && runCatching {
        Settings.System.getInt(context.contentResolver, MATERIAL_STROKE, 0) == 1
    }.getOrDefault(false)

    private fun boolResource(name: String, fallback: Boolean): Boolean {
        val id = context.resources.getIdentifier(name, "bool", context.packageName)
        return if (id == 0) fallback else context.resources.getBoolean(id)
    }
}

private val GateLock = Any()

@Volatile
private var strokeState: MutableState<Boolean>? = null

/**
 * `isStrokeEffectEnable()`, observed the way `COUIMaterialStateManager.registerMaterialStateObserver`
 * (`:618-660`) observes it: the stroke switch, `low_power` and `super_powersave_mode_state`, with the
 * theme and animation-level gates re-read on every change.
 */
@Composable
private fun rememberSystemStrokeEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    return remember(context) { obtainStrokeState(context) }.value
}

private fun obtainStrokeState(appContext: Context): MutableState<Boolean> {
    strokeState?.let { return it }
    return synchronized(GateLock) {
        strokeState ?: run {
            val gate = BottomBarMaterialGate(appContext)
            val state = mutableStateOf(gate.strokeEnabled())
            runCatching {
                // The settings are process-wide, so the observer lives as long as the process does.
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        state.value = gate.strokeEnabled()
                    }
                }
                val resolver = appContext.contentResolver
                resolver.registerContentObserver(Settings.System.getUriFor(MATERIAL_STROKE), false, observer)
                resolver.registerContentObserver(Settings.Global.getUriFor(LOW_POWER_MODE), false, observer)
                resolver.registerContentObserver(
                    Settings.System.getUriFor(SUPER_POWER_SAVE_MODE),
                    false,
                    observer,
                )
            }
            strokeState = state
            state
        }
    }
}

@Composable
internal actual fun rememberPlatformBottomBarBackdropRenderer(): BottomBarBackdropRenderer? {
    val context = LocalContext.current.applicationContext
    // `updateState` turns the effect off before it reads any switch, and `isBlurEnabled()` already
    // carries the blur switch and the power-saving modes; with blur off the bar must paint its
    // opaque fill instead, so the Compose engine takes the bar back.
    val materialUsable = remember(context) { BottomBarMaterialGate(context).materialUsable() }
    val blurEnabled = isBlurEnabled()
    if (!materialUsable || !blurEnabled) return null
    return remember {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            null
        } else {
            val api = runCatching<BottomBarBackdropApi> { OplusBottomBarBackdropApi() }
                .recoverCatching {
                    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        "No backdrop RenderEffect before Android 16"
                    }
                    PlatformBottomBarBackdropApi()
                }
                .onFailure { Log.d(TAG, "Native bottom bar material unavailable", it) }
                .getOrNull()
            api?.let(::AndroidBottomBarBackdropRenderer)
        }
    }
}

private class AndroidBottomBarBackdropRenderer(private val api: BottomBarBackdropApi) : BottomBarBackdropRenderer {
    private val material: OplusMaterialApi? = runCatching { OplusMaterialApi() }
        .onFailure { Log.d(TAG, "Oplus melt-ball APIs unavailable", it) }
        .getOrNull()

    override val drawsOwnStroke: Boolean get() = api.drawsOwnStroke
    override val drawsOwnShadow: Boolean get() = api.drawsOwnShadow
    override val supportsMeltBall: Boolean get() = material != null

    @Composable
    override fun CapsuleMaterial(isDark: Boolean, modifier: Modifier) {
        val stroke = rememberSystemStrokeEnabled()
        TouchTransparentInterop(modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> BottomBarMaterialView(ctx, api, BottomBarShape.Capsule) },
                onRelease = { it.release() },
                update = { view -> view.setMode(isDark, stroke) },
            )
        }
    }

    @Composable
    override fun AccessoryMaterial(isDark: Boolean, modifier: Modifier) {
        val stroke = rememberSystemStrokeEnabled()
        TouchTransparentInterop(modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { BottomBarMaterialView(it, api, BottomBarShape.Circle) },
                onRelease = { it.release() },
                update = { view -> view.setMode(isDark, stroke) },
            )
        }
    }

    @Composable
    override fun MeltBallHost(geometry: BottomBarMeltBallGeometry, isDark: Boolean, modifier: Modifier) {
        val stroke = rememberSystemStrokeEnabled()
        TouchTransparentInterop(modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> MeltBallHostView(ctx, api, material) },
                onRelease = { it.release() },
                update = { host -> host.update(geometry, isDark, stroke) },
            )
        }
    }
}
