package com.example.mocap01

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
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

class DetectSitUp : AppCompatActivity() {

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

    // 정확도 , 쿨다운 시간
    private val COOLDOWN_TIME_MS = 1000L
    private var firstSitUpTime = 0L
    private var lastSitUpTime = 0L
    private var sit_ups = 0
    private var isSitUpting = false
    private var isSitUpSitting = false
    private var isSitUpStanding = false
    private val wrongList = mutableListOf<Int>()
    private var wrongCount = 0
    private var wrongSitUp = 0

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
        setContentView(R.layout.activity_detect_sit_up)
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

                val BODY_PARTS = arrayOf(
                    "nose","left_eye","right_eye","left_ear","right_ear",
                    "left_shoulder", "right_shoulder",
                    "left_elbow", "right_elbow",
                    "left_wrist", "right_wrist",
                    "left_hip" , "right_hip",
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

                // SitUp검출 조건
                val currentTime = System.currentTimeMillis()
                var wrongSitUp = wrongList.count {it ==1 }

                if (10 >= LshoulderInDegrees && 10 >= RshoulderInDegrees
                    && 10 >= LangleInDegrees && 10 >= RangleInDegrees ) // 일어선 상태
                {
                    isSitUpting = true
                    paint.color = Color.WHITE
                }

                if (60 <= LangleInDegrees && 60 <= RangleInDegrees &&
                    60 <= LshoulderInDegrees && 60 <= RshoulderInDegrees && allNonZero
                ) {
                    isSitUpting = true
                    firstSitUpTime = currentTime // 처음 앉은 시간
                    paint.color = Color.GREEN  // 올바른 자세에 도달했을 때
                    wrongList.add(0)
                    Handler(Looper.getMainLooper()).postDelayed({
                        paint.color = Color.WHITE
                    }, 500)
                }

                if (isSitUpting && currentTime - firstSitUpTime >= COOLDOWN_TIME_MS
                    && 10 >= LangleInDegrees && 10 >= RangleInDegrees ) // 스쿼트 후 앉지 않고 일어섰을 때
                {
                    sit_ups++ // 스쿼트 횟수 증가
                    isSitUpting = false
                }

                if (isSitUpting && currentTime - firstSitUpTime >= COOLDOWN_TIME_MS
                    && 30 >= LangleInDegrees && LangleInDegrees > 10 &&
                    30 >= RangleInDegrees && RangleInDegrees >10 ) // 주저 앉았을때
                {
                    paint.color = Color.RED
                    isSitUpting = false
                    isSitUpStanding =false
                    isSitUpSitting = true
                    if (wrongList.isEmpty() || wrongList.last() != 1) {
                        wrongList.add(1)
                    }
                }

                if (isSitUpSitting && 10 >= LangleInDegrees && 10 >= RangleInDegrees ) // 앉은 후 일어섰을때
                {
                    isSitUpSitting = false
                    isSitUpStanding = true
                    paint.color = Color.WHITE
                    wrongList.add(0)
                }

                // sit_up 횟수 텍스트 뷰 업데이트
                val squatsTextView = findViewById<TextView>(R.id.sit_upsTextView)
                squatsTextView.text = "현재 sit_up 개수: $sit_ups"
                val squatsTextView2 = findViewById<TextView>(R.id.sit_upsTextView2)
                squatsTextView2.text = "틀린 sit_up 개수 : $wrongSitUp"
                imageView.setImageBitmap(mutable)
            }
        }
    }


