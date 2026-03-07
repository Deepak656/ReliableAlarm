package com.reliablealarm.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.reliablealarm.app.R

class TaskPickerBottomSheet(
    private val onTaskSelected: (taskKey: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_task_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tasks = mapOf(
            R.id.taskMath       to "task_math",
            R.id.taskWalk       to "task_steps",
            R.id.taskShake      to "task_shake",
            R.id.taskScanQr     to "task_scanqr",
            R.id.taskTyping     to "task_typing",
            R.id.taskTap        to "task_tap",
            R.id.taskColorBalls to "task_color_balls"
        )

        tasks.forEach { (viewId, taskKey) ->
            view.findViewById<View>(viewId).setOnClickListener {
                onTaskSelected(taskKey)
                dismiss()
            }
        }
    }
}