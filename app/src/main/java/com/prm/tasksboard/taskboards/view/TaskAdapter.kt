package com.prm.tasksboard.taskboards.view

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prm.tasksboard.R
import com.prm.tasksboard.taskboards.entity.TaskItem

class TaskAdapter(private var tasks: List<TaskItem>, private val onTaskFinished: (TaskItem) -> Unit) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(task: TaskItem, onTaskFinished: (TaskItem) -> Unit, context: android.content.Context) {
            val titleView = itemView.findViewById<TextView>(R.id.taskTitle)
            val checkBox = itemView.findViewById<CheckBox>(R.id.taskCheckBox)

            titleView.text = task.title
            checkBox.setOnCheckedChangeListener(null) // Clear previous listeners
            checkBox.isChecked = task.status == "Finished"
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Update task status to "Finished" before deletion
                    task.status = "Finished"
                    onTaskFinished(task)
                }
            }

            itemView.setOnClickListener {
                val intent = Intent(context, TaskDetailActivity::class.java).apply {
                    putExtra("taskName", task.title)
                    putExtra("taskDescription", task.description)
                    putExtra("taskDueDate", task.dueDate)
                    putExtra("taskPriority", task.priority)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position], onTaskFinished, holder.itemView.context)
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<TaskItem>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}