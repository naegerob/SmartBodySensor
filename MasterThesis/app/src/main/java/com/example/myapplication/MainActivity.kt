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

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: DeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressDialog: Dialog

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

        bleScanHandler = BluetoothScanHandler(deviceList, adapter)

        printInfo("Enable GPS!")
        // Check for BLE
        configBLE()

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


