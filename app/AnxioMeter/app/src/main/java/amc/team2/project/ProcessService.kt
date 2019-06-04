package amc.team2.project

import android.app.IntentService
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.content.Intent
import android.util.Log
import android.support.v4.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentFilter
import java.lang.Math.ceil
import java.util.*
import kotlin.concurrent.timerTask


class ProcessService : IntentService("ProcessService") {

    private var isRunning = false

    private val SERVICE_ID      = UUID.fromString("00000000-0000-0000-0000-00000000337d")
    private val GSR_ID          = UUID.fromString("00001a11-0000-1000-8000-00805f9b34fb")
    private val HEARTRATE_ID    = UUID.fromString("00001a12-0000-1000-8000-00805f9b34fb")

    private var currentLevel = 0
    private var currentGSR = 0
    private var currentHeartRate = 0

    private var characteristics: List<BluetoothGattCharacteristic> = ArrayList()

    companion object {
        val ACTION_PING = ProcessService::class.simpleName + ".PING"
        val ACTION_PONG = ProcessService::class.simpleName + ".PONG"
        val ACTION_VALUES = ProcessService::class.simpleName + ".VALUES"
    }

    override fun onHandleIntent(workIntent: Intent) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, IntentFilter(ACTION_PING))

        Log.d(this::class.simpleName, "Start Loop")
        mainLoop(workIntent.getStringExtra("device_name"), workIntent.getStringExtra("device_address"))
        Log.d(this::class.simpleName, "ENDED Loop")
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        super.onDestroy()
    }

    private fun mainLoop(deviceName: String, deviceAddress: String) {
        Log.d(this::class.simpleName, "Name: $deviceName with Address: $deviceAddress")

        val device = getBLEAdapter()!!.getRemoteDevice(deviceAddress)
        val gatt = device.connectGatt(this, false, bleCallback)

        isRunning = true
        Timer().scheduleAtFixedRate(
            timerTask {
                loop(gatt)

                if (!isRunning) {
                    gatt.disconnect()
                    gatt.close()
                    this.cancel()
                }
            }, 0, 500
        )
        while(isRunning);
    }

    private fun loop(gatt: BluetoothGatt) {
        for (characteristic in this.characteristics) {
            gatt.readCharacteristic(characteristic)
        }
    }

    // Open App directly
    private fun splashScreen(level: Int) {
        Log.d(this@ProcessService::class.simpleName, "Splash Screen")
        val intent = Intent(this, ActionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.putExtra("level", level)
        startActivity(intent)
        this.stopSelf()
    }

    // Show notification when pressed open App
    private fun sendAnxietyAlarm(level: Int) {
        Log.d(this@ProcessService::class.simpleName, "Notification")
        val resultIntent = Intent(this, ActionActivity::class.java).apply {
            putExtra("level", level)
        }
        NotificationManager.getInstance(this).sendNotification("Anxiety Alarm","Click to start Help Action", resultIntent)
        this.stopSelf()
    }

    private fun sendValuesToScreen(heartrate: Int, gsr: Int, level: Int) {
        val manager = LocalBroadcastManager.getInstance(applicationContext)
        val intent = Intent(ACTION_VALUES)
        intent.putExtra("level", level)
        intent.putExtra("gsr", gsr)
        intent.putExtra("heartrate", heartrate)
        manager.sendBroadcast(intent)
    }

    // PING/PONG
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PING) {
                // Log.v(this@ProcessService::class.simpleName, "Received Ping")
                val manager = LocalBroadcastManager.getInstance(applicationContext)
                manager.sendBroadcast(Intent(ACTION_PONG))
            }
        }
    }

    fun process(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
        when (characteristic.uuid) {
            GSR_ID -> {
                currentGSR = value
                Log.d(TAG, "GSR: $value")
            }
            HEARTRATE_ID -> {
                currentHeartRate = value
                Log.d(TAG, "HR: $value")
            }
        }

        // TODO: process Sensor data
        if (currentHeartRate != 0) {
            currentLevel = currentGSR / currentHeartRate
        } else {
            currentLevel = 0
        }
        sendValuesToScreen(currentHeartRate, currentGSR, currentLevel)

        // splashScreen(1)
        // sendAnxietyAlarm(1)
    }

    private var connectionState = STATE_DISCONNECTED

    private val bleCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionState = STATE_CONNECTED
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(TAG, "Attempting to start service discovery: " + gatt.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    isRunning = false
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            for (service in gatt!!.services) {
                Log.d(TAG, service.uuid.toString())
                if (service.uuid == SERVICE_ID) {
                    characteristics = service.characteristics
                }
            }
        }

        // Result of a characteristic read operation
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Log.d(TAG, characteristic.uuid.toString())
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    process(characteristic)
                }
            }
        }
    }
}