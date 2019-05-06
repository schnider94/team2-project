package amc.team2.project

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class DeviceRecyclerAdapter(private val devices: ArrayList<BluetoothDevice>,
                            private val saveDevice: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceRecyclerAdapter.DeviceViewHolder>() {

    override fun getItemCount() = devices.size

    override fun onCreateViewHolder(parent: ViewGroup, row: Int): DeviceViewHolder {
        val inflatedView = parent.inflate(R.layout.device_recycler_view, false)
        return DeviceViewHolder(inflatedView).listen { pos,_ ->
            saveDevice(devices[pos])
        }
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, row: Int) {
        holder.bindDevice(devices[row])
    }

    class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var device: BluetoothDevice? = null

        private var nameTextView: TextView = v.findViewById(R.id.device_name)
        private var isConnectedView: TextView = v.findViewById(R.id.device_address)

        fun bindDevice(device: BluetoothDevice) {
            this.device = device
            nameTextView.text = "${device.name} - ${device.address}"
            val prefs = isConnectedView.context.getSharedPreferences(isConnectedView.context.getString(R.string.prefs_filename), 0)

            var isConnected = prefs.getString("device_address", "") == device.address
            if (isConnected) {
                isConnectedView.text = "Connected"
            } else {
                isConnectedView.text = "Not connected"
            }
        }
    }

}