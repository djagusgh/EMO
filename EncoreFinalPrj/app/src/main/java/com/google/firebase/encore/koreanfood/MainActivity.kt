package com.google.firebase.encore.koreanfood

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.encore.koreanfood.navigation.InfoFragment
import com.google.firebase.encore.koreanfood.navigation.MypageFragment
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("회원존재2", FirebaseAuth.getInstance().currentUser.toString())

        progress_bar.visibility = View.VISIBLE

        // Bottom Navigation View
        bottomnavigation_main.setOnNavigationItemSelectedListener(this)

        bottomnavigation_main.selectedItemId = R.id.navigation_info

        setSupportActionBar(my_toolbar4)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.navigation_info -> {
                val infoFragment = InfoFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, infoFragment).commit()

                return true
            }
            R.id.navigation_photo -> {
                startActivity<PhotoActivity>()

                return true
            }
            R.id.navigation_myhistory -> {
                val mypageFragment = MypageFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, mypageFragment).commit()
                return true
            }
        }
        return false
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
