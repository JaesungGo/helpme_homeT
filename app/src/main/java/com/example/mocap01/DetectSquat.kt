package com.example.mocap01

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mocap01.ml.LiteModelMovenetSingleposeThunderTfliteFloat164
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.System.currentTimeMillis
import java.util.*

class DetectSquat : AppCompatActivity() {

    // Firebase Authentication 인스턴스 가져오기
    private lateinit var auth: FirebaseAuth
    // Firebase Realtime Database 레퍼런스 객체 가져오기
    private lateinit var database: DatabaseReference
    // ActiveValue 값을 저장할 사용자 노드 생성하기
    private lateinit var userRef: DatabaseReference
    // ActiveValue 값 업데이트에 사용될 변수들 선언하기
    private var cameraDevice: CameraDevice? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    // 스쿼트 정확도 , 쿨다운 시간 , 최근 스쿼트 타임을 정의
    private val COOLDOWN_TIME_MS = 1300L
    private var firstSitTime = 0L
    //private var lastSquatTime = 0L
    private var squats = 0
    private var isSquatting = false
    private val wrongList = mutableListOf<Int>()

    // 안드로이드 파일 관련 정의
    val paint = Paint()
    val angleHistoryList = mutableListOf<MutableList<Double>>()
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
        setContentView(R.layout.activity_detect_squat)
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

        paint.setColor(Color.BLACK)

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
                val BODY_PARTS = arrayOf(
                    "nose", "left_eye", "right_eye", "left_ear", "right_ear",
                    "left_shoulder", "right_shoulder",
                    "left_elbow", "right_elbow",
                    "left_wrist", "right_wrist",
                    "left_hip", "right_hip",
                    "left_knee", "right_knee",
                    "left_ankle", "right_ankle"
                )

                val circles = FloatArray(BODY_PARTS.size * 2)

                // 원 그리기 및 좌표 계산
                while (x <= 49) {
                    val confidence = outputFeature0[x + 2]
                    if (confidence > 0.45) {
                        val xIndex = x + 1
                        val yIndex = x
                        val xCoordinate = outputFeature0[xIndex] * w
                        val yCoordinate = outputFeature0[yIndex] * h
                        canvas.drawCircle(xCoordinate, yCoordinate, 10f, paint)
                        circles[2 * x / 3] = xCoordinate
                        circles[2 * x / 3 + 1] = yCoordinate
                    }
                    x += 3
                }

