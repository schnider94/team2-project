package amc.team2.project

import android.app.IntentService
import android.content.Intent
import android.util.Log
import android.support.v4.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import java.util.*
import kotlin.concurrent.schedule


class ProcessService : IntentService("ProcessService") {

    companion object {
        val ACTION_PING = ProcessService::class.simpleName + ".PING"
        val ACTION_PONG = ProcessService::class.simpleName + ".PONG"
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

        // Temporary timer to simulate processing
        Timer().schedule(5000) {
            splashScreen(2)
            // sendAnxietyAlarm(1)
        }

        while(true) {}
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
}