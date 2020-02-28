package com.valkriaine.glasslauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.settings_activity.*
import kotlin.system.exitProcess


class SettingsActivity : AppCompatActivity() {

    private val key = "APPTILELISTPREFERENCE"
    private val firstLaunch = "isFirstLaunch"
    private var isStorageAccessEnabled = false
    private val requestId = 0
    private lateinit var storageSwitch : Switch

    //todo: better design this page

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        storageSwitch = StorageSwitch
        storageSwitch.isChecked = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

    }

    fun confirmSettings(view : View)
    {
        confirm()
    }

    fun confirm()
    {
        if (isStorageAccessEnabled) {
            HomeScreen.isFirstLaunch = false
            val sharedPreferences = getSharedPreferences(key, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(firstLaunch, HomeScreen.isFirstLaunch).apply()
            startActivity(Intent(this, HomeScreen :: class.java))
            finish()
        }
    }

    fun permissionRequest(view : View)
    {
        if (!isStorageAccessEnabled)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestId)
        else
            storageSwitch.isChecked = isStorageAccessEnabled
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)
    {
        when (requestCode) {
            requestId -> {
                // If request is cancelled, the result arrays are empty.
                isStorageAccessEnabled = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                return
            }
        }
        storageSwitch.isChecked = isStorageAccessEnabled
    }

    override fun onBackPressed() {
        if (HomeScreen.isFirstLaunch && !isStorageAccessEnabled)
        {
            moveTaskToBack(true)
            exitProcess(-1)
        }
        else if (isStorageAccessEnabled)
        {
           confirm()
        }
        else
        {
            super.onBackPressed()
        }
    }
}