package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothClass.Device
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import com.example.myapplication.Constants.KEY_DEVICE_ADDRESS
import com.example.myapplication.Constants.KEY_TEMP_DATA
import java.util.UUID

private const val SCAN_DURATION_MS = 5000L

@SuppressLint("MissingPermission")
class BluetoothConnectionHandler(private val context: Context) : BluetoothGattCallback() {

	private val dataPresenterIntent = Intent(context, DataPresenter::class.java)
	private val bondStateReceiver = BondStateReceiver()
	private val bondFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
	private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

	companion object {
		private const val TAG = "BluetoothHandler"
		val tempNode = BluetoothDevices("Temp_Node", "CD:7A:3C:A7:19:3B")
		val UUID_HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
		val UUID_HEART_RATE_CHARACTERISTICS: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
		val UUID_HEART_RATE_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
	}

	init {
		Log.d(TAG, "Creating BluetoothConnectionHandler")
		context.registerReceiver(bondStateReceiver, bondFilter)
	}

	fun connectOrBondSensor()
	{
		val btDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(tempNode.macAddress)
		when (btDevice.bondState)
		{
			BluetoothDevice.BOND_BONDED ->
			{
				Log.d(TAG, "Already bonded. Connecting to ${btDevice.address}")
				btDevice.connectGatt(context, false, this)
			}
			BluetoothDevice.BOND_NONE ->
			{
				Log.d(TAG, "Not bonded. ${btDevice.address}")
				btDevice.createBond()
			}
			BluetoothDevice.BOND_BONDING ->
			{
				Log.d(TAG, "Bonding in process. ${btDevice.address}")
			}
		}
	}

	/**
	 * onCharacteristicRead is used for synchronous data exchange
	 * onCharacteristicChanged is used for async data exchange
	 */
	override fun onCharacteristicRead(gatt: BluetoothGatt?,	characteristic: BluetoothGattCharacteristic, status: Int) {
		super.onCharacteristicRead(gatt, characteristic, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			characteristic.let { char ->
				if (char.uuid == UUID_HEART_RATE_CHARACTERISTICS)
				{
					val byteArray = characteristic.value
					Log.d(TAG, "onCharacteristicRead $byteArray")
				}
			}
		} else {
			Log.e(TAG, "onCharacteristicRead failed with status $status")
		}
	}

	override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
		super.onCharacteristicChanged(gatt, characteristic)
		if (characteristic.uuid == UUID_HEART_RATE_CHARACTERISTICS) {
			val byteArray = characteristic.value
			Log.d(TAG, "onCharacteristicChanged $byteArray")

			dataPresenterIntent.run {
				putExtra(KEY_TEMP_DATA, byteArray.toString())
				// several startActivity calls are handled in DataPresenter
				dataPresenterIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
				context.startActivity(dataPresenterIntent)
			}

		}
	}
	override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
		super.onConnectionStateChange(gatt, status, newState)
		if (newState == BluetoothProfile.STATE_CONNECTED) {
			Log.d(TAG, "${gatt.device.address} connected")
			gatt.discoverServices()
			startNewDataPresenter(gatt.device.address)
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			Log.d(TAG, "${gatt.device.address} disconnected")
			gatt.close()
		}
	}

	override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
		super.onServicesDiscovered(gatt, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(TAG, "${gatt.device.address} services discovered")
			val heartRateService = gatt.getService(UUID_HEART_RATE_SERVICE)
			val heartRateCharacteristic = heartRateService?.getCharacteristic(UUID_HEART_RATE_CHARACTERISTICS)
			heartRateCharacteristic?.let { heartRateChar ->
				gatt.setCharacteristicNotification(heartRateChar, true)
				heartRateChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				val heartRateDescriptor = heartRateChar.getDescriptor(UUID_HEART_RATE_DESCRIPTOR)
				heartRateDescriptor?.let { desc ->
					desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
					gatt.writeDescriptor(desc)
				}
				gatt.setCharacteristicNotification(heartRateChar, true)
			}
		} else {
			Log.e(TAG, "${gatt.device.address} service discovery failed")
			gatt.disconnect()
		}
	}

	private fun startNewDataPresenter(targetDeviceAddress: String)
	{
		dataPresenterIntent.run {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
			putExtra(KEY_DEVICE_ADDRESS, targetDeviceAddress)
			context.startActivity(dataPresenterIntent)
		}
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
	private val macAddressFilter: ScanFilter = ScanFilter.Builder()
		.setDeviceName("Temp_Node") // Replace with your desired MAC address
		.build()

	// Create a list of ScanFilter objects that will be used to filter devices during the scan
	private val filterList = listOf(macAddressFilter)
	private val scanSettings = ScanSettings.Builder()
		.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
		.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
		.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
		.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
		.setReportDelay(0)
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
		bluetoothLeScanner?.startScan(filterList, scanSettings, this)
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

class BondStateReceiver : BroadcastReceiver() {

	companion object {
		private const val TAG = "BondStateReceiver"
	}


	override fun onReceive(context: Context?, intent: Intent?) {

		Log.d(TAG, "OnReceive called")
		val action = intent?.action

		if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
			val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
			when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
				BluetoothDevice.BOND_BONDED -> {
					// Device is bonded
					Log.d(TAG, "${device!!.address} is bonded")
				}
				BluetoothDevice.BOND_BONDING -> {
					// Device is bonding
					Log.d(TAG, "${device!!.address} is bonding")
				}
				BluetoothDevice.BOND_NONE -> {
					// Bond has been removed
					Log.d(TAG, "${device!!.address} bond has been removed")
				}
			}
		}
	}
}
