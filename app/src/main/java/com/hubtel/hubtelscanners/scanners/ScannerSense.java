package com.hubtel.hubtelscanners.scanners;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import com.socketmobile.scanapi.*;

import java.util.Locale;

/**
 * Created by nykbMac on 16/06/2018.
 */

public class ScannerSense {


    public static final String DEFAULT_SCANAPI_CONFIGURATION = "Server:ScanAPI-1";

    public static final String START_EZ_PAIR = ScannerSense.class.getName()
            + ".StartEzPair";

    public static final String STOP_EZ_PAIR = ScannerSense.class.getName()
            + ".StopEzPair";

    public static final String NOTIFY_EZ_PAIR_COMPLETED = ScannerSense.class.getName()
            + ".NotifyEzPairCompleted";

    public static final String EXTRA_EZ_PAIR_DEVICE = ScannerSense.class.getName()
            + ".EzPairDevice";

    public static final String EXTRA_EZ_PAIR_HOST_ADDRESS = ScannerSense.class.getName()
            + ".EzPairHostAddress";

    // to manage the sound configuration for the data confirmation beep
    public static final String SET_DATA_CONFIRMATION = ScannerSense.class.getName()
            + ".SetDataConfirmation";

    public static final String GET_SOUND_CONFIG = ScannerSense.class.getName()
            + ".GetSoundConfig";

    public static final String SET_SOUND_CONFIG = ScannerSense.class.getName()
            + ".SetSoundConfig";

    public static final String GET_SOUND_CONFIG_COMPLETE = ScannerSense.class.getName()
            + ".GetSoundConfigComplete";

    public static final String SET_SOUND_CONFIG_COMPLETE = ScannerSense.class.getName()
            + ".SetSoundConfigComplete";

    public static final String EXTRA_SOUND_CONFIG_FREQUENCY = ScannerSense.class.getName()
            + ".SoundConfigFrequency";

    public static final String SOUND_CONFIG_FREQUENCY_HIGH = "SoundFrequencyHigh";

    public static final String SOUND_CONFIG_FREQUENCY_MEDIUM = "SoundFrequencyMedium";

    public static final String SOUND_CONFIG_FREQUENCY_LOW = "SoundFrequencyLow";

    public static final String GET_SOFTSCAN_COMPLETE = ScannerSense.class.getName()
            + ".GetSoftScanComplete";

    public static final String SET_SOFTSCAN_COMPLETE = ScannerSense.class.getName()
            + ".SetSoftScanComplete";

    public static final String SET_TRIGGER_COMPLETE = ScannerSense.class.getName()
            + ".SetTriggerComplete";

    public static final String SET_OVERLAYVIEW_COMPLETE = ScannerSense.class.getName()
            + ".SetOverlayViewComplete";

    public static final String NOTIFY_SCANPI_INITIALIZED = ScannerSense.class.getName()
            + ".NotifyScanApiInitialized";

    public static final String NOTIFY_SCANNER_ARRIVAL = ScannerSense.class.getName()
            + ".NotifyScannerArrival";

    public static final String NOTIFY_SCANNER_REMOVAL = ScannerSense.class.getName()
            + ".NotifyScannerRemoval";

    public static final String NOTIFY_DECODED_DATA = ScannerSense.class.getName()
            + ".NotifyDecodedData";

    public static final String NOTIFY_ERROR_MESSAGE = ScannerSense.class.getName()
            + ".NotifyErrorMessage";

    public static final String NOTIFY_CLOSE_ACTIVITY = ScannerSense.class.getName()
            + ".NotifyCloseActivity";

    public static final String EXTRA_SOFTSCAN_STATUS = ScannerSense.class.getName()
            + ".SoftScanStatus";

    public static final String EXTRA_ISSOFTSCAN = ScannerSense.class.getName()
            + ".IsSoftScan";

    public static final String EXTRA_ERROR = ScannerSense.class.getName() + ".Error";

    public static final String EXTRA_ERROR_MESSAGE = ScannerSense.class.getName()
            + ".ErrorMessage";

    public static final String EXTRA_DEVICENAME = ScannerSense.class.getName()
            + ".DeviceName";

    public static final String EXTRA_SYMBOLOGY_NAME = ScannerSense.class.getName()
            + ".SymbologyName";

    public static final String EXTRA_DECODEDDATA = ScannerSense.class.getName()
            + ".DecodedData";

