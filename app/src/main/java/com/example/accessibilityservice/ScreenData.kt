package com.example.accessibilityservice

import android.graphics.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

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
    val viewId: String? = null,
    val hintText: String? = null,
    @Transient val isVisible: Boolean = false,
    val actionList: List<String>,
    val inputType: Int,
    val labelFor: String? = null,
    val stateDescription: String? = null,
    val tooltipText: String? = null,
    val uniqueId: Int,
    val rangeInfo: RangeInfoData?,
)
@Serializable
data class RangeInfoData(val current: Float, val min: Float, val max: Float, val type: Int)


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
