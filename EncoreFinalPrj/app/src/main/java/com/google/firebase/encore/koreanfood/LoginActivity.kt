package com.google.firebase.encore.koreanfood

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity


class LoginActivity : AppCompatActivity() {

    // Firbase Authentication 관리 클래스
    var auth: FirebaseAuth? = null

    // GoogleLogin 관리 클래스
    var googleSIgnInClient: GoogleSignInClient? = null
    // GoogleLogin
    val GOOGLE_LOGIN_CODE = 9001 // Intent Request ID

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



        // Firebase 로그인 통합 관리하는 Object 만들기
        auth = FirebaseAuth.getInstance()


        // 구글 로그인 옵션
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        // 구글 로그인 클래스를 만듦
        googleSIgnInClient = GoogleSignIn.getClient(this, gso)

        // 구글 로그인 버튼 세팅
        google_sign_in_button.setOnClickListener { googleLogin() }

        // 이메일 로그인 세팅
        email_login_button.setOnClickListener { emailLogin() }

    } // end of Oncreate

    fun moveMainPage(user : FirebaseUser?){
        // User is signed in
        if (user != null) {
            Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show()
            startActivity<MainActivity>()
            finish()
        }
    }

    // 이메일 회원 가입 및 로그인 메소드
    fun createAndLoginEmail() {

        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),
            password_edittext.text.toString())?.addOnCompleteListener {task ->
            progress_bar.visibility = View.GONE

            if(task.isSuccessful) {
                // 아이디 생성이 성공했을 경우
                Toast.makeText(this, getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()
                moveMainPage(auth?.currentUser)
            } else if (task.exception?.message.isNullOrEmpty()){
                // 회원 가입 에러가 발생했을 경우
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            } else {
                signInEmail()
            }

        }
    } // end of createAndLoginEmail

    // id, password 텍스트 창이 비어있는지 확인
    fun emailLogin() {

        if (email_edittext.text.toString().isNullOrEmpty() || password_edittext.text.toString().isNullOrEmpty()){
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        } else {
            progress_bar.visibility = View.VISIBLE
            createAndLoginEmail()
        }
    }

    // 이메일 로그인 메서드
    fun signInEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener { task ->
            progress_bar.visibility = View.GONE

            if (task.isSuccessful){
                // 로그인 성공 및 다음 페이지 호출
                moveMainPage(auth?.currentUser)
            } else {
                // 로그인 실패
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun googleLogin(){
        progress_bar.visibility = View.VISIBLE
        var signInIntent = googleSIgnInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 구글에서 승인된 정보를 가지고 오기
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (result.isSuccess){
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                progress_bar.visibility = View.GONE
            }
        }
    } // end of onActivityResult

    // 구글 로그인이 성공했을 때, Token값을 Credential로 변환하고 Firebase에 넘겨줘서 계정을 생성하는 코드
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            progress_bar.visibility = View.GONE

            if (task.isSuccessful) {
                // 다음 페이지 호출
                moveMainPage(auth?.currentUser)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 자동 로그인 설정
        moveMainPage(auth?.currentUser)
    }

    override fun onBackPressed() {

    }

}
