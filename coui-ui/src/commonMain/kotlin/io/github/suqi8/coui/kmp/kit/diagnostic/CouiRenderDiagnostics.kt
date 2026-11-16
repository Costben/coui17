// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.diagnostic

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import kotlin.time.TimeSource

/** Temporary render diagnostics entry point; enabled by the app only in canary builds, always called on main thread. */
internal object CouiRenderDiagnostics {
    private class Source(val id: String, val backdrop: CouiBackdrop, val redraw: () -> Unit)

    private val clockOrigin = TimeSource.Monotonic.markNow()
    private var reporter: ((String) -> Unit)? = null
    private var nextId = 0L
    private val sources = linkedMapOf<CouiBackdrop, Source>()
    private val probes = linkedMapOf<RenderProbeNode, String>()
    private val events = ArrayDeque<String>()

    val enabled: Boolean get() = reporter != null

    internal fun uptimeMs(): Long = clockOrigin.elapsedNow().inWholeMilliseconds

    fun enable(reporter: (String) -> Unit) {
        this.reporter = reporter
        event("enable")
    }

    fun event(message: String) {
        if (!enabled) return
        val entry = "${uptimeMs()} $message"
        if (events.size == 64) events.removeFirst()
        events.addLast(entry)
        reporter?.invoke(entry)
    }

    fun snapshot(): String = CouiDiagnosticJsonObject()
        .put("uptimeMs", uptimeMs())
        .put(
            "sources",
            CouiDiagnosticJsonArray().apply {
                sources.values.forEach { source ->
                    put(source.backdrop.diagnosticStateJson().put("id", source.id))
                }
            },
        )
        .put(
            "probes",
            CouiDiagnosticJsonArray().apply {
                probes.forEach { (node, id) -> put(node.snapshotJson().put("id", id)) }
            },
        )
        .put(
            "events",
            CouiDiagnosticJsonArray().apply {
                events.forEach { put(it) }
            },
        )
        .toJsonString()

    fun sourceIds(target: String): List<String> = selectedSources(target).map { it.id }

    suspend fun captureSource(id: String): ImageBitmap = selectedSources(id).single().backdrop.diagnosticCapture()

    fun redraw(target: String) {
        val selectedSources = sources.values.filter { target == it.id || target == it.backdrop.diagnosticName }
        val selectedProbes = probes.keys.filter { matches(it.name, target) }
        require(selectedSources.isNotEmpty() || selectedProbes.isNotEmpty()) { "No matching render targets: $target" }
        selectedSources.forEach { it.redraw() }
        selectedProbes.forEach { it.invalidateDraw() }
        event("redraw target=$target sources=${selectedSources.size} probes=${selectedProbes.size}")
    }

    fun recreateSource(id: String) {
        val source = selectedSources(id).single()
        event("recreate source=${source.id} name=${source.backdrop.diagnosticName}")
        source.backdrop.diagnosticRecreate()
    }

    fun setBottomHidden(hidden: Boolean) {
        val selected = probes.keys.filter { it.name == "bottom-bar" }
        require(selected.isNotEmpty()) { "No active bottom-bar render probe" }
        selected.forEach {
            it.hidden = hidden
            it.invalidateDraw()
        }
        event("bottom hidden=$hidden probes=${selected.size}")
    }

    fun reset() {
        probes.keys.filter { it.hidden }.forEach {
            it.hidden = false
            it.invalidateDraw()
        }
        event("reset")
    }

    internal fun attachSource(backdrop: CouiBackdrop, redraw: () -> Unit) {
        if (!enabled) return
        val source = Source("source-${++nextId}", backdrop, redraw)
        sources[backdrop] = source
        event("source.attach id=${source.id} name=${backdrop.diagnosticName}")
    }

    internal fun detachSource(backdrop: CouiBackdrop) {
        val source = sources.remove(backdrop) ?: return
        event("source.detach id=${source.id} name=${backdrop.diagnosticName}")
    }

    internal fun attachProbe(node: RenderProbeNode) {
        probes[node] = "probe-${++nextId}"
        event("probe.attach id=${probes[node]} name=${node.name}")
    }

    internal fun detachProbe(node: RenderProbeNode) {
        val id = probes.remove(node) ?: return
        event("probe.detach id=$id name=${node.name}")
    }

    private fun selectedSources(target: String): List<Source> {
        val selected = sources.values.filter {
            target == "all" || target == it.id || target == it.backdrop.diagnosticName
        }
        require(selected.isNotEmpty()) { "No matching sampling layers: $target" }
        return selected
    }

