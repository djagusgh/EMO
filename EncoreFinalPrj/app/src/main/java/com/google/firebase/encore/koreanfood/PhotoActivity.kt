package com.google.firebase.encore.koreanfood

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.encore.koreanfood.dataModel.FoodMap
import com.google.firebase.encore.koreanfood.dataModel.InfoDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions
import com.google.firebase.storage.FirebaseStorage
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton
import kotlinx.android.synthetic.main.activity_photo.*
import kotlinx.android.synthetic.main.custom_cookie.*
import org.aviran.cookiebar2.CookieBar
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class PhotoActivity : AppCompatActivity() {

    private val GALLERY = 2
    private val CAMERA = 3

    private var interpreter: FirebaseModelInterpreter? = null
    private lateinit var conditionsBuilder: FirebaseModelDownloadConditions.Builder
    private lateinit var inputOutputOptions: FirebaseModelInputOutputOptions

    // Firebase
    var firestore: FirebaseFirestore? = null
    var firebaseStorage: FirebaseStorage? = null

    private var auth: FirebaseAuth? = null

    // Bitmap 이미지
    var bitmap: Bitmap? = null

    // 저장된 사진의 절대 경로, 음식 이름, 최대 확률 음식의 인덱스
    var photoPath: String? = null
    var foodName: String? = null
    var argmax: String? = null


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        setSupportActionBar(my_toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Firebase 인스턴스 얻기
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()


        // 오른쪽 하단 FloatingActionButton 만들기
        makeFloatingBtn()

        // 딥러닝 모델 관련 초기화
        initKoreanFoodClassifier()

        // 자동으로 갤러리/카메라 쪽으로 넘어가게
        showPictureDialog()

        // 사진 찍는 버튼 액션 추가
        retryBtn.setOnClickListener {
            showPictureDialog()
        }


    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    fun makeFloatingBtn(){
        val icon = ImageView(this) // Create an icon
        icon.setImageResource(R.drawable.plus)

        val actionButton = FloatingActionButton.Builder(this).setContentView(icon).build()

        //// Sub button 추가

        // 리뷰 추가 버튼
        val itemBuilder = SubActionButton.Builder(this)
        val reviewIcon = ImageView(this)
        reviewIcon.setImageResource(R.drawable.review)
        val reviewBtn = itemBuilder.setContentView(reviewIcon).build()

        // 음식 정보 확인 버튼
        val itemBuilder2 = SubActionButton.Builder(this)
        val infoIcon = ImageView(this)
        infoIcon.setImageResource(R.drawable.info)
        val infoBtn = itemBuilder2.setContentView(infoIcon).build()

        // 알레르기 위험 정보 확인 버튼
        val itemBuilder3 = SubActionButton.Builder(this)
        val allergyIcon = ImageView(this)
        allergyIcon.setImageResource(R.drawable.warning)
        val allergyBtn = itemBuilder3.setContentView(allergyIcon).build()


        // 액션 추가
        reviewBtn.setOnClickListener {
            if (foodName != null) {
                goReviewWithPhotoPath(photoPath)
            } else {
                toast("take a picture first!")
            }

        }

        allergyBtn.setOnClickListener {
            if (foodName != null) {
                openCookieBar()
                setAllergyInfo()
            } else {
                toast("take a picture first!")
            }
        }

        infoBtn.setOnClickListener {
            if (foodName != null) {
                startActivity<EachfoodActivity>(
                    "argmax" to argmax
                )
            } else {
                toast("take a picture first!")
            }
        }

        val actionMenu = FloatingActionMenu.Builder(this).addSubActionView(reviewBtn)
            .addSubActionView(infoBtn).addSubActionView(allergyBtn)
            .attachTo(actionButton).build()

    } // end of MakeFloatingBtn

    fun openCookieBar () {

        CookieBar.build(this@PhotoActivity).setCustomView(R.layout.custom_cookie)
            .setCustomViewInitializer{ it ->
            }.setAction("Close") {
                CookieBar.dismiss(this@PhotoActivity)
            }.setTitle("${foodName}")
            .setMessage(("4 allergy risk information"))
            .setBackgroundColor(R.color.cookie)
            .setEnableAutoDismiss(false)
            .setSwipeToDismiss(false).setCookiePosition(Gravity.BOTTOM).show()

    }

    // 음식별 알러지 정보를 쿠키바에 표시하는 함수

    private fun setAllergyInfo() {

        firestore?.collection("food_info")?.whereEqualTo("name_e", foodName)?.addSnapshotListener{
        querySnapshot, firebaseFirestoreException ->

            // a_e : egg, a_m : milk, a_s : seafood, a_t : nuts
            Log.d("스냅샷은 ", querySnapshot.toString())

            if (querySnapshot != null) {

                var infoDTO = querySnapshot.documents[0].toObject(InfoDTO:: class.java)

                Log.d("음식이름DTO", infoDTO.toString())

                Log.d("음식이름번호1", infoDTO?.a_e)
                Log.d("음식이름번호2", infoDTO?.a_t)
                Log.d("음식이름번호3", infoDTO?.a_m)
                Log.d("음식이름번호4", infoDTO?.a_s)

                // OK, Caution, Danger 여부를 텍스트 뷰에 표시!

                decideMessageAndColor(infoDTO?.a_e, eggAllergy)
                decideMessageAndColor(infoDTO?.a_t, nutsAllergy)
                decideMessageAndColor(infoDTO?.a_m, milkAllergy)
                decideMessageAndColor(infoDTO?.a_s, seafoodAllergy)

//                Log.d("음식이름에그 ", eggAllergy.text.toString())
            }
        }
    }


    @SuppressLint("ResourceAsColor")
    private fun decideMessageAndColor(name: String?, infoText: TextView){
        when(name) {
            "0" -> {
                infoText.text = "OK"
                infoText.setTextColor(R.color.colorEmailSignInPressed)
            }
            "1" -> {
                infoText.text = "Caution"
                infoText.setTextColor(R.color.colorPrimary)
            }
            "2" -> {
                infoText.text = "Danger"
                infoText.setTextColor(R.color.colorGoogleSignInPressed)
            }
        }
    }


    // 사진 촬영, 갤러리에서 가져온 후
    fun goReviewWithPhotoPath(path: String?){
        startActivity<ReviewActivity>(
            "photoPath" to photoPath,
            "foodName" to foodName
        )
    }

    private fun initKoreanFoodClassifier(){
        // 1) FIrebase 호스팅 모델 소스 구성
            conditionsBuilder = FirebaseModelDownloadConditions.Builder().requireWifi()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder.requireCharging().requireDeviceIdle()
        }
        val conditions = conditionsBuilder.build()

        // Build a FirebaseCloudModelSource object by specifying the name you assigned the model
        // when you uploaded it in the Firebase console.

        val cloudSource = FirebaseCloudModelSource.Builder("korean_food_classifier")
            .enableModelUpdates(true)
            .setInitialDownloadConditions(conditions)
            .build()

        Log.d("클라우드", cloudSource.toString())

        FirebaseModelManager.getInstance().registerCloudModelSource(cloudSource)

        // 2) 로컬 모델 소스 구성
        val localSource = FirebaseLocalModelSource.Builder("local_classifier")
            .setAssetFilePath("190209_MobileNetV2.tflite").build()

        Log.d("로컬", localSource.toString())

        FirebaseModelManager.getInstance().registerLocalModelSource(localSource)

        // 3) 모델 소스에서 인터프리터 만들기
        val options = FirebaseModelOptions.Builder()
            .setCloudModelName("korean_food_classifier")
            .setLocalModelName("local_classifier").build()

        interpreter = FirebaseModelInterpreter.getInstance(options)

        // 4) 모델의 입 출력 지정
        // ---> input, output 지정
        inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 150)).build()

    }

    fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems){
                dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    fun choosePhotoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takePhotoFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA)
    }

    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GALLERY) {
            if (data != null) {
                val contentURI = data.data
                try {
                    // PhotoFragment에 있는 imageview, resultText, resultProbText를 받아오는 코드
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                    this.bitmap = bitmap

                    photoView.setImageBitmap(bitmap)
                    // 음식 종류
                    foodInference(bitmap)
                    saveImage(bitmap)
                    //toast("Image Saved!")
                    Handler().postDelayed({
                        openCookieBar()
                        setAllergyInfo()
                    }, 1000)

                }
                catch (e: IOException) {
                    e.printStackTrace()
                    toast("Failed")
                }
            }
        }
        else if (requestCode == CAMERA) {
            if (data!= null){
                val bitmap = data.extras!!.get("data") as Bitmap
                this.bitmap = bitmap

                photoView.setImageBitmap(bitmap)
                // 음식 종류
                foodInference(bitmap)
                saveImage(bitmap)
                toast("Image Saved!")

                // 바로 실행시키면 foodName 변수가 null이 되어 앱이 종료됨 -> 1초 지연시킴
                Handler().postDelayed({
                    openCookieBar()
                    setAllergyInfo()
                }, 1000)

            }
        }
    }

    private fun foodInference(img: Bitmap){

        // 1) 입력 데이터에 대한 추론 수행
        val bitmap = Bitmap.createScaledBitmap(img, 224, 224, true)
        val batchNum = 0
        val input = Array(1) {Array(224) { Array(224) {FloatArray(3) }}}

        for (x in 0..223) {
            for (y in 0..223) {
                val pixel = bitmap.getPixel(x, y)
                // Normalize channel values to [0.0, 1.0]
                input[batchNum][x][y][0] = Color.red(pixel) / 255.0f
                input[batchNum][x][y][1] = Color.green(pixel) / 255.0f
                input[batchNum][x][y][2] = Color.blue(pixel) / 255.0f
            }
        }

        // 2) 입력 데이터로 FirebaseModelInputs 객체를 만들고, 객체의 입출력 사양을
        // 모델 인터프리터의 run 메서드에 전달
        val inputs = FirebaseModelInputs.Builder()
            .add(input).build()

        interpreter!!.run(inputs, inputOutputOptions)?.addOnSuccessListener{ result ->

            val foodMap = FoodMap().foodMap
            val output = result.getOutput<Array<FloatArray>>(0)
            val probabilities = output[0]
            // 최댓값의 index, 확률
            val argmax = probabilities.indexOf(probabilities.max()!!).toString()

            this.argmax = argmax

            val max_prob = probabilities.max()!! * 100

            // 음식 이름 -> reviewActivity로 전달
            foodName = foodMap[argmax]


            // 결과를 텍스트 뷰에 출력
            resultText?.text = "Name : ${foodMap[argmax]}"
            resultProbText?.text = "Probability : ${max_prob}%"


        }?.addOnFailureListener(
            object : OnFailureListener {
                override fun onFailure(e: Exception) {
                    // Task failed with an exception
                    // ...
                    toast("Classification Failed")
                }
            })
    }

    fun saveImage(myBitmap: Bitmap):String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val wallpaperDirectory = File(
            (Environment.getExternalStorageDirectory()).toString() + IMAGE_DIRECTORY)
        // have the object build the directory structure, if needed.
        Log.d("fee",wallpaperDirectory.toString())
        if (!wallpaperDirectory.exists())
        {
            wallpaperDirectory.mkdirs()
        }
        try
        {
            Log.d("heel",wallpaperDirectory.toString())
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                .timeInMillis).toString() + ".jpg"))
            Log.d("파일경로==", f.toString())
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(this,
                arrayOf(f.path),
                arrayOf("image/jpeg"), null)
            fo.close()
            Log.d("TAG", "File Saved::--->" + f.absolutePath)

            photoPath = f.absolutePath
            Log.d("포토처음", photoPath.toString())



            return f.absolutePath
        }
        catch (e1: IOException) {
            e1.printStackTrace()
        }
        return ""
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.sign_out_menu -> {
                // sign out
                FirebaseAuth.getInstance().signOut()
                startActivity<LoginActivity>()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    companion object {
        private val IMAGE_DIRECTORY = "/EMO"
    }

}
