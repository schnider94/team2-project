package amc.team2.project

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class ActionActivity : Activity() {

    private var level: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action)
        Toast.makeText(this, "HELLO ACTION", Toast.LENGTH_SHORT).show()
        level = this.intent.getIntExtra("level", 0)
        val helperTextView: TextView = findViewById(R.id.helperText)
        helperTextView.text = "Level of Anxiety: $level"
    }

}