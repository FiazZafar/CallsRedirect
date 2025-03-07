package com.sahiwal.callsredirect.BroadCast


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat.startActivity

class CallReceivers(private val context: Context) : PhoneStateListener() {

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d("CallReceiver", "Incoming call from: $phoneNumber")
                redirectCallToMillisAI("9876543210") // Replace with MillisAI's number
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d("CallReceiver", "Call ended")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d("CallReceiver", "Call answered")
            }
        }
    }
    private fun redirectCallToMillisAI(millisAIPhoneNumber: String) {
        try {
            // End the current call
            val telephonyClass = Class.forName("com.android.internal.telephony.ITelephony")
            val telephonyStub = telephonyClass.getDeclaredMethod("getITelephony")
            telephonyStub.isAccessible = true
            val telephonyService = telephonyStub.invoke(null)
            val endCallMethod = telephonyClass.getDeclaredMethod("endCall")
            endCallMethod.invoke(telephonyService)

            // Dial MillisAI's number
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$millisAIPhoneNumber")
//            startActivity(intent)
        } catch (e: Exception) {
            Log.e("CallReceiver", "Failed to redirect call", e)
        }
    }
}