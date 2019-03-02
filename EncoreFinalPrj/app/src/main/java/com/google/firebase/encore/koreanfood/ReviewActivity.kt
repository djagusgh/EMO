package com.google.firebase.encore.koreanfood

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.encore.koreanfood.dataModel.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_review.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReviewActivity : AppCompatActivity() {

    // Firebase
    var firestore: FirebaseFirestore? = null
    var firebaseStorage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null

    var photoPath: String? = null
    var photoFile: Uri? = null
    var foodName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        setSupportActionBar(my_toolbar3)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Firebase 인스턴스 얻기
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        // 업로드 버튼 비활성화
        addphoto_btn_upload.isEnabled = false

        photoPath = intent.getStringExtra("photoPath")
        foodName = intent.getStringExtra("foodName")

        if (foodName != null){
            foodnameText.text = foodName
        } else {
            foodnameText.text = "take a picture first"
        }


        // Log.d("포토경로", photoPath)

        if (photoPath != null){
            photoFile = Uri.fromFile(File(photoPath))
        }
        addphoto_image.setImageURI(photoFile)

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }

        addphoto_edit_explain.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            // 찍은 사진이 존재하고, 후기 작성 칸이 빈칸이 아닐 때만 값 추가하기
            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (charSequence.toString().trim{it <= ' '}.length > 0 && photoFile != null) {
                    addphoto_btn_upload.isEnabled = true
                } else {
                    addphoto_btn_upload.isEnabled = false
                }
            }
        })

    }
    // firebase Storage에 사진을, cloudFireStore에 후기 정보를 업로드하는 함수
    fun contentUpload() {

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_.png"

        var photoFile: Uri? = null
        if (photoPath != null){
            photoFile = Uri.fromFile(File(photoPath))
        }

        val photoStorageRef = firebaseStorage?.reference?.child("photos")?.child(imageFileName)

        // DB 추가하는 함수
        photoStorageRef?.putFile(photoFile!!)?.addOnCompleteListener { task ->
            toast("Upload Success")
            if (task.isSuccessful) {
                val contentDTO = ContentDTO()
                // 사진 주소
                contentDTO.photoUrl = photoStorageRef.toString()
                // 유저의 UID
                contentDTO.uid = auth?.currentUser?.uid
                // 사진의 리뷰
                contentDTO.review = addphoto_edit_explain.text.toString()
                // 유저의 아이디
                contentDTO.userId = auth?.currentUser?.email
                // 게시물 업로드 시간
                contentDTO.timestamp = timeStamp.toString()
                // 음식 이름
                contentDTO.foodname = foodName

                // 게시물에 데이터 생성
                firestore?.collection("photos")?.document(imageFileName)?.set(contentDTO)

                startActivity<MainActivity>()
            }
        }
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



}
