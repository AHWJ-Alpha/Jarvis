package com.jarvis.app.runtime

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

class CameraCaptureActivity : Activity() {
    private var output: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA) else launchCamera()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) launchCamera() else finish()
    }

    private fun launchCamera() {
        val directory = File(filesDir, "jarvis/captures").apply { mkdirs() }
        output = File(directory, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.files", output!!)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_CAPTURE)
    }

    @Deprecated("Android activity result compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE) {
            sendBroadcast(Intent(ACTION_CAPTURED).putExtra("path", output?.absolutePath).putExtra("success", resultCode == RESULT_OK))
            finish()
        }
    }

    companion object {
        const val ACTION_CAPTURED = "com.jarvis.app.CAMERA_CAPTURED"
        private const val REQUEST_CAMERA = 4101
        private const val REQUEST_CAPTURE = 4102
    }
}
