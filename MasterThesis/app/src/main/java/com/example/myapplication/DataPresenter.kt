package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract.Constants
import android.util.Log
import android.widget.TextView
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA

class DataPresenter : AppCompatActivity()
{
	private lateinit var deviceMacAddress: String
	private lateinit var tvData: TextView
	private lateinit var tvMacAddress: TextView

	companion object {
		const val TAG = "DataPresenter"
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

		this.tvData = findViewById(R.id.tvDummyText)
		this.tvMacAddress = findViewById(R.id.tvMacAddress)

		this.deviceMacAddress = intent.getStringExtra(KEY_DEVICE_ADDRESS).toString()

		tvMacAddress.text = deviceMacAddress
		Log.d(TAG, "DataPresenter created: $deviceMacAddress")
	}


	override fun onNewIntent(dataPresenterIntent: Intent?)
	{
		super.onNewIntent(dataPresenterIntent)
		Log.d(TAG, "OnNewIntent")
		dataPresenterIntent?.let { intent ->
			val myString = intent.getStringExtra(KEY_TEMP_DATA).toString()
			tvData.text = myString
		}
	}



}


