package com.vubq.auto_eht

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CompletableFuture


class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var params: WindowManager.LayoutParams? = null

    private var pathData: String = "/storage/1775-1612/AutoEHT/";

    var type: Int = 0;
    var selection: Int = 0;

    @Volatile
    private var isAuto = false

    private val handler = Handler(Looper.getMainLooper())

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams", "CutPasteId")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        params?.gravity = Gravity.TOP or Gravity.START
        params?.x = 0
        params?.y = 700

        val context = ContextThemeWrapper(this, R.style.Theme_AutoEHT)

        // Inflate layout với context đã bọc theme
        val inflater = LayoutInflater.from(context)
        floatingView = inflater.inflate(R.layout.floating_window_layout, null)

        val autoSelection1: MaterialAutoCompleteTextView = floatingView.findViewById(R.id.auto_type)
        val autoSelection2: MaterialAutoCompleteTextView =
            floatingView.findViewById(R.id.auto_selection)

        val items1 =
            listOf("Auto trang bị", "Auto cường hóa", "Auto tẩy thuộc tính", "Auto thú cưỡi")
        autoSelection1.setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                items1
            )
        )
        autoSelection1.setText(items1[0], false)

        val items2d =
            listOf("Giáp", "Găng", "Giày", "Dây chuyền", "Nhẫn", "Vũ khí")
        val adapter2 =
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items2d)
        autoSelection2.setAdapter(adapter2)
        autoSelection2.setText(items2d[0], false)

        autoSelection1.setOnItemClickListener { parent, view, position, id ->

            type = position

            val items2 = when (position) {
                0 -> listOf("Giáp", "Găng", "Giày", "Dây chuyền", "Nhẫn", "Vũ khí")
                1 -> listOf("Ô 1", "Ô 2", "Ô 3", "Ô 4", "Ô 5", "Ô 6", "Ô 7", "Ô 8")
                2 -> listOf("Ô 1", "Ô 2", "Ô 3", "Ô 4", "Ô 5", "Ô 6", "Ô 7", "Ô 8")
                3 -> emptyList()
                else -> emptyList()
            }

            autoSelection2.setAdapter(
                ArrayAdapter(
                    context,
                    android.R.layout.simple_list_item_1,
                    items2
                )
            )
            if (items2.isNotEmpty()) autoSelection2.setText(items2[0], false)

            if (position == 0) {
                floatingView.findViewById<FrameLayout>(R.id.f_layout_b).visibility = View.VISIBLE
            } else {
                floatingView.findViewById<FrameLayout>(R.id.f_layout_b).visibility = View.GONE
            }

            if (position == 3) {
                floatingView.findViewById<FrameLayout>(R.id.f_layout_c).visibility = View.GONE
            } else {
                floatingView.findViewById<FrameLayout>(R.id.f_layout_c).visibility = View.VISIBLE
            }
        }

        autoSelection2.setOnItemClickListener { parent, view, position, id ->
            selection = position
        }

