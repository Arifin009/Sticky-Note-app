package com.example.stickynotes

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Html
import android.util.Base64
import android.util.Log
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import jp.wasabeef.richeditor.RichEditor
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Stack
import java.util.UUID

class Edit_Text : AppCompatActivity(),ColorPickerDialogListener {

    private lateinit var editText: RichEditor

    private lateinit var frameLayout: FrameLayout
    private lateinit var imgFrame: LinearLayout
    private val undoStack = Stack<Int>()
    private val redoStack = Stack<Int>()


    private  val DEFAULT_COLOR = android.graphics.Color.WHITE
    private lateinit var bg_color: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        setContentView(R.layout.activity_edit_text)
        supportActionBar?.hide()
        editText=findViewById(R.id.editTextNote)
        frameLayout = findViewById<FrameLayout>(R.id.frameLayout)
        imgFrame = findViewById<LinearLayout>(R.id.imgthem)
        bg_color=DEFAULT_COLOR.toString()
        Database(this,null).apply{
           intent.getStringExtra("id")?.let { getColorById(it) }
           ?.let { applyColor(it.toInt())
           bg_color=it


           }
       }


        handaleDarkMode()

        editText.setPlaceholder("type here....")
        editText.setPadding(30, 30, 30, 30)


        val note = intent.getStringExtra("note")
        editText.setHtml(note)
        findViewById<ImageButton>(R.id.boldButton).setOnClickListener { editText.setBold() }
        findViewById<ImageButton>(R.id.unnderlineBtn).setOnClickListener { editText.setUnderline() }
        findViewById<ImageButton>(R.id.italicBtn).setOnClickListener { editText.setItalic() }
        findViewById<ImageButton>(R.id.bgButton).setOnClickListener { showColorPicker(0) }
        findViewById<ImageButton>(R.id.textSizeInc).setOnClickListener { showFontSizeMenu() }
        findViewById<ImageButton>(R.id.colorButton).setOnClickListener { showColorPicker(1) }
        findViewById<ImageButton>(R.id.highlightButton).setOnClickListener { showColorPicker(2) }
        findViewById<ImageButton>(R.id.align_left).setOnClickListener { editText.setAlignLeft() }
        findViewById<ImageButton>(R.id.align_middle).setOnClickListener { editText.setAlignCenter() }
        findViewById<ImageButton>(R.id.align_right).setOnClickListener { editText.setAlignRight() }
        findViewById<ImageButton>(R.id.order_list).setOnClickListener {editText.setNumbers() }
        findViewById<ImageButton>(R.id.unorder_list).setOnClickListener { editText.setBullets()  }

        findViewById<ImageButton>(R.id.redoButton).setOnClickListener {
            editText.redo()

            if (redoStack.isNotEmpty()) {
                // Store the current background color before changing
                if (undoStack.isEmpty() || undoStack.peek() != bg_color.toInt()) {
                    undoStack.push(bg_color.toInt())
                }

                val lastColor = redoStack.pop()
                applyColor(lastColor)
                bg_color = lastColor.toString()
            }
        }

        findViewById<ImageButton>(R.id.undoButton).setOnClickListener {
            editText.undo()
            if (undoStack.isNotEmpty()) {
                // Store the current background color before changing
                if (redoStack.isEmpty() || redoStack.peek() != bg_color.toInt()) {
                    redoStack.push(bg_color.toInt())
                }

                val lastColor = undoStack.pop()
                applyColor(lastColor)
                bg_color = lastColor.toString()
            }
        }




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



    private fun handaleDarkMode() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isDarkMode){
            findViewById<ImageButton>(R.id.boldButton).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.unnderlineBtn).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.italicBtn).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.textSizeInc).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.highlightButton).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.align_left).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.align_middle).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.align_right).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.order_list).setColorFilter(ContextCompat.getColor(this, R.color.white))
            findViewById<ImageButton>(R.id.unorder_list).setColorFilter(ContextCompat.getColor(this, R.color.white))

        }


    }

    private fun applyColor(color: Int) {

        editText.setBackgroundColor(color)

        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE // Set the shape (e.g., RECTANGLE, OVAL)
        gradientDrawable.setColor(color) // Set the background color
        gradientDrawable.cornerRadius = 20f // Set the corner radius
//            gradientDrawable.setStroke(2, Color.BLACK)
        frameLayout.background = gradientDrawable
        bg_color=color.toString()

    }

    private fun showColorPicker(dialogId: Int) {
        ColorPickerDialog.newBuilder()
            .setDialogType(ColorPickerDialog.TYPE_PRESETS)  // Dialog type (PRESETS or CUSTOM)
            .setAllowCustom(true)  // Allow custom colors
            .setShowAlphaSlider(true)  // Show alpha slider (transparency)
            .setDialogId(dialogId)  // Dialog ID
            .show(this)  // Show dialog

    }
    override fun onColorSelected(dialogId: Int, color: Int) {
        //Log.d("color",color.toString())
        if (dialogId==1)
        {
            editText.setTextColor(color)

        }
        else if (dialogId==0)
        {
            if (undoStack.isEmpty() || undoStack.peek() != color) {
                undoStack.push(bg_color.toInt())
            }
            applyColor(color)



        }
        else if (dialogId==2)
        {
            editText.setTextBackgroundColor(color)
        }

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
                    updateNoteById(id, title,editText.html.toString(),bg_color)
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
                        addNote(id,title,date,editText.html.toString(),bg_color)
                    }
                }

            }
    fun extractTitle(userInput: String): String {
        val sp = userInput.trim().split("\\s+".toRegex())

        return when {
            sp.size >= 2 -> "${sp[0]} ${sp[1]}"
            sp.isNotEmpty() -> sp[0]
            else -> "No title"
        }
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
        val popupMenu = PopupMenu(this, findViewById<ImageButton>(R.id.textSizeInc))
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