package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract.Constants
import android.widget.TextView

class DataPresenter : AppCompatActivity()
{
	private var deviceMacAddress: String? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

		deviceMacAddress = intent.getStringExtra("Key").toString()

		val tvMacAddress = findViewById<TextView>(R.id.tvMacAddress)
		tvMacAddress.text = deviceMacAddress
	}
}