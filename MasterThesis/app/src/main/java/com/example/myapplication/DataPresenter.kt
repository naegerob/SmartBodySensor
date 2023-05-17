package com.example.myapplication

import android.annotation.SuppressLint
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
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
	private var temperatureDifferenceArrayDouble: DoubleArray? = DoubleArray(sizeTemperatureDifferenceArray)
	private var batteryLevelStateArray: ByteArray? = ByteArray(sizeBatteryLevel)
	private var limitDataPacketCounter: Int = 0 // datapacket counter up to 5 datapackets
	private var dataPacketCounter: Int = 0 // datapacket counter endless
	private var temperatureArrayList = ArrayList<DoubleArray>(numberOfDataArraysReceived)
	private var jsonEntryList = ArrayList<JsonEntry>()

	companion object {
		const val TAG = "DataPresenter"
		const val sizeTemperatureDifferenceArray = 12
		const val sizeBatteryLevel = 2
		const val numberOfDataArraysReceived = 5
		private const val secondsPerMinute = 60
		private const val dataPointPerDelta = 10
		const val dividerDataPointsToMinutes = secondsPerMinute / dataPointPerDelta
		const val temperatureDifferenceTenth = 10
		const val adcReference = 0.6
		const val adcGain = 6
		const val adcResolutionBits = 14
		const val maskBits0To5 = 0x3FU
		const val maskBits0To13: UShort = 0x3FFFU
		const val maskBits6To7 = 0xC0U
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_data_presenter)

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
			Log.d(TAG, dataArray.toString())
		} ?: Log.d(TAG, "intent is null!")

		val (batteryState, batteryVoltageLevelVolt) = readBatteryStateAndVoltageLevel()

		Log.d(TAG, "Battery state: $batteryState")
		Log.d(TAG, "Battery voltage: $batteryVoltageLevelVolt")

		// limit dataPoints for the graph
		limitNumberDataAndIncreaseCounter()

		val currentTemperatureDifferencePoints = convertArrayToDataPoints()
		// Update GUI
		CoroutineScope(Dispatchers.Main).launch {
			updateBattery(batteryVoltageLevelVolt)
		}
		CoroutineScope(Dispatchers.Main).launch {
			updateGraph(currentTemperatureDifferencePoints)
		}

		convertJson(batteryVoltageLevelVolt)

		Log.d(TAG, "At the end!")
	}

	private fun convertJson(batteryVoltageLevelVolt: Double?)
	{
		for (i in 0 until sizeTemperatureDifferenceArray)
		{
			val jsonEntry = JsonEntry(sizeTemperatureDifferenceArray * (dataPacketCounter - 1)+ i, temperatureDifferenceArrayDouble?.get(i), batteryVoltageLevelVolt)
			jsonEntryList.add(jsonEntry)
		}
	}

	private fun limitNumberDataAndIncreaseCounter()
	{
		dataPacketCounter++
		// limit ArrayList to 60 entries
		if(limitDataPacketCounter == numberOfDataArraysReceived)
		{
			temperatureArrayList.removeAt(0)
		}
		else {
			limitDataPacketCounter++
		}
	}

	private fun readBatteryStateAndVoltageLevel(): Pair<BatteryStates?, Double?>
	{
		val batteryState = batteryLevelStateArray?.let {
			(it[0].toUInt() and maskBits6To7) shr 6
		}?.toEnum<BatteryStates>()
		val batteryVoltageLevelVolt = batteryLevelStateArray?.let {
			val batteryVoltageLevelDigits = ((it[0].toUInt() and maskBits0To5) or it[1].toUInt()).toUShort().and(maskBits0To13).toFloat()
			val batteryLevel = batteryVoltageLevelDigits * adcReference * adcGain / 2F.pow(adcResolutionBits)
			batteryLevel
		}
		return Pair(batteryState, batteryVoltageLevelVolt)
	}

	private fun updateBattery(batteryVoltageLevelVolt: Double?) {
		val df = DecimalFormat("#.##")
		tvBatteryLevel.text = df.format(batteryVoltageLevelVolt).toString() + 'V'

		if (batteryVoltageLevelVolt != null) {
			if(batteryVoltageLevelVolt > 3.0F) {
				ivBatteryState.setImageResource(R.drawable.battery_full)
			} else {
				ivBatteryState.setImageResource(R.drawable.battery_empty)
			}
		}

	}

	private fun updateGraph(currentTemperatureDifferencePoints: Array<DataPoint>) {
		// show the 60 most actual data points
		graphView.viewport.scrollToEnd()
<<<<<<< HEAD
		graphView.viewport.setMinX((currentTemperatureDifferencePoints.size.toDouble() -
				limitDataPacketCounter * sizeTemperatureDifferenceArray) * dataPacketCounter)
		graphView.viewport.setMaxX(currentTemperatureDifferencePoints.size.toDouble() * dataPacketCounter)
=======
		graphView.viewport.setMinX(currentTemperatureDifferencePoints.size.toDouble() - limitDataPacketCounter * sizeTemperatureDifferenceArray)
		graphView.viewport.setMaxX(currentTemperatureDifferencePoints.size.toDouble() / dividerDataPointsToMinutes)
>>>>>>> fc16b0b837b7f164d072010207b07053057786ee
		graphView.viewport.isXAxisBoundsManual = true
		graphView.viewport.isScrollable = true
		val series = LineGraphSeries(currentTemperatureDifferencePoints)
		graphView.removeAllSeries()
		graphView.addSeries(series)
	}

	private fun convertArrayToDataPoints() :Array<DataPoint>{
		// convert to Double[]
		temperatureDifferenceArrayDouble = temperatureDifferenceArray?.map { it.toDouble() }?.toDoubleArray()

		temperatureDifferenceArrayDouble?.let {
			temperatureArrayList.add(it)
		}

		val flattenedArray = temperatureArrayList.flatMap { it.asIterable() }.toDoubleArray()
		return flattenedArray.let {
			val dataPoints = Array(limitDataPacketCounter * sizeTemperatureDifferenceArray) { i ->
				DataPoint(i.toDouble() / dividerDataPointsToMinutes, it[i] / temperatureDifferenceTenth)
			}
			dataPoints
		}
	}

	//UInt to Enum
	private inline fun <reified T : Enum<T>> UInt.toEnum(): T? {
		return enumValues<T>().firstOrNull { it.ordinal.toUInt() == this }
	}
}
