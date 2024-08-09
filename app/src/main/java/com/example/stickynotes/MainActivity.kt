package com.example.stickynotes

import Database
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
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
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
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


            // Create an AlertDialog to edit the note
            val alert = AlertDialog.Builder(this)
            alert.setTitle("Edit Note")

            val input = EditText(this)
            input.setText(currentNote)
            alert.setView(input)

            alert.setPositiveButton("Save") { dialog, which ->
                val updatedNote = input.text.toString()

                // Update the note in the database
                db.updateNoteById(ids[position],extractTitle(updatedNote) , updatedNote)

                // Update the note in the RecyclerView
                notes[position] = updatedNote
                titles[position] = extractTitle(updatedNote)  // Update the title if necessary
                myAdapter.notifyItemChanged(position)

                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
            }

            alert.setNegativeButton("Cancel", null)
            alert.show()

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
            val alert =  AlertDialog.Builder(this);
            alert.setTitle("Edit")
            val input = EditText(this)
            alert.setView(input)

            alert.setPositiveButton("Save") { dialog, which ->
                val userInput = input.text.toString()



                val id = if (ids.isNotEmpty()) ids.last() + 1 else 1

                val date =formattedDate.toString()

                db.addNote(id, title.toString(),date,userInput)

                myAdapter.addItem(id, title.toString(),date,userInput)
                recyclerView.scrollToPosition(titles.size - 1)


            }
            alert.show()

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
}