//                // 원 그리기 코드
//                while (x <= 49) {
//                    if (outputFeature0.get(x + 2) > 0.45) {
//                        canvas.drawCircle(
//                            outputFeature0.get(x + 1) * w,
//                            outputFeature0.get(x) * h,
//                            10f,
//                            paint
//                        )
//                        circles[2 * x / 3] = outputFeature0.get(x + 1) * w
//                        circles[2 * x / 3 + 1] = outputFeature0.get(x) * h
//                    }
//                    x += 3
//                }
//                // 선 어레이에 정의
//                val edges = arrayOf(
//                    Pair(0, 1),
//                    Pair(0, 2),
//                    Pair(1, 3),
//                    Pair(2, 4),
//                    Pair(0, 5),
//                    Pair(0, 6),
//                    Pair(5, 7),
//                    Pair(7, 9),
//                    Pair(6, 8),
//                    Pair(8, 10),
//                    Pair(5, 6),
//                    Pair(5, 11),
//                    Pair(6, 12),
//                    Pair(11, 12),
//                    Pair(11, 13),
//                    Pair(13, 15),
//                    Pair(12, 14),
//                    Pair(14, 16)
//                )
//                // 관절 - 관절 이루는 선 그리기
//                for ((i, j) in edges) {
//                    val x1 = circles[2 * i]
//                    val y1 = circles[2 * i + 1]
//                    val x2 = circles[2 * j]
//                    val y2 = circles[2 * j + 1]
//
//                    if (x1 != 0f && y1 != 0f && x2 != 0f && y2 != 0f) {
//                        canvas.drawLine(x1, y1, x2, y2, paint)
//                    }
//                }
//
//                // 좌표값의 인덱스
//                val NOSE_INDEX = BODY_PARTS.indexOf("nose")
//                val LEFT_EYE = BODY_PARTS.indexOf("left_eye")
//                val RIGHT_EYE = BODY_PARTS.indexOf("right_eye")
//                val LEFT_EAR = BODY_PARTS.indexOf("left_ear")
//                val RIGHT_EAR = BODY_PARTS.indexOf("right_ear")
//                val LEFT_ANKLE_INDEX = BODY_PARTS.indexOf("left_ankle")
//                val LEFT_KNEE_INDEX = BODY_PARTS.indexOf("left_knee")
//                val LEFT_HIP_INDEX = BODY_PARTS.indexOf("left_hip")
//                val RIGHT_ANKLE_INDEX = BODY_PARTS.indexOf("right_ankle")
//                val RIGHT_KNEE_INDEX = BODY_PARTS.indexOf("right_knee")
//                val RIGHT_HIP_INDEX = BODY_PARTS.indexOf("right_hip")
//                val LEFT_SHOULDER_INDEX = BODY_PARTS.indexOf("left_shoulder")
//                val RIGHT_SHOULDER_INDEX = BODY_PARTS.indexOf("right_shoulder")
//                val RIGHT_WRIST = BODY_PARTS.indexOf("right_wrist")
//                val LEFT_WRIST = BODY_PARTS.indexOf("left_wrist")
//                val RIGHT_ELBOW = BODY_PARTS.indexOf("right_elbow")
//                val LEFT_ELBOW = BODY_PARTS.indexOf("left_elbow")
//
//
//                // 각 관절의 표시값을 받아온다.
//                val noseX = circles[2 * NOSE_INDEX]
//                val noseY = circles[2 * NOSE_INDEX + 1]
//                val lefteyeX = circles[2 * LEFT_EYE]
//                val lefteyeY = circles[2 * LEFT_EYE + 1]
//                val righteyeX = circles[2 * RIGHT_EYE]
//                val righteyeY = circles[2 * RIGHT_EYE +1]
//                val leftearX = circles[2 * LEFT_EAR]
//                val leftearY = circles[2 * LEFT_EAR + 1]
//                val rightearX = circles[2 * RIGHT_EAR]
//                val rightearY = circles[2 * RIGHT_EAR + 1]
//                val leftelbowX = circles[2*LEFT_ELBOW]
//                val leftelbowY = circles[2*LEFT_ELBOW+1]
//                val rightelbowX = circles[2*RIGHT_ELBOW]
//                val rightelbowY = circles[2*RIGHT_ELBOW+1]
//                val leftwristX = circles[2*LEFT_WRIST]
//                val leftwristY = circles[2*LEFT_WRIST+1]
//                val rightwristX = circles[2*RIGHT_WRIST]
//                val rightwristY = circles[2*RIGHT_WRIST+1]
//                val leftShoulderX = circles[2 * LEFT_SHOULDER_INDEX]
//                val leftShoulderY = circles[2 * LEFT_SHOULDER_INDEX + 1]
//                val rightShoulderX = circles[2* RIGHT_SHOULDER_INDEX]
//                val rightShoulderY = circles[2* RIGHT_SHOULDER_INDEX+1]
//                val leftAnkleX = circles[2 * LEFT_ANKLE_INDEX]
//                val leftAnkleY = circles[2 * LEFT_ANKLE_INDEX + 1]
//                val leftKneeX = circles[2 * LEFT_KNEE_INDEX]
//                val leftKneeY = circles[2 * LEFT_KNEE_INDEX + 1]
//                val leftHipX = circles[2 * LEFT_HIP_INDEX]
//                val leftHipY = circles[2 * LEFT_HIP_INDEX + 1]
//                val rightAnkleX = circles[2 * RIGHT_ANKLE_INDEX]
//                val rightAnkleY = circles[2 * RIGHT_ANKLE_INDEX + 1]
//                val rightKneeX = circles[2 * RIGHT_KNEE_INDEX]
//                val rightKneeY = circles[2 * RIGHT_KNEE_INDEX + 1]
//                val rightHipX = circles[2 * RIGHT_HIP_INDEX]
//                val rightHipY = circles[2 * RIGHT_HIP_INDEX + 1]
//
//                val LankleKneeSlope = (leftAnkleY - leftKneeY) / (leftAnkleX - leftKneeX)
//                val LkneeHipSlope = (leftKneeY - leftHipY) / (leftKneeX - leftHipX)
//                val RankleKneeSlope = (rightAnkleY - rightKneeY) / (rightAnkleX - rightKneeX)
//                val RkneeHipSlope = (rightKneeY - rightHipY) / (rightKneeX - rightHipX)
//
//                val LshoulderSlope = (leftHipY - leftShoulderY) / (leftHipX - leftShoulderX)
//                val RshoulderSlope = (rightHipY - rightShoulderY) / (rightHipX - rightShoulderX)
//
//                val Lsangle = Math.atan(((LshoulderSlope - LkneeHipSlope) / (1 + LkneeHipSlope * LshoulderSlope)).toDouble())
//                var LshoulderInDegrees = Math.toDegrees(Lsangle)
//                if (LshoulderInDegrees < 0) {
//                    LshoulderInDegrees += 180 // 음수인 경우 360을 더해 양수로 변환
//                }
//
//                val Rsangle = Math.atan(((RshoulderSlope - RkneeHipSlope) / (1 + RkneeHipSlope * RshoulderSlope)).toDouble())
//                var RshoulderInDegrees = Math.toDegrees(Rsangle)
//                if (RshoulderInDegrees < 0) {
//                    RshoulderInDegrees += 180 // 음수인 경우 360을 더해 양수로 변환
//                }
//
//
//                val Langle = Math.atan(((LkneeHipSlope - LankleKneeSlope) / (1 + LankleKneeSlope * LkneeHipSlope)).toDouble())
//                var LangleInDegrees = Math.toDegrees(Langle)
//                if (LangleInDegrees < 0) {
//                    LangleInDegrees += 180 // 음수인 경우 360을 더해 양수로 변환
//                }
//
//                val Rangle = Math.atan(((LkneeHipSlope - LankleKneeSlope) / (1 + LankleKneeSlope * LkneeHipSlope)).toDouble())
//                var RangleInDegrees = Math.toDegrees(Rangle)
//                if (RangleInDegrees < 0) {
//                    RangleInDegrees += 180 // 음수인 경우 360을 더해 양수로 변환
//                }
//
//                var allNonZero = false
//                if(leftShoulderX != 0f && leftShoulderY != 0f && rightShoulderX != 0f && rightShoulderY != 0f && leftAnkleX != 0f && leftAnkleY != 0f &&
//                    leftKneeX != 0f && leftKneeY != 0f && leftHipX != 0f && leftHipY != 0f && rightAnkleX != 0f && rightAnkleY != 0f && rightKneeX != 0f && rightKneeY != 0f && rightHipX != 0f && rightHipY != 0f){
//                    allNonZero = true
//                }
//
//                val currentTime = currentTimeMillis()
//                if (currentTime - lastSitUpTime >= COOLDOWN_TIME_MS && 140>= LangleInDegrees &&
//                    140>= RangleInDegrees && 60<=LshoulderInDegrees && 60<= RshoulderInDegrees && allNonZero ) {
//                    sit_ups++ // 스쿼트 횟수 증가
//                    lastSitUpTime = currentTime // 마지막 스쿼트 시간 업데이트
//                    Log.d(TAG, "Number of sit_ups: $sit_ups")
//                    paint.setColor(Color.GREEN)
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        paint.color = Color.RED
//                    }, 1500)
//                }
//
//                val sit_upsTextView = findViewById<TextView>(R.id.sit_upsTextView)
//                sit_upsTextView.rotation = 90f
//                sit_upsTextView.text = "현재 싯업 개수: $sit_ups"
//
//                imageView.setImageBitmap(mutable)
//            }
//        }
//    }

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
        val activeValue = sit_ups // 새로운 ActiveValue 값
        val activaValue2 = wrongSitUp
        val squatsRef = userRef.child("Check2").child(today).child("SitUp").child(today2)
        val squatsRef2 = userRef.child("Check2").child(today).child("WrongSitUp").child(today2)

        // 해당 날짜에 해당하는 노드가 없으면 생성하고 값을 저장
        squatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    squatsRef.setValue(activeValue)
                    squatsRef2.setValue(activaValue2)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to check if node exists: $error")
            }
        })
//        val intent = Intent(this, Exercise01::class.java)
//        startActivity(intent)
        finish()
    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        model.close()
//    }
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