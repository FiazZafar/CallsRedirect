package com.sahiwal.callsredirect.activities

import com.sahiwal.callsredirect.R
import com.sahiwal.callsredirect.fragments.HomeFragment
import com.sahiwal.callsredirect.fragments.SettingFragment

import com.sahiwal.callsredirect.fragments.CallHistoryFragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    // Permissions required for the app
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.USE_SIP,
        Manifest.permission.INTERNET,
        Manifest.permission.READ_PHONE_STATE, // Add for call detection
        Manifest.permission.ANSWER_PHONE_CALLS // Add for auto-answer
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.nav_view)

        // Set up the Toolbar
        setSupportActionBar(toolbar)

        // Check and request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }

        // Set up the BottomNavigationView
        setupBottomNavigation()

        // Load the default fragment if no saved state exists
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            toolbar.title = "Home"
        }
    }

    // Check if all required permissions are granted
    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("MainActivity", "All permissions granted")
            } else {
                val permanentlyDenied = permissions.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }

                if (permanentlyDenied) {
//                    showSettingsDialog()
                } else {
                    // Ensure the dialog is not called repeatedly
                    if (!hasPermissions()) {
                        showPermissionExplanation()
                    }
                }
            }
        }
    }

//    private fun showSettingsDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Permissions Required")
//            .setMessage("You have denied some permissions permanently. Please enable them in settings to use this feature.")
//            .setPositiveButton("Go to Settings") { _, _ ->
//                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                val uri = Uri.fromParts("package", packageName, null)
//                intent.data = uri
//                startActivity(intent)
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()
//    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs phone and audio permissions to detect and answer calls.")
            .setPositiveButton("OK") { _, _ ->
                // Do not request permissions here, just show the dialog
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        // Request permissions only after the dialog is dismissed
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    // Set up BottomNavigationView item selection
    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    toolbar.title = "Home"
                    true
                }
                R.id.navigation_dashboard -> {
                    replaceFragment(CallHistoryFragment())
                    toolbar.title = "Dashboard"
                    true
                }
                R.id.navigation_notifications -> {
                    replaceFragment(SettingFragment())
                    toolbar.title = "Settings"
                    true
                }
                else -> false
            }
        }
    }

    // Replace the current fragment with a new one
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}