                // 관절 연결
                val edges = arrayOf(
                    Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4),
                    Pair(0, 5), Pair(0, 6), Pair(5, 7), Pair(7, 9),
                    Pair(6, 8), Pair(8, 10), Pair(5, 6), Pair(5, 11),
                    Pair(6, 12), Pair(11, 12), Pair(11, 13), Pair(13, 15),
                    Pair(12, 14), Pair(14, 16)
                )

                for ((i, j) in edges) {
                    val x1 = circles[2 * i]
                    val y1 = circles[2 * i + 1]
                    val x2 = circles[2 * j]
                    val y2 = circles[2 * j + 1]

                    if (x1 != 0f && y1 != 0f && x2 != 0f && y2 != 0f) {
                        canvas.drawLine(x1, y1, x2, y2, paint)
                    }
                }

                // 좌표 인덱스 매핑
                val partIndices = BODY_PARTS.mapIndexedNotNull { index, part ->
                    part to index
                }.toMap()

                // 각 관절의 좌표 계산
                fun getJointCoordinates(partName: String): Pair<Float, Float> {
                    val index = partIndices[partName] ?: return Pair(0f, 0f)
                    val xIndex = index * 2
                    val yIndex = xIndex + 1
                    return Pair(circles[xIndex], circles[yIndex])
                }

                // 각 관절의 X, Y 좌표 계산
                val noseX = getJointCoordinates("nose").first
                val noseY = getJointCoordinates("nose").second
                val lefteyeX = getJointCoordinates("left_eye").first
                val lefteyeY = getJointCoordinates("left_eye").second
                val righteyeX = getJointCoordinates("right_eye").first
                val righteyeY = getJointCoordinates("right_eye").second
                // 나머지 관절에 대한 X, Y 계산 추가

                // 각 관절 간의 기울기 계산 함수
                fun calculateSlope(part1: String, part2: String): Double {
                    val (x1, y1) = getJointCoordinates(part1)
                    val (x2, y2) = getJointCoordinates(part2)
                    return if (x1 != 0f && y1 != 0f && x2 != 0f && y2 != 0f) {
                        (y2 - y1) / (x2 - x1).toDouble()
                    } else {
                        0.0 // 무효한 값 반환
                    }
                }

                // 관절 간의 기울기 계산 및 각도 변환
                val LshoulderSlope = calculateSlope("left_shoulder","left_hip")
                val LankleKneeSlope = calculateSlope("left_ankle", "left_knee")
                val LkneeHipSlope = calculateSlope("left_knee", "left_hip")
                val RshoulderSlope = calculateSlope("right_shoulder","right_hip")
                val RankleKneeSlope = calculateSlope("right_ankle", "right_knee")
                val RkneeHipSlope = calculateSlope("right_knee", "right_hip")

                // 나머지 관절 간의 기울기 계산 추가

                // 관절 간의 각도 계산 함수
                fun calculateAngle(slope1: Double, slope2: Double): Double {
                    return Math.toDegrees(Math.atan(Math.abs((slope2 - slope1) / (1 + slope1 * slope2))))
                }

                // 관절 간의 각도 계산 및 양수 변환
                val Lsangle = calculateAngle(LshoulderSlope, LkneeHipSlope)
                val LshoulderInDegrees = if (Lsangle < 0) Lsangle + 180 else Lsangle

                val Rsangle = calculateAngle(RshoulderSlope, RkneeHipSlope)
                val RshoulderInDegrees = if (Rsangle < 0) Rsangle + 180 else Rsangle

                val Langle = calculateAngle(LankleKneeSlope, LkneeHipSlope)
                val LangleInDegrees = if (Langle < 0) Langle + 180 else Langle

                val Rangle = calculateAngle(RankleKneeSlope, RkneeHipSlope)
                val RangleInDegrees = if (Rangle < 0) Rangle + 180 else Rangle

                Log.d(TAG, "Lshoulder: $LshoulderInDegrees")
                Log.d(TAG, "Rshoulder: $RshoulderInDegrees")
                Log.d(TAG, "Lankle: $LangleInDegrees")
                Log.d(TAG, "Rankle: $RangleInDegrees")


                // 관절 검출에 필요한 관절 이름만 포함하는 배열 정의
                val REQUIRED_PARTS = arrayOf(
                    "left_ankle", "left_knee", "left_hip",
                    "right_ankle", "right_knee", "right_hip",
                    "left_shoulder", "right_shoulder",
                    "left_elbow", "right_elbow",
                    "left_wrist", "right_wrist",
                )

                // 관절 검출에 필요한 관절이 모두 감지되었는지 확인
                val allNonZero = REQUIRED_PARTS.all { partName ->
                    val (x, y) = getJointCoordinates(partName)
                    x != 0f && y != 0f
                }

                // 스쿼트 검출 조건
                val currentTime = System.currentTimeMillis()
                if (60 <= LangleInDegrees && 60 <= RangleInDegrees &&
                    60 <= LshoulderInDegrees && 60 <= RshoulderInDegrees && allNonZero
                ) {
                    isSquatting = true
                    firstSitTime = currentTime // 처음 앉은 시간
                    paint.color = Color.GREEN
                    wrongList.add(0)           // 제대로 했을때도 추가 0
                    Handler(Looper.getMainLooper()).postDelayed({
                        paint.color = Color.BLACK
                    }, 500)
                }
                if (isSquatting && currentTime - firstSitTime >= COOLDOWN_TIME_MS
                    && 10 >= LangleInDegrees && 10 >= RangleInDegrees
                    && 10 >= LshoulderInDegrees && 10 >=RshoulderInDegrees ) // 스쿼트 후 앉지 않고 일어섰을 때
                {
                    squats++ // 스쿼트 횟수 증가
                    isSquatting = false
                }
                if (isSquatting && 30 >= LangleInDegrees && LangleInDegrees > 10 &&
                    30 >= RangleInDegrees && 30 >= RshoulderInDegrees && 30 >= LshoulderInDegrees
                    && RshoulderInDegrees >10 && LshoulderInDegrees >10 ) // 주저 앉았을때
                {
                    paint.color = Color.RED
                    isSquatting = false
                    wrongList.add(1) // 앉았을때의 오류 배열 값:1
                }

//                if( isSquatting && 25 >= LshoulderInDegrees && 25 >= RshoulderInDegrees
//                    && LshoulderInDegrees > 10 && RshoulderInDegrees >10) // 허리를 굽혔을 때 어떻게 측정? 정면 불가
//                {
//                    paint.color = Color.RED
//                    isSquatting = false
//                }


                // 스쿼트 횟수 텍스트 뷰 업데이트
                val squatsTextView = findViewById<TextView>(R.id.squatsTextView)
                squatsTextView.text = "현재 스쿼트 개수: $squats"
                imageView.setImageBitmap(mutable)
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
        val activeValue = squats // 새로운 ActiveValue 값
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
//        onPause()
//        closeCamera()
//        stopBackgroundThread()
        finish()
    }


    override fun onPause() {
        super.onPause()

        cameraCaptureSession?.close()
        cameraCaptureSession = null
    }

    private fun closeCamera(){
        cameraDevice?.close()
        cameraDevice = null
    }


    override fun onDestroy() {
        super.onDestroy()
        model.close()

    }
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    @SuppressLint("MissingPermission")
    fun open_camera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[1],
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
                    p0.close()
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