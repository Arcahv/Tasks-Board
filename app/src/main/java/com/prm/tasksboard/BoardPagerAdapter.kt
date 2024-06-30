package com.prm.tasksboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class BoardPagerAdapter(private val boardList: MutableList<BoardItem>) : RecyclerView.Adapter<BoardPagerAdapter.BoardViewHolder>() {

    class BoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TODO: Define the view components of each item here
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.board_item, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        // TODO: Bind the data to the view components of each item here
    }

    override fun getItemCount(): Int {
        return boardList.size
    }
}