    protected static final int defaultConnectedTimeout = 0;

    private final int CLOSE_SCAN_API = 1;

    private DeviceInfo _softScanDevice;

    private static Context _singleton;

    private ScanApiHelper _scanApiHelper;

    private SktScanApiOwnership _scanApiOwnership;

    private Event _consumerTerminatedEvent;
// event to know when the ScanAPI terminate event has been received

    private int _viewCount;
// View counter (each activity increase or decrease this count when created or destroyed respectively)

    private boolean _forceCloseUI;// flag to force to close the UI


    private Intent _lastBroadcastedState;

    private String _originalScanAPIConfiguration = "Server:ScanAPI-1";

    private String _ezPairDeviceName;

    private String _ezPairHostAddress;

    private boolean _ezPairInProgress = false;

    private boolean _reopenScanApiWhenBluetoothIsOnAgain = false;

    private static Context context;

    public ScannerSense(Context context) {
        this.context = context;
    }

    class Event {

        private boolean _set;

        public Event(boolean set) {
            _set = set;
        }

        public synchronized void set() {
            _set = true;
            notify();
        }

        public synchronized void reset() {
            _set = false;
        }

        public synchronized boolean waitFor(long timeoutInMilliseconds) {
            long t1, t2;
            for (; !_set; ) {
                t1 = System.currentTimeMillis();
                try {
                    wait(timeoutInMilliseconds);
                } catch (InterruptedException e) {
                    break;
                }
                t2 = System.currentTimeMillis();
                if (!_set) {
                    if (t2 >= (t1 + timeoutInMilliseconds)) {
                        break;
                    } else {
                        timeoutInMilliseconds = (t1 + timeoutInMilliseconds) - t2;
                    }
                } else {
                    break;
                }
            }
            return _set;
        }
    }


    public void initScannerProperties() {
        _viewCount = 0;// there is no view created for this application yet
        _forceCloseUI = false;
        _lastBroadcastedState = null;

        _consumerTerminatedEvent = new Event(true);


        // create a ScanAPI Helper
        _scanApiHelper = new ScanApiHelper();
        _scanApiHelper.setNotification(_scanApiHelperNotification);

        // create a ScanAPI ownership
        _scanApiOwnership = new SktScanApiOwnership(_scanApiOwnershipNotification, "scanner");

        IntentFilter filter;
        filter = new IntentFilter(START_EZ_PAIR);
        context.registerReceiver(_broadcastReceiver, filter);

        filter = new IntentFilter(STOP_EZ_PAIR);
        context.registerReceiver(_broadcastReceiver, filter);

        filter = new IntentFilter(SET_DATA_CONFIRMATION);
        context.registerReceiver(_broadcastReceiver, filter);

        filter = new IntentFilter(GET_SOUND_CONFIG);
        context.registerReceiver(_broadcastReceiver, filter);

        filter = new IntentFilter(SET_SOUND_CONFIG);
        context.registerReceiver(_broadcastReceiver, filter);

        // add this for receiving Bluetooth Radio ON or OFF
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(_broadcastReceiver, filter);
    }


