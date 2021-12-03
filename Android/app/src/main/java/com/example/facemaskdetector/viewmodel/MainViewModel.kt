package com.example.facemaskdetector.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.support.label.Category


class MainViewModel @ViewModelInject constructor(private val detectorRepository: DetectorRepository,
                                                 private val cameraFacingRepository: CameraFacingRepository,
                                                 private val mqttRepository: MQTTRepository
) : ViewModel() {

    //相機轉向(前鏡頭/後鏡頭)
    val liveCameraController: LiveData<Int> = cameraFacingRepository.cameraController

    fun changeCameraFacing() = cameraFacingRepository.changeCameraFacing()

    //相機掃描判斷結果
    val liveAnalysisResultData: LiveData<List<Category>> = detectorRepository.resultLiveData

    fun getAnalyst() = detectorRepository.getAnalyst()

    //MQTT
    fun connect()= mqttRepository.connect()

    fun publish(message:String) = mqttRepository.publish(message)

    fun unsubscribe() = mqttRepository.unsubscribe()

    fun disconnect() = mqttRepository.disconnect() // disconnect應該在Application層，但是目前沒辦法在application層去disconnect。
                                                   // 因為相同的ClientID，disconnect以後就不能重新連線，連判斷是否有connect都不行
                                                   // 可以用Service onBind去解決，但是時間不夠。
                                                   // 是說如果用MVC架構就不會有這問題，那為什麼要自找這個麻煩呢？(干Orz)

}