package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA
import com.google.gson.Gson
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.pow

enum class BatteryStates(val value: UInt) {
    Normal(0U), Battery_low(1U), Battery_high(2U), Reserve(3U)
}

class DataPresenter : AppCompatActivity() {
    private lateinit var deviceMacAddress: String
    private lateinit var tvMacAddress: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvCurrentPower: TextView
    private lateinit var ivBatteryState: ImageView
    private lateinit var graphView: GraphView
    private lateinit var bluetoothConnectionHandler: BluetoothConnectionHandler
    private var temperatureDifferenceArray: ByteArray? = ByteArray(sizeTemperatureDifferenceArray)
    private var temperatureDifferenceArrayDouble: DoubleArray? = DoubleArray(sizeTemperatureDifferenceArray)
    private var batteryLevelStateArray: ByteArray? = ByteArray(sizeBatteryLevel)
    private var limitDataPacketCounter: Int = 0 // datapacket counter up to 5 datapackets
    private var dataPacketCounter: Int = 0 // datapacket counter endless
    private var temperatureArrayList = ArrayList<DoubleArray>(numberOfDataArraysReceived)
    private var jsonEntryList = ArrayList<JsonEntry>()
    private lateinit var fileOutputStream: FileOutputStream
    private lateinit var jsonFile: File

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
        private const val bodyTemperature = 36		// °C
        private const val ambientTemperature = 26		// °C
        private const val ocVoltageMeasured = 45		// mV
        // in 1mV/K
        const val seebeck_coefficient_mVperK = ocVoltageMeasured/(bodyTemperature - ambientTemperature)		// Measured Seebeck-Coefficient of 4 TEG1-30-30 in serie
        const val innerResistanceTEG = 6.8 // two times 4 TEGs with 3.4Ohm in series and parallel
    }

    override fun onCreate(savedInstanceState: Bundle?)
	{
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_presenter)

        this.tvMacAddress = findViewById(R.id.tvMacAddress)
        this.tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        this.tvCurrentPower = findViewById(R.id.tvCurrentPower)
        this.ivBatteryState = findViewById(R.id.ivBatteryState)
        this.deviceMacAddress = intent.getStringExtra(KEY_DEVICE_ADDRESS).toString()
        this.tvMacAddress.text = deviceMacAddress
        this.graphView = findViewById(R.id.GraphView)
        bluetoothConnectionHandler = BluetoothConnectionManager.connectionHandler
        createJsonPath()
        Log.d(TAG, "DataPresenter created: $deviceMacAddress")
    }

    private fun createJsonPath()
	{
        val path = System.getProperty("user.dir")
        if (path != null)
        {
            Log.d(TAG, path)
        }
        // Pfad: internal/Android/data/com.example.myapplication/files/Documents/
        try {
            val externalDocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val directoryName = "SmartBodySensor_logs"
            val directory = File(externalDocumentsDir, directoryName)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val timeStamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val fileName = "log_$timeStamp.json"
            jsonFile = File(directory, fileName)
            Log.d(TAG, directory.toString())

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
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
            updatePower()
        }
        CoroutineScope(Dispatchers.Main).launch {
            batteryVoltageLevelVolt?.let {
                updateBattery(it)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            updateGraph(currentTemperatureDifferencePoints)
        }
        // Store in json file
        CoroutineScope(Dispatchers.IO).launch {
            convertJson(batteryVoltageLevelVolt)
        }


        Log.d(TAG, "At the end!")
    }

    private fun convertJson(batteryVoltageLevelVolt: Double?)
	{
        for (i in 0 until sizeTemperatureDifferenceArray) {
            val jsonEntry = JsonEntry(
                sizeTemperatureDifferenceArray * (dataPacketCounter - 1) + i,
                temperatureDifferenceArrayDouble?.get(i)?.div(10),
                batteryVoltageLevelVolt
            )
            jsonEntryList.add(jsonEntry)
        }

        try
        {
            val jsonSeries = Gson().toJson(jsonEntryList)
            fileOutputStream = FileOutputStream(jsonFile)
            fileOutputStream.write(jsonSeries.toByteArray())
            fileOutputStream.close()
            Log.d(TAG, jsonSeries)
        } catch (e :Exception)
        {
            e.printStackTrace()
        }
    }

    private fun limitNumberDataAndIncreaseCounter()
	{
        dataPacketCounter++
        // limit ArrayList to 60 entries
        if (limitDataPacketCounter == numberOfDataArraysReceived) {
            temperatureArrayList.removeAt(0)
        } else {
            limitDataPacketCounter++
        }
    }

    private fun readBatteryStateAndVoltageLevel(): Pair<BatteryStates?, Double?>
	{
        val batteryState = batteryLevelStateArray?.let {
            (it[0].toUInt() and maskBits6To7) shr 6
        }?.toEnum<BatteryStates>()
        val batteryVoltageLevelVolt = batteryLevelStateArray?.let {
            val batteryVoltageLevelDigits =
                ((it[0].toUInt() and maskBits0To5) or it[1].toUInt()).toUShort().and(maskBits0To13)
                    .toFloat()
            val batteryLevel =
                batteryVoltageLevelDigits * adcReference * adcGain / 2F.pow(adcResolutionBits)
            batteryLevel
        }
        return Pair(batteryState, batteryVoltageLevelVolt)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBattery(batteryVoltageLevelVolt: Double?)
	{
        val df = DecimalFormat("#.##")
        tvBatteryLevel.text = df.format(batteryVoltageLevelVolt).toString() + 'V'
        if (batteryVoltageLevelVolt != null) {
            if (batteryVoltageLevelVolt > 3.0F) {
                ivBatteryState.setImageResource(R.drawable.battery_full)
            } else {
                ivBatteryState.setImageResource(R.drawable.battery_empty)
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePower()
    {
        val powerInuW = (temperatureDifferenceArrayDouble?.average()?.times(seebeck_coefficient_mVperK))?.pow(2)
            ?.div(4 * innerResistanceTEG * 100) // 100: divider because ten times temperature Difference.
        val df = DecimalFormat("#.#")
        tvCurrentPower.text = df.format(powerInuW).toString() + "uW"
    }

    private fun updateGraph(currentTemperatureDifferencePoints: Array<DataPoint>)
	{
        // show the 60 most actual data points
        graphView.viewport.scrollToEnd()

        graphView.viewport.setMinX(sizeTemperatureDifferenceArray.toDouble() * (dataPacketCounter - limitDataPacketCounter) / dividerDataPointsToMinutes)
        graphView.viewport.setMaxX(sizeTemperatureDifferenceArray.toDouble() * dataPacketCounter / dividerDataPointsToMinutes)

        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.isScrollable = true
        val series = LineGraphSeries(currentTemperatureDifferencePoints)
        graphView.removeAllSeries()
        graphView.addSeries(series)
    }

    private fun convertArrayToDataPoints(): Array<DataPoint>
	{
        // convert to Double[]
        temperatureDifferenceArrayDouble =
            temperatureDifferenceArray?.map { it.toDouble() }?.toDoubleArray()

        temperatureDifferenceArrayDouble?.let {
            temperatureArrayList.add(it)
        }

        val flattenedArray = temperatureArrayList.flatMap { it.asIterable() }.toDoubleArray()
        return flattenedArray.let {
            val dataPoints = Array(limitDataPacketCounter * sizeTemperatureDifferenceArray) { i ->
                DataPoint(
                    // The calculation for the x axis is for the
                    ((dataPacketCounter - limitDataPacketCounter) * sizeTemperatureDifferenceArray + i.toDouble()) / dividerDataPointsToMinutes,
                    it[i] / temperatureDifferenceTenth
                )
            }
            dataPoints
        }
    }

    //UInt to Enum
    private inline fun <reified T : Enum<T>> UInt.toEnum(): T?
	{
        return enumValues<T>().firstOrNull { it.ordinal.toUInt() == this }
    }

    @SuppressLint("MissingPermission")
    fun btStop(view: View)
	{
        bluetoothConnectionHandler.disconnect()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