    protected ICommandContextCallback _onGetSoundConfigDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            String frequency = SOUND_CONFIG_FREQUENCY_HIGH;
            Intent intent;
            if (SktScanErrors.SKTSUCCESS(result)) {
                char freq = scanObj.getProperty().getArray().getValue()[3];
                if (freq == ISktScanProperty.values.soundFrequency.kSktScanSoundFrequencyMedium) {
                    frequency = SOUND_CONFIG_FREQUENCY_MEDIUM;
                } else if (freq
                        == ISktScanProperty.values.soundFrequency.kSktScanSoundFrequencyLow) {
                    frequency = SOUND_CONFIG_FREQUENCY_LOW;
                }
                intent = new Intent(GET_SOUND_CONFIG_COMPLETE);
                intent.putExtra(EXTRA_SOUND_CONFIG_FREQUENCY, frequency);
            } else {
                Debug.MSG(Debug.kLevelError,
                        "Get Sound Config Device Complete returns an error: " + result);
                intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE,
                        "Unable to get the device sound configuration: " + result
                                + ". Power cycle the scanner and try again.");
            }
            context.sendBroadcast(intent);
        }
    };

    protected ICommandContextCallback _onSetSoundConfigDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();

            if (!SktScanErrors.SKTSUCCESS(result)) {
                Debug.MSG(Debug.kLevelError,
                        "Set Sound Config Device Complete returns an error: " + result);
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE,
                        "Unable to set the device sound configuration: " + result
                                + ". Power cycle the scanner and try again.");
                context.sendBroadcast(intent);
            }
        }
    };

    private ScanApiHelper.ScanApiHelperNotification _scanApiHelperNotification = new ScanApiHelper.ScanApiHelperNotification() {
        /**
         * receive a notification indicating ScanAPI has terminated,
         * then send an intent to finish the activity if it is still
         * running
         */
        public void onScanApiTerminated() {
            _consumerTerminatedEvent.set();
            if (_forceCloseUI) {
                Intent intent = new Intent(NOTIFY_CLOSE_ACTIVITY);
                context.sendBroadcast(intent);
            }
        }

        /**
         * ScanAPI is now initialized, if there is an error
         * then ask the activity to display it
         */
        public void onScanApiInitializeComplete(long result) {
            // if ScanAPI couldn't be initialized
            // then display an error
            if (!SktScanErrors.SKTSUCCESS(result)) {
                _consumerTerminatedEvent.set();
                _scanApiOwnership.releaseOwnership();
                String text = "ScanAPI failed to initialize with error: " + result;
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
                context.sendBroadcast(intent);
                _lastBroadcastedState = intent;
            } else {
                Intent intent = new Intent(NOTIFY_SCANPI_INITIALIZED);
                context.sendBroadcast(intent);
                _lastBroadcastedState = intent;

                // check if the ScanAPI configuration is correct.
                // if not then put the default configuration.
                _scanApiHelper.postGetScanAPIConfiguration(
                        ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                        _onGetScanApiConfiguration);

            }
        }

        /**
         * ask the activity to display any asynchronous error
         * received from ScanAPI
         */
        public void onError(long result) {
            Debug.MSG(Debug.kLevelError, "receive an error:" + result);
            String text = "ScanAPI is reporting an error: " + result;
            if (result == SktScanErrors.ESKT_UNABLEINITIALIZE) {
                text = "Unable to initialize the scanner. Please power cycle the scanner.";
            }
            Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
            intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            context.sendBroadcast(intent);
            _lastBroadcastedState = intent;
            if (_ezPairInProgress) {
                // make sure to restore ScanAPI configuration
                _scanApiHelper.postSetScanAPIConfiguration(
                        ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                        _originalScanAPIConfiguration, _onSetScanApiConfiguration);
            }
            _ezPairInProgress = false;// no longer in EZ Pair mode
            // when we receive this error, ScanAPI won't receive and manage any
            // connection. The only way to recover is either to change the ScanAPI configuration
            // or simply close and reopen ScanAPI.
            // this error could come if the Bluetooth radio is turned OFF, which in this case
            // we set a flag to re-open ScanAPI when this application receive the Bluetooth
            // radio ON notification
            if (result == SktScanErrors.ESKT_NOTHINGTOLISTEN) {
                // check if Bluetooth is present and on
                BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
                if (bluetooth == null) {
                    // there is no Bluetooth on this device
                }
                // is bluetooth off

                else if (!bluetooth.isEnabled()) {
                    // then close ScanAPI
                    closeScanApi();
                    _reopenScanApiWhenBluetoothIsOnAgain = true;
                }
            }
        }

        /**
         * a device has disconnected. Update the UI accordingly
         */
        public void onDeviceRemoval(DeviceInfo deviceRemoved) {
            if (!_ezPairInProgress) {
                Intent intent = new Intent(NOTIFY_SCANNER_REMOVAL);
                intent.putExtra(EXTRA_DEVICENAME, deviceRemoved.getName());
                if (deviceRemoved.getTypeString().equals("Soft Scanner")) {
                    _softScanDevice = null;
                    intent.putExtra(EXTRA_ISSOFTSCAN, true);
                } else {
                    intent.putExtra(EXTRA_ISSOFTSCAN, false);
                }
                context.sendBroadcast(intent);
                _lastBroadcastedState = intent;
            }
            // in ez pair mode, restore the original ScanAPI configuration
            else {
                _scanApiHelper.postSetScanAPIConfiguration(
                        ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                        _originalScanAPIConfiguration,
                        _onSetScanApiConfiguration);

                Intent intent = new Intent(NOTIFY_EZ_PAIR_COMPLETED);
                context.sendBroadcast(intent);
                _ezPairInProgress = false;// no longer in EZ Pair mode
            }
        }

        /**
         * a device is connecting, update the UI accordingly
         */
        public void onDeviceArrival(long result, DeviceInfo newDevice) {
            Intent intent = null;
            if (SktScanErrors.SKTSUCCESS(result)) {
                if (!_ezPairInProgress) {
                    intent = new Intent(NOTIFY_SCANNER_ARRIVAL);
                    intent.putExtra(EXTRA_DEVICENAME, newDevice.getName());
                    if (newDevice.getTypeString().equals("Soft Scanner")) {
                        _softScanDevice = newDevice;
                        intent.putExtra(EXTRA_ISSOFTSCAN, true);
                    } else {
                        intent.putExtra(EXTRA_ISSOFTSCAN, false);
                    }
                    // retrieve the device Timers information to check if it needs to be changed
                    _scanApiHelper.postGetTimersDevice(newDevice, _onGetTimersDevice);

                } else {
                    _scanApiHelper.postSetProfileConfigDevice(newDevice, _ezPairHostAddress,
                            _onSetProfileConfigDevice);
                    _scanApiHelper.postSetDisconnectDevice(newDevice, _onSetDisconnectDevice);
                }
            } else {
                String text = "Error " + result +
                        " during device arrival notification";
                intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            }
            if (intent != null) {
                context.sendBroadcast(intent);
            }
            _lastBroadcastedState = intent;
        }

        /**
         * ScanAPI is delivering some decoded data
         * as the activity to display them
         */
        public void onDecodedData(DeviceInfo deviceInfo,
                                  ISktScanDecodedData decodedData) {
            Intent intent = new Intent(NOTIFY_DECODED_DATA);
            intent.putExtra(EXTRA_SYMBOLOGY_NAME, decodedData.getSymbologyName());
            intent.putExtra(EXTRA_DECODEDDATA, decodedData.getData());
            context.sendBroadcast(intent);
        }

        /**
         * an error occurs during the retrieval of ScanObject
         * from ScanAPI, this is critical error and only a restart
         * can fix this.
         */
        public void onErrorRetrievingScanObject(long result) {
            Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
            String text = "Error unable to retrieve ScanAPI message: ";
            text += "(" + result + ")";
            text += "Please close this application and restart it";
            intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            context.sendBroadcast(intent);
            _lastBroadcastedState = intent;
        }
    };
    /**
     * register for ScanAPI ownership
     */


    private SktScanApiOwnership.Notification _scanApiOwnershipNotification = new SktScanApiOwnership.Notification() {

        public void onScanApiOwnershipChange(Context context, boolean release) {
            if (release) {
                closeScanApi();
            } else {
                openScanApi();
            }
        }

        @Override
        public void onScanApiOwnershipFailed(Context context, String applicationGettingOwnership) {
            // the ownership has been given to another App.
            // you can display an error message, or maybe just retry
        }
    };

    private void registerScanApiOwnership() {
        _scanApiOwnership.register(context);
    }

    /**
     * unregister from ScanAPI ownership
     */
    private void unregisterScanApiOwnership() {
        _scanApiOwnership.unregister();
    }

    /**
     * open ScanAPI by first claiming its ownership
     * then checking if the previous instance of ScanAPI has
     * been correctly close. ScanAPI initialization is done in a
     * separate thread, because it performs some internal testing
     * that requires some time to complete and we want the UI to be
     * responsive and present on the screen during that time.
     */
    private void openScanApi() {
        _scanApiOwnership.claimOwnership();
        // check this event to be sure the previous
        // ScanAPI consumer has been shutdown
        Debug.MSG(Debug.kLevelTrace, "Wait for the previous terminate event to be set");

        if (_consumerTerminatedEvent.waitFor(3000)) {
            Debug.MSG(Debug.kLevelTrace, "the previous terminate event has been set");
            _consumerTerminatedEvent.reset();
            _scanApiHelper.removeCommands(null);// remove all the commands
            _scanApiHelper.open();
        } else {
            Debug.MSG(Debug.kLevelTrace, "the previous terminate event has NOT been set");
            Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
            intent.putExtra(EXTRA_ERROR_MESSAGE,
                    "Unable to start ScanAPI because the previous close hasn't been completed. Restart this application.");
            context.sendBroadcast(intent);
        }
    }

    /**
     * close ScanAPI by first releasing its ownership and
     * by sending an abort. This allows ScanAPI to shutdown
     * gracefully by asking to close any Scanner Object if
     * they were opened. When ScanAPI is done a kSktScanTerminate event
     * is received in the ScanObject consumer timer thread.
     */
    private void closeScanApi() {
        _scanApiOwnership.releaseOwnership();
        _scanApiHelper.close();
    }

    private ICommandContextCallback _onGetSoftScanStatus = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            Intent intent = new Intent(GET_SOFTSCAN_COMPLETE);
            intent.putExtra(EXTRA_ERROR, scanObj.getMessage().getResult());
            intent.putExtra(EXTRA_SOFTSCAN_STATUS, scanObj.getProperty().getByte());
            context.sendBroadcast(intent);
        }
    };

    protected ICommandContextCallback _onGetScanApiConfiguration = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (SktScanErrors.SKTSUCCESS(result)) {
                _originalScanAPIConfiguration = scanObj.getProperty().getString().getValue();
                if (!_originalScanAPIConfiguration.toLowerCase(Locale.US).contains("server")) {
                    _originalScanAPIConfiguration = DEFAULT_SCANAPI_CONFIGURATION;
                    if (!_ezPairInProgress) {
                        _scanApiHelper.postSetScanAPIConfiguration(
                                ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                                _originalScanAPIConfiguration,
                                _onSetScanApiConfiguration);
                    }
                }
            } else {
                String text = "Error " + result +
                        " getting ScanAPI configuration";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            }
        }
    };

    protected ICommandContextCallback _onSetScanApiConfiguration = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (!SktScanErrors.SKTSUCCESS(result)) {
                String text = "Error " + result +
                        " setting ScanAPI configuration";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
                context.sendBroadcast(intent);
            }
        }
    };

    protected ICommandContextCallback _onSetProfileConfigDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (!SktScanErrors.SKTSUCCESS(result)) {
                String text = "Error " + result +
                        " setting Device profile Configuration";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
                context.sendBroadcast(intent);
            }
        }
    };

    protected ICommandContextCallback _onSetDisconnectDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (!SktScanErrors.SKTSUCCESS(result)) {
                String text = "Error " + result +
                        " disconnecting the device";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            }
        }
    };

    protected ICommandContextCallback _onSetTimersDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (!SktScanErrors.SKTSUCCESS(result)) {
                String text = "Error " + result +
                        " setting the device timers";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            }
        }
    };

    protected ICommandContextCallback _onGetTimersDevice = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            long result = scanObj.getMessage().getResult();
            if (!SktScanErrors.SKTSUCCESS(result)) {
                String text = "Error " + result +
                        " getting the device timers information";
                Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                intent.putExtra(EXTRA_ERROR_MESSAGE, text);
            } else {
                char[] deviceTimers = scanObj.getProperty().getArray().getValue();
                int value;
                if (scanObj.getProperty().getArray().getLength() >= 8) {
                    value = deviceTimers[6];
                    value <<= 8;
                    value += deviceTimers[7];

                    if (value > defaultConnectedTimeout) {
                        CommandContext context = (CommandContext) scanObj.getProperty()
                                .getContext();
                        _scanApiHelper.postSetTimersDevice(context.getDeviceInfo(),
                                ISktScanProperty.values.timers.kSktScanTimerPowerOffConnected,
                                0, 0, defaultConnectedTimeout, _onSetTimersDevice);
                    }
                } else {
                    String text = "the device timers information has an incorrect format";
                    Intent intent = new Intent(NOTIFY_ERROR_MESSAGE);
                    intent.putExtra(EXTRA_ERROR_MESSAGE, text);
                }
            }
        }
    };


    public BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contains(START_EZ_PAIR)) {
                if (!_ezPairInProgress) {
                    _ezPairDeviceName = intent.getStringExtra(EXTRA_EZ_PAIR_DEVICE);
                    _ezPairHostAddress = intent.getStringExtra(EXTRA_EZ_PAIR_HOST_ADDRESS);
                    _ezPairInProgress = true;

                    // backup the original ScanAPI configuration
                    _scanApiHelper.postGetScanAPIConfiguration(
                            ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                            _onGetScanApiConfiguration);

                    // change the ScanAPI configuration to make it connect
                    // to a specific scanner
                    _scanApiHelper.postSetScanAPIConfiguration(
                            ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                            "client:" + _ezPairDeviceName,
                            _onSetScanApiConfiguration);
                }
            } else if (intent.getAction().contains(STOP_EZ_PAIR)) {
                if (_ezPairInProgress) {
                    _ezPairInProgress = false;
                    // restore the original ScanAPI configuration
                    _scanApiHelper.postSetScanAPIConfiguration(
                            ISktScanProperty.values.configuration.kSktScanConfigSerialComPort,
                            _originalScanAPIConfiguration,
                            _onSetScanApiConfiguration);
                }
            } else if (intent.getAction().contains(SET_DATA_CONFIRMATION)) {
                DeviceInfo device = (DeviceInfo) _scanApiHelper.getDevicesList().lastElement();
                _scanApiHelper.postSetDataConfirmation(device, null);
            } else if (intent.getAction().contains(GET_SOUND_CONFIG)) {
                DeviceInfo device = (DeviceInfo) _scanApiHelper.getDevicesList().lastElement();

                // ask for the sound config of the scanner
                _scanApiHelper.postGetSoundConfigDevice(
                        device,
                        ISktScanProperty.values.soundActionType.kSktScanSoundActionTypeGoodScan,
                        _onGetSoundConfigDevice);
            } else if (intent.getAction().contains(SET_SOUND_CONFIG)) {
                DeviceInfo device = (DeviceInfo) _scanApiHelper.getDevicesList().lastElement();
                short[] soundConfig = new short[3];
                soundConfig[0] = ISktScanProperty.values.soundFrequency.kSktScanSoundFrequencyHigh;
                soundConfig[1] = 200;
                soundConfig[2] = 100;
                String frequency = intent.getStringExtra(EXTRA_SOUND_CONFIG_FREQUENCY);
                if (frequency.contains(SOUND_CONFIG_FREQUENCY_MEDIUM)) {
                    soundConfig[0]
                            = ISktScanProperty.values.soundFrequency.kSktScanSoundFrequencyMedium;
                } else if (frequency.contains(SOUND_CONFIG_FREQUENCY_LOW)) {
                    soundConfig[0]
                            = ISktScanProperty.values.soundFrequency.kSktScanSoundFrequencyLow;
                }
                // set the scanner sound config
                _scanApiHelper.postSetSoundConfigDevice(
                        device,
                        ISktScanProperty.values.soundActionType.kSktScanSoundActionTypeGoodScan,
                        soundConfig,
                        _onSetScanApiConfiguration);
            }
            // notification about the Bluetooth Radio ON / OFF
            else if (intent.getAction().equalsIgnoreCase(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    // we rely on ScanAPI reporting the error -47 ESKT_NOTHINGTOLISTEN
                    // to detect if Bluetooth is OFF, we could also have ignored this error
                    // and close ScanAPI here instead of closing ScanAPI in its onError handler
                    Debug.MSG(Debug.kLevelTrace,
                            "Receive Bluetooth ACTION_STATE_CHANGED with STATE_TURNING_OFF");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Debug.MSG(Debug.kLevelTrace,
                            "Receive Bluetooth ACTION_STATE_CHANGED with STATE_TURNING_ON");
                    // Bluetooth is ON again, check if we need to openScanApi again
                    if (_reopenScanApiWhenBluetoothIsOnAgain) {
                        openScanApi();
                        _reopenScanApiWhenBluetoothIsOnAgain = false;
                    }
                }
            }
        }
    };


    public Handler _messageHandler = new Handler(new Handler.Callback() {

        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CLOSE_SCAN_API:
                    Debug.MSG(Debug.kLevelTrace,
                            "Receive a CLOSE SCAN API Message and View Count=" + _viewCount
                                    + "ScanAPI open:" + _scanApiHelper.isScanApiOpen());
                    // if we receive this message and the view count is 0
                    // and ScanAPI is open then we should close it
                    if ((_viewCount == 0) && (_scanApiHelper.isScanApiOpen())) {
                        unregisterScanApiOwnership();
                        closeScanApi();
                    }
                    break;
            }
            return false;
        }
    });

    /**
     * increase the view count.
     * <br>this is called typically on each Activity.onCreate
     * <br>If the view count was 0 then it asks this application object
     * to register for ScanAPI ownership notification and to open ScanAPI
     */
    public void increaseViewCount() {
        if (!_scanApiHelper.isScanApiOpen()) {
            if (_viewCount == 0) {
                registerScanApiOwnership();
                openScanApi();
            } else {
                Debug.MSG(Debug.kLevelWarning,
                        "There is more View created without ScanAPI opened??");
            }
        } else {
            if (_lastBroadcastedState != null) {
                context.sendBroadcast(_lastBroadcastedState);
            }
        }
        ++_viewCount;
        Debug.MSG(Debug.kLevelTrace, "Increase View count, New view count: " + _viewCount);
    }

    /**
     * decrease the view count.
     * <br> this is called typically on each Activity.onDestroy
     * <br> If the view Count comes to 0 then it will try to close
     * ScanAPI and unregister for ScanAPI ownership notification unless
     * this decreaseViewCount is happening because of a screen rotation
     */
    public void decreaseViewCount() {
        // if the view count is going to be 0
        // and ScanAPI is open and there hasn't
        // been a screen rotation then close ScanApi
        if ((_viewCount == 1) && (_scanApiHelper.isScanApiOpen())) {
            // it's probably OK to close ScanAPI now, but
            // just send a CLOSE_SCAN_API request delayed by .5s
            // to give the View a chance to be recreated
            // if it was just a screen rotation
            Debug.MSG(Debug.kLevelTrace, "Post a differed request to close ScanAPI");
            _messageHandler.sendEmptyMessageDelayed(CLOSE_SCAN_API, 500);
        }
        --_viewCount;
        if (_viewCount < 0) {
            _viewCount = 0;
            Debug.MSG(Debug.kLevelWarning, "try to decrease more view count that possible");
        }
        Debug.MSG(Debug.kLevelTrace, "Decrease View count, New view count: " + _viewCount);
    }

    private ICommandContextCallback _onSetTrigger = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            Intent intent = new Intent(SET_TRIGGER_COMPLETE);
            intent.putExtra(EXTRA_ERROR, scanObj.getMessage().getResult());
            context.sendBroadcast(intent);
        }
    };

    private ICommandContextCallback _onSetOverlayView = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            Intent intent = new Intent(SET_OVERLAYVIEW_COMPLETE);
            intent.putExtra(EXTRA_ERROR, scanObj.getMessage().getResult());
            context.sendBroadcast(intent);
        }
    };

    private ICommandContextCallback _onSetSoftScanStatus = new ICommandContextCallback() {

        @Override
        public void run(ISktScanObject scanObj) {
            Intent intent = new Intent(SET_SOFTSCAN_COMPLETE);
            intent.putExtra(EXTRA_ERROR, scanObj.getMessage().getResult());
            context.sendBroadcast(intent);
        }
    };


    public void setTraces(boolean bTracesOn) {
        _scanApiHelper.postSetScanAPITraces(bTracesOn);
    }

    public void getSoftScanStatus() {
        _scanApiHelper.postGetSoftScanStatus(_onGetSoftScanStatus);
    }

    public void setSoftScanStatus(int status) {
        _scanApiHelper.postSetSoftScanStatus(status, _onSetSoftScanStatus);
    }

    public void setSoftScanTrigger(char action) {
        _scanApiHelper.postSetTriggerDevice(_softScanDevice, action, _onSetTrigger);
    }

    public void setOverlayView(Object overlayview) {
        _scanApiHelper.postSetOverlayView(_softScanDevice, overlayview, _onSetOverlayView);
    }


    public boolean isScannerDeviceConnected() {

        return _scanApiHelper.isDeviceConnected();
    }


    public void disconnectDevice() {
        _scanApiHelper.postSetDisconnectDevice(_softScanDevice, _onSetDisconnectDevice);
    }


    /**
     * Notification helping to manage ScanAPI ownership.
     * Only one application at a time can have access to ScanAPI.
     * When another application is claiming ScanAPI ownership, this
     * callback is called with release set to true asking this application
     * to release scanAPI. When the other application is done with ScanAPI
     * it calls releaseOwnership, causing this callback to be called again
     * but this time with release set to false. At that moment this application
     * can reclaim the ScanAPI ownership.
     */
}
