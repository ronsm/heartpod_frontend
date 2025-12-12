package org.hwu.care.healthub

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttClientManager(private val context: Context) {

    private var mqttClient: MqttAndroidClient? = null
    private val TAG = "MqttClientManager"
    private val SERVER_URI = "tcp://192.168.2.150:1883" // Pi 2 Broker
    private val CLIENT_ID = "TemiHealthHubApp"

    // Callback for incoming messages
    var onMessageReceived: ((topic: String, message: String) -> Unit)? = null

    fun connect() {
        mqttClient = MqttAndroidClient(context, SERVER_URI, CLIENT_ID)
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
        }

        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connected to MQTT Broker")
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to connect to MQTT Broker", exception)
                }
            })

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "Connection lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: return
                    Log.d(TAG, "Message received: $topic -> $payload")
                    topic?.let { onMessageReceived?.invoke(it, payload) }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Not used for now
                }
            })

        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopics() {
        try {
            mqttClient?.subscribe("temi/command", 1)
            Log.d(TAG, "Subscribed to temi/command")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient?.publish(topic, mqttMessage)
            Log.d(TAG, "Published to $topic: $message")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
