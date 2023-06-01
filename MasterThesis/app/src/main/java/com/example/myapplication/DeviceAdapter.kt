package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val context: Context, private val deviceList: List<BluetoothDevice>) :
    RecyclerView.Adapter<DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder
    {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_device, parent, false) //TODO: what if without parent.context?
        Log.d("DeviceAdapter", "OnCreateViewHolder called")
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int
    {
        return deviceList.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]


        holder.bind(device)
        Log.d("DeviceAdapter", "onBindViewHolder called")
        holder.itemView.setOnClickListener {
        }

        // Hintergrundfarbe aktualisieren, basierend auf gerader oder ungerader Position in der Liste.
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.lightgrey
                )
            )
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

    }
}


class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val deviceName: TextView = itemView.findViewById(R.id.tvdevice_name)
    private val deviceAddress: TextView = itemView.findViewById(R.id.tvdevice_address)

    @SuppressLint("MissingPermission")
    fun bind(device: BluetoothDevice)
    {
        Log.d(
            "DeviceViewHolder",
            "bind called " + device.name.toString() + " " + device.address.toString()
        )
        if (device.name != null) {
            deviceName.text = device.name
        } else {
            deviceName.text = "null"
        }
        deviceAddress.text = device.address

    }
}