package com.example.accessibilityservice

import kotlinx.serialization.Serializable

/**
 * Represents a single action to be performed by the accessibility service.
 *
 * @property type The type of action
 * @property viewId The ID of the target UI element.
 * @property argument The argument for the action, if applicable.
 * @property navigationType The type of navigation, if applicable.
 * @property packageName The package name of the app, if applicable.
 */
@Serializable
data class DeviceAction(
    val type: String,
    val viewId: String = "",
    val argument: String = "",
    val navigationType: String = "",
    val packageName: String = "",
    val bounds: String = "",
    val traverseDirection:String = ""
)