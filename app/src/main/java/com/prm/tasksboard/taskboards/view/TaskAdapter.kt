package com.prm.tasksboard.taskboards.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.prm.tasksboard.R
import com.prm.tasksboard.taskboards.entity.TaskItem
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private var tasks: List<TaskItem>,
    private val onTaskFinished: (TaskItem) -> Unit,
    private val onTaskStatusChanged: (TaskItem) -> Unit,
    private val onEditTask: (TaskItem) -> Unit,
    private val onDeleteTask: (TaskItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //        private val checkBox = itemView.findViewById<CheckBox>(R.id.taskCheckBox)
        private val titleView = itemView.findViewById<TextView>(R.id.taskSummaryTextView)
        private val expandArrowImageView =
            itemView.findViewById<ImageView>(R.id.expandArrowImageView)
        private val taskDetailsLayout = itemView.findViewById<LinearLayout>(R.id.taskDetailsLayout)
        private val descriptionView = itemView.findViewById<TextView>(R.id.taskDescriptionTextView)
        private val dueDateView = itemView.findViewById<TextView>(R.id.taskDueDateTextView)
        private val priorityView = itemView.findViewById<TextView>(R.id.taskPriorityTextView)
        private val moreOptionsImageView =
            itemView.findViewById<ImageView>(R.id.moreOptionsImageView)

        init {
            moreOptionsImageView.setOnClickListener { v ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, tasks[adapterPosition]) // Pass the current task item
                }
            }
        }

        fun bind(task: TaskItem, onTaskStatusChanged: (TaskItem) -> Unit) {
//            checkBox.isChecked = task.status == "finished"
            titleView.text = task.title
            descriptionView.text =
                itemView.context.getString(R.string.task_description, task.description)
            dueDateView.text = itemView.context.getString(
                R.string.task_due_date,
                convertTimestampToString(task.dueDate)
            )
            priorityView.text = itemView.context.getString(R.string.task_priority, task.priority)

            /*checkBox.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) "finished" else "pending"
                if (task.status != newStatus) {
                    task.status = newStatus
                    onTaskStatusChanged(task)
                    Log.d("TaskStatusChange", "Task ID: ${task.taskId}, New Status: $newStatus")

                }
            }*/
            expandArrowImageView.setOnClickListener {
                if (taskDetailsLayout.visibility == View.GONE) {
                    taskDetailsLayout.visibility = View.VISIBLE
                    expandArrowImageView.setImageResource(R.drawable.ic_arrow_down)
                } else {
                    taskDetailsLayout.visibility = View.GONE
                    expandArrowImageView.setImageResource(R.drawable.ic_arrow_right)
                }
            }
        }

        private fun showPopupMenu(view: View, task: TaskItem) {
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.task_options_menu) // Inflate your menu resource
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_task -> {
                        onEditTask(task) // Call the lambda function for edit task
                        true
                    }

                    R.id.delete_task -> {
                        onDeleteTask(task)
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        private fun convertTimestampToString(timestamp: Timestamp): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(timestamp.toDate())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position], onTaskStatusChanged)
    }

    override fun getItemCount() = tasks.size
}
