package com.reliablealarm.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.adapters.AlarmAdapter

/**
 * Activity to view and manage all alarms.
 *
 * WHY: User needs central place to see all alarms.
 * Supports:
 * - View all alarms
 * - Toggle enabled/disabled
 * - Edit alarm
 * - Delete alarm
 * - Create new alarm
 *
 * Uses RecyclerView for efficient list rendering.
 */
class AlarmListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var fab: FloatingActionButton

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var adapter: AlarmAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Alarms"

        alarmRepository = AlarmRepository(this)
        alarmScheduler = AlarmScheduler(this)

        recyclerView = findViewById(R.id.alarmRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        fab = findViewById(R.id.fab)

        // Setup RecyclerView
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> toggleAlarm(alarm, enabled) },
            onEdit = { alarm -> editAlarm(alarm) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // FAB to create new alarm
        fab.setOnClickListener {
            createNewAlarm()
        }

        loadAlarms()
    }

    override fun onResume() {
        super.onResume()
        loadAlarms()
    }

    /**
     * Load and display all alarms.
     */
    private fun loadAlarms() {
        val alarms = alarmRepository.getAllAlarms()

        if (alarms.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(alarms)
        }
    }

    /**
     * Toggle alarm enabled/disabled.
     */
    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        alarmRepository.toggleAlarm(alarm.id, enabled)

        if (enabled) {
            alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
            Toast.makeText(this, "Alarm enabled", Toast.LENGTH_SHORT).show()
        } else {
            alarmScheduler.cancelAlarm(alarm.id)
            Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show()
        }

        loadAlarms()
    }

    /**
     * Edit existing alarm.
     */
    private fun editAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmEditActivity::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        startActivity(intent)
    }

    /**
     * Delete alarm.
     */
    private fun deleteAlarm(alarm: Alarm) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete \"${alarm.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                alarmScheduler.cancelAlarm(alarm.id)
                alarmRepository.deleteAlarm(alarm.id)
                Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show()
                loadAlarms()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Create new alarm.
     */
    private fun createNewAlarm() {
        startActivity(Intent(this, AlarmEditActivity::class.java))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

