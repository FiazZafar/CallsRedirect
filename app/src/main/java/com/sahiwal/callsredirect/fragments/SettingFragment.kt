package com.sahiwal.callsredirect.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sahiwal.callsredirect.R
import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat

class SettingFragment : Fragment() {

    private lateinit var editTextAgentId: EditText
    private lateinit var editTextPublicKey: EditText
    private lateinit var editTextServerUrl: EditText
    private lateinit var saveSettingBtn: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Initialize views
        editTextAgentId = view.findViewById(R.id.agent_id)
        editTextPublicKey = view.findViewById(R.id.public_key)
        editTextServerUrl = view.findViewById(R.id.server_url)
        saveSettingBtn = view.findViewById(R.id.saveSettingBtn)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("MySettings", Context.MODE_PRIVATE)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved data
        val savedAgentId = sharedPreferences.getString("agentId", "")
        val savedPublicKey = sharedPreferences.getString("publicKey", "")
        val savedServerUrl = sharedPreferences.getString("serverUrl", "")

//        editTextServerUrl.setHint(savedServerUrl)


        saveSettingBtn.setOnClickListener {
            val agentId = editTextAgentId.text.toString()
            val publicKey = editTextPublicKey.text.toString()
            val serverUrl = editTextServerUrl.text.toString()

            if (agentId.isEmpty() || publicKey.isEmpty() || serverUrl.isEmpty()) {
                Toast.makeText(requireContext(), "Configuration missing. Please check settings.", Toast.LENGTH_SHORT).show()
            } else if (serverUrl.equals("https://api-west.millis.ai/")){
                // Save to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("agentId", agentId)
                editor.putString("publicKey", publicKey)
                editor.putString("serverUrl", serverUrl)
                editor.apply()

                editTextAgentId.setText("")
                editTextPublicKey.setText("")
                editTextServerUrl.setText("")

                Toast.makeText(requireContext(), "Settings Saved!", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(requireContext(), "Invalid Url", Toast.LENGTH_SHORT).show()

            }
        }
    }
}