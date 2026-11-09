package com.example.dynamictheme.ui

import android.os.Bundle
import com.example.dynamictheme.databinding.ActivitySecondBinding

/** Shows that the API theme carries across screens with no extra code. */
class SecondActivity : BaseThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonBack.setOnClickListener { finish() }
    }
}
