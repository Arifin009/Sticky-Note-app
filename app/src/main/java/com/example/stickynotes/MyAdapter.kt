package com.example.yourapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stickynotes.R

class MyAdapter(
    private val ids: MutableList<String>,
    private val titles: MutableList<String>,
    private val subtitles: MutableList<String>,
    private val notes: MutableList<String>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit,

) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.itemTitle)
        val subtitleTextView: TextView = itemView.findViewById(R.id.itemDate)

        init {

            // Set a one-click listener on the itemView
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
            }
            // Set a long-click listener on the itemView
            itemView.setOnLongClickListener {
                onItemLongClick(adapterPosition)
                true
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.titleTextView.text = titles[position]
        holder.subtitleTextView.text = subtitles[position]
    }


    override fun getItemCount(): Int {
        return titles.size
    }

    // Method to add new items to the list
    fun addItem(id:String,title: String, subtitle: String,note:String) {
        ids.add(id)
        titles.add(title)
        subtitles.add(subtitle)
        notes.add(note)
        notifyItemInserted(titles.size - 1)
    }
    fun removeItem(position: Int) {

        ids.removeAt(position)
        titles.removeAt(position)
        subtitles.removeAt(position)
        notes.removeAt(position)
        notifyItemRemoved(position)
    }

}
