package com.sahiwal.callsredirect.broadcaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.sahiwal.callsredirect.services.WebRTCService

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Log.d("CallReceiver", "Incoming call detected")

                    // Start WebRTCService when the call is ringing
                    val serviceIntent = Intent(context, WebRTCService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    // Optionally, auto-answer the call (see next section)
                    answerCall(context)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d("CallReceiver", "Call answered, starting WebRTC...")
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d("CallReceiver", "Call ended, stopping WebRTC...")
                    context.stopService(Intent(context, WebRTCService::class.java))
                }
            }
        }
    }

    // Function to auto-answer the call
    private fun answerCall(context: Context) {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val c = Class.forName(telephonyManager.javaClass.name)
            val method = c.getDeclaredMethod("getITelephony")
            method.isAccessible = true
            val telephonyService = method.invoke(telephonyManager)

            val answerMethod = telephonyService.javaClass.getMethod("answerRingingCall")
            answerMethod.invoke(telephonyService)

            Log.d("CallReceiver", "Call answered automatically")
        } catch (e: Exception) {
            Log.e("CallReceiver", "Auto-answer failed: ${e.message}")
        }
    }
}