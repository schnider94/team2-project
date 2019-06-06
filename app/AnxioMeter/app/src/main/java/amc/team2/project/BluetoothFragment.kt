package amc.team2.project


import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import java.util.*
import kotlin.concurrent.schedule

/**
 * A simple [Fragment] subclass.
 *
 */
class BluetoothFragment : Fragment() {

    companion object {
        private const val REQUEST_ENABLE_BT = 12
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private lateinit var refreshButton: Button
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceRecyclerAdapter
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linearLayoutManager = LinearLayoutManager(activity)
        recyclerView = view.findViewById(R.id.devices_view)
        recyclerView.layoutManager = linearLayoutManager
        adapter = DeviceRecyclerAdapter(deviceList, saveDeviceFunc)
        recyclerView.adapter = adapter
        refreshButton = view.findViewById(R.id.refresh_button)

        bluetoothAdapter = activity?.getBLEAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        loadDevices()

        refreshButton.setOnClickListener {
            loadDevices()
        }
    }

    private val saveDeviceFunc : (BluetoothDevice) -> Unit =  { device ->
        val prefs = activity!!.getSharedPreferences(getString(R.string.prefs_filename), 0)
        val edit = prefs.edit()
        edit.putString("device_name", device.name)
        edit.putString("device_address", device.address)
        edit.commit()
        adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        bluetoothLeScanner?.stopScan(bleScanner)
        super.onStop()
    }

    private fun loadDevices() {
        if (bluetoothAdapter == null) {
            val dialogBuilder = AlertDialog.Builder(activity!!)
            dialogBuilder.setMessage("There was an error loading the Bluetooth Manager. This probably happens because you are in a Simulator")
                .setCancelable(false)
                .setPositiveButton("Okay") { dialog, _ ->
                    dialog.dismiss()
                }

            val alert = dialogBuilder.create()
            alert.setTitle("No Bluetooth Manager")
            alert.show()
            return
        }

        bluetoothAdapter.takeIf { !it!!.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        requestLocationPermission()

        bluetoothLeScanner?.startScan(bleScanner)
        Timer().schedule(10000) {
            bluetoothLeScanner?.stopScan(bleScanner)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == REQUEST_ENABLE_BT) {
            loadDevices()
        }
    }

    private fun requestLocationPermission() {

    }

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device ?: return
            if (deviceList.contains(device)) {
                return
            }
            activity?.runOnUiThread {
                Log.d("DeviceListActivity","onScanResult: ${result?.device?.address} - ${result?.device?.name}")
                this@BluetoothFragment.deviceList.add(device)
                adapter.notifyItemInserted(this@BluetoothFragment.deviceList.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("DeviceListActivity", "onScanFailed: $errorCode")
        }
    }

}
