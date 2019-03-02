package com.google.firebase.encore.koreanfood.navigation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.firebase.encore.koreanfood.MainActivity
import com.google.firebase.encore.koreanfood.R
import kotlinx.android.synthetic.main.activity_main.*

class InfoFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val mainView = inflater.inflate(R.layout.fragment_info, container, false)
        var foodWebView = mainView.findViewById<WebView>(R.id.foodWebView)


        foodWebView.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
        }

        foodWebView.loadUrl("http://54.180.124.181/")


        return mainView
    }

    override fun onResume() {
        super.onResume()
        var mainActivity = activity as MainActivity
        mainActivity.progress_bar.visibility = View.INVISIBLE

    }

    companion object {
        fun newInstance(): InfoFragment = InfoFragment()
    }
}
