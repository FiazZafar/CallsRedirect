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

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Set the Toolbar as the ActionBar

        // Initialize the BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        // Set the initial fragment when the activity is created
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment()) // Load the default fragment
            toolbar.title = "Home" // Set the default title
        }

        // Handle BottomNavigationView item selection
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment()) // Replace with HomeFragment
                    toolbar.title = "Home" // Update Toolbar title
                    true
                }
                R.id.navigation_dashboard -> {
                    replaceFragment(CallHistoryFragment()) // Replace with DashboardFragment
                    toolbar.title = "Dashboard" // Update Toolbar title
                    true
                }
                R.id.navigation_notifications -> {
                    replaceFragment(SettingFragment()) // Replace with NotificationsFragment
                    toolbar.title = "Notifications" // Update Toolbar title
                    true
                }
                else -> false
            }
        }
    }

    // Function to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment) // Use the FrameLayout as the container
            .commit()
    }
}