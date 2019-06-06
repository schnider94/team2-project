package amc.team2.project

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            val fragment = MainFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commit()
        }
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                val fragment = MainFragment()
                supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_bluetooth -> {
                val fragment = BluetoothFragment()
                supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                val fragment = SettingsFragment()
                supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun openTab(id: Int) {
        bottomNavigationView.selectedItemId = id
    }
}
