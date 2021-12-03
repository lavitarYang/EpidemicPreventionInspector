package com.example.facemaskdetector.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.example.facemaskdetector.ml.FackMaskDetection
import com.example.facemaskdetector.oriclass.BitmapOutputAnalysis
import com.example.facemaskdetector.oriclass.CameraBitmapOutputListener
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import javax.inject.Inject

class DetectorRepository @Inject constructor() {

    @Inject
    lateinit var analysis: BitmapOutputAnalysis

    @Inject
    lateinit var faceMaskDetection: FackMaskDetection

    val resultLiveData: MutableLiveData<List<Category>> by lazy { MutableLiveData<List<Category>>() }

    fun getAnalyst(): BitmapOutputAnalysis {
        val listener: CameraBitmapOutputListener = { bitmap -> setupMLOutput(bitmap) }
        return analysis.apply { setAction(listener) }
    }

    //設定AI判斷後的輸出內容
    private fun setupMLOutput(bitmap: Bitmap) {
        val tensorImage: TensorImage = TensorImage.fromBitmap(bitmap)
        val result: FackMaskDetection.Outputs = faceMaskDetection.process(tensorImage)
        val output: List<Category> =
            result.probabilityAsCategoryList.apply {
                sortByDescending { res -> res.score }
            }
        resultLiveData.postValue(output)
    }

}
