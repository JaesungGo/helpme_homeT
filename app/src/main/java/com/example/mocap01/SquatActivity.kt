package com.example.mocap01


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class SquatActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var tiltValuesTextView: TextView
    private var lastYrAngle: Int? = null
    private val handler = Handler(Looper.getMainLooper())


    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_squat)
        tiltValuesTextView = findViewById(R.id.tiltValuesTextView)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val r = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

            val xrAngle = (90 - acos(x / r) * 180 / PI).toInt()
            val yrAngle = (90 - acos(y / r) * 180 / PI).toInt()
            Log.d("TAG", "onSensorChanged: xrAngle: $xrAngle, yrAngle: $yrAngle")

            if (yrAngle in 60..80) {
                tiltValuesTextView.text = "정상"

                if (lastYrAngle == null || lastYrAngle != yrAngle) {
                    lastYrAngle = yrAngle
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({
                        sensorManager.unregisterListener(this)
                        val intent = Intent(this, DetectSquat::class.java)
                        startActivity(intent)
                    }, 3000)
                }

            } else {
                tiltValuesTextView.text = "불가"
                handler.removeCallbacksAndMessages(null)
                lastYrAngle = null
            }
        }
    }



    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) { }
}