package com.storyous.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contact_sync_layout.view.*

class MainActivity : AppCompatActivity(), BarcodeCallback {

    companion object {
        const val MY_PERMISSIONS_REQUEST_CAMERA = 1
        const val MY_PERMISSIONS_REQUEST_PHONE_STATE = 2
    }

    private lateinit var contactSyncLayoutSet: ConstraintSet
    private val viewModel by viewModels<CallSyncViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        content.configInstructions.setOrderedListText(
            resources.getStringArray(R.array.config_instructions),
            true
        )
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

        content.scanner.viewFinder.setLaserVisibility(false)
        content.scanner.initializeFromIntent(intent)
        content.scanner.setStatusText("")
        content.scanner.barcodeView.decoderFactory =
            DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        content.scanner.decodeContinuous(this)
    }

    override fun onStart() {
        super.onStart()

        if (!viewModel.isConfigured && !isCameraPermissionGranted()) {
            askForCameraPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isConfigured) {
            content.scanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        content.scanner.pause()
    }

    private fun onSyncEnabledChanged(enabled: Boolean) {
        if (enabled && !isReadPhoneStatePermissionGranted()) {
            content.synchronize.isChecked = false
            askForPhoneStatePermissions()
        } else {
            content.synchronize.isChecked = enabled
            if (enabled) {
                content.configuredMessage.setOrderedListText(
                    resources.getStringArray(R.array.create_order_instructions),
                    true
                )
            } else {
                content.configuredMessage.setText(R.string.syncDisabledMessage)
            }
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
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.INTERNET
            ),
            MY_PERMISSIONS_REQUEST_PHONE_STATE
        )
    }

    private fun updateView(configured: Boolean, cameraPermissionGranted: Boolean) {

        if (configured) {
            content.scanner.pause()
        } else {
            content.scanner.resume()
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
            R.id.configurate,
            if (!configured) View.VISIBLE else View.INVISIBLE
        )

        TransitionManager.beginDelayedTransition(content as ConstraintLayout)
        contactSyncLayoutSet.applyTo(content as ConstraintLayout)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    private fun isReadPhoneStatePermissionGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_PHONE_STATE) &&
                isPermissionGranted(Manifest.permission.READ_CALL_LOG) &&
                isPermissionGranted(Manifest.permission.INTERNET)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // If request is cancelled, the result arrays are empty.
        val granted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> updateView(viewModel.isConfigured, granted)
            MY_PERMISSIONS_REQUEST_PHONE_STATE -> onSyncEnabledChanged(granted)
        }
    }

    override fun barcodeResult(result: BarcodeResult) {
        result.text?.also { viewModel.onQRCodeReceived(result.text) }
    }
}
