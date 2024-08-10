package com.example.stickynotes

import Database
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stickynotes.databinding.ActivityMainBinding
import com.example.yourapp.MyAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.sql.Date
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter
    private lateinit var fab: FloatingActionButton

    private val ids = mutableListOf<Int>()
    private val titles = mutableListOf<String>()
    private val dates = mutableListOf<String>()
    private val notes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)






        // Extract data from cursor
        getAndAddNotes()


        val db = Database(this, null)


        myAdapter = MyAdapter(ids, titles, dates, notes, { position ->
            val currentNote = notes[position]

            // Use the function to show the edit dialog
            showEditNoteDialog(currentNote) { updatedNote ->
                val updatedTitle = extractTitle(updatedNote)

                // Update the note in the database
                db.updateNoteById(ids[position], updatedTitle, updatedNote)

                // Update the note in the RecyclerView
                notes[position] = updatedNote
                titles[position] = updatedTitle
                myAdapter.notifyItemChanged(position)

                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
            }
        }, { position ->
            // Long press detected, confirm deletion
            val posToDelete = position

            // Show confirmation dialog
            AlertDialog.Builder(this).apply {
                setTitle("Delete Note")
                setMessage("Are you sure you want to delete this note?")
                setPositiveButton("Yes") { _, _ ->
                    // Remove from database
                    db.deleteNoteById(ids[posToDelete])

                    // Remove from RecyclerView
                    myAdapter.removeItem(position)

                    Toast.makeText(this@MainActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("No", null)
            }.create().show()
        })


        recyclerView.adapter = myAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val myDateObj = LocalDateTime.now();

        val myFormatObj = DateTimeFormatter.ofPattern("E, MMM dd yyyy");
        val formattedDate = myDateObj.format(myFormatObj);



        fab.setOnClickListener {

            showEditNoteDialog(""){ updatedNote ->

                val title=extractTitle(updatedNote)
                val id = if (ids.isNotEmpty()) ids.last() + 1 else 1

                val date =formattedDate.toString()

                db.addNote(id, title,date,updatedNote)

                myAdapter.addItem(id, title,date,updatedNote)
                recyclerView.scrollToPosition(titles.size - 1)
            }







        }
    }




    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun getAndAddNotes(){
        val db = Database(this, null)
        val cursor = db.getName()
        cursor?.let {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val date = it.getString(it.getColumnIndexOrThrow("date"))
                val note = it.getString(it.getColumnIndexOrThrow("note"))
                ids.add(id)
                titles.add(title)
                dates.add(date)
                notes.add(note)
            }
            it.close()
        }
    }
    fun extractTitle(userInput: String): String {
        val sp=userInput.split(" ").filter { it.isNotEmpty() }

        val title = when {
            sp.size >= 3 -> sp[0] + " " + sp[1] + " " + sp[2]
            sp.size == 2 -> sp[0] + " " + sp[1]
            sp.isNotEmpty() -> sp[0]
            else -> "No title"
        }
        return title
    }
    fun showEditNoteDialog(
        currentNote: String,
        onNoteUpdated: (updatedNote: String) -> Unit
    ) {
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Edit Note")

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_text_templete, null)

        // Find the EditText and buttons from the custom layout
        val editText = dialogView.findViewById<EditText>(R.id.editTextNote)
        val boldButton = dialogView.findViewById<ImageButton>(R.id.boldButton)
        val textSizeReduceButton = dialogView.findViewById<ImageButton>(R.id.textSizeReduce)
        val textSizeIncButton = dialogView.findViewById<ImageButton>(R.id.textSizeInc)
        val colorButton = dialogView.findViewById<ImageButton>(R.id.colorButton)

        // Set the initial text of EditText
        editText.setText(currentNote)

        // Set up button click listeners
        boldButton.setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd

            if (start != end) {
                val spannableString = SpannableString(editText.text)
                val existingSpans = spannableString.getSpans(start, end, StyleSpan::class.java)
                val isBold = existingSpans.any { it.style == Typeface.BOLD }

                if (isBold) {
                    // Remove bold style if already applied
                    spannableString.removeSpan(existingSpans.find { it.style == Typeface.BOLD })
                } else {
                    // Apply bold style
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                // Convert SpannableString to Editable and update EditText
                val editableText = Editable.Factory.getInstance().newEditable(spannableString)
                editText.text = editableText

                // Move cursor to end of selection
                editText.setSelection(end)
            } else {
                Toast.makeText(this, "Select text to apply bold", Toast.LENGTH_SHORT).show()
            }
        }

        var currentTextSize = editText.textSize
        textSizeReduceButton.setOnClickListener {
            currentTextSize = (currentTextSize - 2f).coerceAtLeast(8f) // Decrease text size
            editText.textSize = currentTextSize / resources.displayMetrics.scaledDensity // Convert pixels to sp
        }

        textSizeIncButton.setOnClickListener {
            currentTextSize = (currentTextSize + 2f).coerceAtMost(72f) // Increase text size
            editText.textSize = currentTextSize / resources.displayMetrics.scaledDensity // Convert pixels to sp
        }
        colorButton.setOnClickListener {
            val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
            AlertDialog.Builder(this).apply {
                setTitle("Pick a color")
                setItems(colors.map { "Color" }.toTypedArray()) { _, which ->
                    editText.setTextColor(colors[which])
                }
                create().show()
            }
        }

        // Set the custom layout to AlertDialog
        alert.setView(dialogView)

        alert.setPositiveButton("Save") { dialog, which ->
            val updatedNote = editText.text.toString()
            onNoteUpdated(updatedNote)  // Return the updated note through the callback
        }

        alert.setNegativeButton("Cancel", null)
        alert.show()
    }


}