    private fun matches(name: String, target: String): Boolean = name == target || name.startsWith("$target/")
}

/** Placed inside clipped layers for targeted redraws; bottom-bar also supports draw suppression without altering layout or hits. */
internal fun Modifier.renderDiagnosticProbe(name: String?): Modifier = if (name == null || !CouiRenderDiagnostics.enabled) this else then(RenderProbeElement(name))

private data class RenderProbeElement(val name: String) : ModifierNodeElement<RenderProbeNode>() {
    override fun create() = RenderProbeNode(name)
    override fun update(node: RenderProbeNode) {
        node.name = name
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "renderDiagnosticProbe"
        properties["target"] = this@RenderProbeElement.name
    }
}

internal class RenderProbeNode(var name: String) :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null
    private var drawCount = 0L
    private var lastDrawMs = 0L
    var hidden = false

    override fun onAttach() {
        CouiRenderDiagnostics.attachProbe(this)
    }

    override fun onDetach() {
        CouiRenderDiagnostics.detachProbe(this)
        coordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        drawCount++
        lastDrawMs = CouiRenderDiagnostics.uptimeMs()
        if (!hidden) drawContent()
    }

    fun snapshotJson(): CouiDiagnosticJsonObject = CouiDiagnosticJsonObject()
        .put("name", name)
        .put("drawCount", drawCount)
        .put("lastDrawMs", lastDrawMs)
        .put("hidden", hidden)
        .put("coordinates", diagnosticCoordinates(coordinates))

    fun snapshot(): String = snapshotJson().toJsonString()
}

internal fun diagnosticCoordinates(coordinates: LayoutCoordinates?): CouiDiagnosticJsonObject? {
    if (coordinates?.isAttached != true) return null
    val origin = coordinates.localToScreen(Offset.Zero)
    return CouiDiagnosticJsonObject()
        .put("width", coordinates.size.width)
        .put("height", coordinates.size.height)
        .put("screenX", if (origin.x.isFinite()) origin.x.toDouble() else null)
        .put("screenY", if (origin.y.isFinite()) origin.y.toDouble() else null)
}

internal class CouiDiagnosticJsonObject {
    private val entries = linkedMapOf<String, String>()

    fun put(key: String, value: String?): CouiDiagnosticJsonObject = apply {
        entries[key] = if (value == null) "null" else quoteJsonString(value)
    }

    fun put(key: String, value: Boolean): CouiDiagnosticJsonObject = apply {
        entries[key] = value.toString()
    }

    fun put(key: String, value: Number?): CouiDiagnosticJsonObject = apply {
        entries[key] = formatJsonNumber(value)
    }

    fun put(key: String, value: CouiDiagnosticJsonObject?): CouiDiagnosticJsonObject = apply {
        entries[key] = value?.toJsonString() ?: "null"
    }

    fun put(key: String, value: CouiDiagnosticJsonArray): CouiDiagnosticJsonObject = apply {
        entries[key] = value.toJsonString()
    }

    fun toJsonString(): String = entries.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
        "${quoteJsonString(k)}:$v"
    }

    override fun toString(): String = toJsonString()
}

internal class CouiDiagnosticJsonArray {
    private val items = mutableListOf<String>()

    fun put(value: String?): CouiDiagnosticJsonArray = apply {
        items += if (value == null) "null" else quoteJsonString(value)
    }

    fun put(value: CouiDiagnosticJsonObject): CouiDiagnosticJsonArray = apply {
        items += value.toJsonString()
    }

    fun toJsonString(): String = items.joinToString(prefix = "[", postfix = "]", separator = ",")

    override fun toString(): String = toJsonString()
}

private fun formatJsonNumber(value: Number?): String = when (value) {
    null -> "null"
    is Double -> if (value.isFinite()) value.toString() else "null"
    is Float -> if (value.isFinite()) value.toString() else "null"
    else -> value.toString()
}

private fun quoteJsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    for (ch in value) {
        val escaped = when (ch) {
            '"' -> "\\\""
            '\\' -> "\\\\"
            '\b' -> "\\b"
            '\u000C' -> "\\f"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> null
        }
        if (escaped != null) {
            append(escaped)
        } else if (ch < ' ') {
            append("\\u")
            append(ch.code.toString(16).padStart(4, '0'))
        } else {
            append(ch)
        }
    }
    append('"')
}
