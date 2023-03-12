package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val context: Context, private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

	private val bluetoothConnectionHandler = BluetoothConnectionHandler(context)


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
			// Handle clicks on recyclerView
			// Connect to device
			bluetoothConnectionHandler.connectBluetoothDevice(device)

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