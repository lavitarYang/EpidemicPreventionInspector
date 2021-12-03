package com.example.facemaskdetector.viewmodel

import androidx.camera.core.*
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

class CameraFacingRepository @Inject constructor() {
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT // 預設應該是要往前照，但是因為初始設定的關係所以要設定反過來。

    val cameraController: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    // 判斷鏡頭的方向，並旋轉。
    fun changeCameraFacing() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        cameraController.postValue(lensFacing)
    }

}
