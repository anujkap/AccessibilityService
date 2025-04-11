package com.example.accessibilityservice

/**
 * Represents a single action to be performed by the accessibility service.
 *
 * @property type The type of action
 * @property targetId The ID of the target UI element.
 * @property textToType The text to type, if applicable.
 * @property navigationType The type of navigation, if applicable.
 * @property packageName The package name of the app, if applicable.
 */
@kotlinx.serialization.Serializable
data class DeviceAction(
    val type: String,
    val targetId: String = "",
    val textToType: String = "",
    val navigationType: String = "",
    val packageName: String = "",
    val uniqueId:String = ""
)