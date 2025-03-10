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
import android.content.pm.PackageManager

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    // Permissions required for the app
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.USE_SIP,
        Manifest.permission.INTERNET
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
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, proceed with app logic
            } else {
                // Permissions denied, show a message or disable functionality
            }
        }
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