package com.example.facemaskdetector.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.facemaskdetector.R
import com.example.facemaskdetector.databinding.ActivityMainBinding
import com.example.facemaskdetector.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


//import com.example.facemaskdetector.ml.FackMaskDetection

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    private lateinit var mBinding: ActivityMainBinding

    private var preview: Preview? = null
//    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

//    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

//    private var camera: Camera? = null

//    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.lifecycleOwner = this
//        mBinding.vm = viewModel // 使用ViewBinding，而非DataBinding。

        initObservable()

        initEvent()

//        initView()

        viewModel.connect()

        if (!allPermissionsGranted) {
            requireCameraPermission()
        } else {
            setupCamera()
        }
    }

    private fun initObservable() {
        viewModel.liveCameraController.observe(this, {
//            Timber.d("收到要轉的方向是=>${it}")
            if (it != CameraSelector.LENS_FACING_FRONT && it != CameraSelector.LENS_FACING_BACK) { // 沒有前鏡頭也沒有後鏡頭，關閉相機按紐
                mBinding.btnCameraLensFace.isEnabled = false
                return@observe
            }

            mBinding.btnCameraLensFace.setImageDrawable(
                AppCompatResources.getDrawable(
                    applicationContext,
                    if (it == CameraSelector.LENS_FACING_FRONT) R.drawable.ic_baseline_camera_rear_24 else R.drawable.ic_baseline_camera_front_24
                )
            )

            setupCameraUseCases(it)
        })

        viewModel.liveAnalysisResultData.observe(this, {
            var MLresult: String = "" // return with_mask or no_mask
            it.firstOrNull()?.let { category ->
                mBinding.apply {
                    tvOutput.text = category.label
                    tvOutput.setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            if (category.label == "without_mask") R.color.red else R.color.green

                        )
                    )

                    overlay.background = ContextCompat.getDrawable(
                        this@MainActivity,
                        if (category.label == "without_mask") R.drawable.red_border else R.drawable.green_border
                    )

                    pbOutput.progressTintList = AppCompatResources.getColorStateList(
                        applicationContext,
                        if (category.label == "without_mask") R.color.red else R.color.green
                    )
                    pbOutput.progress = (category.score * 100).toInt()
                    MLresult = if (category.label == "without_mask") "OFF" else "ON"
                    MQTT_SERVICE(MLresult)
                }
            }

        })
    }

    private fun initEvent() {
        mBinding.btnCameraLensFace.setOnClickListener {
            viewModel.changeCameraFacing()
        }
    }

    private fun initView() {
        mBinding.btnCameraLensFace.isEnabled = runCatching {
            (hasBackCamera && hasFrontCamera).apply {
                return@runCatching this
            }
        }.isFailure.not()
    }

    private fun MQTT_SERVICE(MLresult: String) { // Sending Result to MQTT SERVICE, including Publisher, Subscriber
        viewModel.publish(MLresult)
//         println(MLresult) // Debug output for Face-Mask Detection
    }

    private fun setupCamera() {

        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()

            val lensFacing = when {
            hasFrontCamera -> CameraSelector.LENS_FACING_FRONT
            hasBackCamera -> CameraSelector.LENS_FACING_BACK
            else -> throw IllegalStateException("No cameras on this devices")
        }

            initView() // 應該要在Provider完成以後才能設定按鈕是否Enable才對
//            setupCameraControllers()
            setupCameraUseCases(lensFacing) // 預設使用鏡頭
        }, ContextCompat.getMainExecutor(this))
    }

    //設定攝影機要使用哪一個方向的，處理後指定值。
    private fun setupCameraUseCases(lensFacing: Int) {
        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(lensFacing)
                .build()

        val metrics: DisplayMetrics =
            DisplayMetrics().also { mBinding.previewView.display.getRealMetrics(it) }
        val rotation: Int = mBinding.previewView.display.rotation
        val screenAspectRatio: Int = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        cameraProvider?.unbindAll()
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, viewModel.getAnalyst())
            }

        try {
            cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(mBinding.previewView.createSurfaceProvider())
        } catch (exc: Exception) {
            Timber.e(exc, "Use case binding failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unsubscribe()
//        viewModel.disconnect() // 於此呼叫的話，螢幕轉向會造成crash閃退。
    }

//    override fun onConfigurationChanged(newConfig: Configuration) { // ??? 沒有設定 android:configChanges為何要覆寫這個方法？永遠不會被觸發阿...
//        super.onConfigurationChanged(newConfig)
////        setupCameraControllers()
//    }

    private val hasBackCamera: Boolean
        get() {
            return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
        }

    private val hasFrontCamera: Boolean
        get() {
            return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
        }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio: Double = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun requireCameraPermission() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private val allPermissionsGranted: Boolean
        get() {
            return REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(
                    baseContext, it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        grantedCameraPermission(requestCode)
    }

    private fun grantedCameraPermission(requestCode: Int) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted) {
                setupCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val RATIO_4_3_VALUE: Double = 4.0 / 3.0
        private const val RATIO_16_9_VALUE: Double = 16.0 / 9.0

        //        private const val TAG = "Fask-Mask-Detection"
        private const val REQUEST_CODE_PERMISSIONS = 0x98
        private val REQUIRED_PERMISSIONS: Array<String> = arrayOf(Manifest.permission.CAMERA)
    }

}