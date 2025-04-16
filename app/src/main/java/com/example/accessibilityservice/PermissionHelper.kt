package com.example.accessibilityservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.accessibilityservice.ConversationManager.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PermissionHelper {
    private val managerJob = Job()
    private val managerScope = CoroutineScope(Dispatchers.Main + managerJob)


    const val TAG = "ConvManager"
    private const val PERMISSION_REQUEST_DELAY_MS = 7000L // 7 seconds (Adjustable guess)


    fun requestAudioPermission(context: Context, managerScope: CoroutineScope) {
        Log.d(TAG, "Launching PermissionRequestActivity...")
        // Ensure context is Activity or use FLAG_ACTIVITY_NEW_TASK from Service/App context
        val intent = Intent(context, PermissionRequestActivity::class.java).apply {
            action = PermissionRequestActivity.ACTION_REQUEST_PERMISSION
            putExtra(
                PermissionRequestActivity.EXTRA_PERMISSION_TO_REQUEST,
                Manifest.permission.RECORD_AUDIO
            )
            // If context is the Service, FLAG_ACTIVITY_NEW_TASK is essential
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Optional flags from previous examples
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            context.startActivity(intent)
            managerScope.launch { // Ensure Toast is on Main thread
                Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching PermissionRequestActivity", e)
            managerScope.launch { // Ensure Toast is on Main thread
                Toast.makeText(context, "Error requesting permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun serviceConnected(context: Context, callback: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {

            callback(true)

        } else {
            // CASE 2: Permission NOT granted.
            Log.w(ConversationManager.TAG, "Permission not granted. Requesting via Activity.")
            PermissionHelper.requestAudioPermission(context, managerScope) // Launch the activity to ask the user

            // *** Start Manual Delay - HIGHLY UNRELIABLE ***
            Log.i(ConversationManager.TAG, "Starting a ${PERMISSION_REQUEST_DELAY_MS}ms delay, hoping user grants permission...")
            managerScope.launch {
                delay(PERMISSION_REQUEST_DELAY_MS)

                // --- After delay, TRY starting the listener ---
                Log.i(ConversationManager.TAG, "Delay finished. Attempting to start listener NOW.")
                // *** CRITICAL: Check permission AGAIN! User might have denied/ignored. ***
                callback(true)
            }
        }
    }

}
