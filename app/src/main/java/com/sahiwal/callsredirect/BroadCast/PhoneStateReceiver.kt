package com.sahiwal.callsredirect.BroadCast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast


class PhoneStateReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION") // Suppress deprecation warning for EXTRA_INCOMING_NUMBER
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "BroadcastReceiver triggered with action: ${intent.action}", Toast.LENGTH_LONG).show()
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d("CALLTAG", "Incoming number: $incomingNumber")
            Toast.makeText(context, "Call state: $state", Toast.LENGTH_LONG).show()

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Toast.makeText(context, "Incoming call detected", Toast.LENGTH_LONG).show()
                    if (incomingNumber != null) {
                        Toast.makeText(context, "Incoming call from: $incomingNumber", Toast.LENGTH_LONG).show()

                    } else {
                        Toast.makeText(context, "Incoming call number not available (deprecated in Android 10+)", Toast.LENGTH_LONG).show()

                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d("CALLTAG", "Call answered or ongoing")
                    Toast.makeText(context, "Incoming call number not available (deprecated in Android 10+)", Toast.LENGTH_LONG).show()

                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d("CALLTAG", "Call ended")
                }
                else -> {
                    Log.d("CALLTAG", "Unknown call state: $state")
                }
            }
        } else {
            Log.d("CALLTAG", "Received intent with action: ${intent.action}")
        }
    }
}