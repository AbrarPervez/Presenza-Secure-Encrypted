package com.example.presenza_secureencrypted

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Start the Animated Gradient Background
        val rootLayout = findViewById<ConstraintLayout>(R.id.main_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(4000)
            start()
        }

        // Load Login Page by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}