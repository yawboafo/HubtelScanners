package com.hubtel.hubtelscanners.TestActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.hubtel.hubtelscanners.R
import com.hubtel.hubtelscanners.scannerService.ConnectScannerService
import com.hubtel.hubtelscanners.scanners.ScannerSense
import com.socketmobile.scanapi.ISktScanProperty
import com.socketmobile.scanapi.SktScanErrors
import java.util.HashMap

class Main3Activity : AppCompatActivity() {
    private var _soundConfigReadyForChange: Boolean = false
    private var _previousSoftScanStatus = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_scanner)




        var button = findViewById<Button>(R.id.button)

        button.setOnClickListener {


            startService(Intent(this, ConnectScannerService::class.java))


        }
    }


    override fun onResume() {
        super.onResume()
        registerReceivers()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(itemScanReceiver)
        unregisterReceiver(broadcastReveiver)
    }

    fun registerReceivers(){

        var filter: IntentFilter
        filter = IntentFilter(ScannerSense.NOTIFY_ERROR_MESSAGE)
        registerReceiver(broadcastReveiver, filter)
        filter = IntentFilter(ScannerSense.NOTIFY_EZ_PAIR_COMPLETED)
        registerReceiver(broadcastReveiver, filter)
        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_ARRIVAL)
        registerReceiver(broadcastReveiver, filter)


        // var filter : IntentFilter
        filter = IntentFilter(ScannerSense.NOTIFY_SCANPI_INITIALIZED)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_ARRIVAL)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_REMOVAL)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_DECODED_DATA)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_ERROR_MESSAGE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_CLOSE_ACTIVITY)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_SOUND_CONFIG_COMPLETE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.GET_SOUND_CONFIG_COMPLETE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.GET_SOFTSCAN_COMPLETE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_SOFTSCAN_COMPLETE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_TRIGGER_COMPLETE)
        registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_OVERLAYVIEW_COMPLETE)
        registerReceiver(itemScanReceiver, filter)
    }

    private val itemScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == null)
                return

            if (intent.action!!.equals(ScannerSense.NOTIFY_SCANPI_INITIALIZED, ignoreCase = true)) {

                // activate this if you want to see all the traces
                // don't leave the traces in the final application as it will
                // slow down  the overall application
                /// scannerBrain.scannerSense.setTraces(false)

                // asking for the SoftScan status
                // scannerSense.getSoftScanStatus()
            } else if (intent.action!!.equals(ScannerSense.NOTIFY_SCANNER_ARRIVAL, ignoreCase = true)) {
                val softScan = intent.getBooleanExtra(ScannerSense.EXTRA_ISSOFTSCAN, false)
                val text = intent.getStringExtra(ScannerSense.EXTRA_DEVICENAME)

                // Sam i think we can improve this



                if (softScan) {

                    // before triggering the softscanner, the overlay view must be set
                    // with the context of this app.
                    val overlay = HashMap<String, Any>()
                    overlay[ISktScanProperty.values.softScanContext.kSktScanSoftScanContext] = context
                    //  scannerSense.setOverlayView(overlay)
                } else {
                    // ask for the sound confirmation config of the connected scanner
                    val newIntent = Intent(ScannerSense.GET_SOUND_CONFIG)
                    context.sendBroadcast(newIntent)
                }
            } else if (intent.action!!
                    .equals(ScannerSense.NOTIFY_SCANNER_REMOVAL, ignoreCase = true)
            ) {
                val softScan = intent
                    .getBooleanExtra(ScannerSense.EXTRA_ISSOFTSCAN, false)

                // Utils.showToast(getApplicationContext(),"Waiting for scanner...");
                _soundConfigReadyForChange = false
            } else if (intent.action!!.equals(ScannerSense.NOTIFY_DECODED_DATA, ignoreCase = true)) {
                val data = intent.getCharArrayExtra(ScannerSense.EXTRA_DECODEDDATA)

                val dataString = String(data)


                // Toast.makeText(context,dataString, Toast.LENGTH_LONG).show()


                //  searchByBarCode(dataString)




            } else if (intent.action!!
                    .equals(ScannerSense.NOTIFY_ERROR_MESSAGE, ignoreCase = true)
            ) {
                val text = intent.getStringExtra(ScannerSense.EXTRA_ERROR_MESSAGE)
                // Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            } else if (intent.action!!
                    .equals(ScannerSense.GET_SOUND_CONFIG_COMPLETE, ignoreCase = true)
            ) {
                val text = intent
                    .getStringExtra(ScannerSense.EXTRA_SOUND_CONFIG_FREQUENCY)
                if (text.contains(ScannerSense.SOUND_CONFIG_FREQUENCY_HIGH)) {
                } else if (text.contains(ScannerSense.SOUND_CONFIG_FREQUENCY_MEDIUM)) {
                } else if (text.contains(ScannerSense.SOUND_CONFIG_FREQUENCY_LOW)) {
                }

                _soundConfigReadyForChange = true
            } else if (intent.action!!
                    .equals(ScannerSense.GET_SOFTSCAN_COMPLETE, ignoreCase = true)
            ) {
                val result = intent.getLongExtra(
                    ScannerSense.EXTRA_ERROR,
                    SktScanErrors.ESKT_NOERROR
                )
                if (SktScanErrors.SKTSUCCESS(result)) {
                    val status = intent.getCharExtra(
                        ScannerSense.EXTRA_SOFTSCAN_STATUS,
                        ISktScanProperty.values.enableordisableSoftScan.kSktScanSoftScanNotSupported
                    ).toInt()
                    _previousSoftScanStatus = status


                }
            } else if (intent.action!!
                    .equals(ScannerSense.SET_SOFTSCAN_COMPLETE, ignoreCase = true)
            ) {
                val result = intent.getLongExtra(
                    ScannerSense.EXTRA_ERROR,
                    SktScanErrors.ESKT_NOERROR
                )
                // restore the previous softscan setting in case of error
                if (!SktScanErrors.SKTSUCCESS(result)) {

                    // the status cannot move from enable to not supported without being first disabled

                } else {

                }
            } else if (intent.action!!
                    .equals(ScannerSense.SET_OVERLAYVIEW_COMPLETE, ignoreCase = true)
            ) {
                val result = intent.getLongExtra(
                    ScannerSense.EXTRA_ERROR,
                    SktScanErrors.ESKT_NOERROR
                )
                if (SktScanErrors.SKTSUCCESS(result)) {

                }
            } else if (intent.action!!.equals(ScannerSense.SET_TRIGGER_COMPLETE, ignoreCase = true)) {
                val result = intent.getLongExtra(ScannerSense.EXTRA_ERROR, SktScanErrors.ESKT_NOERROR)
                if (!SktScanErrors.SKTSUCCESS(result)) {
                    val text = context.getString(R.string.formaterrorwhiletriggering)
                    val msg = String.format(text, result)
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }// get softscan status
            // get sound config complete received
            // an error has occurred
            // decoded Data received from a scanner
            // a Scanner has disconnected
            // a Scanner has connected
        }
    }

    private val broadcastReveiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("ree", "")

            if (intent.action!!.contains(ScannerSense.NOTIFY_ERROR_MESSAGE)) {





            } else if (intent.action!!.contains(ScannerSense.NOTIFY_EZ_PAIR_COMPLETED)) {





                //NOTIFY_SCANNER_ARRIVAL

            } else if (intent.action!!.contains(ScannerSense.NOTIFY_SCANNER_ARRIVAL)) {


            }
        }
    }
}
