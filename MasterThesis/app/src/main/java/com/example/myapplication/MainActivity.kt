package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: DeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressDialog: Dialog
    private lateinit var httpClient: io.ktor.client.HttpClient

    // Bluetooth Handlers
    private lateinit var bleScanHandler: BluetoothScanHandler
    private lateinit var bluetoothConnectionHandler: BluetoothConnectionHandler

    companion object {
        private const val TAG = "MainActivity"
    }

    /***
     * Methods
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.rvDeviceList)
        adapter = DeviceAdapter(this, deviceList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        bluetoothConnectionHandler = BluetoothConnectionHandler(this)
        BluetoothConnectionManager.connectionHandler = bluetoothConnectionHandler
        httpClient = HttpClient(CIO)
        bleScanHandler = BluetoothScanHandler(deviceList, adapter)

        printInfo("Enable GPS!")
        // Check for BLE
        configBLE()


        val job = CoroutineScope(Dispatchers.IO).launch {
            val response = httpClient.request("http://192.168.56.1:8080") {
                method = HttpMethod.Get
            }

            Log.d(TAG, response.toString())
        }

        runBlocking {
            job.join()
        }


        Log.d("MainActivity", "onCreate called")


    }

    /*
     *  Check for BLE enabling of the device
     */
    private fun configBLE()
    {
        if (!bleScanHandler.checkBLE()) {
            printInfo("BLE adapter is null")
            return
        }
        val REQUEST_ENABLE_BT = 1000
        val LOCATION_PERMISSION_REQUEST_CODE = 1
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            printInfo("Request Permission")
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun printInfo(msg: String)
    {
        val textview: TextView = findViewById(R.id.tvDeviceInfo)
        textview.text = msg
    }

    @SuppressLint("MissingPermission")
    fun btScan(view: View)
    {
        printInfo("start BLE Scan")
        bleScanHandler.startScan()
    }


    @SuppressLint("MissingPermission")
    fun btConnectSensor(view: View)
    {
        printInfo("Connect to Sensor")
        CoroutineScope(Dispatchers.Main).launch {
            showProgressDialog()
        }
        bluetoothConnectionHandler.connectOrBondSensor()
    }

    private fun showProgressDialog()
    {
        progressDialog = Dialog(this)
        progressDialog.setContentView(R.layout.dialog_progress)
        progressDialog.show()
    }

}


