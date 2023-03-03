package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothClass.Device
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

	// Define the interface for onClick listener
	interface DeviceClickListener {
		fun onDeviceClick(device: BluetoothDevice)
	}

	// Create a variable to store the onClick listener
	//private var listener: OnDeviceClickListener? = null

	// Set the onClick listener
	/*fun setOnDeviceClickListener(listener: OnDeviceClickListener) {
		this.listener = listener
	}*/
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_device, parent, false)
		Log.d("DeviceAdapter", "OnCreateViewHolder called")
		return DeviceViewHolder(view)
	}

	override fun getItemCount(): Int
	{
		return deviceList.size
	}

	@SuppressLint("MissingPermission")
	override fun onBindViewHolder(holder: DeviceViewHolder, position: Int)
	{
		val device = deviceList[position]
		holder.bind(device)
		Log.d("DeviceAdapter", "onBindViewHolder called")
		holder.itemView.setOnClickListener {
			val context = holder.itemView.context

			val connectGatt = device.connectGatt(context, false, gattCallback)
			// TODO: Set to default is open
			/*
			holder.itemView.background = ContextCompat.getDrawable(
				context, R.drawable.selected_device_in_list
			)
			*/
		}
	}
	private val gattCallback = object : BluetoothGattCallback() {
		// handle connection state changes, services discovered, etc.
		//Log.d("DeviceAdapter", "Bluetoothcallback called")

		@SuppressLint("MissingPermission")
		override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				// Connection successful, discover services
				gatt?.discoverServices()
				Log.d("DeviceAdapter", "Connection successful")
			}
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// Services discovered successfully
				// Do something with the services

				Log.d("DeviceAdapter", "Services discovered")
				// TODO
			}
		}
	}

}


class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	private val deviceName: TextView = itemView.findViewById(R.id.tvdevice_name)
	private val deviceAddress: TextView = itemView.findViewById(R.id.tvdevice_address)

	@SuppressLint("MissingPermission")
	fun bind(device: BluetoothDevice)
	{
		Log.d("DeviceViewHolder", "bind called " + device.name.toString() + " " + device.address.toString())
		if(device.name != null)
		{
			deviceName.text = device.name
		}
		else {
			deviceName.text = "null"
		}
		deviceAddress.text = device.address

	}
}