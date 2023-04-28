package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries



class DataPresenter : AppCompatActivity()
{
	private lateinit var deviceMacAddress: String
	private lateinit var tvData: TextView
	private lateinit var tvMacAddress: TextView
	private lateinit var graphView: GraphView
	private var temperatureDifference: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
	private var temperatureDifferencePrevious: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
	private var batteryLevelState: ByteArray? = ByteArray(sizeBatteryLevel)
	private var temperatureSeries: LineGraphSeries<DataPoint> = LineGraphSeries()


	companion object {
		const val TAG = "DataPresenter"
		const val sizeTemperatureDifferenceArray = 12
		const val sizeBatteryLevel = 2
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

		this.tvData = findViewById(R.id.tvDummyText)
		this.tvMacAddress = findViewById(R.id.tvMacAddress)

		this.deviceMacAddress = intent.getStringExtra(KEY_DEVICE_ADDRESS).toString()

		tvMacAddress.text = deviceMacAddress

		graphView =  findViewById(R.id.idGraphView)



		Log.d(TAG, "DataPresenter created: $deviceMacAddress")
	}


	override fun onNewIntent(dataPresenterIntent: Intent?)
	{
		super.onNewIntent(dataPresenterIntent)
		dataPresenterIntent?.let { intent ->
			val byteArray: ByteArray? = intent.getByteArrayExtra(KEY_TEMP_DATA)
			batteryLevelState = byteArray?.copyOfRange(0, sizeBatteryLevel)
			temperatureDifference = byteArray?.copyOfRange(sizeBatteryLevel, byteArray.size)
			tvData.text = byteArray.toString()
			Log.d(TAG, byteArray.toString())
		} ?: Log.d(TAG, "intent is null!")

		// convert byte array into datapoints
		temperatureDifferencePrevious = temperatureDifference
		temperatureSeries.resetData(temperatureDifferencePrevious)
		// shitty code
		// TODO: check if temperaturedifference is null
		for (i in temperatureDifference?.indices!!)
		{
			temperatureSeries.appendData(DataPoint(i.toDouble(), (temperatureDifference!![i] / 10).toDouble()), true, temperatureDifference!!.size )
		}
		graphView.addSeries(temperatureSeries)

	}



}


