import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stickynotes.R

class MyAdapter(
    private val ids: MutableList<String>,
    val titles: MutableList<String>,
    val subtitles: MutableList<String>,
    val notes: MutableList<String>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit,
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    // These lists will hold the original data and the filtered data
    private val originalIds = ids.toMutableList()
    private val originalTitles = titles.toMutableList()
    private val originalSubtitles = subtitles.toMutableList()
    private val originalNotes = notes.toMutableList()

    // The filtered lists
    private val filteredIds = mutableListOf<String>()
    private val filteredTitles = mutableListOf<String>()
    private val filteredSubtitles = mutableListOf<String>()
    private val filteredNotes = mutableListOf<String>()

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.itemTitle)
        val subtitleTextView: TextView = itemView.findViewById(R.id.itemDate)

        init {
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
            }
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

    fun addItem(id: String, title: String, subtitle: String, note: String) {
        ids.add(id)
        titles.add(title)
        subtitles.add(subtitle)
        notes.add(note)

        originalIds.add(id)
        originalTitles.add(title)
        originalSubtitles.add(subtitle)
        originalNotes.add(note)

        // Update the filtered lists based on current filter query
        if (filteredTitles.isEmpty() || originalTitles.contains(title)) {
            filter(" ") // Apply a dummy filter to update the filtered lists
        } else {
            notifyItemInserted(titles.size - 1)
        }
    }

    fun removeItem(position: Int) {
        if (position in ids.indices) {
            val itemId = ids[position]
            val originalIndex = originalIds.indexOf(itemId)

            if (originalIndex != -1) {
                originalIds.removeAt(originalIndex)
                originalTitles.removeAt(originalIndex)
                originalSubtitles.removeAt(originalIndex)
                originalNotes.removeAt(originalIndex)
            }

            ids.removeAt(position)
            titles.removeAt(position)
            subtitles.removeAt(position)
            notes.removeAt(position)

            // Update the filtered lists
            val filteredIndex = filteredIds.indexOf(itemId)
            if (filteredIndex != -1) {
                filteredIds.removeAt(filteredIndex)
                filteredTitles.removeAt(filteredIndex)
                filteredSubtitles.removeAt(filteredIndex)
                filteredNotes.removeAt(filteredIndex)
            }

            notifyItemRemoved(position)
        }
    }

    fun filter(query: String) {
        filteredTitles.clear()
        filteredSubtitles.clear()
        filteredIds.clear()
        filteredNotes.clear()

        if (query.isEmpty()) {
            filteredTitles.addAll(originalTitles)
            filteredSubtitles.addAll(originalSubtitles)
            filteredIds.addAll(originalIds)
            filteredNotes.addAll(originalNotes)
        } else {
            for (i in originalTitles.indices) {
                if (originalTitles[i].contains(query, ignoreCase = true)) {
                    filteredTitles.add(originalTitles[i])
                    filteredSubtitles.add(originalSubtitles[i])
                    filteredIds.add(originalIds[i])
                    filteredNotes.add(originalNotes[i])
                }
            }
        }

        titles.clear()
        titles.addAll(filteredTitles)

        subtitles.clear()
        subtitles.addAll(filteredSubtitles)

        ids.clear()
        ids.addAll(filteredIds)

        notes.clear()
        notes.addAll(filteredNotes)

        notifyDataSetChanged()
    }
}
