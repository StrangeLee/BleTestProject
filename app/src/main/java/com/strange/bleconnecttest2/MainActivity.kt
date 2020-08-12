package com.strange.bleconnecttest2

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast


private const val TAG = "Central"
private const val REQUEST_ENABLE_BT = 1000
private const val SCAN_PERIOD : Long = 10000

class MainActivity : AppCompatActivity() {

    // flag for scanning
    private var isScanning : Boolean = false
    // scan results
    private var arrayDevices =  java.util.ArrayList<BluetoothDevice>()
    // handler
    private val handler = Handler()

    // ble 지원 체크 함수
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    // 블루투스 adapter 설정
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private lateinit var leDeviceListAdapter : LeDeviceListAdapter
    private var fullDeviceInfo = ArrayList<HashMap<BluetoothDevice, ByteArray>>()
    private var rssiList = ArrayList<Int>()

    // device scan callback
    private var leScanCallback : BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback{ device, rssi, scanRecord ->
        runOnUiThread {
            leDeviceListAdapter.addDevices(device)
            if (!arrayDevices.contains(device)) {
                arrayDevices.add(device)
                val hashMap = hashMapOf(device to scanRecord)
                fullDeviceInfo.add(hashMap)
                rssiList.add(rssi)
            }
            leDeviceListAdapter.notifyDataSetChanged()
            lv_ble_devices.adapter = leDeviceListAdapter
        }
    }

    // gps 켜져있는지 확인하기 위한 변수
    private lateinit var locationManager : LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leDeviceListAdapter = LeDeviceListAdapter()

        // list view 아이템 클릭시 Device Control 화면으로 이동
        lv_ble_devices.setOnItemClickListener { parent, view, position, id ->
            getAdvertisingData(position)
        }

        // 위치 권한 요청
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        Log.d("Package", this.packageName)

        // Scanning 버튼 클릭 이벤트
        btn_scan_refresh.setOnClickListener {
            toast("Scanning..")
            arrayDevices.clear()
            fullDeviceInfo.clear()
            leDeviceListAdapter.clear()
            leDeviceListAdapter.notifyDataSetChanged()
            startScan()
        }

        // gps 켜져 있는지 확인
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toast("BLE기기를 겁색하기 위해서 GPS를 켜주셔야합니다.")
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

   override fun onResume() {
        super.onResume()
        // ble 지원 체크
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            toast("ble 통신을 지원하지 않는 기기입니다.")
            finish()
        }

        // 블루투스 on
        val apply = bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun startScan() {
        isScanning = true
        scanLeDevice(isScanning)
    }

    private fun scanLeDevice(enable : Boolean) {
        when (enable) {
            true -> {
                handler.postDelayed({
                    isScanning = false
                    bluetoothAdapter!!.stopLeScan(leScanCallback)
                    Log.d("Error", "there are no devices")
                }, SCAN_PERIOD)

                isScanning = true
                if (bluetoothAdapter == null) {
                    toast("Bluetooth adapter is null")
                }

                isScanning = true
                bluetoothAdapter!!.startLeScan(leScanCallback)
                Log.i(TAG, "Scanning...")
            }
            else -> {
                isScanning = false
                bluetoothAdapter!!.stopLeScan(leScanCallback)
                Log.d("Error", "there are errogood ")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: kotlin.Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
              for (i in permissions.indices) {
                  if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                      toast(permissions[i] + " 권한 승인됨.")
                      toast("위치 권한이 승인되었습니다.")
                  } else {
//                      toast(permissions[i] + " 권한 승인 거부됨.")
                      toast("위치 권한이 승인되지 않았습니다.")
                  }
              }
            }
        }
    }

    // Advertising Data 를 BandInfoActivity 로 보내는 작업
    private fun getAdvertisingData(position : Int) {
        // SharedPreferences 로 데이터 보내기
        val intent = Intent(this, BandInfoActivity::class.java)
        intent.putExtra("data", fullDeviceInfo[position][arrayDevices[position]])
        intent.putExtra("rssi", rssiList[position])// rssi 데이터 넘기기

        startActivity(intent) // 현재 Advertising data 화면 으로 이동
    }
}
