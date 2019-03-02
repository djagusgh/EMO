package com.google.firebase.encore.koreanfood

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebViewClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_eachfood.*
import org.jetbrains.anko.startActivity

class EachfoodActivity : AppCompatActivity() {

    var argmax: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eachfood)

        argmax = intent.getStringExtra("argmax")
        Log.d("argmax===", argmax.toString())

        val food_idx = (argmax!!.toInt() + 1).toString()

        // 웹뷰 설정
        eachFoodWebView.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
        }

        // 액션 바 추가
        setSupportActionBar(my_toolbar4)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 웹뷰 url 에 추가
        eachFoodWebView.loadUrl("http://54.180.124.181/eng_info/$food_idx")
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
