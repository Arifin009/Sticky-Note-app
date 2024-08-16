package com.example.stickynotes
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.MenuInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener

import jp.wasabeef.richeditor.RichEditor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Edit_Text : AppCompatActivity(),ColorPickerDialogListener {

    private lateinit var editText: RichEditor
    private lateinit var buttonFontSize: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        setContentView(R.layout.activity_edit_text)
        supportActionBar?.hide()
        editText=findViewById(R.id.editTextNote)
        editText.setPlaceholder("type here....")
        editText.setPadding(30, 30, 30, 30)

        val note = intent.getStringExtra("note")
        editText.setHtml(note)

        findViewById<ImageButton>(R.id.boldButton).setOnClickListener { editText.setBold() }
       findViewById<ImageButton>(R.id.unnderlineBtn).setOnClickListener { editText.setUnderline() }
        findViewById<ImageButton>(R.id.italicBtn).setOnClickListener { editText.setItalic() }
        findViewById<ImageButton>(R.id.undoButton).setOnClickListener { editText.undo() }
 //       findViewById<ImageButton>(R.id.copyButton).setOnClickListener { editText.setCopy() }
//        findViewById<ImageButton>(R.id.cutButton).setOnClickListener { cutText() }


         buttonFontSize = findViewById<ImageButton>(R.id.textSizeInc)
        buttonFontSize.setOnClickListener {
            showFontSizeMenu()
        }
       findViewById<ImageButton>(R.id.colorButton).setOnClickListener { showColorPicker() }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {

            Log.d("HTMLContent", editText.html)
        if(intent.hasExtra("id")){
            updateToDatabase()
        }else{
            addToDatabase()
        }
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
                   }



    }

    private fun showColorPicker() {
        ColorPickerDialog.newBuilder()
            .setDialogType(ColorPickerDialog.TYPE_PRESETS)  // Dialog type (PRESETS or CUSTOM)
            .setAllowCustom(true)  // Allow custom colors
            .setShowAlphaSlider(true)  // Show alpha slider (transparency)
            .setDialogId(0)  // Dialog ID
            .show(this)  // Show dialog

    }
    override fun onColorSelected(dialogId: Int, color: Int) {
        //Log.d("color",color.toString())
        editText.setTextColor(color)
    }

    override fun onDialogDismissed(dialogId: Int) {

    }


    private fun updateToDatabase() {
       val id= intent.getStringExtra("id")
        val title = extractTitle(Html.fromHtml(editText.html).toString())
        if (editText.html.toString().isNotEmpty())
        {
            Database(this,null).apply{
                if (id != null) {
                    updateNoteById(id, title,editText.html.toString())
                }
            }
        }
        else
        {
            Database(this,null).apply{
                if (id != null) {
                    deleteNoteById(id)
                }
            }
        }

    }


    private fun addToDatabase()
            {
                val id = UUID.randomUUID().toString()
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, MMM dd yyyy"))
                val title = extractTitle(Html.fromHtml(editText.html).toString())
                Log.d("id",id)
                Log.d("title",title)
                Log.d("date",date.toString())
                Log.d("note",editText.html.toString())
                if (editText.html.toString().isNotEmpty()){
                    Database(this, null).apply {
                        addNote(id,title,date,editText.html.toString())
                    }
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

    override fun onBackPressed() {

        if(intent.hasExtra("id")){
            updateToDatabase()
        }else{
            addToDatabase()
        }
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
            super.onBackPressedDispatcher.onBackPressed()

    }


    private fun showFontSizeMenu() {
        val popupMenu = PopupMenu(this, buttonFontSize)
        val menuInflater: MenuInflater = popupMenu.menuInflater
        menuInflater.inflate(R.menu.font_size_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            val fontSize = when (item.itemId) {
                R.id.font_size_2-> 2
                R.id.font_size_5 -> 5
                R.id.font_size_10-> 10


                else -> 3
            }
            Log.d("size",fontSize.toString())
           editText.setFontSize(fontSize)
            true
        }

        popupMenu.show()
    }

}