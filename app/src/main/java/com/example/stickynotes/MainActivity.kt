package com.example.stickynotes

import Database
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private var selectedPosition: Int = -1
    private val ids = mutableListOf<Int>()
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

        val titles = mutableListOf<String>()
        val dates = mutableListOf<String>()

        // Add  data
        val db = Database(this, null)
        val cursor = db.getName()

        // Extract data from cursor
        cursor?.let {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val date = it.getString(it.getColumnIndexOrThrow("date"))
                val note = it.getString(it.getColumnIndexOrThrow("note"))
                ids.add(id)
                titles.add(title)
                dates.add(date)
            }
            it.close()
        }


        myAdapter = MyAdapter(ids,titles, dates) { position ->
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
        }


        recyclerView.adapter = myAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val myDateObj = LocalDateTime.now();

        val myFormatObj = DateTimeFormatter.ofPattern("E, MMM dd yyyy");
        val formattedDate = myDateObj.format(myFormatObj);



        fab.setOnClickListener {

            val id = if (ids.isNotEmpty()) ids.last() + 1 else 1
            val title = "Bad Time"
            val date =formattedDate.toString()
            val note="This the bad time for me, one day you will be also realise" +
                    " that how I was love you in that time you will find me very " +
                    "badly and miss me very deeply but it will be the wrong moment "
            db.addNote(id,title,date,note)

            myAdapter.addItem(id,title,date)
            recyclerView.scrollToPosition(titles.size - 1)
        }
    }




    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}