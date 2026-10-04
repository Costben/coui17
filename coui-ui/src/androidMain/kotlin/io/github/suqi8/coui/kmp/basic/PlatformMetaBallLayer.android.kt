// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** Public vendor APIs, resolved once; no hidden-API exemptions or app-package dependencies. */
private class MaterialApi {
    private val util = Class.forName("com.oplus.view.material.OplusMaterialUtil")
    private val base = Class.forName("com.oplus.view.material.OplusMaterialBaseParams")
    private val corner = Class.forName("com.oplus.view.material.OplusMaterialCornerParams")
    private val meta = Class.forName("com.oplus.view.material.OplusMaterialMetaBallParams")
    private val edge = Class.forName("com.oplus.view.material.OplusMaterialEdgeParams")
    private val baseConstructor = base.getConstructor(Int::class.javaPrimitiveType, Rect::class.java)
    private val cornerConstructor = corner.getConstructor(Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
    private val metaConstructor = meta.getConstructor(
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val edgeConstructor = edge.getConstructor(
        Int::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
        Float::class.javaPrimitiveType,
    )
    private val setBase = util.getMethod("setBaseParams", View::class.java, base)
    private val setCorner = util.getMethod("setCornerParams", View::class.java, corner)
    private val setMeta = util.getMethod("setMetaBallParams", View::class.java, meta)
    private val setEdge = util.getMethod("setEdgeParams", View::class.java, edge)
    private val setChain = util.getMethod("setMetaBallChain", Set::class.java)

    fun update(view: View, width: Int, height: Int, radius: Float, index: Int, body: Color, rim: Color, extension: Int, factorX: Float, factorY: Float, rimWidth: Float) {
        // Transparent display-list content is essential: an opaque View background covers the
        // native material silhouette, even when every parameter setter returns true.
        setBase.invoke(null, view, baseConstructor.newInstance(body.toArgb(), Rect(0, 0, width, height)))
        setCorner.invoke(null, view, cornerConstructor.newInstance(radius, if (radius >= minOf(width, height) / 2f - 0.5f) 1f else 2f))
        setMeta.invoke(null, view, metaConstructor.newInstance(index, 0, extension, factorX, factorY))
        setEdge.invoke(null, view, if (rim.alpha > 0f) edgeConstructor.newInstance(1, rimWidth, rim.alpha, 0f) else null)
    }

    fun link(views: Set<View>) {
        setChain.invoke(null, views)
    }

    fun clear(view: View) {
        setMeta.invoke(null, view, null)
        setEdge.invoke(null, view, null)
        setBase.invoke(null, view, null)
        setCorner.invoke(null, view, null)
    }
}

private class MaterialChainView(context: Context, val api: MaterialApi) : FrameLayout(context) {
    val particles = List(3) {
        View(context).also {
            it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            addView(it, LayoutParams(1, 1))
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    private var linkedMask = -1
    fun link(active: Set<View>, mask: Int) {
        if (mask != linkedMask) {
            api.link(active)
            linkedMask = mask
        }
    }

    fun release() {
        particles.forEach(api::clear)
    }
}

@Composable
internal actual fun rememberPlatformMetaBallRenderer(): MetaBallRenderer? = remember {
    runCatching { AndroidMetaBallRenderer(MaterialApi()) }.getOrNull()
}

private class AndroidMetaBallRenderer(private val api: MaterialApi) : MetaBallRenderer {
    @Composable
    override fun Draw(
        particles: List<MetaBallParticle>,
        body: Color,
        rim: Color,
        extension: Dp,
        factorX: Float,
        factorY: Float,
        modifier: Modifier,
        fallback: @Composable () -> Unit,
    ) {
        var failed by remember { mutableStateOf(false) }
        if (failed || particles.size > 3) {
            fallback()
            return
        }
        val density = LocalDensity.current
        AndroidView(
            modifier = modifier,
            factory = { MaterialChainView(it, api) },
            onRelease = { runCatching { it.release() } },
            update = { chain ->
                runCatching {
                    val active = linkedSetOf<View>()
                    var activeMask = 0
                    chain.particles.forEachIndexed { i, view ->
                        val particle = particles.getOrNull(i)
                        if (particle == null || particle.width.value <= 0f || particle.height.value <= 0f || particle.alpha <= 0f) {
                            view.visibility = View.GONE
                            api.clear(view)
                        } else {
                            view.visibility = View.VISIBLE
                            val width = with(density) { particle.width.roundToPx() }.coerceAtLeast(1)
                            val height = with(density) { particle.height.roundToPx() }.coerceAtLeast(1)
                            if (view.layoutParams.width != width || view.layoutParams.height != height) {
                                view.layoutParams = FrameLayout.LayoutParams(width, height, Gravity.TOP or Gravity.LEFT)
                            }
                            view.pivotX = width / 2f
                            view.pivotY = height / 2f
                            view.translationX = with(density) { particle.centerX.toPx() } - width / 2f
                            view.translationY = with(density) { particle.centerY.toPx() } - height / 2f
                            view.scaleX = particle.scaleX
                            view.scaleY = particle.scaleY
                            view.alpha = particle.alpha.coerceIn(0f, 1f)
                            api.update(
                                view, width, height, with(density) { particle.cornerRadius.toPx() },
                                if (i == 0) 1 else 2, body, rim,
                                with(density) { extension.toPx() }.roundToInt(), factorX, factorY,
                                with(density) { MetaBallDefaults.RimWidth.toPx() },
                            )
                            active.add(view)
                            activeMask = activeMask or (1 shl i)
                        }
                    }
                    chain.link(active, activeMask)
                    chain.invalidate()
                }.onFailure { failed = true }
            },
        )
    }
}
