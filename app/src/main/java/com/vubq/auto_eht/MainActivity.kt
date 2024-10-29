package com.vubq.auto_eht

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_main)

        checkDrawOverlayPermission(this)
        checkStoragePermission(this)
        checkNotificationPermission(this)

        val startFloatingButton: Button = findViewById(R.id.startFloatingButton)
        startFloatingButton.setOnClickListener {
            startService(Intent(this, FloatingWindowService::class.java))
        }

        val stopFloatingButton: Button = findViewById(R.id.stopFloatingButton)
        stopFloatingButton.setOnClickListener {
            stopService(Intent(this, FloatingWindowService::class.java))
        }
    }

    private fun checkDrawOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "You need to grant permission to draw overlays.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
        }
    }

    private fun checkStoragePermission(context: Context) {
        // Kiểm tra nếu quyền MANAGE_EXTERNAL_STORAGE chưa được cấp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Yêu cầu quyền quản lý toàn bộ bộ nhớ
                Toast.makeText(
                    context,
                    "You need to grant permission to manage all files.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent)
            }
        }
    }

    private fun checkNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                Toast.makeText(
                    context,
                    "You need to grant notification permission.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }
        } else {
            // Đối với các phiên bản Android cũ hơn, không cần kiểm tra quyền thông báo
            Toast.makeText(
                context,
                "Notification permission is not required for this Android version.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
