package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log

class BluetoothConnectionHandler(private val context: Context, private val targetDeviceAddress: String) : BluetoothGattCallback() {


	@SuppressLint("MissingPermission")
	fun connectBluetoothDevice(context: Context, device: BluetoothDevice)
	{
		device.connectGatt(context, true, this)
	}

	/**
	 * Callbacks
	 */
	@SuppressLint("MissingPermission")
	override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
	{
		super.onConnectionStateChange(gatt, status, newState)
		if (newState == BluetoothProfile.STATE_CONNECTED)
		{
			if (gatt.device.address == targetDeviceAddress) {
				// Start the new activity here
				startNewDataPresenter(targetDeviceAddress)
			}
			gatt.discoverServices()
			Log.d("BluetoothHandler", "Connection successful")
		}
	}
	override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
		super.onServicesDiscovered(gatt, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d("BluetoothHandler", "Services discovered")
		}
	}
	override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
		super.onCharacteristicRead(gatt, characteristic, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			// Characteristic read, do something
			// TODO: Handle characteristic read
			Log.d("BluetoothHandler", "Characteristics read")
		} else {
			// TODO: Handle failed characteristic read
		}
	}
	override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
		super.onCharacteristicChanged(gatt, characteristic)

		//if (characteristic.uuid == YOUR_CHARACTERISTIC_UUID) {
			val data = characteristic.value
			// Process the received data
		//}
		Log.d("BluetoothHandler", "Characteristics changed")
	}

	fun startNewDataPresenter(targetDeviceAddress: String)
	{
		val intent = Intent(context, DataPresenter::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		intent.putExtra("Key", targetDeviceAddress)
		context.startActivity(intent)
	}
}

class BluetoothScanHandler(private var deviceList: MutableList<BluetoothDevice>,
						   private var adapter: DeviceAdapter) : ScanCallback() {

	// Bluetooth Handlers
	private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

	private val scanSettings = ScanSettings.Builder()
		.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
		.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
		.build()

	@SuppressLint("MissingPermission")
	fun startScan()
	{
		val handler = Handler()
		handler.postDelayed({
			bluetoothLeScanner?.stopScan(this)
		}, 5_000)
		bluetoothLeScanner?.startScan(null, scanSettings, this)
	}

	fun checkBLE() : Boolean
	{
		if (bluetoothAdapter == null)
		{
			return false
		}
		return true
	}

	/**
	 * Callbacks
	 */
	@SuppressLint("NotifyDataSetChanged")
	override fun onScanResult(callbackType: Int, result: ScanResult) {
		super.onScanResult(callbackType, result)

		if(!deviceList.contains(result.device))
		{
			deviceList.add(result.device)
		}
		// TODO try to realize more specific
		adapter.notifyDataSetChanged()
		Log.d("MainActivity", "onScanResult called")
	}
	override fun onScanFailed(errorCode: Int) {
		super.onScanFailed(errorCode)
		// Handle scan failures
		when (errorCode) {
			SCAN_FAILED_ALREADY_STARTED -> Log.d("BluetoothScanCallback", "Scan failed, already started")
			SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.d("BluetoothScanCallback", "Scan failed, application registration failed")
			SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.d("BluetoothScanCallback", "Scan failed, feature unsupported")
			SCAN_FAILED_INTERNAL_ERROR -> Log.d("BluetoothScanCallback", "Scan failed, internal error")
		}
	}
}