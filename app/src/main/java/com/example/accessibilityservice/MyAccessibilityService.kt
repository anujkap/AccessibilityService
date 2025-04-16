package com.example.accessibilityservice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.json.Json

class MyAccessibilityService : AccessibilityService() {

    private val debugTag = "MyAccessibilityService"
    private val conversationManager = ConversationManager(this)

    companion object{
        private var service: MyAccessibilityService? = null
        fun getService(): MyAccessibilityService? {
            return service
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(debugTag, "Service connected")
        service = this

        // Configure the service (you can also do this in the XML)
        val info = AccessibilityServiceInfo()
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo = info
        // TODO send info context to LLM
        val globalActions: List<AccessibilityNodeInfo.AccessibilityAction> = systemActions
        val globalActionNames: MutableList<String> = mutableListOf()
        for (action in globalActions) {
            if (action.id == GLOBAL_ACTION_BACK ||
                action.id == GLOBAL_ACTION_HOME ||
                action.id == GLOBAL_ACTION_NOTIFICATIONS ||
                action.id == GLOBAL_ACTION_RECENTS ||
                action.id == GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE ||
                action.id == GLOBAL_ACTION_TAKE_SCREENSHOT
            )
                globalActionNames.add(getGlobalActionName(action))
        }
        Log.d(debugTag, "Global actions: $globalActionNames")

        val packageManager = packageManager
        val installedPackages = packageManager.getInstalledPackages(0)
        val packageNames = installedPackages.map { it.packageName }
//        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        Log.d(debugTag, "Installed packages: $packageNames")
        conversationManager.serviceConnected()



    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(debugTag, "Accessibility event: ${event.eventType}")

        // Get the root node of the current window
        val rootNode = rootInActiveWindow ?: return

        // Extract screen information
        val screenData = extractScreenData(rootNode)

        // Send data to LLM (example: print to log)
        val jsonString = Json.encodeToString(screenData)

        // Split the JSON string into chunks of 4000 characters (max for logcat)
        val chunkSize = 4000
        val jsonChunks = jsonString.chunked(chunkSize)

        jsonChunks.forEachIndexed { index, chunk ->
            Log.d(debugTag, "Screen Data Chunk ${index + 1}/${jsonChunks.size}: $chunk")
        }
        conversationManager.UIUpdated(withJson = jsonString)
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
            windowId = node.windowId,
            viewId = node.viewIdResourceName?.toString(),
            hintText = node.hintText?.toString(),
            actionList = node.actionList.map { it.toString() },
            inputType = node.inputType,
            labelFor = node.labelFor?.toString(),
            stateDescription = node.stateDescription?.toString(),
            tooltipText = node.tooltipText?.toString(),
            uniqueId = node.hashCode(),
            rangeInfo = node.rangeInfo?.run { RangeInfoData(current, min, max, type) },

        )
        views.add(viewData)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                traverseNode(it, views)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(debugTag, "Service interrupted")
    }

    /**
     * Processes a list of actions received from the LLM.
     *
     * @param actions The list of actions to process.
     */
    fun processActions(actions: List<DeviceAction>) {
        for (action in actions) {
            executeAction(action)
        }
    }

    /**
     * Executes a single action.
     *
     * @param action The action to execute.
     */
    private fun executeAction(action: DeviceAction) {
        try {
            when {
                action.type == "navigate" -> performNavigation(action.navigationType, action.packageName)
                else -> performNodeActionWrapper(action.targetId, action.uniqueId, action.type, action.textToType)

            }
        } catch (e: Exception) {
            Log.e(debugTag, "Exception during action execution: ${e.message}", e)
        }
    }

