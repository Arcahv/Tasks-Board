package com.prm.tasksboard.taskboards.view

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.prm.tasksboard.R
import com.prm.tasksboard.taskboards.entity.BoardItem
import com.prm.tasksboard.taskboards.entity.TaskItem
import com.prm.tasksboard.taskboards.firestore.DatabaseHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskboardsActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var addBoardButton: Button
    private lateinit var menuButton: MaterialButton
    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val boardList = mutableListOf<BoardItem>()
    private val tasks = mutableListOf<TaskItem>()
    private val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val dbHandler = DatabaseHandler()
    private var currentBoardId: String? = null
    private var selectedDueDate = ""
    private var selectedPriority = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_taskboards)
        setWindowInsetsListener()
        tabLayout = findViewById(R.id.tabLayout)
        addBoardButton = findViewById(R.id.addBoardButton)
        menuButton = findViewById(R.id.menuButton)
        emptyView = findViewById(R.id.emptyView)
        recyclerView = findViewById(R.id.tasksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(tasks, { taskItem ->
            // This lambda is for handling task finish action, which might not be relevant to onTaskStatusChanged directly.
            // You might need to adjust this part based on your actual requirements.
        }, { taskItem ->
            // Assuming this lambda is for handling the status change of tasks.
            dbHandler.updateTaskItem(currentBoardId!!, taskItem.taskId, mapOf("status" to taskItem.status)) {
                // Handle database update success or failure
            }
        })
        recyclerView.adapter = taskAdapter

        dbHandler.checkFirestoreConnection()

        setTabLayoutListeners()
        updateEmptyViewVisibility()

        fetchAndDisplayBoards()

        addBoardButton.setOnClickListener {
            showAddTaskDialog()
        }

        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(this, menuButton)
            if (boardList.isEmpty()) {
                popupMenu.menuInflater.inflate(
                    R.menu.board_options_menu_without_delete,
                    popupMenu.menu
                )
            } else {
                popupMenu.menuInflater.inflate(R.menu.board_options_menu, popupMenu.menu)
            }
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add_board -> {
                        createNewBoard()
                        true
                    }

                    R.id.delete_board -> {
                        deleteBoard()
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun setWindowInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setTabLayoutListeners() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                currentBoardId = boardList[position].boardId
                displayTasks(currentBoardId!!)
                tab.view.setOnLongClickListener {
                    showRenameDialog(position)
                    true
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateEmptyViewVisibility() {
        emptyView.visibility = if (boardList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun createNewBoard() {
        // Create a new BoardItem
        val newBoard = BoardItem(
            createdAt = Timestamp.now(),
            name = "New Board #${boardList.size + 1}",
            updatedAt = Timestamp.now(),
            userId = loggedInUserId,
        )
        // Add it to your boardList
        boardList.add(newBoard)
        // Add it to Firestore
        dbHandler.addBoardItem(newBoard) { newBoardId ->
            boardList[boardList.size - 1].boardId = newBoardId
        }
        setupTabLayout()
        updateEmptyViewVisibility()
    }

    private fun deleteBoard() {
        currentBoardId?.let { boardId ->
            val currentItem = boardList.indexOfFirst { it.boardId == boardId }
            // Delete the board from firestore
            dbHandler.deleteBoardItem(boardId)
            // Remove the board from your boardList
            boardList.removeAt(currentItem)
            // Notify the adapter that the dataset has changed
            if (boardList.isEmpty()) {
                clearAndHideTaskList() // Clear and hide the task list
            } else {
                setupTabLayout()
                displayTasksForCurrentBoard()
            }
            updateEmptyViewVisibility()
        }
    }

    private fun clearAndHideTaskList() {
        // Find the RecyclerView and clear its adapter
        recyclerView.adapter = null
        // Optionally, update visibility or show a placeholder
        emptyView.visibility = View.VISIBLE
    }

    private fun showRenameDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rename Board")

        val input = EditText(this)
        input.setText(boardList[position].name)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newName = input.text.toString()
            boardList[position].name = newName
            tabLayout.getTabAt(position)?.text = newName
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun fetchAndDisplayBoards() {
        dbHandler.getBoardItemsByUserId().addOnSuccessListener { result ->
            boardList.clear() // Clear existing items if refreshing the entire list
            val newItems = result.map { document ->
                document.toObject(BoardItem::class.java)
            }
                .sortedBy { it.createdAt } // Sort by createdAt timestamp or change it to another attribute like name
            boardList.addAll(newItems)
            setupTabLayout()
            updateEmptyViewVisibility()
        }
    }

    private fun setupTabLayout() {
        tabLayout.removeAllTabs()
        boardList.forEach { board ->
            val tab = tabLayout.newTab().setText(board.name)
            tabLayout.addTab(tab)
        }
        if (boardList.isNotEmpty()) {
            currentBoardId = boardList[0].boardId
            displayTasksForCurrentBoard()
        }
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Task")

        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val taskNameInput = view.findViewById<EditText>(R.id.taskNameInput)
        val taskDescriptionInput = view.findViewById<EditText>(R.id.taskDescriptionInput)
        val dueDateTextView = view.findViewById<TextView>(R.id.dueDateTextView)
        val priorityTextView = view.findViewById<TextView>(R.id.priorityTextView)

        selectedDueDate = ""
        selectedPriority = ""

        dueDateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(this, { _, yearSelected, monthOfYear, dayOfMonth ->
                // Use yearSelected instead of year
                val selectedDate = Calendar.getInstance()
                selectedDate.set(yearSelected, monthOfYear, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDueDate = dateFormat.format(selectedDate.time)
                dueDateTextView.text = selectedDueDate
            }, year, month, day)

            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000 // Disallow past dates
            datePickerDialog.show()
        }

        priorityTextView.setOnClickListener {
            val popupMenu = PopupMenu(this, priorityTextView)
            popupMenu.menu.add("High")
            popupMenu.menu.add("Medium")
            popupMenu.menu.add("Low")
            popupMenu.setOnMenuItemClickListener { item ->
                selectedPriority = item.title.toString()
                priorityTextView.text = selectedPriority
                true
            }
            popupMenu.show()
        }

        builder.setView(view)

        builder.setPositiveButton("OK") { _, _ ->
            val taskName = taskNameInput.text.toString()
            val taskDescription = taskDescriptionInput.text.toString()
            addNewTask(taskName, taskDescription)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun addNewTask(taskName: String, taskDescription: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dueDate = if (selectedDueDate.isNotEmpty()) {
            sdf.parse(selectedDueDate)?.let { Timestamp(it) } ?: Timestamp.now()
        } else {
            Timestamp.now()
        }

        val newTask = TaskItem(
            title = taskName,
            description = taskDescription,
            dueDate = dueDate,
            priority = selectedPriority,
            createdAt = Timestamp.now()
        )

        currentBoardId?.let { boardId ->
            dbHandler.addTaskItem(newTask, boardId) {
                // This callback does not currently do anything. Assuming tasks is a list that holds TaskItems,
                // and taskAdapter is an adapter for a RecyclerView, you might want to:
                tasks.add(newTask) // Add the new task to the list
                taskAdapter.notifyItemInserted(tasks.size - 1) // Notify the adapter that an item has been added
            }
        }
    }

    private fun displayTasksForCurrentBoard() {
        currentBoardId?.let {
            displayTasks(it)
        }
    }

    private fun displayTasks(boardId: String) {
        dbHandler.getTasksByBoardId(boardId) { result ->
            tasks.clear() // Clear existing tasks
            tasks.addAll(result) // Add all fetched tasks
            taskAdapter.notifyDataSetChanged() // Notify adapter to refresh UI
        }
    }

    private fun onTaskFinished(taskItem: TaskItem) {
        // Update task status in Firestore
        val updatedFields = mapOf("status" to "finished")
        currentBoardId?.let { boardId ->
            dbHandler.updateTaskItem(boardId, taskItem.taskId, updatedFields) {
                // Optionally, update UI or remove task from list
                val taskIndex = tasks.indexOfFirst { it.taskId == taskItem.taskId }
                if (taskIndex != -1) {
                    tasks[taskIndex].status = "finished"
                    taskAdapter.notifyItemChanged(taskIndex) // Refresh UI to reflect changes
                }
            }
        }

        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Task Finished")
            .setMessage("Task ${taskItem.title} finished.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
