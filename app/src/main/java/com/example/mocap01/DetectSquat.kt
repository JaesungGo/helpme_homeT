package com.example.mocap01

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.mocap01.ml.LiteModelMovenetSingleposeThunderTfliteFloat164
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.lang.System.currentTimeMillis
import java.util.*

data class ParcelablePair<A, B>(val first: A?, val second: B?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(null) as A,
        parcel.readValue(null) as B
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(first)
        parcel.writeValue(second)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelablePair<Any, Any>> {
        override fun createFromParcel(parcel: Parcel): ParcelablePair<Any, Any> {
            return ParcelablePair(parcel)
        }

        override fun newArray(size: Int): Array<ParcelablePair<Any, Any>?> {
            return arrayOfNulls(size)
        }
    }
}

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
    private val COOLDOWN_TIME_MS = 1100L
    private val SITDOWN_TIME_MS = 500L
    private var firstSquatTime = 0L
    private var firstSitTime = 0L
    private val recordTime = getCurrentDateTime()
    private var isSquatting = false
    private var isSitting = false
    private var isStanding = false
    private var squats = 0
    private val wrongList = mutableListOf<Pair<Int,Any>>()
    private var wrongSquat = 0
    private var measureEnv = false


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
    val intent2 = Intent(this, ResultRecord::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_squat)
        get_permissions()

        val sendButton = findViewById<Button>(R.id.btn2)
        sendButton.setOnClickListener {
            sendDataToOtherActivity()
        }

        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = LiteModelMovenetSingleposeThunderTfliteFloat164.newInstance(this)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        paint.setColor(Color.WHITE)

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
                val lefthipX = getJointCoordinates("left_hip").first
                val lefthipY = getJointCoordinates("left_hip").second
                val righthipX = getJointCoordinates("right_hip").first
                val righthipY = getJointCoordinates("right_hip").second
                val leftkneeX = getJointCoordinates("left_knee").first
                val leftkneeY = getJointCoordinates("left_knee").second
                val rightkneeX = getJointCoordinates("right_knee").first
                val rightkneeY = getJointCoordinates("right_knee").second
                val leftankleX = getJointCoordinates("left_ankle").first
                val leftankleY = getJointCoordinates("left_ankle").second
                val rightankleX = getJointCoordinates("right_ankle").first
                val rightankleY = getJointCoordinates("right_ankle").second
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
                    "left_elbow", "right_elbow"
                )

                // 관절 검출에 필요한 관절이 모두 감지되었는지 확인
                val allNonZero = REQUIRED_PARTS.all { partName ->
                    val (x, y) = getJointCoordinates(partName)
                    x != 0f && y != 0f
                }

                var leftankle = Math.abs(leftankleX - 540)
                var rightankle = Math.abs(rightankleX - 540)

                // 스쿼트 검출 조건
                val currentTime = System.currentTimeMillis()
                wrongSquat = wrongList.count { it.first !in listOf(0, 1) }
                // 중앙 실선
                val startPoint = PointF(540f, 0.0f)
                val endPoint = PointF(540f, h.toFloat())
                val linePaint = Paint()

                if (leftankleY < 1400 && rightankleY < 1400 &&
                    Math.abs(leftankle - Math.abs(rightankle)) <= 50
                    && allNonZero ) {
                    if (!measureEnv) {

                        MotionToast.createColorToast(
                            this@DetectSquat,
                            "시작",
                            "자세 측정을 시작합니다",
                            MotionToastStyle.INFO,
                            MotionToast.GRAVITY_TOP,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(
                                this@DetectSquat,
                                www.sanju.motiontoast.R.font.helvetica_regular
                            )
                        )

                        measureEnv = true // 측정 시작 후 measureEnv를 true로 설정

                        // 초록선을 1초 동안 나타낸 후 다른 선을 그리지 않도록 합니다.
                    }
                    linePaint.color = Color.GREEN
                    linePaint.strokeWidth = 5.0f  // 라인 두께 조절
                    linePaint.style = Paint.Style.STROKE

                    canvas.drawLine(
                        startPoint.x,
                        startPoint.y,
                        endPoint.x,
                        endPoint.y,
                        linePaint
                    )
                } else {
                    // 조건이 충족되지 않는 경우 빨간 선을 그립니다.
                    linePaint.color = Color.RED
                    linePaint.strokeWidth = 5.0f  // 라인 두께 조절
                    linePaint.style = Paint.Style.STROKE

                    canvas.drawLine(
                        startPoint.x,
                        startPoint.y,
                        endPoint.x,
                        endPoint.y,
                        linePaint
                    )
                }

                if (10 >= LangleInDegrees && 10 >= RangleInDegrees &&
                    10 >= LshoulderInDegrees && 10 >= RshoulderInDegrees && allNonZero ) // 그냥 서있을 때
                {
                    isStanding = true
                    paint.color = Color.BLACK
//                    wrongList.add(Pair(0,0))
                }
                else{
                    isStanding = false
                }
                if (60 <= LangleInDegrees && 60 <= RangleInDegrees &&
                    60 <= LshoulderInDegrees && 60 <= RshoulderInDegrees && allNonZero
                ) {
                    isSquatting = true
                    firstSquatTime = currentTime // 처음 앉은 시간W
                    paint.color = Color.GREEN
                    Handler(Looper.getMainLooper()).postDelayed({
                        paint.color = Color.BLACK
                    }, 200)
                }
                if (!isSitting && isSquatting && currentTime - firstSquatTime >= COOLDOWN_TIME_MS
                    && 10 >= LangleInDegrees && 10 >= RangleInDegrees
                ) // 스쿼트 후 앉지 않고 일어섰을 때
                {
                    squats++ // 스쿼트 횟수 증가
                    wrongList.add(Pair(1,recordTime))
                    MotionToast.createColorToast(
                        this@DetectSquat,
                        "성공",
                        "올바른 자세입니다. $squats 개",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_TOP,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(
                            this@DetectSquat,
                            www.sanju.motiontoast.R.font.helvetica_regular
                        )
                    )
                    isSquatting = false
                }

                if ( Math.abs(LangleInDegrees - RangleInDegrees) < 30 &&
                    Math.abs(LshoulderInDegrees - RshoulderInDegrees ) < 30
                    && isSquatting && COOLDOWN_TIME_MS >= currentTime - firstSquatTime
                    && currentTime - firstSquatTime >= SITDOWN_TIME_MS
                    && 45 >= LangleInDegrees && LangleInDegrees > 25 &&
                    45 >= RangleInDegrees && RangleInDegrees > 25 && allNonZero ) // 주저 앉았을때 (엉덩이를 깊게 내렸을 때 )
                {
                    firstSitTime = currentTime
                    paint.color = Color.RED
                    isSquatting = false
                    isSitting = true
                    if (wrongList.lastOrNull()?.first != 2) {
                        wrongList.add(Pair(2, recordTime))
                    }
                    MotionToast.createColorToast(
                        this@DetectSquat,
                        "실패",
                        "틀린 자세입니다. 엉덩이를 조금 더 들어주세요. $wrongSquat 개",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_TOP,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@DetectSquat, www.sanju.motiontoast.R.font.helvetica_regular)
                    )
                }
                if (150 > leftankle + rightankle &&
//                    5 >= Math.abs(lefthipX - leftkneeX) &&
//                    5 >= Math.abs(righthipX - rightankleX )&&
//                    30 >= LangleInDegrees && LangleInDegrees > 10 &&
//                    30 >= RangleInDegrees && RangleInDegrees > 10 &&
//                    30 >= LshoulderInDegrees && LshoulderInDegrees >= 10 &&
//                    30 >= RshoulderInDegrees && RshoulderInDegrees >= 10 &&
                    30 <= LangleInDegrees && 30 <= RangleInDegrees &&
                    30 <= LshoulderInDegrees && 30 <= RshoulderInDegrees&&
                    Math.abs(LangleInDegrees - RangleInDegrees) < 20 &&
                    allNonZero ) // 다리를 좁게 앉았을 때
                {
                    if (wrongList.lastOrNull()?.first != 3) {
                        wrongList.add(Pair(3, recordTime))
                    }
                    paint.color = Color.RED
                    isStanding = false
                    isSquatting = false
                    isSitting = true
                    MotionToast.createColorToast(
                        this@DetectSquat,
                        "실패",
                        "틀린 자세입니다. 다리를 조금 더 벌려주세요. $wrongSquat 개",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_TOP,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@DetectSquat, www.sanju.motiontoast.R.font.helvetica_regular)
                    )
                }

                if (isSitting && 10 >= LangleInDegrees && 10 >= RangleInDegrees
                    && Math.abs(LangleInDegrees - RangleInDegrees) < 20 ) // 앉은 후 일어섰을때
                {
                    isStanding = true
                    isSitting = false
                    paint.color = Color.BLACK
                }

                // 스쿼트 횟수 텍스트 뷰 업데이트
                val squatsTextView = findViewById<TextView>(R.id.squatsTextView)
                squatsTextView.text = "현재 스쿼트 개수: $squats"
                val squatsTextView2 = findViewById<TextView>(R.id.squatsTextView2)
                squatsTextView2.text = "틀린 스쿼트 개수 : $wrongSquat"
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
        val activaValue2 = wrongSquat
        val squatsRef = userRef.child("Check2").child(today).child("Squat").child(today2)
        val squatsRef2 = userRef.child("Check2").child(today).child("WrongSquat").child(today2)

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

    private fun sendDataToOtherActivity() {
        // wrongList 데이터를 ParcelablePair로 변환
        val parcelableWrongList = wrongList.map { (first, second) ->
            ParcelablePair(first, second)
        }

        // 데이터를 전송
        val intent = Intent(this, ResultRecord::class.java)
        intent.putParcelableArrayListExtra("wrongList", ArrayList(parcelableWrongList))
        startActivity(intent)
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }
}