    /**
     * Performs navigation based on the specified navigation type and package name.
     *
     * This function handles various navigation actions, including:
     * - **GLOBAL_ACTION_BACK:** Navigating back to the previous screen.
     * - **GLOBAL_ACTION_HOME:** Navigating to the home screen.
     * - **GLOBAL_ACTION_NOTIFICATIONS:** Opens the notification shade.
     * - **GLOBAL_ACTION_RECENTS:** Opens the recent apps overview.
     * - **GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE:** Closes the notification shade.
     * - **GLOBAL_ACTION_TAKE_SCREENSHOT:** Takes a screenshot of the current screen.
     * - **open_app:** Opens a specific application.
     *
     * For global actions, the corresponding `AccessibilityService.GLOBAL_ACTION_*` constant is used internally.
     *
     * @param navigationType The type of navigation to perform.
     *                       Valid values are:
     *                       - "GLOBAL_ACTION_BACK"
     *                       - "GLOBAL_ACTION_HOME"
     *                       - "GLOBAL_ACTION_NOTIFICATIONS"
     *                       - "GLOBAL_ACTION_RECENTS"
     *                       - "GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE"
     *                       - "GLOBAL_ACTION_TAKE_SCREENSHOT"
     *                       - "open_app"
     * @param packageName    The package name of the application to open (only required when navigationType is "open_app").
     *                       This should be the full package name (e.g., "com.example.app"). Ignored for other navigation types.
     * @throws IllegalArgumentException If an unknown navigation type is provided.
     * @throws PackageManager.NameNotFoundException If the specified package name is invalid when `navigationType` is "open_app". This exception is thrown by the `openApp` function.
     * @throws Exception If any other error occurs during navigation. This exception can be thrown by the `performGlobalAction` or `openApp` functions.
     */
    private fun performNavigation(navigationType: String, packageName: String) {
        when (navigationType) {
            "GLOBAL_ACTION_BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "GLOBAL_ACTION_HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "GLOBAL_ACTION_NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "GLOBAL_ACTION_RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE" -> performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            "GLOBAL_ACTION_TAKE_SCREENSHOT" -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            "open_app" -> openApp(packageName)
            else -> throw IllegalArgumentException("Unknown navigation type: $navigationType")
        }
    }

    private fun performNodeActionWrapper(targetId: String, uniqueId: String,actionName: String, argument: String) {
        val node = findNodeById(targetId, uniqueId)
        if (argument.isNotEmpty()) {
            performNodeActionWithArgument(node, actionName, argument)

        } else {
            performNodeAction(node, actionName)
        }
    }



