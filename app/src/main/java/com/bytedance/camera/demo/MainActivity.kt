package com.bytedance.camera.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        findViewById<View>(R.id.take_picture).setOnClickListener {
//            startActivity(
//                Intent(
//                    this@MainActivity,
//                    TakePictureActivity::class.java
//                )
//            )
//        }
//        findViewById<View>(R.id.record_video).setOnClickListener {
//            startActivity(
//                Intent(
//                    this@MainActivity,
//                    SimpleVideoRecordActivity::class.java
//                )
//            )
//        }
        findViewById<View>(R.id.camera).setOnClickListener {
            if (!checkPermissionAndStartRecord()) {
                if ((ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    )
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    )
                            != PackageManager.PERMISSION_GRANTED)
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        ),
                        REQUEST_PERMISSIONS
                    )
                }
            }
        }
    }

    private fun checkPermissionAndStartRecord(): Boolean {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            )
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            )
                    == PackageManager.PERMISSION_GRANTED)
        ) {
            val intent = Intent(this@MainActivity, VideoRecordActivity::class.java)
            startActivity(intent)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionAndStartRecord()
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
    }
}