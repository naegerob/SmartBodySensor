package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract.Constants
import android.widget.TextView
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA

class DataPresenter : AppCompatActivity()
{
	private lateinit var deviceMacAddress: String
	private val tvData: TextView = findViewById(R.id.tvDummyText)


	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

		this.deviceMacAddress = intent.getStringExtra(KEY_DEVICE_ADDRESS).toString()

		val tvMacAddress = findViewById<TextView>(R.id.tvMacAddress)
		tvMacAddress.text = deviceMacAddress
	}

	override fun onNewIntent(dataPresenterIntent: Intent?)
	{
		super.onNewIntent(dataPresenterIntent)

		dataPresenterIntent?.let { intent ->
			val myString = intent.getStringExtra(KEY_TEMP_DATA).toString()
			tvData.text = myString
		}

	}



}