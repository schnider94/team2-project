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
import android.util.JsonReader
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.timerTask

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

        val SERVICE_UUID      = UUID.fromString("0000337d-0000-1000-8000-00805f9b34fb")!!
        val GSR_UUID          = UUID.fromString("00001a11-0000-1000-8000-00805f9b34fb")!!
        val HEARTRATE_UUID    = UUID.fromString("00001a12-0000-1000-8000-00805f9b34fb")!!

        var TIMEPOINTS = 30
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

        var counter = 0
        timer.scheduleAtFixedRate(
            timerTask {
                for (characteristic in characteristics) {
                    if (characteristic.uuid == HEARTRATE_UUID && counter == 0) {
                        Log.v(TAG, "[ReadCharacteristicStart]" + characteristic.uuid.toString())
                        gatt.readCharacteristic(characteristic)
                        counter = 1
                        break
                    } else if (characteristic.uuid == GSR_UUID && counter == 1) {
                        Log.v(TAG, "[ReadCharacteristicStart]" + characteristic.uuid.toString())
                        gatt.readCharacteristic(characteristic)
                        counter = 0
                        break
                    }
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
            Log.v(TAG, "[ReadCharacteristicHandler] " + characteristic.uuid.toString() + " Status: $status")
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
                Log.v(TAG, "GSR: $value")
                currentGSR = value
                gsrBuffer.add(value)
            }
            HEARTRATE_UUID -> {
                Log.v(TAG, "HR: $value")
                currentHeartRate = value
                heartRateBuffer.add(value)
            }
        }

        Log.v(TAG, "HeartRateSize: " + heartRateBuffer.size)
        Log.v(TAG, "GSRSize: " + gsrBuffer.size)

        if (heartRateBuffer.size >= TIMEPOINTS && gsrBuffer.size >= TIMEPOINTS && !isProcessing) {
            isProcessing = true
            val heartRates = heartRateBuffer
            val gsrs = gsrBuffer

            val strHeartRates: MutableList<String> = mutableListOf()
            for (heartRate in heartRates) {
                strHeartRates.add(heartRate.toString())
                if (strHeartRates.size == TIMEPOINTS) {
                    break
                }
            }

            Log.v(TAG, "Start Processing")
            val vectors = readFile("vectors.json")
            val coefficients = readFile("coefficients.json")
            val svcClass = SVC.main(strHeartRates.toTypedArray(), vectors.toTypedArray(), coefficients.toTypedArray())

            if (svcClass == 1) {
                currentLevel = 1
            }
            Log.d(TAG, "Current Level is $currentLevel")

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
            else -> {
                Log.v(TAG, "No Anxiety detected")
            }
        }
    }

    private fun readFile(fileName: String) : MutableList<DoubleArray> {
        val data = mutableListOf<DoubleArray>()
        val stream = resources.assets.open(fileName)
        val reader = JsonReader(InputStreamReader(stream))

        reader.beginArray()
        while (reader.hasNext()) {
            val doubles = mutableListOf<Double>()
            reader.beginArray()
            while (reader.hasNext()) {
                doubles.add(reader.nextDouble())
            }
            reader.endArray()
            data.add(doubles.toDoubleArray())
        }
        reader.endArray()
        return data
    }
}