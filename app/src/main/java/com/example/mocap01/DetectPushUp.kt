package com.example.mocap01

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import com.example.mocap01.ml.LiteModelMovenetSingleposeThunderTfliteFloat164
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.System.currentTimeMillis
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class DetectPushUp : AppCompatActivity() {

    // Firebase Authentication 인스턴스 가져오기
    private lateinit var auth: FirebaseAuth
    // Firebase Realtime Database 레퍼런스 객체 가져오기
    private lateinit var database: DatabaseReference
    // ActiveValue 값을 저장할 사용자 노드 생성하기
    private lateinit var userRef: DatabaseReference
    // ActiveValue 값 업데이트에 사용될 변수들 선언하기

    // 정확도 , 쿨다운 시간
    private val COOLDOWN_TIME_MS = 2500L
    private var lastPushUpTime = 0L
    private var push_ups = 0

    // 안드로이드 파일 관련 정의
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: LiteModelMovenetSingleposeThunderTfliteFloat164
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var handler: Handler

    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_detect_push_up)
        get_permissions()

        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = LiteModelMovenetSingleposeThunderTfliteFloat164.newInstance(this)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        paint.setColor(Color.RED)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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

                val inputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.UINT8)
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
                    "nose","left_eye","right_eye","left_ear","right_ear",
                    "left_shoulder", "right_shoulder",
                    "left_elbow", "right_elbow",
                    "left_wrist", "right_wrist",
                    "left_hip" , "right_hip",
                    "left_knee", "right_knee",
                    "left_ankle", "right_ankle"
                )


                // 원 그리기 코드
                while (x <= 49) {
                    if (outputFeature0.get(x + 2) > 0.45) {
                        canvas.drawCircle(
                            outputFeature0.get(x + 1) * w,
                            outputFeature0.get(x) * h,
                            10f,
                            paint
                        )
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

                // 좌표값의 인덱스
                val NOSE_INDEX = BODY_PARTS.indexOf("nose")
                val LEFT_EYE = BODY_PARTS.indexOf("left_eye")
                val RIGHT_EYE = BODY_PARTS.indexOf("right_eye")
                val LEFT_EAR = BODY_PARTS.indexOf("left_ear")
                val RIGHT_EAR = BODY_PARTS.indexOf("right_ear")
                val LEFT_ANKLE_INDEX = BODY_PARTS.indexOf("left_ankle")
                val LEFT_KNEE_INDEX = BODY_PARTS.indexOf("left_knee")
                val LEFT_HIP_INDEX = BODY_PARTS.indexOf("left_hip")
                val RIGHT_ANKLE_INDEX = BODY_PARTS.indexOf("right_ankle")
                val RIGHT_KNEE_INDEX = BODY_PARTS.indexOf("right_knee")
                val RIGHT_HIP_INDEX = BODY_PARTS.indexOf("right_hip")
                val LEFT_SHOULDER_INDEX = BODY_PARTS.indexOf("left_shoulder")
                val RIGHT_SHOULDER_INDEX = BODY_PARTS.indexOf("right_shoulder")
                val RIGHT_WRIST = BODY_PARTS.indexOf("right_wrist")
                val LEFT_WRIST = BODY_PARTS.indexOf("left_wrist")
                val RIGHT_ELBOW = BODY_PARTS.indexOf("right_elbow")
                val LEFT_ELBOW = BODY_PARTS.indexOf("left_elbow")


                // 각 관절의 표시값을 받아온다.
                val noseX = circles[2 * NOSE_INDEX]
                val noseY = circles[2 * NOSE_INDEX + 1]
                val lefteyeX = circles[2 * LEFT_EYE]
                val lefteyeY = circles[2 * LEFT_EYE + 1]
                val righteyeX = circles[2 * RIGHT_EYE]
                val righteyeY = circles[2 * RIGHT_EYE +1]
                val leftearX = circles[2 * LEFT_EAR]
                val leftearY = circles[2 * LEFT_EAR + 1]
                val rightearX = circles[2 * RIGHT_EAR]
                val rightearY = circles[2 * RIGHT_EAR + 1]
                val leftelbowX = circles[2*LEFT_ELBOW]
                val leftelbowY = circles[2*LEFT_ELBOW+1]
                val rightelbowX = circles[2*RIGHT_ELBOW]
                val rightelbowY = circles[2*RIGHT_ELBOW+1]
                val leftwristX = circles[2*LEFT_WRIST]
                val leftwristY = circles[2*LEFT_WRIST+1]
                val rightwristX = circles[2*RIGHT_WRIST]
                val rightwristY = circles[2*RIGHT_WRIST+1]
                val leftShoulderX = circles[2 * LEFT_SHOULDER_INDEX]
                val leftShoulderY = circles[2 * LEFT_SHOULDER_INDEX + 1]
                val rightShoulderX = circles[2* RIGHT_SHOULDER_INDEX]
                val rightShoulderY = circles[2* RIGHT_SHOULDER_INDEX+1]
                val leftAnkleX = circles[2 * LEFT_ANKLE_INDEX]
                val leftAnkleY = circles[2 * LEFT_ANKLE_INDEX + 1]
                val leftKneeX = circles[2 * LEFT_KNEE_INDEX]
                val leftKneeY = circles[2 * LEFT_KNEE_INDEX + 1]
                val leftHipX = circles[2 * LEFT_HIP_INDEX]
                val leftHipY = circles[2 * LEFT_HIP_INDEX + 1]
                val rightAnkleX = circles[2 * RIGHT_ANKLE_INDEX]
                val rightAnkleY = circles[2 * RIGHT_ANKLE_INDEX + 1]
                val rightKneeX = circles[2 * RIGHT_KNEE_INDEX]
                val rightKneeY = circles[2 * RIGHT_KNEE_INDEX + 1]
                val rightHipX = circles[2 * RIGHT_HIP_INDEX]
                val rightHipY = circles[2 * RIGHT_HIP_INDEX + 1]

                val l_angle_elbow = calculateAngle(
                    listOf(circles[2 * LEFT_WRIST], circles[2 * LEFT_WRIST + 1]),
                    listOf(circles[2 * LEFT_ELBOW], circles[2 * LEFT_ELBOW + 1]),
                    listOf(circles[2 * LEFT_SHOULDER_INDEX], circles[2 * LEFT_SHOULDER_INDEX + 1])
                )
                val l_elbow_angle = 180 - l_angle_elbow

                val r_angle_elbow = calculateAngle(
                    listOf(circles[2 * RIGHT_WRIST], circles[2 * RIGHT_WRIST + 1]),
                    listOf(circles[2 * RIGHT_ELBOW], circles[2 * RIGHT_ELBOW + 1]),
                    listOf(circles[2 * RIGHT_SHOULDER_INDEX], circles[2 * RIGHT_SHOULDER_INDEX + 1])
                )
                val r_elbow_angle = 180 - r_angle_elbow

//                val l_angle_shoulder = calculateAngle(
//                    listOf(circles[2 * LEFT_ELBOW], circles[2 * LEFT_ELBOW + 1]),
//                    listOf(circles[2 * LEFT_SHOULDER_INDEX], circles[2 * LEFT_SHOULDER_INDEX + 1]),
//                    listOf(circles[2 * RIGHT_SHOULDER_INDEX], circles[2 * RIGHT_SHOULDER_INDEX + 1])
//                )
//                val l_shoulder_angle = 180 - l_angle_shoulder
//
//                val r_angle_shoulder = calculateAngle(
//                    listOf(circles[2 * RIGHT_ELBOW], circles[2 * RIGHT_ELBOW + 1]),
//                    listOf(circles[2 * RIGHT_SHOULDER_INDEX], circles[2 * RIGHT_SHOULDER_INDEX + 1]),
//                    listOf(circles[2 * LEFT_SHOULDER_INDEX], circles[2 * LEFT_SHOULDER_INDEX + 1])
//                )
//                val r_shoulder_angle = 180 - r_angle_shoulder

                val l_hip_shoulder = calculateAngle(
                    listOf(circles[2 * LEFT_ELBOW], circles[2 * LEFT_ELBOW + 1]),
                    listOf(circles[2 * LEFT_SHOULDER_INDEX], circles[2 * LEFT_SHOULDER_INDEX + 1]),
                    listOf(circles[2 * LEFT_HIP_INDEX], circles[2 * LEFT_HIP_INDEX + 1])
                )
                val l_shoulder_hip = 180 - l_hip_shoulder

                val r_hip_shoulder = calculateAngle(
                    listOf(circles[2 * RIGHT_ELBOW], circles[2 * RIGHT_ELBOW + 1]),
                    listOf(circles[2 * RIGHT_SHOULDER_INDEX], circles[2 * RIGHT_SHOULDER_INDEX + 1]),
                    listOf(circles[2 * RIGHT_HIP_INDEX], circles[2 * RIGHT_HIP_INDEX + 1])
                )
                val r_shoulder_hip = 180 - r_hip_shoulder

                var allNonZero = false
                if ((leftwristX != 0f && leftwristY != 0f && leftelbowX != 0f && leftelbowY != 0f && leftShoulderX != 0f && leftShoulderY != 0f && leftHipX != 0f && leftHipY != 0f) ||
                    (rightwristX != 0f && rightwristY != 0f && rightelbowX != 0f && rightelbowY != 0f && rightShoulderX != 0f && rightShoulderY != 0f && rightHipX != 0f && rightHipY != 0f)) {
                    allNonZero = true;
                }

                val currentTime = currentTimeMillis()
                if (currentTime - lastPushUpTime >= COOLDOWN_TIME_MS && 90<= l_elbow_angle &&
                    80<= l_hip_shoulder && allNonZero || 90<= r_elbow_angle && 80<= r_hip_shoulder && allNonZero ) {
                    push_ups++ // 스쿼트 횟수 증가
                    lastPushUpTime = currentTime // 마지막 스쿼트 시간 업데이트
                    Log.d(TAG, "Number of push_ups: $push_ups")
                    paint.setColor(Color.GREEN)
                    Handler(Looper.getMainLooper()).postDelayed({
                        paint.color = Color.RED
                    }, 1500)
                }

                val push_upsTextView = findViewById<TextView>(R.id.push_upsTextView)
                push_upsTextView.rotation = 90f
                push_upsTextView.text = "현재 푸쉬업 개수: $push_ups"

                imageView.setImageBitmap(mutable)

                val angle1 = findViewById<TextView>(R.id.angle1)
               angle1.rotation = 90f
               angle1.text = "팔꿈치-어깨-엉덩이: $l_shoulder_hip"

                val angle2 = findViewById<TextView>(R.id.angle2)
                angle2.rotation = 90f
                angle2.text = "손목-팔꿈치-어깨: $l_elbow_angle"
            }
        }
    }

    // 뒤로가기 버튼 눌렀을 시 노드 생성 및 저장
    override fun onBackPressed() {
        super.onBackPressed()
        // Firebase Authentication 인스턴스 초기화하기
        auth = FirebaseAuth.getInstance()
        // Firebase Realtime Database의 레퍼런스 객체 가져오기
        database = FirebaseDatabase.getInstance().reference
        // 현재 로그인된 사용자의 ID 가져오기
        val userId = auth.currentUser?.uid
        // ActiveValue 값을 저장할 사용자 노드 생성하기
        if (userId != null) {
            userRef = database.child("Users").child(userId)
        }
        // 오늘 날짜에 해당하는 노드에 ActiveValue 값을 업로드하기
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val today2 = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        val activeValue = push_ups // 새로운 ActiveValue 값
        val squatsRef = userRef.child("Check2").child(today).child("Squat").child(today2)

        // 해당 날짜에 해당하는 노드가 없으면 생성하고 값을 저장
        squatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    squatsRef.setValue(activeValue)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to check if node exists: $error")
            }
        })
        val intent = Intent(this, Exercise01::class.java)
        startActivity(intent)
    }
    private fun calculateAngle(a: List<Float>, b: List<Float>, c: List<Float>): Float {
        val aVec = b.subtract(a) // First
        val bVec = b.subtract(c) // Mid
        val radians = atan2(bVec[1], bVec[0]) - atan2(aVec[1], aVec[0])
        var angle = abs(radians * 180.0f / PI.toFloat())
        if (angle > 180.0f) {
            angle = 360.0f - angle
        }
        return angle
    }

    fun List<Float>.subtract(other: List<Float>): List<Float> {
        return mapIndexed { index, value -> value - other[index] }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    fun open_camera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    var captureRequest = p0.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    var surface = Surface(textureView.surfaceTexture)
                    captureRequest.addTarget(surface)
                    p0.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {
                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                }
            },
            handler
        )
    }

    fun get_permissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) get_permissions()
    }
}