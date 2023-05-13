package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlin.math.pow


enum class BatteryStates (val value: UInt){
	Normal(0U),
	Battery_low(1U),
	Battery_high(2U),
	Reserve(3U)
}

class DataPresenter : AppCompatActivity()
{
	private lateinit var deviceMacAddress: String
	private lateinit var tvData: TextView
	private lateinit var tvMacAddress: TextView
	private lateinit var tvBatteryLevel: TextView
	private lateinit var ivBatteryState: ImageView
	private lateinit var graphView: GraphView
	private var temperatureDifferenceArray: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
	private var batteryLevelStateArray: ByteArray? = ByteArray(sizeBatteryLevel)
	private var limitDataPacketCounter: Int = 0 // datapacket counter up to 5 datapackets
	private var dataPacketCounter: Int = 0 // datapacket counter endless
	private var temperatureArrayList = ArrayList<DoubleArray>(numberOfDataArraysReceived)

	companion object {
		const val TAG = "DataPresenter"
		const val sizeTemperatureDifferenceArray = 12
		const val sizeBatteryLevel = 2
		const val numberOfDataArraysReceived = 5
		private const val secondsPerMinute = 60
		private const val dataPointPerDelta = 10
		const val dividerDataPointsToMinutes = secondsPerMinute / dataPointPerDelta
		const val temperatureDifferenceTenth = 10
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

		this.tvData = findViewById(R.id.tvDummyText)
		this.tvMacAddress = findViewById(R.id.tvMacAddress)
		this.tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
		this.ivBatteryState = findViewById(R.id.ivBatteryState)
		this.deviceMacAddress = intent.getStringExtra(KEY_DEVICE_ADDRESS).toString()
		this.tvMacAddress.text = deviceMacAddress
		this.graphView =  findViewById(R.id.GraphView)

		Log.d(TAG, "DataPresenter created: $deviceMacAddress")
	}

	/**
	 * The plot should show 10 minutes of temperature difference
	 * This equals to 6 received data packets
	 * Also a conversion from byteArray to a LineGraphSeries with DataPoint is made
	 * byteArray -> ArrayList<Double[]> -> Double[] -> DataPoints[] -> LineGraphSeries[DataPoint]
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

		// Parse battery state and voltage level
		val batteryState: BatteryStates? = batteryLevelStateArray?.let {
			(it[0].toUInt() and 0xC0U) shr 6
		}?.toEnum<BatteryStates>()

		val batteryVoltageLevelVolt = batteryLevelStateArray?.let {
			val batteryVoltageLevelDigits = ((it[0].toUInt() and 0x3FU) or it[1].toUInt()).toUShort().and(0x3FFFU).toFloat()
			val temp1 = batteryVoltageLevelDigits * 0.6
			val temp2 = temp1 * 6
			val temp3 = temp2 / 2F.pow(14)
			temp3
		}

		// Update GUI
		// Todo: Maybe insert in Coroutine!
		tvBatteryLevel.text = batteryVoltageLevelVolt.toString()

		if (batteryVoltageLevelVolt != null) {
			if(batteryVoltageLevelVolt > 3.0F) {
				ivBatteryState.setImageResource(R.drawable.battery_full)
			} else {
				ivBatteryState.setImageResource(R.drawable.battery_empty)
			}
		}


		Log.d(TAG, "Battery state: $batteryState")
		Log.d(TAG, "Battery voltage: $batteryVoltageLevelVolt")



		dataPacketCounter++

		// limit ArrayList to 60 entries
		if(limitDataPacketCounter == numberOfDataArraysReceived)
		{
			temperatureArrayList.removeAt(0)
		}
		else {
			limitDataPacketCounter++
		}
		temperatureDifferenceArrayDouble?.let {
			temperatureArrayList.add(it)
		}

		val flattenedArray = temperatureArrayList.flatMap { it.asIterable() }.toDoubleArray()

		// convert double[] to DataPoint[]
		/*
		val currentTemperatureDifferencePoints = flattenedArray.mapIndexed { index, value ->

			DataPoint(index.toDouble() / dividerDataPointsToMinutes, value / temperatureDifferenceTenth)
		}.toTypedArray()

		*/
		val currentTemperatureDifferencePoints = flattenedArray.let {
			val dataPoints = Array(limitDataPacketCounter * sizeTemperatureDifferenceArray) { i ->
				DataPoint(i.toDouble(), it[i] / temperatureDifferenceTenth/ dividerDataPointsToMinutes)
			}
			dataPoints
		}


		// show the 60 most actual data points
		graphView.viewport.scrollToEnd()
		graphView.viewport.setMinX(currentTemperatureDifferencePoints.size.toDouble() - limitDataPacketCounter * sizeTemperatureDifferenceArray)
		graphView.viewport.setMaxX(currentTemperatureDifferencePoints.size.toDouble())
		graphView.viewport.isXAxisBoundsManual = true
		graphView.viewport.isScrollable = true

		val series = LineGraphSeries(currentTemperatureDifferencePoints)

		graphView.removeAllSeries()
		graphView.addSeries(series)
		Log.d(TAG, "At the end!")
	}

	//UInt to Enum
	private inline fun <reified T : Enum<T>> UInt.toEnum(): T? {
		return enumValues<T>().firstOrNull { it.ordinal.toUInt() == this }
	}
}
