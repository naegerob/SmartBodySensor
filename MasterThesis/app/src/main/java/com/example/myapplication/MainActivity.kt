package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.UiModeManager.MODE_NIGHT_NO
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.checkBluetoothAddress
import android.bluetooth.BluetoothAdapter.getDefaultAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity()
{

    private lateinit var adapter: DeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val bluetoothAdapter: BluetoothAdapter? = getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .build()

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            deviceList.add(result.device)
            adapter.notifyDataSetChanged()
            printInfo("Device added to list")
            Log.d("MainActivity", "onScanResult called")
        }
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // Handle scan failures
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> printInfo( "Scan failed, already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> printInfo("Scan failed, application registration failed")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> printInfo("Scan failed, feature unsupported")
                SCAN_FAILED_INTERNAL_ERROR -> printInfo("Scan failed, internal error")
            }
        }
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            printInfo("onBatchScanResult")
        }
    }

    /***
     * Methods
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = findViewById<RecyclerView>(R.id.rvDeviceList)

        adapter = DeviceAdapter(deviceList)
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(this)
        // Check for BLE
        configBLE()
        if (bluetoothAdapter == null)
        {
            printInfo("BluetoothAdapter is null")
        }
        if (bluetoothLeScanner == null)
        {

            Toast.makeText(this, "bluetoothScanner is null", Toast.LENGTH_SHORT).show()
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Log.d("MainActivity", "onCreate called")
    }

    private fun printInfo(msg :String)
    {
        val textview: TextView = findViewById(R.id.tvDeviceInfo)
        textview.text = msg
    }

    /*
     *  Check for BLE enabling of the device
     */
    private fun configBLE()
    {
        if (bluetoothAdapter == null)
        {
            printInfo("BLE adapter not supported")
        }
        else {
            if (!bluetoothAdapter.isEnabled)
            {
                printInfo("Bluetooth is not enabled")
            }
            else
            {
                printInfo("Bluetooth is enabled")
            }
            val REQUEST_ENABLE_BT: Int = 1000
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            {
                printInfo("Request Permission")
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ENABLE_BT)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun btScan(view: View) {
        //val filter:
        // Stop after 10s
        val handler = Handler()
        handler.postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            printInfo("Scan stopped")
        }, 10_000) //10s
        bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
    }
}

class DeviceAdapter(private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main, parent, false)
        Log.d("DeviceAdapter", "OnCreateViewHolder called")
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.bind(device)

        Log.d("DeviceAdapter", "onBindViewHolder called")
    }
}

class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @SuppressLint("MissingPermission")
    fun bind(device: BluetoothDevice) {

        //itemView.findViewById<TextView>(R.id.tvdevice_name).text = device.name
        itemView.tvdevice_address.text = device.address
        itemView.findViewById<RecyclerView>(R.id.rvDeviceList).

        Log.d("DeviceViewHolder", "bind called" + device.name.toString() + " " + device.address.toString())
    }
}