package com.hubtel.hubtelscanners.TestActivities

import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.hubtel.hubtelscanners.R
import com.hubtel.hubtelscanners.Interfaces.ScannerConnectionDelegate
import com.hubtel.hubtelscanners.Interfaces.ScannerDataDelegate
import com.hubtel.hubtelscanners.scannerBrain.ScannerBrain


class Main2Activity : AppCompatActivity() , ScannerConnectionDelegate,ScannerDataDelegate {


    var scannerBrain : ScannerBrain? = null

    override fun scannerConnectBegan() {


        Log.d("debug","scannerConnectBegan")
    }
    override fun scannerConnectFailed(value: String) {

        Log.d("debug","scannerConnectFailed")

    }
    override fun scannerConnectComplete() {

        Log.d("debug","scannerConnectComplete")

    }
    override fun scannerArrived() {

        Log.d("debug","scannerArrived")

    }
    override fun ScannedData(data: String) {
        Toast.makeText(Main2Activity@this,data, Toast.LENGTH_LONG).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_scanner)


        isStoragePermissionGranted()

       scannerBrain = ScannerBrain(applicationContext,
           this,this)


        var button = findViewById<Button>(R.id.button)

        button.setOnClickListener {


            scannerBrain?.connecToScanner()

        }
    }
    override fun onResume() {
        super.onResume()


        scannerBrain?.setUp()

    }
    override fun onDestroy() {
        super.onDestroy()

        scannerBrain?.unregisterReceivers()


    }
    fun isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG", "Permission is granted")

            } else {

                Log.v("tag", "Permission is revoked")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.STORAGE_REQUEST_CODE
                )

            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG", "Permission is granted")

        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.STORAGE_REQUEST_CODE ->

                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //                    Utils.LogDebug(ForgetPasswordFragment.class, "GRANTED");
                    //                    new GeoLocationTracker(HomeActivity.this);

                    //connect to scanner
                    //                    scannerConnectionFragment.co


                } else {


                }
        }
    }





}
