package com.reliablealarm.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm

/**
 * RecyclerView adapter for alarm list.
 *
 * WHY: Displays alarms in modern card format.
 * Features:
 * - Toggle switch for enable/disable
 * - Edit button
 * - Delete button
 * - Alpha dimming for disabled alarms
 */
class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onEdit: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private var alarms = listOf<Alarm>()

    fun submitList(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(alarms[position])
    }

    override fun getItemCount() = alarms.size

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.alarmTime)
        private val nameText: TextView = itemView.findViewById(R.id.alarmName)
        private val daysText: TextView = itemView.findViewById(R.id.alarmDays)
        private val ringsInText: TextView = itemView.findViewById(R.id.alarmRingsIn)

        private val enableSwitch: SwitchCompat = itemView.findViewById(R.id.alarmEnabled)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(alarm: Alarm) {
            timeText.text = alarm.getFormattedTime()
            nameText.text = alarm.name
            daysText.text = alarm.getFormattedDays()

            if (alarm.isEnabled) {
                val msUntil = alarm.getNextTriggerTime() - System.currentTimeMillis()
                val hours = msUntil / (1000 * 60 * 60)
                val minutes = (msUntil % (1000 * 60 * 60)) / (1000 * 60)
                ringsInText.text = when {
                    hours > 0 -> "rings in ${hours}h ${minutes}m"
                    minutes > 0 -> "rings in ${minutes}m"
                    else -> "ringing soon"
                }
                ringsInText.visibility = View.VISIBLE
            } else {
                ringsInText.visibility = View.GONE
            }
            // Set switch without triggering listener
            enableSwitch.setOnCheckedChangeListener(null)
            enableSwitch.isChecked = alarm.isEnabled
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            // Apply dimming for disabled alarms
            val alpha = if (alarm.isEnabled) 1.0f else 0.5f
            timeText.alpha = alpha
            nameText.alpha = alpha
            daysText.alpha = alpha
            ringsInText.alpha = alpha

            // Click whole card to edit
            itemView.setOnClickListener {
                onEdit(alarm)
            }

            // Edit button
            editButton.setOnClickListener {
                onEdit(alarm)
            }

            // Delete button
            deleteButton.setOnClickListener {
                onDelete(alarm)
            }
        }
    }
}