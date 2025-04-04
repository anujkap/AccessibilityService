package com.example.accessibilityservice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class MyAccessibilityService : AccessibilityService() {

    private val debugTag = "MyAccessibilityService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(debugTag, "Service connected")

        // Configure the service (you can also do this in the XML)
        val info = AccessibilityServiceInfo()
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(debugTag, "Accessibility event: ${event.eventType}")

        // Get the root node of the current window
        val rootNode = rootInActiveWindow ?: return

        // Extract screen information
        val screenData = extractScreenData(rootNode)

        // Send data to LLM (example: print to log)
        val jsonData = Json.encodeToString(screenData)
        Log.d(debugTag, "Screen Data: $jsonData")

        // Clean up the root node
        rootNode.recycle()
    }

    private fun extractScreenData(node: AccessibilityNodeInfo): ScreenData {
        val screenData = ScreenData(mutableListOf())
        traverseNode(node, screenData.views)
        return screenData
    }

    private fun traverseNode(node: AccessibilityNodeInfo, views: MutableList<ViewData>) {
        val viewData = ViewData(
            text = node.text?.toString(),
            className = node.className?.toString(),
            bounds = Rect().apply { node.getBoundsInScreen(this) },
            contentDescription = node.contentDescription?.toString(),
            packageName = node.packageName?.toString(),
            windowId = node.windowId
        )
        views.add(viewData)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                traverseNode(it, views)
                it.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(debugTag, "Service interrupted")
    }
}

@Serializable
data class ScreenData(val views: MutableList<ViewData>)

@Serializable
data class ViewData(
    val text: String? = null,
    val className: String? = null,
    @Serializable(with = RectSerializer::class)
    val bounds: Rect,
    val contentDescription: String? = null,
    val packageName: String? = null,
    val windowId: Int = -1,
    @Transient val isVisible: Boolean = false
)

object RectSerializer : KSerializer<Rect> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rect") {
        element<Int>("left")
        element<Int>("top")
        element<Int>("right")
        element<Int>("bottom")
    }

    override fun serialize(encoder: Encoder, value: Rect) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.left)
            encodeIntElement(descriptor, 1, value.top)
            encodeIntElement(descriptor, 2, value.right)
            encodeIntElement(descriptor, 3, value.bottom)
        }
    }

    override fun deserialize(decoder: Decoder): Rect {
        return decoder.decodeStructure(descriptor) {
            var left = 0
            var top = 0
            var right = 0
            var bottom = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> left = decodeIntElement(descriptor, 0)
                    1 -> top = decodeIntElement(descriptor, 1)
                    2 -> right = decodeIntElement(descriptor, 2)
                    3 -> bottom = decodeIntElement(descriptor, 3)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Rect(left, top, right, bottom)
        }
    }
}