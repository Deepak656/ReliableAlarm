package com.reliablealarm.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm

class AlarmListAdapter(
    private val alarmRepository: AlarmRepository,
    private val onEdit: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmListAdapter.AlarmViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val timeText: TextView = itemView.findViewById(R.id.alarmTime)
        private val nameText: TextView = itemView.findViewById(R.id.alarmName)
        private val daysText: TextView = itemView.findViewById(R.id.alarmDays)
        private val enabledSwitch: Switch = itemView.findViewById(R.id.alarmEnabled)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(alarm: Alarm) {
            timeText.text = alarm.getFormattedTime()
            nameText.text = alarm.name
            daysText.text = alarm.getFormattedDays()

            enabledSwitch.setOnCheckedChangeListener(null)
            enabledSwitch.isChecked = alarm.isEnabled

            enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                alarmRepository.toggleAlarm(alarm.id, isChecked)
                submitList(alarmRepository.getAllAlarms())
            }

            editButton.setOnClickListener {
                onEdit(alarm)
            }

            deleteButton.setOnClickListener {
                alarmRepository.deleteAlarm(alarm.id)
                submitList(alarmRepository.getAllAlarms())
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Alarm>() {
            override fun areItemsTheSame(old: Alarm, new: Alarm): Boolean =
                old.id == new.id

            override fun areContentsTheSame(old: Alarm, new: Alarm): Boolean =
                old == new
        }
    }
}
