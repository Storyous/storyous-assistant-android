package com.storyous.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contact_sync_layout.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView

class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    companion object {
        const val MY_PERMISSIONS_REQUEST_CAMERA = 1
    }

    private lateinit var contactSyncLayoutSet: ConstraintSet
    private val viewModel by viewModels<ContactsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        content.scanner.setFormats(listOf(BarcodeFormat.QR_CODE))
        content.givePermission.setOnClickListener { askForCameraPermissions() }
        content.removeConfiguration.setOnClickListener { viewModel.deleteConfiguration() }
        viewModel.syncEnabled.observe(this, Observer { enabled ->
            content.synchronize.isChecked = enabled
            content.configuredMessage.setText(
                if (enabled) R.string.syncEnabledMessage else R.string.syncDisabledMessage
            )
        })
        content.synchronize.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableSync(isChecked)
        }

        contactSyncLayoutSet = ConstraintSet().apply { clone(content as ConstraintLayout) }

        viewModel.isConfigured.observe(this, Observer { configured ->
            updateView(configured, isCameraPermissionGranted())
        })

        if (viewModel.isConfigured.value == false && !isCameraPermissionGranted()) {
            askForCameraPermissions()
        }
    }

    private fun askForCameraPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            MY_PERMISSIONS_REQUEST_CAMERA
        )
    }

    private fun updateView(configured: Boolean, cameraPermissionGranted: Boolean) {
        contactSyncLayoutSet.setVisibility(
            R.id.configured,
            if (configured) View.VISIBLE else View.GONE
        )

        contactSyncLayoutSet.setVisibility(
            R.id.noPermissions,
            if (!configured && !cameraPermissionGranted) View.VISIBLE else View.GONE
        )

        contactSyncLayoutSet.setVisibility(
            R.id.scanner,
            if (!configured && cameraPermissionGranted) View.VISIBLE else View.INVISIBLE
        )

        TransitionManager.beginDelayedTransition(content as ConstraintLayout)
        contactSyncLayoutSet.applyTo(content as ConstraintLayout)
    }

    override fun onResume() {
        super.onResume()
        content.scanner.setResultHandler(this)
        content.scanner.startCamera()
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onPause() {
        super.onPause()
        content.scanner.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        viewModel.onContactsConfigReceived(rawResult.text)
        Toast.makeText(this, "scanned ${rawResult.text}", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                updateView(viewModel.isConfigured.value == true, granted)
                return
            }
        }
    }
}
