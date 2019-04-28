package amc.team2.project


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.content.LocalBroadcastManager
import android.content.IntentFilter
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.concurrent.timer


/**
 * A simple [Fragment] subclass.
 *
 */
class MainFragment : Fragment() {

    private var backgroundTimer: Timer? = null
    private var backgroundServiceIsRunning = false
    private var startBackgroundButton: Button? = null
    private var broadcastManager: LocalBroadcastManager? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startBackgroundButton = view.findViewById(R.id.start_background)
        startBackgroundButton?.setOnClickListener(startBackground)
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
            val backgroundIntent = Intent(activity, ProcessService::class.java).apply {
                putExtra("device_name", "Name")
                putExtra("device_address", "Address")
            }
            activity?.startService(backgroundIntent)
            backgroundServiceIsRunning = true
            updateBackgroundButton()
        }
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
        broadcastManager?.sendBroadcast(Intent(ProcessService.ACTION_PING))
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ProcessService.ACTION_PONG) {
                backgroundServiceIsRunning = true
                // Log.v(this@MainFragment::class.simpleName, "Received Pong")
            }
        }
    }
}
