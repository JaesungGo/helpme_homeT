package com.example.mocap01

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import com.example.mocap01.ml.LiteModelMovenetSingleposeThunderTfliteFloat164
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.System.currentTimeMillis
import kotlin.math.abs
import kotlin.math.atan2

class DetectSquat : AppCompatActivity() {


    // 스쿼트 정확도 , 쿨다운 시간 , 최근 스쿼트 타임을 정의
    private val SQUAT_THRESHOLD = 0.3
    private val COOLDOWN_TIME_MS = 2000L
    private var lastSquatTime = 0L
    private var lastLogTime = 0L

    private var squats = 0

    // 안드로이드 파일 관련 정의
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: LiteModelMovenetSingleposeThunderTfliteFloat164
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var handler:Handler

    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_squat)
        get_permissions()

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = LiteModelMovenetSingleposeThunderTfliteFloat164.newInstance(this)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        paint.setColor(Color.RED)

        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)
                tensorImage = imageProcessor.process(tensorImage)

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                var canvas = Canvas(mutable)
                var h = bitmap.height
                var w = bitmap.width
                var x = 0
                var circles = FloatArray(34)
                val BODY_PARTS = arrayOf(
                    "left_ankle", "left_knee", "left_hip",
                    "right_ankle", "right_knee", "right_hip",
                    "left_shoulder", "right_shoulder",
                    "left_wrist", "right_wrist",
                    "left_elbow", "right_elbow"
                )



                // 원 그리기 코드
                while(x <= 49){
                    if(outputFeature0.get(x+2) > 0.45){
                        canvas.drawCircle(outputFeature0.get(x+1)*w, outputFeature0.get(x)*h, 10f, paint)
                        circles[2 * x / 3] = outputFeature0.get(x + 1) * w
                        circles[2 * x / 3 + 1] = outputFeature0.get(x) * h
                    }
                    x += 3
                }
                // 선 어레이에 정의
                val edges = arrayOf(
                    Pair(0, 1),
                    Pair(0, 2),
                    Pair(1, 3),
                    Pair(2, 4),
                    Pair(0, 5),
                    Pair(0, 6),
                    Pair(5, 7),
                    Pair(7, 9),
                    Pair(6, 8),
                    Pair(8, 10),
                    Pair(5, 6),
                    Pair(5, 11),
                    Pair(6, 12),
                    Pair(11, 12),
                    Pair(11, 13),
                    Pair(13, 15),
                    Pair(12, 14),
                    Pair(14, 16)
                )
                // 관절 - 관절 이루는 선 그리기
                for ((i, j) in edges) {
                    val x1 = circles[2 * i]
                    val y1 = circles[2 * i + 1]
                    val x2 = circles[2 * j]
                    val y2 = circles[2 * j + 1]

                    if (x1 != 0f && y1 != 0f && x2 != 0f && y2 != 0f) {
                        canvas.drawLine(x1, y1, x2, y2, paint)
                    }
                }

                // 오른 무릎과 엉덩이의 인덱스
                val LEFT_KNEE_INDEX = BODY_PARTS.indexOf("left_knee")
                val LEFT_HIP_INDEX = BODY_PARTS.indexOf("left_hip")
                val RIGHT_KNEE_INDEX = BODY_PARTS.indexOf("right_knee")
                val RIGHT_HIP_INDEX = BODY_PARTS.indexOf("right_hip")

                // 각 관절의 표시값을 받아온다.
                val leftKneeX = circles[2 * LEFT_KNEE_INDEX]
                val leftKneeY = circles[2 * LEFT_KNEE_INDEX + 1]
                val leftHipX = circles[2 * LEFT_HIP_INDEX]
                val leftHipY = circles[2 * LEFT_HIP_INDEX + 1]
                val rightKneeX = circles[2 * RIGHT_KNEE_INDEX]
                val rightKneeY = circles[2 * RIGHT_KNEE_INDEX + 1]
                val rightHipX = circles[2 * RIGHT_HIP_INDEX]
                val rightHipY = circles[2 * RIGHT_HIP_INDEX + 1]



                // 스쿼트 측정 로직
                val leftLegAngle = Math.toDegrees(
                    atan2(leftKneeY - leftHipY, leftKneeX - leftHipX).toDouble()
                )
                val rightLegAngle = Math.toDegrees(
                    atan2(rightKneeY - rightHipY, rightKneeX - rightHipX).toDouble()
                )
                val squatValue = 1 - abs((leftLegAngle + rightLegAngle) / 180 - 1)

                val currentTime = currentTimeMillis()
                if (currentTime - lastSquatTime >= COOLDOWN_TIME_MS && squatValue > SQUAT_THRESHOLD)
                {
                    squats++ // 스쿼트 횟수 증가
                    lastSquatTime = currentTime // 마지막 스쿼트 시간 업데이트
                    Log.d(TAG, "Squat detected! Squat value: $squatValue")
                    Log.d(TAG, "Number of squats: $squats")
                }

                val squatsTextView = findViewById<TextView>(R.id.squatsTextView)
                squatsTextView.text = "Number of squats: $squats"


                imageView.setImageBitmap(mutable)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                var captureRequest = p0.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                captureRequest.addTarget(surface)
                p0.createCaptureSession(listOf(surface), object:CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }

    fun get_permissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }
    override fun onRequestPermissionsResult(  requestCode: Int, permissions: Array<out String>, grantResults: IntArray  ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) get_permissions()
    }
}