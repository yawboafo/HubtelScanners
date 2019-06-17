package com.hubtel.hubtelscanners.scannerBrain

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.widget.Toast
import com.hubtel.hubtelscanners.Interfaces.ScannerConnectionDelegate
import com.hubtel.hubtelscanners.R
import com.hubtel.hubtelscanners.Interfaces.ScannerDataDelegate
import com.hubtel.hubtelscanners.scanners.ScannerSense
import com.socketmobile.scanapi.ISktScanProperty
import com.socketmobile.scanapi.SktScanErrors
import java.lang.reflect.InvocationTargetException
import java.util.HashMap

class ScannerBrain(var context: Context,
                   var scannerConnectDelegate: ScannerConnectionDelegate? = null,
                   var scannedDataDelegate: ScannerDataDelegate? = null)  {




    private var _deviceSelectedToPairWith   = ""
    private var _hostBluetoothAddress = ""
    private var  scannerSense = ScannerSense(context)
    private var _soundConfigReadyForChange: Boolean = false
    private var _previousSoftScanStatus = -1


    init {

        scannerSense = ScannerSense(context)
        scannerSense.initScannerProperties()
        scannerSense.increaseViewCount()

    }


    fun connecToScanner(){




        val intent = Intent(ScannerSense.START_EZ_PAIR)
        // remove the bluetooth address and keep only the device friendly name
        if (_deviceSelectedToPairWith != null) {
            if (_deviceSelectedToPairWith.length > 18) {
                _deviceSelectedToPairWith =
                  _deviceSelectedToPairWith.substring(0, _deviceSelectedToPairWith?.length - 18)
            }
            intent.putExtra(ScannerSense.EXTRA_EZ_PAIR_DEVICE, _deviceSelectedToPairWith)
            intent.putExtra(ScannerSense.EXTRA_EZ_PAIR_HOST_ADDRESS, _hostBluetoothAddress)
            context.sendBroadcast(intent)


            if (scannerConnectDelegate != null)  {

                scannerConnectDelegate?.scannerConnectBegan()
            }


        } else {


            if (scannerConnectDelegate != null)  {

                scannerConnectDelegate?.scannerConnectFailed("Cannot find device to connect to")
            }

            Log.d("isnull", "")
        }
    }

    fun setUp(){
        initBluetoothPairingProperties()
        initScannerBrain()
        registerToReceiveScannerNotification()
    }
    fun unregisterReceivers(){
        try {

            context.unregisterReceiver(broadcastReveiver)
            context.unregisterReceiver(itemScanReceiver)

        } catch (e: IllegalArgumentException) {

            e.printStackTrace()
        }
    }

    private fun initScannerBrain() {


        var filter: IntentFilter
        filter = IntentFilter(ScannerSense.NOTIFY_ERROR_MESSAGE)
        context.registerReceiver(broadcastReveiver, filter)
        filter = IntentFilter(ScannerSense.NOTIFY_EZ_PAIR_COMPLETED)
        context.registerReceiver(broadcastReveiver, filter)
        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_ARRIVAL)
        context.registerReceiver(broadcastReveiver, filter)





    }
    private fun initBluetoothPairingProperties() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        try {

            if (bluetoothAdapter != null) {

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    _hostBluetoothAddress = bluetoothAdapter.address
                } else {
                    //_hostBluetoothAddress = Settings.Secure.getString(context.contentResolver, "bluetooth_address")

                    if (_hostBluetoothAddress == null)
                        _hostBluetoothAddress = getBluetoothMacAddress()
                    Log.d("macaddress", getBluetoothMacAddress())
                }
                _hostBluetoothAddress = _hostBluetoothAddress?.replace(":", "")

                val pairedDevices = bluetoothAdapter.bondedDevices


                if (pairedDevices.size > 0) {
                    for (device in pairedDevices) {
                        if (device.name.contains("Socket") || device.address.contains("Socket")) {
                            _deviceSelectedToPairWith = device.name + "\n" + device.address
                            Log.d(javaClass.toString(), "when looping devices $_deviceSelectedToPairWith")
                        }
                    }

                }
            } else {
                val noDevices = context.resources.getText(R.string.none_paired).toString()
                _deviceSelectedToPairWith = noDevices

            }
        } catch (e: NullPointerException) {
            e.printStackTrace()


        }


    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun getBluetoothMacAddress(): String {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var bluetoothMacAddress = ""
        try {
            val mServiceField = bluetoothAdapter.javaClass.getDeclaredField("mService")
            mServiceField.isAccessible = true

            val btManagerService = mServiceField.get(bluetoothAdapter)

            if (btManagerService != null) {
                bluetoothMacAddress =
                    btManagerService.javaClass.getMethod("getAddress").invoke(btManagerService) as String
            }
        } catch (ignore: NoSuchFieldException) {

        } catch (ignore: NoSuchMethodException) {
        } catch (ignore: IllegalAccessException) {
        } catch (ignore: InvocationTargetException) {
        }

        return bluetoothMacAddress
    }
    private val broadcastReveiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("ree", "")

            if (intent.action!!.contains(ScannerSense.NOTIFY_ERROR_MESSAGE)) {



                if (scannerConnectDelegate != null)  {

                    scannerConnectDelegate?.scannerConnectFailed("Failed to connect to scanner")
                }

            } else if (intent.action!!.contains(ScannerSense.NOTIFY_EZ_PAIR_COMPLETED)) {



                if (scannerConnectDelegate != null)  {

                    scannerConnectDelegate?.scannerConnectComplete()
                }

                //NOTIFY_SCANNER_ARRIVAL

            } else if (intent.action!!.contains(ScannerSense.NOTIFY_SCANNER_ARRIVAL)) {
                if (scannerConnectDelegate != null)  {

                    scannerConnectDelegate?.scannerArrived()
                }


            }
        }
    }

    private fun registerToReceiveScannerNotification() {



        var filter: IntentFilter
        filter = IntentFilter(ScannerSense.NOTIFY_SCANPI_INITIALIZED)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_ARRIVAL)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_SCANNER_REMOVAL)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_DECODED_DATA)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_ERROR_MESSAGE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.NOTIFY_CLOSE_ACTIVITY)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_SOUND_CONFIG_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.GET_SOUND_CONFIG_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.GET_SOFTSCAN_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_SOFTSCAN_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_TRIGGER_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)

        filter = IntentFilter(ScannerSense.SET_OVERLAYVIEW_COMPLETE)
        context.registerReceiver(itemScanReceiver, filter)
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

                if (scannedDataDelegate != null ){

                    scannedDataDelegate?.ScannedData(dataString)
                }
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

}