package com.example.facemaskdetector.viewmodel

import com.example.facemaskdetector.const.ClientSetting
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class MQTTRepository @Inject constructor() {

    @Inject
    lateinit var mqttClient: MqttAndroidClient

    private var mustSend: AtomicBoolean = AtomicBoolean(true) // 2021/11/15 改為收到訊息只送一次  //主防護

    private var haveSent = false // 2021/11/15 改為收到訊息只送一次 // 副防護

    fun MqttMessage.isKeyWord(stringList: MutableList<String>): Boolean {
        stringList.forEach { if (it == this.toString().trim()) return true }
        return false
    }

    fun connect() {
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Timber.d("Receive message: ${message.toString()} from topic: $topic")
                mustSend = AtomicBoolean((message?.isKeyWord(ClientSetting.kewWords) == true && topic == ClientSetting.subscribeTopic) && !mustSend.get()).apply {
                    haveSent = false
                }

                Timber.d("messageArrived,此時的mustSend是=>${mustSend},message是=>$message,,,topic是=>$topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Timber.d("Connection lost ${cause.toString()}")
                connect()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Connection success")
                    subscribe(ClientSetting.subscribeTopic)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Connection failure")
                    exception?.printStackTrace()
                    connect()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribe(topic: String, qos: Int = ClientSetting.qos) {
        try {

            mqttClient.subscribe(topic, qos, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Subscribing to $topic")
//                    subscribeStatus = SubscribeStatus.Subscribing
                    mustSend = AtomicBoolean(false) //初始化
//                    haveSended = false //初始化
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe() {
        unsubscribe(ClientSetting.subscribeTopic)
    }

    private fun unsubscribe(topic: String) {
        try {
            mqttClient.unsubscribe(topic, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Unsubscribed to $topic")
                    mustSend = AtomicBoolean(false) //初始化
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Failed to unsubscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(msg: String) {
//        Timber.e("外層發布收到的內容是=>${msg}")
        if (mustSend.get() && !haveSent) {
//            Timber.e("mustSend是=>${mustSend}")
            publish(ClientSetting.subscribeTopic, msg)
        }
    }

    private fun publish(topic: String, msg: String, qos: Int = ClientSetting.qos, retained: Boolean = false) {
        try {
            val message = MqttMessage().apply {
                payload = msg.toByteArray()
                this.qos = qos
                isRetained = retained
            }
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("$msg published to $topic")
                    haveSent = true // 2021/11/15 改為收到訊息只送一次 // 雙重防護
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Failed to publish $msg to $topic")
                    exception?.printStackTrace()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}