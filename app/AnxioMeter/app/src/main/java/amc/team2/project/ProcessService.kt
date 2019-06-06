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
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.floor


class ProcessService : IntentService("ProcessService") {

    private var isRunning = false
    private var isProcessing = false

    private var currentLevel = 0
    private var currentGSR = 0
    private var currentHeartRate = 0

    private var characteristics: List<BluetoothGattCharacteristic> = ArrayList()
    private var heartRateBuffer: MutableList<Int> = mutableListOf()
    private var gsrBuffer: MutableList<Int> = mutableListOf()

    companion object {
        val ACTION_PING = ProcessService::class.simpleName + ".PING"
        val ACTION_PONG = ProcessService::class.simpleName + ".PONG"
        val ACTION_VALUES = ProcessService::class.simpleName + ".VALUES"

        val SERVICE_UUID      = UUID.fromString("00000000-0000-0000-0000-00000000337d")!!
        val GSR_UUID          = UUID.fromString("00001a11-0000-1000-8000-00805f9b34fb")!!
        val HEARTRATE_UUID    = UUID.fromString("00001a12-0000-1000-8000-00805f9b34fb")!!

        var TIMEPOINTS = 10
    }

    override fun onHandleIntent(workIntent: Intent) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, IntentFilter(ACTION_PING))

        Log.d(this::class.simpleName, "Start Loop")
        mainLoop(workIntent.getStringExtra("device_name"), workIntent.getStringExtra("device_address"))
        Log.d(this::class.simpleName, "Ended Loop")
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
        val timer = Timer()
        timer.scheduleAtFixedRate(
            timerTask {
                for (characteristic in characteristics) {
                    gatt.readCharacteristic(characteristic)
                }
            }, 0, 500
        )
        while(isRunning);

        timer.cancel()
        timer.purge()
        gatt.disconnect()
        gatt.close()
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

    private fun broadcastValues(heartrate: Int, gsr: Int, level: Int) {
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
                // Log.v(TAG, "Received Ping")
                val manager = LocalBroadcastManager.getInstance(applicationContext)
                manager.sendBroadcast(Intent(ACTION_PONG))
            }
        }
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
                Log.v(TAG, service.uuid.toString())
                if (service.uuid == SERVICE_UUID) {
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

    fun process(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
        when (characteristic.uuid) {
            GSR_UUID -> {
                currentGSR = value
                gsrBuffer.add(value)
                Log.v(TAG, "GSR: $value")
            }
            HEARTRATE_UUID -> {
                currentHeartRate = value
                heartRateBuffer.add(value)
                Log.v(TAG, "HR: $value")
            }
        }

        if (heartRateBuffer.size >= TIMEPOINTS && gsrBuffer.size >= TIMEPOINTS && !isProcessing) {
            isProcessing = true
            val heartRates = heartRateBuffer
            val gsrs = gsrBuffer

            // TODO: Start processing
            Log.v(TAG, "Start Processing")
            val heartRateAVG = heartRates.average()
            val gsrAVG =gsrs.average()
            currentLevel = floor((gsrAVG + heartRateAVG) / 100).toInt()
            Log.v(TAG, "Current Level is $currentLevel")
            // END Processing

            heartRateBuffer.clear()
            gsrBuffer.clear()
            isProcessing = false
        }
        broadcastValues(currentHeartRate, currentGSR, currentLevel)

        when (currentLevel) {
            1 -> {
                Log.v(TAG, "Level 1 Anxiety detected -> Notify")
                sendAnxietyAlarm(currentLevel)
            }
            2 -> {
                Log.v(TAG, "Level 2 Anxiety detected -> Splash")
                splashScreen(currentLevel)
            }
            else -> {
                Log.v(TAG, "No Anxiety detected")
            }
        }
    }
}