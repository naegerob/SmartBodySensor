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
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import java.util.UUID

class BluetoothConnectionHandler(private val context: Context) : BluetoothGattCallback() {


	private var btPeripheralList = ArrayList<BluetoothDevices>()


	companion object {
		const val TAG = "BluetoothHandler"
		val UUID_HEART_RATE_SERVICE: UUID				= UUID(0x180D.toLong(), 0x0000.toLong())
		val UUID_HEART_RATE_CHARACTERISTICS: UUID 		= UUID(0x2A37.toLong(), 0x0000.toLong())
		val UUID_HEART_RATE_DESCRIPTOR: UUID 			= UUID(0x2902.toLong(), 0x0000.toLong())
	}

	init
	{
		addPeripheralsInList()
	}
	@SuppressLint("MissingPermission")
	fun connectBluetoothDevice(device: BluetoothDevice)
	{
		val isTargetBlePeripheral = btPeripheralList.any { bleDevice ->
			bleDevice.macAddress.contains(device.address.toString())
		}
		// Connect only if in Peripheral List
		if (isTargetBlePeripheral)
		{
			Log.d(TAG, device.address.toString())
			device.connectGatt(context, false, this)
		}
		else
		{
			// TODO: Notify not in correct list
		}
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
			startNewDataPresenter(gatt.device.address)

			gatt.discoverServices()
			Log.d(TAG, "Connection successful")
		}
	}

	private fun addPeripheralsInList()
	{
		// Extend with
		var btDevice = BluetoothDevices("Forerunner735XT", "F8:B6:6B:8E:EF:1F")
		btPeripheralList.add(btDevice)
		btDevice = BluetoothDevices("Samsung Series TV", "70:2A:D5:52:FF:81")
		btPeripheralList.add(btDevice)
		btDevice = BluetoothDevices("Petkit", "A4:C1:38:4B:DF:7C")
		btPeripheralList.add(btDevice)
		btDevice = BluetoothDevices("LEBose", "2C:41:A1:DA:C2:AF")
		btPeripheralList.add(btDevice)
		btDevice = BluetoothDevices("Smart Body Sensor", "CD:7A:3C:A7:19:3B")
		btPeripheralList.add(btDevice)
	}

	@SuppressLint("MissingPermission")
	override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
		super.onServicesDiscovered(gatt, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(TAG, "Services discovered")
			// Enable notification
			val service = gatt.getService(UUID_HEART_RATE_SERVICE)
			val characteristic = service.getCharacteristic(UUID_HEART_RATE_CHARACTERISTICS)

			characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
			val descriptor = characteristic.getDescriptor(UUID_HEART_RATE_DESCRIPTOR)
			descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
			gatt.writeDescriptor(descriptor)


		}
	}
	override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
		super.onCharacteristicRead(gatt, characteristic, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			// Characteristic read, do something
			// TODO: Handle characteristic read

			Log.d(TAG, "Characteristics read")
		} else {
			// TODO: Handle failed characteristic read
		}
	}
	override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
		super.onCharacteristicChanged(gatt, characteristic)

		if (characteristic.uuid == UUID_HEART_RATE_CHARACTERISTICS) {
			val data = characteristic.value
		// TODO: Process the received data
		}
		Log.d(TAG, "Characteristics changed")
	}

	private fun startNewDataPresenter(targetDeviceAddress: String)
	{
		val intent = Intent(context, DataPresenter::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		intent.putExtra(KEY_DEVICE_ADDRESS, targetDeviceAddress)
		//intent.putExtra(KEY_DEVICE_NAME, btPeripheralList)
		context.startActivity(intent)
	}
}
@SuppressLint("MissingPermission")
class BluetoothScanHandler(private var deviceList: MutableList<BluetoothDevice>,
						   private var adapter: DeviceAdapter) : ScanCallback() {

	// Bluetooth Handlers
	private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

	private val bondedDevices = bluetoothAdapter?.bondedDevices

	private val scanSettings = ScanSettings.Builder()
		.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
		.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
		.build()


	companion object {
		const val TAG = "BluetoothScanHandler"
	}


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
		Log.d(TAG, "onScanResult called")
	}
	override fun onScanFailed(errorCode: Int) {
		super.onScanFailed(errorCode)
		// Handle scan failures
		when (errorCode) {
			SCAN_FAILED_ALREADY_STARTED -> Log.d(TAG, "Scan failed, already started")
			SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.d(TAG, "Scan failed, application registration failed")
			SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.d(TAG, "Scan failed, feature unsupported")
			SCAN_FAILED_INTERNAL_ERROR -> Log.d(TAG, "Scan failed, internal error")
		}
	}
}