    /**
     * Performs a specific action on an AccessibilityNodeInfo with a given argument.
     *
     * This function supports several specific actions that require additional arguments to be passed
     * to the AccessibilityNodeInfo. It handles the creation of the appropriate argument Bundle
     * based on the provided action name.
     *
     * @param node The AccessibilityNodeInfo on which to perform the action.
     * @param actionName The name of the action to perform. Supported actions are:
     *                   - "ACTION_SET_TEXT": Sets the text content of the node.
     *                   - "ACTION_SET_SELECTION": Sets the selection range of the node.
     *                   - "ACTION_SCROLL_TO_POSITION": Scrolls to a specific position in a scrollable node.
     * @param argument The argument to be used with the action. The format of the argument depends on the action:
     *                 - "ACTION_SET_TEXT": The new text content as a String.
     *                 - "ACTION_SET_SELECTION": A string in the format "start-end" where start and end are integers
     *                   representing the start and end indices of the selection.
     *                 - "ACTION_SCROLL_TO_POSITION": A string in the format "row-column" where row and column are integers
     *                   representing the target row and column to scroll to.
     *
     * @throws ElementNotFoundException if the provided `node` is null.
     * @throws IllegalArgumentException if an unsupported `actionName` is provided or if the argument format is incorrect.
     * @throws NumberFormatException if argument contains non integer values where integers are expected
     */
    private fun performNodeActionWithArgument(node: AccessibilityNodeInfo?, actionName: String, argument: String) {
        val actionId = getAccessibilityNodeInfoActionId(actionName)
        when(actionName) {
            "ACTION_SET_TEXT" -> {
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    argument
                )
                node?.performAction(actionId!!, arguments)
                    ?: throw ElementNotFoundException("Element not found")

            }

            "ACTION_SET_SELECTION" -> {
                val arguments = Bundle()
                arguments.putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                    argument.split("-")[0].toInt()
                )
                arguments.putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                    argument.split("-")[1].toInt()
                )
                node?.performAction(actionId!!, arguments)
                    ?: throw ElementNotFoundException("Element not found")
            }

            "ACTION_SCROLL_TO_POSITION" -> {
                val arguments = Bundle()
                arguments.putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT,
                    argument.split("-")[0].toInt()
                )
                arguments.putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT,
                    argument.split("-")[1].toInt()
                )

                node?.performAction(actionId!!, arguments)
                    ?: throw ElementNotFoundException("Element not found")
            }

            else -> {
                throw IllegalArgumentException("Unknown action name: $actionName")
            }
        }
    }

    /**
     * Performs a specified action on a given AccessibilityNodeInfo.
     *
     * This function takes an AccessibilityNodeInfo and an action name as input.
     * It attempts to find the corresponding action ID for the given action name using the
     * `getAccessibilityNodeInfoActionId` function. If the action ID is found, it performs
     * the action on the node. If the action ID is not found, it logs an error message.
     * If the node is null after successfully mapping the action it will throw an exception
     *
     * @param node The AccessibilityNodeInfo on which to perform the action. Can be null.
     * @param actionName The name of the action to perform (e.g., "click", "longClick", "scrollForward").
     * @throws ElementNotFoundException if the node is null after an actionId was found.
     * @throws IllegalArgumentException if `actionName` is not a valid action supported by `getAccessibilityNodeInfoActionId`
     */
    private fun performNodeAction(node: AccessibilityNodeInfo?, actionName: String) {
        val actionId = getAccessibilityNodeInfoActionId(actionName)
        if (actionId != null) {
            node?.performAction(actionId) ?: throw ElementNotFoundException("Element not found")

        }else{
            Log.e("Error", "Unknown action name: $actionName")

        }
    }

    /**
     * Opens an app with the given package name.
     *
     * @param packageName The package name of the app to open.
     */
    private fun openApp(packageName: String) {
        val launchIntent: Intent? = this.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(it)
        } ?: throw AppNotFoundException("App with package name: $packageName not found")
    }

    /**
     * Finds an AccessibilityNodeInfo by its ID.
     *
     * @param id The ID of the node to find.
     * @return The AccessibilityNodeInfo if found, null otherwise.
     */
    private fun findNodeById(id: String, uniqueId: String): AccessibilityNodeInfo? {
        val rootInActiveWindow = this.rootInActiveWindow
        rootInActiveWindow?.let { root ->
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNullOrEmpty()) {
                return null
            }

            for(node in nodes){
                if (node.uniqueId == uniqueId){
                    return node
                }
            }
            return null
        } ?: return null
    }

    /**
     * Returns the corresponding AccessibilityNodeInfo action ID for a given action name.
     *
     * @param actionName The name of the action (e.g., "ACTION_CLICK", "ACTION_SCROLL_FORWARD").
     * @return The AccessibilityNodeInfo action ID, or null if the action name is invalid.
     *
     * **/
    private fun getAccessibilityNodeInfoActionId(actionName: String): Int? {
        val actionId = when (actionName) {
            "ACTION_ACCESSIBILITY_FOCUS" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS
            "ACTION_CLEAR_FOCUS" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_FOCUS
            "ACTION_CLEAR_SELECTION" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_SELECTION
            "ACTION_CLICK" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK
            "ACTION_COLLAPSE" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE
            "ACTION_CONTEXT_CLICK" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK
            "ACTION_COPY" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY
            "ACTION_CUT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT
            "ACTION_DISMISS" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS
            "ACTION_EXPAND" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND
            "ACTION_FOCUS" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_FOCUS
            "ACTION_IME_ENTER" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER
            "ACTION_LONG_CLICK" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK
//            "ACTION_MOVE_WINDOW" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_MOVE_WINDOW
//            "ACTION_NEXT_AT_MOVEMENT_GRANULARITY" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            "ACTION_NEXT_HTML_ELEMENT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_NEXT_HTML_ELEMENT
            "ACTION_PAGE_DOWN" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN
            "ACTION_PAGE_LEFT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_LEFT
            "ACTION_PAGE_RIGHT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_RIGHT
            "ACTION_PAGE_UP" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP
            "ACTION_PASTE" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE
//            "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
//            "ACTION_PREVIOUS_HTML_ELEMENT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PREVIOUS_HTML_ELEMENT
            "ACTION_SCROLL_BACKWARD" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
            "ACTION_SCROLL_FORWARD" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD
            "ACTION_SCROLL_TO_POSITION" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION
            "ACTION_SELECT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT
//            "ACTION_SET_PROGRESS" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS
            "ACTION_SET_SELECTION" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION
            "ACTION_SET_TEXT" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT
            "ACTION_SHOW_ON_SCREEN" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN
          else -> null
        }
        return actionId?.id
    }

    private fun getGlobalActionName(action: AccessibilityNodeInfo.AccessibilityAction): String {
        return when (action.id) {
            GLOBAL_ACTION_BACK -> "GLOBAL_ACTION_BACK"
            GLOBAL_ACTION_HOME -> "GLOBAL_ACTION_HOME"
            GLOBAL_ACTION_NOTIFICATIONS -> "GLOBAL_ACTION_NOTIFICATIONS"
            GLOBAL_ACTION_RECENTS -> "GLOBAL_ACTION_RECENTS"
            GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE -> "GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE"
            GLOBAL_ACTION_TAKE_SCREENSHOT -> "GLOBAL_ACTION_TAKE_SCREENSHOT"
            else -> "Unknown Global Action"
        }
    }


    class ElementNotFoundException(message: String) : RuntimeException(message)
    class AppNotFoundException(message: String) : RuntimeException(message)
}
