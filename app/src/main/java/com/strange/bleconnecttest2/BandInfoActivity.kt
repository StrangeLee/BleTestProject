package com.strange.bleconnecttest2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.strange.bleconnecttest2.data.AdvertisingData
import com.strange.bleconnecttest2.databinding.ActivityBandInfoBinding
import kotlinx.android.synthetic.main.activity_band_info.view.*
import org.jetbrains.anko.toast

class BandInfoActivity : AppCompatActivity(){

    private lateinit var binding: ActivityBandInfoBinding
    private lateinit var scanResult: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_band_info)

        // advertising data 인텐트로 받아오기
        scanResult = intent.extras.get("data") as ByteArray

        // xml 파일에 data 넘겨주는 과정
        binding.advertising = getAdvertisingData(scanResult)

        // row data text view 클릭시 clipboard 에 복사
        binding.root.tv_rowdata.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip : ClipData = ClipData.newPlainText("Row data를 복사했습니다.", binding.root.tv_rowdata.text.toString())
            clipboard.primaryClip = clip
            toast("Row Data값을 복사했습니다.")
        }

        printAdvertisingData(scanResult)
    }

    // log 찍기
    private fun printAdvertisingData(data : ByteArray) {
        var msg : String = ""
        for (b : Byte in data) {
            msg += String.format("%02x", b)
        }

        // data 변환 로그 출력
        Log.d("Data", "whole data : $msg")
        Log.d("Data", "band id : ${msg.substring(14, 18)}")
        if (msg.substring(18, 20) == "00") {
            Log.d("Data", "band mode : 일반 모드")
        } else {
            Log.d("Data", "band mode : 운동 모드")
        }

        when (msg.substring(20, 22)) {
            "00" -> Log.d("Data", "운동 타입 : 걷기 모드")
            "01" -> Log.d("Data", "운동 타입 : 달리기 모드")
            "02" -> Log.d("Data", "운동 타입 : 등산 모드")
            "03" -> Log.d("Data", "운동 타입 : 자전거 모드")
        }

        Log.d("Data", "HRM : ${msg.substring(22, 24).toInt(16)}")
        Log.d("Data", "Step : ${msg.substring(24, 28).toInt(16)}") // 16진수 변환 후 다시 10진수 String 으로 변환
        Log.d("Data", "Calorie : ${msg.substring(28, 32)}")
        Log.d("Data", "distance : ${(msg.substring(32, 36).toInt(16).toString().toInt() / 100).toFloat()}")
        Log.d("Data", "sleep status : ${msg.substring(36, 38).toInt(16)}")
        Log.d("Data", "hands off : ${msg.substring(38, 40).toInt(16)}")
        Log.d("Data", "sleep time : ${msg.substring(40, 44).toInt(16)}")
        Log.d("Data", "condition : ${msg.substring(44, 48).toInt(16)}")
    }

    // AdvertisingData.class 에 맞춰서 데이터 변환하기
    private fun getAdvertisingData(data: ByteArray) : AdvertisingData {
        var msg : String = ""
        for (b : Byte in data) {
            msg += String.format("%02x", b)
        }

        binding.root.tv_rowdata.text = msg

        val bandMode = if (msg.substring(18, 20) == "00") {
            "일반 모드"
        } else {
            "운동 모드"
        }

        val type = when (msg.substring(20, 22)) {
            "00" -> "걷기모드"
            "01" -> "달리기 모드"
            "02" -> "등산 모드"
            "03" -> "자전거 모드"
            else -> "걷기모드"
        }

        val handsOff = if (msg.substring(38, 40).toInt(16).toString() == "00") {
            "미착용"
        } else {
            "착용중"
        }

        return AdvertisingData(
            msg.substring(14, 18),
            "밴드 모드 : $bandMode",
            "운동 타입 : $type",
            "심박수 : " + msg.substring(22, 24).toInt(16).toString(),
            "걸음수 : " + msg.substring(24, 28).toInt(16).toString(),
            "칼로리 : " + msg.substring(28, 32).toInt(16).toString() + "Kcal",
            "이동거리 : " + String.format("%.2f", (msg.substring(32, 36).toInt(16).toString().toInt() / 100).toFloat()) + "km",
            "수면 상태 : " + msg.substring(36, 38).toInt(16).toString(),
            "착용상태 : $handsOff",
            "수면시간 : " + msg.substring(40, 44).toInt(16).toString(),
            "컨디션 인덱스 : " + msg.substring(44, 48).toInt(16).toString()
        )
    }

    // 상단 액션바에 뒤로가기 버튼 추가
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.actionbar_menu, menu)

        return true
    }

    // 액션바 아이템 클릭 이벤트
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_back -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}