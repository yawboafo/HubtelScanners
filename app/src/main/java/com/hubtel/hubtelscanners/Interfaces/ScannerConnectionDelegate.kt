package com.hubtel.hubtelscanners.Interfaces

interface ScannerConnectionDelegate {

    fun scannerConnectBegan()
    fun scannerConnectFailed(value : String)
    fun scannerConnectComplete()
    fun scannerArrived()
}