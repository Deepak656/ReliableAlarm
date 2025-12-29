package com.reliablealarm.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.StreakRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.adapters.AlarmAdapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main activity - Home screen with alarm list.
 *
 * WHY: Central hub for the app.
 * Features:
 * - 7-day streak preview at top
 * - Active alarms listed first (sorted by next trigger time)
 * - Inactive alarms listed below
 * - FAB to add new alarm
 * - Bottom navigation
 *
 * Thread-safe: All operations on main thread.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var streakPreviewContainer: LinearLayout
    private lateinit var alarmCountText: TextView
    private lateinit var streakInfoText: TextView
    private lateinit var activeAlarmsSection: LinearLayout
    private lateinit var activeAlarmsRecyclerView: RecyclerView
    private lateinit var activeAlarmsHeader: TextView
    private lateinit var inactiveAlarmsSection: LinearLayout
    private lateinit var inactiveAlarmsRecyclerView: RecyclerView
    private lateinit var inactiveAlarmsHeader: TextView
    private lateinit var emptyStateText: LinearLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var streakRepository: StreakRepository

    private lateinit var activeAlarmsAdapter: AlarmAdapter
    private lateinit var inactiveAlarmsAdapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide action bar for modern look
        supportActionBar?.hide()

        initializeRepositories()
        initializeViews()
        setupRecyclerViews()
        setupBottomNavigation()
        setupFAB()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initializeRepositories() {
        alarmRepository = AlarmRepository(this)
        alarmScheduler = AlarmScheduler(this)
        streakRepository = StreakRepository(this)
    }

    private fun initializeViews() {
        streakPreviewContainer = findViewById(R.id.streakPreviewContainer)
        alarmCountText = findViewById(R.id.alarmCount)
        streakInfoText = findViewById(R.id.streakInfo)
        activeAlarmsSection = findViewById(R.id.activeAlarmsSection)
        activeAlarmsRecyclerView = findViewById(R.id.activeAlarmsRecyclerView)
        activeAlarmsHeader = findViewById(R.id.activeAlarmsHeader)
        inactiveAlarmsSection = findViewById(R.id.inactiveAlarmsSection)
        inactiveAlarmsRecyclerView = findViewById(R.id.inactiveAlarmsRecyclerView)
        inactiveAlarmsHeader = findViewById(R.id.inactiveAlarmsHeader)
        emptyStateText = findViewById(R.id.emptyStateText)
        fab = findViewById(R.id.fab)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun setupRecyclerViews() {
        // Active alarms adapter
        activeAlarmsAdapter = AlarmAdapter(
            onToggle = { alarm, enabled -> toggleAlarm(alarm, enabled) },
            onEdit = { alarm -> editAlarm(alarm) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        activeAlarmsRecyclerView.layoutManager = LinearLayoutManager(this)
        activeAlarmsRecyclerView.adapter = activeAlarmsAdapter
        activeAlarmsRecyclerView.isNestedScrollingEnabled = false

        // Inactive alarms adapter
        inactiveAlarmsAdapter = AlarmAdapter(
            onToggle = { alarm, enabled -> toggleAlarm(alarm, enabled) },
            onEdit = { alarm -> editAlarm(alarm) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        inactiveAlarmsRecyclerView.layoutManager = LinearLayoutManager(this)
        inactiveAlarmsRecyclerView.adapter = inactiveAlarmsAdapter
        inactiveAlarmsRecyclerView.isNestedScrollingEnabled = false
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = R.id.nav_alarms
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarms -> {
                    // Already on home screen
                    true
                }
                R.id.nav_streak -> {
                    startActivity(Intent(this, StreakHistoryActivity::class.java))
                    false // Don't select, just navigate
                }
                R.id.nav_tasks -> {
                    // TODO: Navigate to tasks
                    startActivity(Intent(this, WakeTaskOptionsActivity::class.java))
                    false
                }
                R.id.nav_reliability -> {
                    // TODO: Navigate to reliability score
                    startActivity(Intent(this, ReliabilityOptionsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        fab.setOnClickListener {
            createNewAlarm()
        }
    }

    private fun loadData() {
        val allAlarms = alarmRepository.getAllAlarms()

        if (allAlarms.isEmpty()) {
            showEmptyState()
            return
        }

        hideEmptyState()

        // Separate active and inactive alarms
        val activeAlarms = allAlarms.filter { it.isEnabled }
        val inactiveAlarms = allAlarms.filter { !it.isEnabled }

        // Sort active alarms by next occurrence (soonest first)
        val sortedActiveAlarms = activeAlarms.sortedBy { getNextOccurrence(it) }

        // Update alarm count
        alarmCountText.text = "⏰ ${activeAlarms.size} Active Alarm${if (activeAlarms.size != 1) "s" else ""}"

        // Load streak info
        loadStreakInfo(allAlarms)

        // Display 7-day streak preview
        display7DayStreakPreview()

        // Update active alarms section
        if (sortedActiveAlarms.isNotEmpty()) {
            activeAlarmsSection.visibility = View.VISIBLE
            activeAlarmsAdapter.submitList(sortedActiveAlarms)
            activeAlarmsHeader.text = "Active Alarms (${sortedActiveAlarms.size})"
        } else {
            activeAlarmsSection.visibility = View.GONE
        }

        // Update inactive alarms section
        if (inactiveAlarms.isNotEmpty()) {
            inactiveAlarmsSection.visibility = View.VISIBLE
            inactiveAlarmsAdapter.submitList(inactiveAlarms)
            inactiveAlarmsHeader.text = "Inactive Alarms (${inactiveAlarms.size})"
        } else {
            inactiveAlarmsSection.visibility = View.GONE
        }
    }

    private fun loadStreakInfo(alarms: List<Alarm>) {
        if (alarms.isEmpty()) {
            streakInfoText.text = "🔥 Start your streak today!"
            return
        }

        // Get the highest streak from all alarms
        val maxStreak = alarms.maxOfOrNull { alarm ->
            streakRepository.getStreak(alarm.id).currentStreak
        } ?: 0

        val streakText = when {
            maxStreak == 0 -> "🔥 Start your streak today!"
            maxStreak == 1 -> "🔥 1 day streak - Keep going!"
            else -> "🔥 $maxStreak day streak - You're on fire!"
        }

        streakInfoText.text = streakText
    }

    private fun display7DayStreakPreview() {
        streakPreviewContainer.removeAllViews()

        val allAlarms = alarmRepository.getAllAlarms()
        if (allAlarms.isEmpty()) {
            streakPreviewContainer.visibility = View.GONE
            return
        }

        streakPreviewContainer.visibility = View.VISIBLE

        // Get confirmations from all alarms and combine them
        val allConfirmations = mutableListOf<com.reliablealarm.app.domain.models.WakeConfirmation>()
        for (alarm in allAlarms) {
            allConfirmations.addAll(streakRepository.getConfirmations(alarm.id))
        }

        val cellSize = resources.getDimensionPixelSize(R.dimen.streak_cell_size)
        val cellMargin = resources.getDimensionPixelSize(R.dimen.streak_cell_margin)

        // Display last 7 days
        for (i in 6 downTo 0) {
            val cell = View(this)
            val params = LinearLayout.LayoutParams(cellSize, cellSize).apply {
                setMargins(cellMargin, 0, cellMargin, 0)
            }
            cell.layoutParams = params

            val date = getDateDaysAgo(i)
            val confirmation = allConfirmations.firstOrNull { it.date == date }

            val drawable = when {
                confirmation == null -> R.drawable.streak_cell_empty
                confirmation.wokeOnTime == true -> R.drawable.streak_cell_success
                confirmation.wokeOnTime == false -> R.drawable.streak_cell_missed
                else -> R.drawable.streak_cell_pending
            }

            cell.background = resources.getDrawable(drawable, null)
            streakPreviewContainer.addView(cell)
        }

        // Add "View More" clickable area
        streakPreviewContainer.setOnClickListener {
            startActivity(Intent(this, StreakHistoryActivity::class.java))
        }
    }

    private fun getDateDaysAgo(daysAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(calendar.time)
    }

    /**
     * Calculate next occurrence for an alarm to sort them.
     * WHY: Show soonest alarm first.
     * Uses the Alarm model's built-in getNextTriggerTime() method.
     */
    private fun getNextOccurrence(alarm: Alarm): Long {
        return alarm.getNextTriggerTime()
    }

    private fun showEmptyState() {
        emptyStateText.visibility = View.VISIBLE
        activeAlarmsSection.visibility = View.GONE
        inactiveAlarmsSection.visibility = View.GONE
        streakPreviewContainer.visibility = View.GONE

        alarmCountText.text = "⏰ No Alarms Yet"
        streakInfoText.text = "🔥 Create your first alarm to start!"
    }

    private fun hideEmptyState() {
        emptyStateText.visibility = View.GONE
    }

    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        alarmRepository.toggleAlarm(alarm.id, enabled)

        if (enabled) {
            alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
            Toast.makeText(this, "Alarm enabled", Toast.LENGTH_SHORT).show()
        } else {
            alarmScheduler.cancelAlarm(alarm.id)
            Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show()
        }

        loadData()
    }

    private fun editAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmEditActivity::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        startActivity(intent)
    }

    private fun deleteAlarm(alarm: Alarm) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete \"${alarm.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                alarmScheduler.cancelAlarm(alarm.id)
                alarmRepository.deleteAlarm(alarm.id)
                Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewAlarm() {
        startActivity(Intent(this, AlarmEditActivity::class.java))
    }
}