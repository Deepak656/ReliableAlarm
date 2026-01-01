package com.reliablealarm.app.waketasks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm

class QrWakeTask : WakeTask {

    private lateinit var context: Context
    private lateinit var alarm: Alarm
    private lateinit var onCompleteCallback: () -> Unit

    private var required = 1
    private var scannedCount = 0
    private val scannedValues = mutableSetOf<String>()

    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var feedback: TextView? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null


    override fun getName() = "QR Code Scan"

    override fun getInstructions(): String =
        "Scan $required QR code(s) to dismiss alarm"

    override fun initialize(
        context: Context,
        alarm: Alarm,
        onComplete: () -> Unit
    ) {
        this.context = context
        this.alarm = alarm
        this.onCompleteCallback = onComplete

        val cfg = alarm.taskSettings["task_scanqr"] as? TaskConfig.QrConfig
        required = cfg?.qrCodesRequired ?: 1
    }

    override fun createView(container: ViewGroup): View {
        val view = LayoutInflater.from(container.context)
            .inflate(R.layout.task_scanqr, container, false)

        progressBar = view.findViewById(R.id.qrProgress)
        progressText = view.findViewById(R.id.qrProgressText)
        feedback = view.findViewById(R.id.qrFeedback)
        previewView = view.findViewById(R.id.qrCameraPreview)

        progressBar?.max = required
        refreshUI()
        startCamera()
        return view
    }

    override fun start() {}
    override fun pause() {}
    override fun resume() {}
    override fun cleanup() {
        cameraProvider?.unbindAll()
    }
    override fun isCompleted() = scannedCount >= required

    override fun getProgress(): Float =
        scannedCount.toFloat() / required

    private fun refreshUI() {
        progressBar?.progress = scannedCount
        progressText?.text = "$scannedCount / $required scanned"
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

            val scanner = BarcodeScanning.getClient()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->

                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val value = barcode.rawValue ?: continue
                                onQrScanned(value)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner(),
                cameraSelector,
                preview,
                analysis
            )

        }, ContextCompat.getMainExecutor(context))
    }
    private fun onQrScanned(value: String) {
        if (scannedValues.contains(value)) return

        scannedValues.add(value)
        scannedCount++
        feedback?.text = "Scanned: $value"
        refreshUI()

        if (scannedCount >= required) {
            onCompleteCallback.invoke()
        }
    }
    private fun lifecycleOwner(): LifecycleOwner {
        return context as LifecycleOwner
    }


}
