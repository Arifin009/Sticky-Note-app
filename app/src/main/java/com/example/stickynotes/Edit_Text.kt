package com.example.stickynotes
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

import jp.wasabeef.richeditor.RichEditor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Edit_Text : AppCompatActivity() {

    private lateinit var editText: RichEditor
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
        findViewById<ImageButton>(R.id.italicBtn).setOnClickListener { toggleItalic(noteId) }
//        findViewById<ImageButton>(R.id.undoButton).setOnClickListener { undoAction() }
//        findViewById<ImageButton>(R.id.copyButton).setOnClickListener { copyText() }
//        findViewById<ImageButton>(R.id.cutButton).setOnClickListener { cutText() }
//        findViewById<ImageButton>(R.id.textSizeReduce).setOnClickListener {}
//        findViewById<ImageButton>(R.id.textSizeInc).setOnClickListener {}
//        findViewById<ImageButton>(R.id.colorButton).setOnClickListener { showColorPicker() }

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




}