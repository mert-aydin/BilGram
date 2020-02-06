package com.mert.bilgram

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class SignInWithTabs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> setTheme(R.style.AppThemeDark)
            Configuration.UI_MODE_NIGHT_NO -> setTheme(R.style.AppThemeLight)
        }

        if (FirebaseAuth.getInstance().currentUser != null)
            startActivity(Intent(this, FeedActivity::class.java))

        setContentView(R.layout.activity_sign_in_with_tabs)

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        val adapter = TabAdapter(supportFragmentManager)

        adapter.addFragment(SignInFragment(), getString(R.string.sign_in))
        adapter.addFragment(SignUpFragment(), getString(R.string.sign_up))

        viewPager.adapter = adapter

        findViewById<TabLayout>(R.id.tabLayout).setupWithViewPager(viewPager)
    }
}