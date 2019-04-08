package com.example.accelerometer2mqtt

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient

val TAG = "bananaumai"

fun sprintSensorEvent(event: SensorEvent) =
    "${event.timestamp / 1000000L}: ${event.values.joinToString(", ")}"

class MainActivity : AppCompatActivity() {

    lateinit var manager: SensorManager
    lateinit var loggingListener: SensorEventListener
    lateinit var mqttListener: MQTTListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        loggingListener = LoggingListener()
        mqttListener = MQTTListener(this)

        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        manager.registerListener(loggingListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        manager.registerListener(mqttListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.unregisterListener(loggingListener)
        manager.unregisterListener(mqttListener)
    }
}

class LoggingListener : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(TAG, "sensor changed")

        if (event?.sensor?.getType() != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        Log.i(TAG, "accelerometer event")
        Log.i(TAG, sprintSensorEvent(event))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
}

class MQTTListener(val context: Context) : SensorEventListener {
    private val client = MqttAndroidClient(context, "tcp://10.0.2.2:1883", MqttClient.generateClientId())

    init {
        Log.d(TAG, "mqtt connect!")
        client.connect(null, object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(TAG, "connection failed")
                throw exception!!
            }
        })
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.getType() != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        if (client.isConnected) {
            Log.d(TAG, "send to MQTT")
            client.publish("test/topic",
                sprintSensorEvent(event).toByteArray(), 0, false)
        } else {
            Log.d(TAG, "not connected to mqtt")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
}