package com.example.adminiums1.ui.limpieza

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityLimpiezaBinding

class LimpiezaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLimpiezaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLimpiezaBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
