# BleTestProject
안드로이드에서 BLE 디바이스를 검색하고 BLE Device의 Advertising 정보를 정리한 프로젝트입니다.


### 준비
BLE 기능을 사용하기위해서는 준비해야할게 많다.
1. 권한 추가 및 확인
2. BLE 지원 확인 및 블루투스 켜기
3. GPS 켜기

#### 1. 권한 추가 및 확인
안드로이드 Manifest에 다음 권한들을 추가한다.
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
이후 안드로이드 앱에 권한을 요청하는 코드를 추가해야한다.

```kotlin
// 위치 권한 요청
override fun onCreate(savedInstanceState: Bundle?) {
~~~
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
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
```

#### 2. BLE 지원 확인 및 블루투스 켜기
해당 안드로이드 기기가 BLE 기능을 지원하는지 확인이 필요하다.
그 후 블루투스를 켜주는 코드를 추가한다.

```kotlin

// ble 지원 확인하기
private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

~~~
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

```

#### 3. GPS 켜기
LocationManager 를 통해서 gps가 켜져있는지 확인후
Intent를 통해사 사용자가 GPS를 키게한다.

```kotlin
~~~
// gps 켜져있는지 확인하기 위한 변수
    private lateinit var locationManager : LocationManager
~~~

private fun checkGpsIsEnabled() {
        // gps 켜져 있는지 확인
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toast("BLE기기를 겁색하기 위해서 GPS를 켜주셔야합니다.")
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, 1000)
        }
    }
```

### BLE Device 검색
이부분이 가장 중요하다고 생각된다.
일단 BLE 기기를 검색할 수 있는 방법이 2가지 있는데 2가지 다 설명하도록 하겠다.

#### 1. BluetoothAdapter.LeScanCallback을 이용하는 방법.
우선 다음 변수들을 미리 선언한다.
~~~kotlin
    // flag for scanning
    private var isScanning : Boolean = false
    // scan results
    private var arrayDevices =  java.util.ArrayList<BluetoothDevice>()
    // handler
    private val handler = android.os.Handler()
    
    // 블루투스 adapter 설정
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
~~~

그 후 Scan을 시작하는 함수를 만든다.
Handler를 사용하는 이유는
핸들러를 사용하지 않으면 계속해서 Bluetooth 기기를 검색하게 되는데
Bluetooth 기기를 검색하는 작업을 지속적으로 하게 되면
배터리 소모를 꽤 많이 하기 때문에 일정시간동안만 하게 한다.

그리고 BluetoothAdapter.stopLeScan 등 일부 함수들을 쓰면
취소선이 그어지는데 이는 무시하면된다. 함수 잘 호출된다 ㅎㅎ
~~~kotlin
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
                }, 5000) // 뒤에 숫자는 얼마동안 검색할 것인지 millis seconds 단위로 넣으면 된다.

                isScanning = true
                if (bluetoothAdapter == null) {
                    toast("Bluetooth adapter is null")
                    return
                }

                isScanning = true
                bluetoothAdapter!!.startLeScan(leScanCallback)
                Log.i(TAG, "Scanning...")
            }
            else -> {
                isScanning = false
                bluetoothAdapter!!.stopLeScan(leScanCallback)
                Log.d("Error", "there are error ")
            }
        }
    }
~~~

마지막으로 BluetoothAdapter.LeScanCallback 변수를 하나 만들면 끝이다.
```kotlin
private var leScanCallback : BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback{ device, rssi, scanRecord ->
        runOnUiThread {
            if (!arrayDevices.contains(device)) {
                leDeviceListAdapter.addDevices(device)
                arrayDevices.add(device)                
            }
            leDeviceListAdapter.notifyDataSetChanged()
            // listview adapter setting
            lv_ble_devices.adapter = leDeviceListAdapter 
        }
}
```
LeScanCallback 에서 넘겨주는 device, rssi, scanRecord 변수들은
device는 BluetoothDevice의 객체로 블루투스 기기의 Mac address, name 등의 정보를 알려준다.
rssi 는 Bluetooth 기기와 어느정도 떨어졌는지를 알려주는데 이를 실제 거리단위로 환산할려면 공식을 이용해야한다.(구글링 ㄱㄱ)
scanRecord 변수는 device에서 뿌려주는 Advertising data를 알려주는데 ByteArray 타입이기 때문에
Byte타입으로 나눈뒤 String 변수에 중첩해야지
기기가 뿌려주는 row data를 확인 할 수 있을 것이다.

참고로 ListView를 따로 만들어서 BluetoothDevice 객체의 address와 name을 보여주게 만들었다.

### 2. BluetoothLeScanner 와 ScanCallback을 이용하는 방법
이 방법은 특정 Mac Address나 UUID를 가진 Device를 필터링 해서 찾고 싶으면 좋다.
나는 Mac Address를 필터링해서 이미 검색한 블루투스 기기를 refresh 하는 기능으로 사용을 했다.

```kotlin
private fun refreshBleScan(enable : Boolean) {
        val handler = Handler()

        // Bluetooth adapter 및 scanner 설정
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner

        // Bluetooth Scan setting을 BLE로 세팅
        val mScanSettings = ScanSettings.Builder()
        mScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        val scanSettings = mScanSettings.build()

        when (enable) {
            true -> {
                handler.postDelayed({
                    mBluetoothLeScanner.stopScan(mScanCallback)
                    refreshButton.isEnabled = true
                }, 5000)              

                val scanFilters = Vector<ScanFilter>()
                val scanFilter = ScanFilter.Builder()
                scanFilter.setDeviceAddress(mDevice.address) // 현재 디바이스의 MAC address 로 Device 검색
                // scanFilter.setsetServiceUuid(new ParcelUuid(UUID)) UUID 를 사용하는 방법
                val scan = scanFilter.build()
                scanFilters.add(scan)
                mBluetoothLeScanner.startScan(scanFilters, scanSettings, mScanCallback)
                return
            }
            false -> {
                mBluetoothLeScanner.stopScan(mScanCallback)               
            }
        }
    }
```

ScanCallback 정의
```kotlin
// device scan callback
    private var mScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("Error", "Error Code : $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            try {
               mBluetoothLeScanner.stopScan(this) // 여러번 Scan 하는것을 방지하기 위해 Scan 성공시 바로 Stop
               // 나름대로 코드 넣기
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }
    }
```

BLE 장치는 GPS를 이용해서 연결 및 데이터를 뿌려주는 구조이기 때문에
안드로이드앱에서도 GPS나 위치권한 등을 반드시 확인하고 앱을 동작 및 만들기 바랍니다.
