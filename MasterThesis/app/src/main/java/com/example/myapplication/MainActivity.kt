package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.getDefaultAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start BLE
        configBLE()

    }



    /***
     *
     */
    private fun configBLE()
    {
        val bluetoothAdapter: BluetoothAdapter? = getDefaultAdapter()
        if (bluetoothAdapter == null)
        {
            printInfo("BLE adapter not supported")
        }
        else {
            if (!bluetoothAdapter.isEnabled) {
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

    /**
     *
     */
    class DeviceListAdapter(context: Context) : ArrayAdapter<BluetoothDevice>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val device = getItem(position)
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_device, parent, false)
            val deviceNameTextView = view.findViewById<TextView>(R.id.deviceName)
            val deviceAddressTextView = view.findViewById<TextView>(R.id.textView)
            deviceNameTextView.text = device?.name
            deviceAddressTextView.text = device?.address
            return view
        }
    }
    /**
     *
     */

    private fun printInfo(msg :String) {
        val textview: TextView = findViewById(R.id.textView)
        textview.text = msg
    }

    fun buttonClicked2(view: View) {
        printInfo("Test")
    }

    fun buttonClicked(view: View) {
        printInfo("Testat")
    }


}