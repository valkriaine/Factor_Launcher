package com.valkriaine.glasslauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {

    private val key = "APPTILELISTPREFERENCE"
    private val firstLaunch = "isFirstLaunch"

    //todo: finish this page...
    //handles launcher initialization (background visibility, blur)
    //should also handle permission requests

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
    }

    fun confirmSettings(view : View)
    {
        HomeScreen.isFirstLaunch = false
        val sharedPreferences = getSharedPreferences(key, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(firstLaunch, HomeScreen.isFirstLaunch).apply()
        super.onBackPressed()
    }
    override fun onBackPressed() {
        if (HomeScreen.isFirstLaunch)
        {
            moveTaskToBack(true)
            exitProcess(-1)
        }
        else {
            super.onBackPressed()
        }
    }
}