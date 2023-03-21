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

private const val SCAN_DURATION_MS = 5000L

@SuppressLint("MissingPermission")
class BluetoothConnectionHandler(private val context: Context) : BluetoothGattCallback() {

	private val peripheralList = listOf(
		BluetoothDevices("Forerunner735XT", "F8:B6:6B:8E:EF:1F"),
		BluetoothDevices("Samsung Series TV", "70:2A:D5:52:FF:81"),
		BluetoothDevices("Petkit", "A4:C1:38:4B:DF:7C"),
		BluetoothDevices("LEBose", "2C:41:A1:DA:C2:AF"),
		BluetoothDevices("Smart Body Sensor", "CD:7A:3C:A7:19:3B")
	)

	companion object {
		private const val TAG = "BluetoothHandler"
		val UUID_HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
		val UUID_HEART_RATE_CHARACTERISTICS: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
		val UUID_HEART_RATE_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
	}

	init {
		Log.d(TAG, "Creating BluetoothConnectionHandler")
	}

	fun connectBluetoothDevice(device: BluetoothDevice) {
		val isTargetBlePeripheral = peripheralList.any { bleDevice ->
			bleDevice.macAddress == device.address.toString()
		}
		if (isTargetBlePeripheral) {
			Log.d(TAG, "Connecting to ${device.address}")
			device.connectGatt(context, false, this)
		} else {
			Log.w(TAG, "${device.address} not in peripheral list")
		}
	}


	override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
		super.onConnectionStateChange(gatt, status, newState)
		if (newState == BluetoothProfile.STATE_CONNECTED) {
			Log.d(TAG, "${gatt.device.address} connected")
			startNewDataPresenter(gatt.device.address)
			gatt.discoverServices()
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			Log.d(TAG, "${gatt.device.address} disconnected")
			gatt.close()
		}
	}

	override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
		super.onServicesDiscovered(gatt, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(TAG, "${gatt.device.address} services discovered")
			val service = gatt.getService(UUID_HEART_RATE_SERVICE)
			val characteristic = service?.getCharacteristic(UUID_HEART_RATE_CHARACTERISTICS)
			characteristic?.let { char ->
				char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				val descriptor = char.getDescriptor(UUID_HEART_RATE_DESCRIPTOR)
				descriptor?.let { desc ->
					desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
					gatt.writeDescriptor(desc)
				}
				gatt.setCharacteristicNotification(char, true)
			}
		} else {
			Log.e(TAG, "${gatt.device.address} service discovery failed")
			gatt.disconnect()
		}
	}

	private fun startNewDataPresenter(targetDeviceAddress: String)
	{
		val intent = Intent(context, DataPresenter::class.java).run {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
			putExtra(KEY_DEVICE_ADDRESS, targetDeviceAddress)
		}
		//intent.putExtra(KEY_DEVICE_NAME, btPeripheralList)
		context.startActivity(intent)
	}
}
@SuppressLint("MissingPermission")
class BluetoothScanHandler(private var deviceList: MutableList<BluetoothDevice>,
						   private var adapter: DeviceAdapter) : ScanCallback() {

	// Bluetooth Handlers
	private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
		BluetoothAdapter.getDefaultAdapter()
	}
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
		return bluetoothAdapter?.let {
			true
		} ?: false
	}

	/**
	 * Callbacks
	 */
	@SuppressLint("NotifyDataSetChanged")
	override fun onScanResult(callbackType: Int, result: ScanResult) {
		super.onScanResult(callbackType, result)

		if (result.device !in deviceList) {
			deviceList.add(result.device)
			adapter.notifyDataSetChanged()
			Log.d(TAG, "onScanResult called")
		}
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