package com.strange.bleconnecttest2

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.jetbrains.anko.layoutInflater

class LeDeviceListAdapter(
) : BaseAdapter() {

    private var mLeDevices = ArrayList<BluetoothDevice>()

    fun addDevices(device : BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
//                if (device.name != null) {
//                    if (device.name.startsWith("DXC")) {
//
//                    }
//                }
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
            Log.i("Info", "BLE 디바이스 리스트가 clear 되었습니다.")
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val context : Context = parent!!.context
            val view : View

            if (convertView == null) {
                val inflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(R.layout.lv_device_item, null, false)
            } else {
                view = View.inflate(context, R.layout.lv_device_item, null)
            }

            val viewHolder = ViewHolder(
                view.findViewById(R.id.tv_device_code),
                view.findViewById(R.id.tv_device_name)
            )

            val device = mLeDevices[position]
            if (device.name == null) {
                viewHolder.deviceName.text = "Null"
            } else {
                viewHolder.deviceName.text = device.name
            }
            viewHolder.deviceAddress.text = device.address

            return view
        }

        override fun getItem(position: Int): Any {
            return mLeDevices[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return mLeDevices.size
    }

    class ViewHolder(
        var deviceName : TextView,
        var deviceAddress : TextView
    ) {
    }
}