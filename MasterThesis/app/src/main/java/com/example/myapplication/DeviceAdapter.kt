package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothClass.Device
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

	// Define the interface for onClick listener
	interface OnDeviceClickListener {
		fun setOnDeviceClick(device: BluetoothDevice, position: Int)
	}

	// Create a variable to store the onClick listener
	private var listener: OnDeviceClickListener? = null

	// Set the onClick listener
	fun setOnDeviceClickListener(listener: OnDeviceClickListener) {
		this.listener = listener
	}
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_device, parent, false)
		Log.d("DeviceAdapter", "OnCreateViewHolder called")
		return DeviceViewHolder(view)
	}

	override fun getItemCount(): Int
	{
		return deviceList.size
	}

	override fun onBindViewHolder(holder: DeviceViewHolder, position: Int)
	{
		val device = deviceList[position]
		holder.bind(device)
		Log.d("DeviceAdapter", "onBindViewHolder called")
		holder.itemView.setOnClickListener {
			listener?.setOnDeviceClick(device, position)
		}
	}
	private fun setOnDeviceClick(device: BluetoothDevice, position: Int) {
		//TODO implement handle when on clicked
		Log.d("DeviceAdapter", "setOnDeviceClick called")
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