//        val sAutoType: Spinner = floatingView.findViewById(R.id.auto_type)
//        val iAutoType = resources.getStringArray(R.array.auto_type)
//        val aAutoType = ArrayAdapter(this, android.R.layout.simple_spinner_item, iAutoType)
//        aAutoType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        sAutoType.adapter = aAutoType
//
//        val sAutoSelection: Spinner = floatingView.findViewById(R.id.auto_selection)
//
//        sAutoType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>,
//                view: View?,
//                position: Int,
//                id: Long,
//            ) {
//                val iAutoSelection = when (position) {
//                    0 -> resources.getStringArray(R.array.equip)
//                    1 -> resources.getStringArray(R.array.strengthen)
//                    2 -> resources.getStringArray(R.array.erase_attribute)
//                    else -> emptyArray()
//                }
//                if (position == 0) {
//                    floatingView.findViewById<FrameLayout>(R.id.f_layout_b).visibility =
//                        View.VISIBLE
//                } else {
//                    floatingView.findViewById<FrameLayout>(R.id.f_layout_b).visibility = View.GONE
//                }
//                if (position == 3) {
//                    floatingView.findViewById<FrameLayout>(R.id.f_layout_at).visibility =
//                        View.GONE
//                } else {
//                    floatingView.findViewById<FrameLayout>(R.id.f_layout_at).visibility =
//                        View.VISIBLE
//                }
//                val aAutoSelection = ArrayAdapter(
//                    this@FloatingWindowService,
//                    android.R.layout.simple_spinner_item,
//                    iAutoSelection
//                )
//                aAutoSelection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                sAutoSelection.adapter = aAutoSelection
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {}
//        }

        val updatedParams = params

        floatingView.setOnTouchListener(object : View.OnTouchListener {

            private var x = 0
            private var y = 0
            private var touchedX = 0f
            private var touchedY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Lưu lại vị trí ban đầu
                        x = updatedParams?.x ?: 0
                        y = updatedParams?.y ?: 0

                        touchedX = event.rawX
                        touchedY = event.rawY
                    }

//                    MotionEvent.ACTION_UP -> {
//                        val Xdiff = (event.rawX - touchedX).toInt()
//                        val Ydiff = (event.rawY - touchedY).toInt()
//
//                        if (Xdiff < 10 && Ydiff < 10) {
//                            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
//                            activityManager?.let { am ->
//                                val taskInfoList = am.getRunningTasks(1)
//                                if (taskInfoList.isNotEmpty()) {
//                                    val topActivity = taskInfoList[0].topActivity
//                                    if (topActivity != null && topActivity.packageName == applicationContext.packageName) {
//                                        val taskId = taskInfoList[0].id
//                                        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
//                                    }
//                                }
//                            }
//                        }
//                    }

                    MotionEvent.ACTION_MOVE -> {
                        updatedParams?.x = (x + (event.rawX - touchedX)).toInt()
                        updatedParams?.y = (y + (event.rawY - touchedY)).toInt()

                        windowManager.updateViewLayout(floatingView, updatedParams)
                    }

                    else -> {
                    }
                }
                return true
            }
        })

        floatingView.findViewById<MaterialButton>(R.id.stop_btn)?.strokeWidth = 0
        floatingView.findViewById<Button>(R.id.stop_btn)?.setOnTouchListener(object : View.OnTouchListener {

            private var x = 0
            private var y = 0
            private var touchedX = 0f
            private var touchedY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Lưu lại vị trí ban đầu
                        x = updatedParams?.x ?: 0
                        y = updatedParams?.y ?: 0

                        touchedX = event.rawX
                        touchedY = event.rawY
                    }

                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - touchedX).toInt()
                        val Ydiff = (event.rawY - touchedY).toInt()

                        if (Xdiff < 10 && Ydiff < 10) {
                            val layoutA: View = floatingView.findViewById(R.id.layout_a)
                            layoutA.visibility = View.VISIBLE
                            val layoutB: View = floatingView.findViewById(R.id.layout_b)
                            layoutB.visibility = View.GONE
                            params?.width = WindowManager.LayoutParams.MATCH_PARENT
                            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
                            params?.let { windowManager.updateViewLayout(floatingView, it) }
                            isAuto = false
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        updatedParams?.x = (x + (event.rawX - touchedX)).toInt()
                        updatedParams?.y = (y + (event.rawY - touchedY)).toInt()

                        windowManager.updateViewLayout(floatingView, updatedParams)
                    }

                    else -> {
                    }
                }
                return true
            }
        })

        floatingView.findViewById<Button>(R.id.stop_btn)?.setOnClickListener {
            val layoutA: View = floatingView.findViewById(R.id.layout_a)
            layoutA.visibility = View.VISIBLE
            val layoutB: View = floatingView.findViewById(R.id.layout_b)
            layoutB.visibility = View.GONE
            params?.width = WindowManager.LayoutParams.MATCH_PARENT
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            params?.let { windowManager.updateViewLayout(floatingView, it) }
            isAuto = false
        }

        floatingView.findViewById<Button>(R.id.hidden_btn)?.setOnClickListener {
            val layoutB: View = floatingView.findViewById(R.id.layout_b)
            layoutB.visibility = View.VISIBLE
            val layoutA: View = floatingView.findViewById(R.id.layout_a)
            layoutA.visibility = View.GONE
            params?.width = WindowManager.LayoutParams.WRAP_CONTENT
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            params?.let { windowManager.updateViewLayout(floatingView, it) }
            params?.x = (1080 / 2) - (params?.width!! / 2)
            params?.y = 0
            params?.let { windowManager.updateViewLayout(floatingView, it) }
        }

        floatingView.findViewById<Button>(R.id.start_btn)?.setOnClickListener {
            val layoutB: View = floatingView.findViewById(R.id.layout_b)
            layoutB.visibility = View.VISIBLE
            val layoutA: View = floatingView.findViewById(R.id.layout_a)
            layoutA.visibility = View.GONE
            params?.width = WindowManager.LayoutParams.WRAP_CONTENT
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            params?.let { windowManager.updateViewLayout(floatingView, it) }
            params?.x = (1080 / 2) - (params?.width!! / 2)
            params?.y = 0
            params?.let { windowManager.updateViewLayout(floatingView, it) }

            if (type == 0) {
                equip(selection, floatingView.findViewById<MaterialCheckBox>(R.id.option_b).isChecked)
            }

            if (type == 1) {
                strengthen(selection)
            }

            if (type == 2) {
                eraseAttribute(selection)
            }

            if (type == 3) {
                ridingAnimal()
            }
        }

        params?.let { windowManager.addView(floatingView, it) }
    }

    private suspend fun recognizeText(image: String): String {
        val inputStream: InputStream = File(image).inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return unicode(visionText.text)
    }

    private fun getTextFromImage(
        fileName: String,
        comparativeWords: List<String>,
        bout: Int,
    ): Boolean = runBlocking {
        val text: String = recognizeText("$pathData$fileName.png")

        if (text.isEmpty()) return@runBlocking false

        val exist = comparativeWords.any { text.contains(it, ignoreCase = true) }

        showToast("$text - $exist")

        val textToAppend = "Lần $bout: $text - $exist" + " - " + getCurrentDateTime()

        BufferedWriter(FileWriter("$pathData$fileName.txt", true)).use { writer ->
            writer.write(textToAppend)
            writer.newLine()
        }

        return@runBlocking exist
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun unicode(text: String): String {
        val diacriticMap = mapOf(
            'á' to 'a', 'à' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
            'ă' to 'a', 'ắ' to 'a', 'ằ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a', 'ặ' to 'a',
            'â' to 'a', 'ấ' to 'a', 'ầ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a', 'ậ' to 'a',
            'é' to 'e', 'è' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
            'ê' to 'e', 'ế' to 'e', 'ề' to 'e', 'ể' to 'e', 'ễ' to 'e', 'ệ' to 'e',
            'í' to 'i', 'ì' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
            'ó' to 'o', 'ò' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
            'ô' to 'o', 'ố' to 'o', 'ồ' to 'o', 'ổ' to 'o', 'ỗ' to 'o', 'ộ' to 'o',
            'ơ' to 'o', 'ớ' to 'o', 'ờ' to 'o', 'ở' to 'o', 'ỡ' to 'o', 'ợ' to 'o',
            'ú' to 'u', 'ù' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
            'ư' to 'u', 'ứ' to 'u', 'ừ' to 'u', 'ử' to 'u', 'ữ' to 'u', 'ự' to 'u',
            'ý' to 'y', 'ỳ' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',
            'Đ' to 'D', 'đ' to 'd'
        )
        return text.map { diacriticMap[it] ?: it }.joinToString("")
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
    }

    private fun String.adbExecution() {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", this))
        process.waitFor()
    }

    private fun String.openApp() {
        "monkey -p $this -c android.intent.category.LAUNCHER 1".adbExecution()
    }

    private fun click(x: Int, y: Int) {
        "input tap $x $y".adbExecution()
    }

    private fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, speed: Int = 500) {
        "input swipe $x1 $y1 $x2 $y2 $speed".adbExecution()
    }

    private fun String.screenCapture() {
        "screencap -p $pathData$this.png".adbExecution()
    }

    private fun adjustBrightness(i: Int) {
        "shell settings put system screen_brightness $i".adbExecution()
    }

    private fun cropImage(fileName: String, x: Int, y: Int, width: Int, height: Int) {
        val filePath = "$pathData$fileName.png"
        val inputStream: InputStream = File(filePath).inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)

        val outputFile = File(filePath)
        val outputStream = FileOutputStream(outputFile)
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun initAuto() {
        //Mở App Backup
        if (!isAuto) return
        "com.machiav3lli.backup".openApp()

        //Nhấn khôi phục
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(840, 2160)

        //Nhấn OK
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(950, 1520)

        //Mở EHT
        if (!isAuto) return
        Thread.sleep(5000)
        if (!isAuto) return
        "com.superplanet.evilhunter".openApp()

        //Nhấn Touch To Start
        if (!isAuto) return
        Thread.sleep(13000)
        if (!isAuto) return
        click(505, 1995)

        //Nhấn đóng
        if (!isAuto) return
        Thread.sleep(27000)
        if (!isAuto) return
        click(530, 1800)
    }

    private fun backup() {
        "com.machiav3lli.backup".openApp()

        //Nhấn sao lưu
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(248, 1604)

        //Nhấn bỏ APK
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(121, 839)

        //Nhấn OK
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(939, 1643)

        if (!isAuto) return
        Thread.sleep(8000)
    }

    private fun equip(selection: Int, optionB: Boolean) {
        isAuto = true
        Thread {
            while (isAuto) {
                initAuto()

                //Nhấn chọn lò rèn hoặc kim hoàn
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 3 || selection == 4) {
                    //Kim hoàn
                    click(735, 1486)
                } else {
                    //Lò rèn
                    click(432, 1361)
                }

                //Nhấn chọn loại đồ
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 0 || selection == 4) {
                    click(283, 933)
                }
                if (selection == 1) {
                    click(390, 930)
                }
                if (selection == 2) {
                    click(488, 926)
                }

                //Nhấn chọn đồ
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 5) {
                    if (!isAuto) break
                    swipe(390, 1510, 390, 985, 500)
                    if (!isAuto) break
                    swipe(390, 1510, 390, 985, 500)
                    if (!isAuto) break
                    swipe(390, 1510, 390, 985, 500)
                    if (!isAuto) break
                    swipe(390, 1510, 390, 985, 500)
                    if (!isAuto) break
                    Thread.sleep(500)
                    if (!isAuto) break
                    click(527, 1471)
                } else {
                    if (!isAuto) break
                    click(796, 1238)
                }

                //Kéo đầy thanh
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                swipe(241, 1786, 965, 1786)

                //Nhấn điều chế
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(364, 1977)

                //Nhấn tìm thuộc tính
                if (!isAuto) break
                Thread.sleep(7000)
                if (!isAuto) break
                click(520, 910)

                //Nhấn thiết lập sẵn A
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(183, 527)

                //Nhấn tìm kiếm
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(335, 2045)

                if (!isAuto) break
                Thread.sleep(2000)
                if (!isAuto) break
                "equip".screenCapture()

                if (!isAuto) break
                cropImage("equip", 85, 865, 623, 107)

                if (!isAuto) break
                val comparativeWords = listOf("4 thuoc tinh co hieu luc")
                val isTrue = getTextFromImage("equip", comparativeWords, 1)

                if (!isAuto) break
                if (isTrue) {
                    isAuto = false
                    break
                }

                if (!isAuto) break
                if (!optionB) continue

                //Nhấn xác nhận
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(527, 2084)

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                //Nhấn xác nhận
                click(527, 2084)

                //Nhấn tìm thuộc tính
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(520, 910)

                //Nhấn thiết lập sẵn B
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(455, 530)

                //Nhấn tìm kiếm
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(335, 2045)

                if (!isAuto) break
                Thread.sleep(2000)
                if (!isAuto) break
                "equip".screenCapture()

                if (!isAuto) break
                cropImage("equip", 85, 865, 623, 107)

                if (!isAuto) break
                val isTrue2 = getTextFromImage("equip", comparativeWords, 2)

                if (!isAuto) break
                if (isTrue2) {
                    isAuto = false
                    break
                }
            }
        }.start()
    }

    private fun strengthen(selection: Int) {
        isAuto = true
        Thread {
            while (isAuto) {
                initAuto()

                //Nhấn chọn cường hóa thần
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(535, 990)

                //Nhấn chọn ô
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 0) click(198, 1746)
                if (selection == 1) click(292, 1746)
                if (selection == 2) click(389, 1746)
                if (selection == 3) click(483, 1746)
                if (selection == 4) click(584, 1746)
                if (selection == 5) click(678, 1746)
                if (selection == 6) click(779, 1746)
                if (selection == 7) click(873, 1746)

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                "strengthen_max".screenCapture()

                if (!isAuto) break
                cropImage("strengthen_max", 109, 1262, 966 - 109, 1360 - 1262)

                if (!isAuto) break
                val isTrue =
                    getTextFromImage(
                        "strengthen_max",
                        listOf("Khong the cuong hoa than them nua"),
                        1
                    )

                if (!isAuto) break
                if (isTrue) {
                    isAuto = false
                    break
                }

                //Nhấn cường hóa
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(303, 2002)

                if (!isAuto) break
                Thread.sleep(7000)
                if (!isAuto) break
                "strengthen".screenCapture()

                if (!isAuto) break
                cropImage("strengthen", 186, 762, 881 - 186, 876 - 762)

                if (!isAuto) break
                val isTrue2 = getTextFromImage("strengthen", listOf("Cuong Hoa Thanh Cong"), 1)

                if (!isAuto) break
                if (isTrue2) backup()
            }
        }.start()
    }

    private fun eraseAttribute(selection: Int) {
        isAuto = true
        Thread {
            while (isAuto) {
                initAuto()

                //Nhấn chọn loại bỏ thuộc tính
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(535, 990)

                //Nhấn chọn ô
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 0) click(198, 1746)
                if (selection == 1) click(292, 1746)
                if (selection == 2) click(389, 1746)
                if (selection == 3) click(483, 1746)
                if (selection == 4) click(584, 1746)
                if (selection == 5) click(678, 1746)
                if (selection == 6) click(779, 1746)
                if (selection == 7) click(873, 1746)

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                "erase_attribute_max".screenCapture()

                if (!isAuto) break
                cropImage("erase_attribute_max", 125, 1490, 817, 87)

                if (!isAuto) break
                val isTrue =
                    getTextFromImage(
                        "erase_attribute_max",
                        listOf("Khong co thuoc tinh am de loai bo"),
                        1
                    )

                if (!isAuto) break
                if (isTrue) {
                    isAuto = false
                    break
                }

                //Nhấn loại bỏ
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(303, 2002)

                if (!isAuto) break
                Thread.sleep(7000)
                if (!isAuto) break
                "erase_attribute".screenCapture()

                if (!isAuto) break
                cropImage("erase_attribute", 206, 783, 663, 85)

                if (!isAuto) break
                val isTrue2 =
                    getTextFromImage(
                        "erase_attribute",
                        listOf("Da loai bo"),
                        1
                    )

                if (!isAuto) break
                if (isTrue2) backup()
            }
        }.start()
    }

    private fun ridingAnimal() {
        isAuto = true
        Thread {
            while (isAuto) {
                initAuto()

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(1005, 910)

                if (!isAuto) break
                Thread.sleep(5000)
                if (!isAuto) break
                click(190, 2265)

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(377, 1672)

                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(274, 1517)

                if (!isAuto) break
                Thread.sleep(2000)
                if (!isAuto) break
                "riding_animal".screenCapture()

                if (!isAuto) break
                cropImage("riding_animal", 98, 754, 266, 68)

                if (!isAuto) break
                val isTrue2 =
                    getTextFromImage(
                        "riding_animal",
                        listOf("LEO S", "BLUBEE S", "PINIA S", "INFERNO S"),
                        1
                    )

                if (!isAuto) break
                if (isTrue2) backup()
            }
        }.start()
    }
}
