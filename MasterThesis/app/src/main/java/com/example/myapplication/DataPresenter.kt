package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries


class DataPresenter() : AppCompatActivity()
{
	private lateinit var deviceMacAddress: String
	private lateinit var tvData: TextView
	private lateinit var tvMacAddress: TextView
	private lateinit var graphView: GraphView
	private var temperatureDifferenceArray: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
	//private var temperatureDifferencePreviousArray: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
	private var batteryLevelStateArray: ByteArray? = ByteArray(sizeBatteryLevel)
	private var temperatureSeries: LineGraphSeries<DataPoint> = LineGraphSeries()
	private var dataCounter: Int = 0
	private var temperatureArrayList = ArrayList<DoubleArray>(numberOfDataArraysReceived)

	companion object {
		const val TAG = "DataPresenter"
		const val sizeTemperatureDifferenceArray = 12
		const val sizeBatteryLevel = 2
		const val numberOfDataArraysReceived = 5
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

	/**
	 * The plot should show 10 minutes of temperature difference
	 * This equals to 6 received data plots
	 */
	override fun onNewIntent(dataPresenterIntent: Intent?)
	{
		super.onNewIntent(dataPresenterIntent)
		dataPresenterIntent?.let { intent ->
			val dataArray: ByteArray? = intent.getByteArrayExtra(KEY_TEMP_DATA)
			batteryLevelStateArray = dataArray?.copyOfRange(0, sizeBatteryLevel)
			temperatureDifferenceArray = dataArray?.copyOfRange(sizeBatteryLevel, dataArray.size)
			tvData.text = dataArray.toString()
			Log.d(TAG, dataArray.toString())
		} ?: Log.d(TAG, "intent is null!")

		// convert to Double[]
		val temperatureDifferenceArrayDouble = temperatureDifferenceArray?.map { it.toDouble() }?.toDoubleArray()
		val batteryLevelStateArrayDouble = batteryLevelStateArray?.map { it.toDouble() }?.toDoubleArray()


		if(dataCounter == numberOfDataArraysReceived)
		{
			temperatureArrayList.removeAt(0)
		}
		else {
			dataCounter++
		}

		graphView.viewport.scrollToEnd()

		graphView.viewport.setMinX(0.0);
		graphView.viewport.setMaxX(dataCounter * sizeTemperatureDifferenceArray.toDouble())
		graphView.viewport.isXAxisBoundsManual = true
		graphView.viewport.isScrollable = true

		temperatureDifferenceArrayDouble?.let {
			temperatureArrayList.add(it)
		}
		// FIXME: flattening does not work -> order is wrong
		val flattenedArray = temperatureArrayList.flatMap { it.asIterable() }.toDoubleArray()

		// convert double[] to DataPoint[]
		val currentTemperatureDifferencePoints = flattenedArray.let {
			val dataPoints = Array(dataCounter * sizeTemperatureDifferenceArray) { i ->
				DataPoint(i.toDouble(), it[i]/10)
			}
			dataPoints
		}

		val series = LineGraphSeries<DataPoint>(currentTemperatureDifferencePoints)

		//series.resetData(currentTemperatureDifferencePoints)
		graphView.removeAllSeries()
		graphView.addSeries(series)
		Log.d(TAG, "At the end!")
	}
}
