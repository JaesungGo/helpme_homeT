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
    private var lastYrAngle: Int = 0 // Default value for lastYrAngle
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

        // Check if there is saved state and restore lastYrAngle if available
        if (savedInstanceState != null) {
            lastYrAngle = savedInstanceState.getInt("lastYrAngle", 0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!intent.getBooleanExtra("fromDetectSquat", false)){
            sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            // "DetectSquat"에서 돌아온 경우, "불가" 상태로 설정
            tiltValuesTextView.text = "불가"
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the lastYrAngle in the instance state bundle
        outState.putInt("lastYrAngle", lastYrAngle)
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

            if (yrAngle in 82..83) {
                tiltValuesTextView.text = "정상"

                if (lastYrAngle == 0 || lastYrAngle != yrAngle) {
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
                lastYrAngle = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) { }
}