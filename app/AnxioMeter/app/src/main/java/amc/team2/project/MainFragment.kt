package amc.team2.project


import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.support.v4.content.LocalBroadcastManager
import android.widget.TextView
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule


/**
 * A simple [Fragment] subclass.
 *
 */
class MainFragment : Fragment() {

    private var backgroundTimer: Timer? = null
    private var backgroundServiceIsRunning = false
    private var startBackgroundButton: Button? = null
    private var broadcastManager: LocalBroadcastManager? = null

    private lateinit var anxietyLevelLabel: TextView
    private lateinit var heartRateLabel: TextView
    private lateinit var skinResponseLabel: TextView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startBackgroundButton = view.findViewById(R.id.start_background)
        startBackgroundButton?.setOnClickListener(startBackground)

        anxietyLevelLabel = view.findViewById(R.id.anxiety_level_number)
        heartRateLabel = view.findViewById(R.id.heart_rate_number)
        skinResponseLabel = view.findViewById(R.id.skin_response_number)

        val deviceTextView: TextView = view.findViewById(R.id.connected_device)
        val deviceInfo = getDeviceInfo(false)
        if (deviceInfo.exists) {
            deviceTextView.text = "Connected Device: \n${deviceInfo.name} - ${deviceInfo.address}"
        }
    }

    override fun onStart() {
        super.onStart()
        broadcastManager = LocalBroadcastManager.getInstance(activity!!)
        backgroundTimer = fixedRateTimer("backgroundPing", false, 0, 2000) {
            checkForBackgroundService()
            Timer().schedule(100) {
                updateBackgroundButton()
            }
        }
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this.activity!!).unregisterReceiver(mReceiver)
        backgroundTimer?.cancel()
        super.onStop()
    }

    private val startBackground = View.OnClickListener {
        if (!backgroundServiceIsRunning) {
            Log.d(this::class.simpleName, "Start Background Task")

            val deviceInfo = getDeviceInfo()
            if (!deviceInfo.exists) {
                return@OnClickListener
            }

            val backgroundIntent = Intent(activity, ProcessService::class.java).apply {
                putExtra("device_name", deviceInfo.name)
                putExtra("device_address", deviceInfo.address)
            }
            activity?.startService(backgroundIntent)
            backgroundServiceIsRunning = true
            updateBackgroundButton()
        } else {
            // TODO: stop background service with broadcast
        }
    }

    data class DeviceInfo(val exists: Boolean, val name: String?, val address: String?)
    private fun getDeviceInfo(showError: Boolean = true) : DeviceInfo {
        val prefs = activity!!.getSharedPreferences(getString(R.string.prefs_filename), 0)
        val deviceName = prefs.getString("device_name", null)
        val deviceAddress = prefs.getString("device_address", null)

        if (deviceAddress == null || deviceName == null) {
            if (showError) {
                val dialogBuilder = AlertDialog.Builder(activity!!)
                dialogBuilder.setMessage("Please connect a device from the Bluetooth Tab")
                    .setCancelable(false)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton("Open") { dialog, _ ->
                        dialog.dismiss()
                        (activity as MainActivity).openTab(R.id.navigation_bluetooth)
                    }

                val alert = dialogBuilder.create()
                alert.setTitle("No Connected Device")
                alert.show()
            }
            return DeviceInfo(false, null, null)
        }
        return DeviceInfo(true, deviceName, deviceAddress)
    }

    private fun updateBackgroundButton() {
        activity?.runOnUiThread {
            if (backgroundServiceIsRunning) {
                startBackgroundButton?.setText(R.string.background_is_running)
            } else {
                startBackgroundButton?.setText(R.string.start_background)
            }
            view?.invalidate()
        }
    }

    private fun checkForBackgroundService() {
        backgroundServiceIsRunning = false
        broadcastManager?.registerReceiver(mReceiver, IntentFilter(ProcessService.ACTION_PONG))
        broadcastManager?.registerReceiver(mReceiver, IntentFilter(ProcessService.ACTION_VALUES))
        broadcastManager?.sendBroadcast(Intent(ProcessService.ACTION_PING))
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ProcessService.ACTION_PONG) {
                backgroundServiceIsRunning = true
                // Log.v(TAG, "Received Pong")
            }
            if (intent.action == ProcessService.ACTION_VALUES) {
                val level = intent.getIntExtra("level", 0)
                val gsr = intent.getIntExtra("gsr", 0)
                val heartrate = intent.getIntExtra("heartrate", 0)

                anxietyLevelLabel.text = level.toString()
                skinResponseLabel.text = gsr.toString()
                heartRateLabel.text = heartrate.toString()
            }
        }
    }
}
