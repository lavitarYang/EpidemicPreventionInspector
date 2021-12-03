package com.example.facemaskdetector.const

import com.example.facemaskdetector.utils.DateTool
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

object ClientSetting {

//    val subscribeTime: Long = DateTool.oneMin // 訂閱時間(超過這個時間就會自動停止訂閱並重新訂閱)
//    val publishInterval: Long = DateTool.oneSec // 發布間隔
    val kewWords: MutableList<String> by lazy { mutableListOf("ON OR OFF", "ON", "OFF") } // 接收到來自MQTT的訊息，要開始送資料的字串。
    val qos = 0
    val serverURI = "tcp://140.131.152.17:1883" // official
//    val serverURI = "tcp://broker.emqx.io:1883" // testing

//        val clientID = "yangyang" // official
    val clientID = MqttAsyncClient.generateClientId() // testing

    val subscribeTopic = "Mask"
}