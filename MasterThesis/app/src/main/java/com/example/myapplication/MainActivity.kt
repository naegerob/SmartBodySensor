package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity()
{

    private lateinit var adapter: DeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()

    private val bluetoothAdapter: BluetoothAdapter? = getDefaultAdapter()

    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            deviceList.add(result.device)
            adapter.notifyDataSetChanged()
            printInfo("Device added to list")

        }
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            printInfo("Scan Failed")
            //Toast.makeText(this, "Scan Failed", Toast.LENGTH_SHORT).show()
            // Handle scan failures
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> Log.e(TAG, "Scan failed, already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.e(TAG, "Scan failed, application registration failed")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.e(TAG, "Scan failed, feature unsupported")
                SCAN_FAILED_INTERNAL_ERROR -> Log.e(TAG, "Scan failed, internal error")
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
                val REQUEST_ENABLE_BT: Int = 1
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

                printInfo("Bluetooth is not enabled")

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED)
                {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
            }
            else
            {
                printInfo("Bluetooth is enabled")
                // Request user to enable BLE

            }
        }
    }

    @SuppressLint("MissingPermission")
    fun btScan(view: View) {

        // Stop after 10s
        val handler = Handler()
        handler.postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            printInfo("Scan stopped")
        }, 10000) //10s
        bluetoothLeScanner?.startScan(null, ScanSettings.Builder().build(), scanCallback)
        Toast.makeText(this, "btScan klicked", Toast.LENGTH_SHORT).show()
    }
}

class DeviceAdapter(private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.bind(device)
    }
}

class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val deviceName: TextView = itemView.findViewById(R.id.device_name)
    private val deviceAddress: TextView = itemView.findViewById(R.id.device_address)

    @SuppressLint("MissingPermission")
    fun bind(device: BluetoothDevice) {
        itemView.findViewById<TextView>(R.id.device_name).text = device.name
        itemView.findViewById<TextView>(R.id.device_address).text = device.address
    }
}