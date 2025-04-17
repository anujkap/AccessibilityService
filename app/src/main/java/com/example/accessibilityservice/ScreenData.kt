package com.example.accessibilityservice

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ScreenData(val views: MutableList<ViewData>)

@Serializable
data class ViewData(
    val text: String? = null,
    val className: String? = null,
    val bounds: String,
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