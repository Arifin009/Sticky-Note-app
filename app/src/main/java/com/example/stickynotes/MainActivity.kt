package com.example.stickynotes


import MyAdapter
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color

import android.os.Bundle
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.SearchView
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stickynotes.databinding.ActivityMainBinding

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView



class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var getResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter
    private lateinit var fab: FloatingActionButton
    private val ids = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val dates = mutableListOf<String>()
    private val notes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CheckDarkMode()
        handleDarkMode()
        setupUI()
        setupFab()
        loadData()
        getTextResult()
        setupRecyclerView()
        setupSearchView()

    }

    private fun CheckDarkMode() {
        setSupportActionBar(binding.appBarMain.toolbar)
        val navView: NavigationView = binding.navView
        val menuItem = navView.menu.findItem(R.id.nav_radios)
        val switchButton = menuItem.actionView?.findViewById<Switch>(R.id.switch_toggle)
        val sharedPreferences = getSharedPreferences("Setting", MODE_PRIVATE)

        if (sharedPreferences.getBoolean("isDarkMode", false))
        {
            if (switchButton != null) {
                switchButton.setChecked(true)
            }
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            delegate.applyDayNight()
        }
    }

    private fun setupFab() {
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, Edit_Text::class.java)
            getResultLauncher.launch(intent)
        }
    }


    private fun setupUI() {
        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow), drawerLayout

        )

        // Set up ActionBar with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up NavigationView with NavController
        navView.setupWithNavController(navController)

        // Add ActionBarDrawerToggle to handle drawer open/close with hamburger button
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        // Add DrawerListener to DrawerLayout
        drawerLayout.addDrawerListener(toggle)

        // Sync the toggle state
        toggle.syncState()
        // Access the RadioButton directly and set up listener

    }
    fun handleDarkMode()
    {
        setSupportActionBar(binding.appBarMain.toolbar)
        val navView: NavigationView = binding.navView
        val menuItem = navView.menu.findItem(R.id.nav_radios)
        val switchButton = menuItem.actionView?.findViewById<Switch>(R.id.switch_toggle)
        val SharedPreferences = getSharedPreferences("Setting", MODE_PRIVATE)
        switchButton?.setOnClickListener {
            if (switchButton.isChecked) {
                // Handle radio button checked state
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                delegate.applyDayNight()
                SharedPreferences.edit().putBoolean("isDarkMode", true).apply()


            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                delegate.applyDayNight()
                SharedPreferences.edit().putBoolean("isDarkMode", false).apply()
            }
        }
    }

private fun getTextResult()
{
    getResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result here
            updateRecyclerView()
            val data: Intent? = result.data

        }
    }
}
    private fun loadData() {
        val db = Database(this, null)
        val cursor = db.getName()
        cursor?.use {
            while (it.moveToNext()) {
                ids.add(it.getString(it.getColumnIndexOrThrow("id")))
                titles.add(it.getString(it.getColumnIndexOrThrow("title")))
                dates.add(it.getString(it.getColumnIndexOrThrow("date")))
                notes.add(it.getString(it.getColumnIndexOrThrow("note")))
            }
        }
    }
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        myAdapter = MyAdapter(ids, titles, dates, notes,
            onItemClick = { position ->
                // Handle item click
                val title = myAdapter.titles[position]
                val subtitle = myAdapter.subtitles[position]
                val note = myAdapter.notes[position]
                Intent(this, Edit_Text::class.java).apply {
                    putExtra("id", ids[position])
                    putExtra("title", title)
                    putExtra("date", subtitle)
                    putExtra("note", note)
                    getResultLauncher.launch(this)
                }

                // Do something with the item
            },
            onItemLongClick = { position ->
                onNoteDelete(position)
            }
        )
        recyclerView.adapter = myAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    private fun onNoteDelete(position: Int) {
        AlertDialog.Builder(this).apply {
            setTitle("Delete Note")
            setMessage("Are you sure you want to delete this note?")
            setPositiveButton("Yes") { _, _ ->
                Database(this@MainActivity, null).deleteNoteById(ids[position])
                myAdapter.removeItem(position)
                Toast.makeText(this@MainActivity, "Note deleted", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("No", null)
        }.create().show()
    }
    private fun updateRecyclerView() {
        // Clear existing data
        ids.clear()
        titles.clear()
        dates.clear()
        notes.clear()

        // Load updated data from the database
        loadData()

        // Notify the adapter of data changes
        myAdapter.notifyDataSetChanged()
    }
    private fun setupSearchView() {
        val searchView: androidx.appcompat.widget.SearchView = findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: Handle the search action when the user submits the query
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the list as the user types in the search box
                myAdapter.filter(newText.orEmpty())
                return true
            }
        })
    }




}
