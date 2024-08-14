package com.example.stickynotes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stickynotes.databinding.ActivityMainBinding
import com.example.yourapp.MyAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Stack
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter
    private lateinit var fab: FloatingActionButton
    private lateinit var editText: EditText

    private val ids = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val dates = mutableListOf<String>()
    private val notes = mutableListOf<String>()
    private val undoStack = Stack<CharSequence>()
    private val formattingList = mutableListOf<TextFormat>()

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
            Log.d("position",ids[position].toString())
            // Use the function to show the edit dialog
            showEditNoteDialog(currentNote,ids[position]) { updatedNote,formatting ->
                val updatedTitle = extractTitle(updatedNote)

                // Update the note in the database
                db.updateNoteById(ids[position], updatedTitle, updatedNote)

                for (format in formatting) {
                    db.addTextFormat(ids[position], format)
                }
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

            openNewNoteDialog() { updatedNote,formatting ->

                val title = extractTitle(updatedNote)
                val id: String = UUID.randomUUID().toString()
               // val id = if (ids.isNotEmpty()) ids.last() + 1 else 1

                val date = formattedDate.toString()

                db.addNote(id, title, date, updatedNote)
                for (format in formatting) {
                    db.addTextFormat(id, format)
                }

                myAdapter.addItem(id, title, date, updatedNote)
                recyclerView.scrollToPosition(titles.size - 1)
            }


        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun getAndAddNotes() {
        val db = Database(this, null)
        val cursor = db.getName()
        cursor?.let {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndexOrThrow("id"))
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
        val sp = userInput.split(" ").filter { it.isNotEmpty() }

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
        noteId: String,
        onNoteUpdated: (updatedNote: String, formatting: List<TextFormat>) -> Unit
    ) {
        formattingList.clear()
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Edit Note")

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_text_templete, null)
        val db = Database(this, null)
        // Find the EditText and buttons from the custom layout

        val boldButton = dialogView.findViewById<ImageButton>(R.id.boldButton)
        editText = dialogView.findViewById(R.id.editTextNote)
        val textSizeReduceButton = dialogView.findViewById<ImageButton>(R.id.textSizeReduce)
        val textSizeIncButton = dialogView.findViewById<ImageButton>(R.id.textSizeInc)
        val colorButton = dialogView.findViewById<ImageButton>(R.id.colorButton)
        val copyButton = dialogView.findViewById<ImageButton>(R.id.copyButton)
        val cutButton = dialogView.findViewById<ImageButton>(R.id.cutButton)
        val undoButton = dialogView.findViewById<ImageButton>(R.id.undoButton)
        val unnderlineBtn = dialogView.findViewById<ImageButton>(R.id.unnderlineBtn)
        val italicBtn = dialogView.findViewById<ImageButton>(R.id.italicBtn)

        // Set the initial text of EditText
        editText.setText(currentNote)
        val spannableString = SpannableStringBuilder(editText.text)
        // Set the format text of EditText
        val dbFormatList = db.getTextFormats(noteId)
        for (format in dbFormatList) {
            Log.d("format",format.styleName+ format.start+format.end).toString()
            when (format.styleName) {

                "BOLD" -> {

                    spannableString.setSpan(
                        StyleSpan(Typeface.BOLD),
                        format.start,
                        format.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editText.setText(spannableString)
                }
                "ITALIC" -> {
                    spannableString.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        format.start,
                        format.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editText.setText(spannableString)
                }
                "COLOR" -> {
                    spannableString.setSpan(
                        ForegroundColorSpan(format.Ext_info.toInt()),
                        format.start,
                        format.end,

                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editText.setText(spannableString)

                }
                "SIZE" -> {
                    spannableString.setSpan(
                        RelativeSizeSpan(format.Ext_info.toFloat()), // 1.5f means 150% of the original size
                        format.start,
                        format.end,
                        // end index of the span
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE // span mode
                    )
                }
                "UNDER" -> {
                    spannableString.setSpan(
                        UnderlineSpan(),
                        format.start,
                        format.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editText.setText(spannableString)// span mode

                }
                // Handle other styles like Italic, Underline, etc.
            }
        }

        disableActionMode()
        // Set up button click listeners
        boldButton.setOnClickListener {
            buttonBold(it)
        }
        unnderlineBtn.setOnClickListener{
         buttonUnderline(it)
        }
        italicBtn.setOnClickListener{
            buttonItalics(it)
        }
        undoButton.setOnClickListener{
            undoAction()
        }
        copyButton.setOnClickListener{
            val start = editText.selectionStart
            val end = editText.selectionEnd

            if (start != end) {
                copySelectedText()
            }
            else{
                copyAllText()
            }

        }
        cutButton.setOnClickListener{
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if (start != end) {
                cutSelectedText()
            }
            else{
                cutAllText()
            }
        }
        textSizeReduceButton.setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if(start!=end){
                selectableTextReduce()
            }
            else{
                nonSlectableTexReduce()
            }

        }

        textSizeIncButton.setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if(start!=end){
                selectableTextInc()
            }
            else{
                nonSlectableTexInc()
            }
        }
        colorButton.setOnClickListener {
            showColorPicker()
        }

        // Set the custom layout to AlertDialog
        alert.setView(dialogView)

        alert.setPositiveButton("Save") { dialog, which ->
            val updatedNote = editText.text.toString()
            onNoteUpdated(updatedNote, formattingList)   // Return the updated note through the callback
        }

        alert.setNegativeButton("Cancel", null)
        alert.show()
    }

    fun openNewNoteDialog(

        onNoteUpdated: (updatedNote: String, formatting: List<TextFormat>) -> Unit
    ) {
        formattingList.clear()
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Edit Note")

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_text_templete, null)
        val db = Database(this, null)
        // Find the EditText and buttons from the custom layout

        val boldButton = dialogView.findViewById<ImageButton>(R.id.boldButton)
        editText = dialogView.findViewById(R.id.editTextNote)
        val textSizeReduceButton = dialogView.findViewById<ImageButton>(R.id.textSizeReduce)
        val textSizeIncButton = dialogView.findViewById<ImageButton>(R.id.textSizeInc)
        val colorButton = dialogView.findViewById<ImageButton>(R.id.colorButton)
        val copyButton = dialogView.findViewById<ImageButton>(R.id.copyButton)
        val cutButton = dialogView.findViewById<ImageButton>(R.id.cutButton)
        val undoButton = dialogView.findViewById<ImageButton>(R.id.undoButton)
        val unnderlineBtn = dialogView.findViewById<ImageButton>(R.id.unnderlineBtn)
        val italicBtn = dialogView.findViewById<ImageButton>(R.id.italicBtn)

        // Set the initial text of EditText
        //editText.setText("")


        disableActionMode()
        // Set up button click listeners
        boldButton.setOnClickListener {
            buttonBold(it)
        }
        unnderlineBtn.setOnClickListener{
            buttonUnderline(it)
        }
        italicBtn.setOnClickListener{
            buttonItalics(it)
        }
        undoButton.setOnClickListener{
            undoAction()
        }
        copyButton.setOnClickListener{
            val start = editText.selectionStart
            val end = editText.selectionEnd

            if (start != end) {
                copySelectedText()
            }
            else{
                copyAllText()
            }

        }
        cutButton.setOnClickListener{
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if (start != end) {
                cutSelectedText()
            }
            else{
                cutAllText()
            }
        }
        textSizeReduceButton.setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if(start!=end){
                selectableTextReduce()
            }
            else{
                nonSlectableTexReduce()
            }

        }

        textSizeIncButton.setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if(start!=end){
                selectableTextInc()
            }
            else{
                nonSlectableTexInc()
            }
        }
        colorButton.setOnClickListener {
            showColorPicker()
        }

        // Set the custom layout to AlertDialog
        alert.setView(dialogView)

        alert.setPositiveButton("Save") { dialog, which ->
            val updatedNote = editText.text.toString()
            onNoteUpdated(updatedNote, formattingList)   // Return the updated note through the callback
        }

        alert.setNegativeButton("Cancel", null)
        alert.show()
    }

    fun buttonBold(view: View) {

        saveState()
        val start = editText.selectionStart
        val end = editText.selectionEnd


        if (start != end) {
            val spannableString = SpannableStringBuilder(editText.text)
            val styleSpans = spannableString.getSpans(start, end, StyleSpan::class.java)
            val isBold = styleSpans.any { it.style == Typeface.BOLD }

            if (isBold) {
                // If the text is already bold, remove bold formatting
                styleSpans.forEach { spannableString.removeSpan(it) }
            } else {
                formattingList.add(TextFormat("BOLD", start, end,""))
                // Apply bold formatting
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            editText.setText(spannableString)
            editText.setSelection(start, end) // Preserve the selection
        }
    }
fun disableActionMode()
{
    editText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: android.view.ActionMode, menu: Menu): Boolean {
            return false // Prevents the action mode from being created
        }

        override fun onPrepareActionMode(mode: android.view.ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: android.view.ActionMode, item: MenuItem): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: android.view.ActionMode) {
            // Nothing to do
        }
    }
}
    fun buttonItalics(view: View) {

        formattingList.add(TextFormat("ITALIC", editText.selectionStart, editText.selectionEnd,""))
        Log.d("italicformat", (editText.selectionEnd).toString())
        saveState()
        val spannableString = SpannableStringBuilder(editText.text)
        spannableString.setSpan(
            StyleSpan(Typeface.ITALIC),
            editText.selectionStart,
            editText.selectionEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE

        )
        editText.setText(spannableString)

    }

    // Method to apply underline style
    fun buttonUnderline(view: View) {
        //val db = Database(this, null)
        saveState()
        val spannableString = SpannableStringBuilder(editText.text)
        formattingList.add(TextFormat("UNDER", editText.selectionStart, editText.selectionEnd,""))

        spannableString.setSpan(
            UnderlineSpan(),
            editText.selectionStart,
            editText.selectionEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        editText.setText(spannableString)

    }

    // Method to remove all formatting
    fun buttonNoFormat(view: View) {
        val stringText = editText.text.toString()
        editText.setText(stringText)
    }

    // Method to align text to the left
    fun buttonAlignmentLeft(view: View) {
        editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        val spannableString = SpannableStringBuilder(editText.text)
        editText.setText(spannableString)
    }

    // Method to align text to the center
    fun buttonAlignmentCenter(view: View) {
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val spannableString = SpannableStringBuilder(editText.text)
        editText.setText(spannableString)
    }

    // Method to align text to the right
    fun buttonAlignmentRight(view: View) {
        editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        val spannableString = SpannableStringBuilder(editText.text)
        editText.setText(spannableString)
    }
    fun selectableTextReduce():String{
        saveState()
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val currentSize = getCurrentTextSize(start, end)
        val newSize = (currentSize - 2f).coerceAtLeast(8f)
       formattingList.add(TextFormat("SIZE", start, end,newSize.toString()))
        applyTextSizeSpan(start, end, newSize)
        return newSize.toString()

    }
    fun selectableTextInc(){
        saveState()
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val currentSize = getCurrentTextSize(start, end)
        val newSize = (currentSize + 2f).coerceAtMost(72f)
        formattingList.add(TextFormat("SIZE", start, end,newSize.toString()))
        applyTextSizeSpan(start, end, newSize)


    }
    fun nonSlectableTexInc()
    {
        saveState()
        var currentTextSize = editText.textSize
        currentTextSize = (currentTextSize + 2f).coerceAtMost(72f) // Increase text size
        editText.textSize =
            currentTextSize / resources.displayMetrics.scaledDensity
    }
    fun nonSlectableTexReduce()
    {
        saveState()
        var currentTextSize = editText.textSize
        currentTextSize = (currentTextSize - 2f).coerceAtLeast(8f) // Decrease text size
        editText.textSize =
            currentTextSize / resources.displayMetrics.scaledDensity
    }
    private fun getCurrentTextSize(start: Int, end: Int): Float {
        val spannableString = editText.text as Spannable
        val spans = spannableString.getSpans(start, end, android.text.style.RelativeSizeSpan::class.java)
        return if (spans.isNotEmpty()) {
            // If there are spans applied, return the first one
            val span = spans[0]
            val sizeRatio = span.sizeChange
            editText.textSize / resources.displayMetrics.scaledDensity * sizeRatio
        } else {
            // If no spans, return the default text size
            editText.textSize / resources.displayMetrics.scaledDensity
        }
    }

    private fun applyTextSizeSpan(start: Int, end: Int, newSize: Float) {
        val spannableString = SpannableStringBuilder(editText.text)
        // Remove previous size spans
        val spans = spannableString.getSpans(start, end, android.text.style.RelativeSizeSpan::class.java)
        spans.forEach { spannableString.removeSpan(it) }

        // Apply new size span
        spannableString.setSpan(
            android.text.style.RelativeSizeSpan(newSize / (editText.textSize / resources.displayMetrics.scaledDensity)),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editText.setText(spannableString)
        editText.setSelection(start, end)
    }
    private fun showColorPicker() {
        val colors = arrayOf(
            "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta",
            "Black", "White", "Gray", "Light Gray", "Dark Gray", "Purple",
            "Orange", "Brown", "Pink", "Light Blue"
        )

        val colorValues = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.BLACK, Color.WHITE, Color.GRAY, Color.LTGRAY, Color.DKGRAY, Color.rgb(128, 0, 128),
            Color.rgb(255, 165, 0), Color.rgb(165, 42, 42), Color.rgb(255, 192, 203), Color.rgb(173, 216, 230)
        )

        val colorPickerDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Choose a color")
            .setItems(colors) { _, which ->
                val selectedColor = colorValues[which]
                //changeBackgroundColor(selectedColor)

                val start = editText.selectionStart
                val end = editText.selectionEnd

                if(start!=end){
                    formattingList.add(TextFormat("COLOR", start, end,selectedColor.toString()))
                    selectableColorSet(selectedColor)
                }
                else{
                    changeTextColor(selectedColor)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        colorPickerDialog.show()
    }
        private fun selectableColorSet(color: Int)
        {
            saveState()
            val start = editText.selectionStart
            val end = editText.selectionEnd

            val spannableString = SpannableStringBuilder(editText.text)
            val styleSpans = spannableString.getSpans(start, end, StyleSpan::class.java)
            styleSpans.forEach { spannableString.removeSpan(it) }

                // Apply bold formatting
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    start,
                    end,

                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )


            editText.setText(spannableString)
            editText.setSelection(start, end)
        }
    private fun changeBackgroundColor(color: Int) {
        // Assuming you want to change the background color of an EditText

        editText.setBackgroundColor(color)

        // If you want to change the background color of a different view, replace `editText` with your view
        // For example, if changing a LinearLayout background color:
        // val layout: LinearLayout = findViewById(R.id.your_linear_layout_id)
        // layout.setBackgroundColor(color)

        // If you want to show a toast to confirm the color change:
        Toast.makeText(this, "Background color changed", Toast.LENGTH_SHORT).show()
    }
    private fun changeTextColor(color: Int) {
        // Assuming you want to change the text color of an EditText
       // Replace with your actual EditText ID
        saveState()
        editText.setTextColor(color)

        // If you want to change the text color of a different view, replace `editText` with your view
        // For example, if changing a TextView color:
        // val textView: TextView = findViewById(R.id.your_text_view_id)
        // textView.setTextColor(color)

        // If you want to show a toast to confirm the color change:
        Toast.makeText(this, "Text color changed", Toast.LENGTH_SHORT).show()
    }
    private fun copySelectedText() {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start != end) {
            val selectedText = editText.text.substring(start, end)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", selectedText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show()
        }
    }
    private fun cutSelectedText() {
        saveState()
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start != end) {
            val selectedText = editText.text.substring(start, end)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Cut Text", selectedText)
            clipboard.setPrimaryClip(clip)

            // Remove the selected text
            editText.text.delete(start, end)
            Toast.makeText(this, "Text cut to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
    private fun copyAllText() {
        val text = editText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "All text copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No text to copy", Toast.LENGTH_SHORT).show()
        }
    }
    private fun cutAllText() {
        saveState()
        val text = editText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Cut Text", text)
            clipboard.setPrimaryClip(clip)

            // Clear all the text
            editText.text.clear()
            Toast.makeText(this, "All text cut to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No text to cut", Toast.LENGTH_SHORT).show()
        }
    }

    private fun undoAction() {
        if (undoStack.isNotEmpty()) {
            editText.setText(undoStack.pop())
            editText.setSelection(editText.text.length)
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveState() {
        undoStack.push(editText.text.toString())
    }

}