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
import androidx.lifecycle.Observer
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contact_sync_layout.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView

class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    companion object {
        const val MY_PERMISSIONS_REQUEST_CAMERA = 1
        const val MY_PERMISSIONS_REQUEST_PHONE_STATE = 2
        const val ASPECT_TOLERANCE = 0.5f
    }

    private lateinit var contactSyncLayoutSet: ConstraintSet
    private val viewModel by viewModels<ContactsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        content.scanner.setFormats(listOf(BarcodeFormat.QR_CODE))
        // this param will make your HUAWEI phone, Lenovo TAB4 10" works great!
        content.scanner.setAspectTolerance(ASPECT_TOLERANCE)
        content.givePermission.setOnClickListener { askForCameraPermissions() }
        content.removeConfiguration.setOnClickListener { viewModel.deleteConfiguration() }
        content.synchronize.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableSync(isChecked)
        }

        contactSyncLayoutSet = ConstraintSet().apply { clone(content as ConstraintLayout) }

        viewModel.syncEnabledLive.observe(this, Observer { enabled ->
            onSyncEnabledChanged(enabled)
        })

        viewModel.isConfiguredLive.observe(this, Observer { configured ->
            updateView(configured, isCameraPermissionGranted())
        })
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.isConfigured && !isCameraPermissionGranted()) {
            askForCameraPermissions()
        }
    }

    private fun onSyncEnabledChanged(enabled: Boolean) {
        if (enabled && !isReadPhoneStatePermissionGranted()) {
            content.synchronize.isChecked = false
            askForPhoneStatePermissions()
        } else {
            content.synchronize.isChecked = enabled
            content.configuredMessage.setText(
                if (enabled) R.string.syncEnabledMessage else R.string.syncDisabledMessage
            )
        }
    }

    private fun askForCameraPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            MY_PERMISSIONS_REQUEST_CAMERA
        )
    }

    private fun askForPhoneStatePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
            MY_PERMISSIONS_REQUEST_PHONE_STATE
        )
    }

    private fun updateView(configured: Boolean, cameraPermissionGranted: Boolean) {

        if (configured) {
            content.scanner.stopCamera()
            content.scanner.stopCameraPreview()
        } else {
            content.scanner.resumeCameraPreview(this)
            content.scanner.startCamera()
        }

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

    private fun isCameraPermissionGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    private fun isReadPhoneStatePermissionGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_PHONE_STATE) &&
                isPermissionGranted(Manifest.permission.READ_CALL_LOG)
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
        val granted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                updateView(viewModel.isConfiguredLive.value == true, granted)
            }
            MY_PERMISSIONS_REQUEST_PHONE_STATE -> {
                // If request is cancelled, the result arrays are empty.
                onSyncEnabledChanged(granted)
            }
        }
    }
}
