package com.hubtel.hubtelscanners.scanners;

import com.socketmobile.scanapi.ISktScanApi;

public class ScanAPIInitialization extends Thread {

    private ICallback _callback = null;

    private ISktScanApi _scanAPI = null;

    public interface ICallback {

        void completed(long result);
    }

    public ScanAPIInitialization(ISktScanApi scanApi, ICallback callback) {
        _scanAPI = scanApi;
        _callback = callback;
    }

    public void run() {
        long result = _scanAPI.Open(null);
        _callback.completed(result);
    }

}

