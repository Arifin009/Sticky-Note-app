package com.example.stickynotes

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class Database(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    // below is the method for creating a database by a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        // below is a sqlite query, where column names
        // along with their data types is given
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " TEXT PRIMARY KEY, "
                + TITLE_COl + " TEXT,"
                + DATE_COL + " TEXT,"
                + NOTE_COL + " TEXT,"
                + BG_COL + " TEXT" +   ")")



        db.execSQL(query)

    }
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }


    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        // this method is to check if table already exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    // This method is for adding data in our database
    fun addNote(id: String,title : String, date : String, note : String, bg_color:String ){

        // below we are creating
        // a content values variable
        val values = ContentValues()

        // we are inserting our values
        // in the form of key-value pair
        values.put(ID_COL, id)
        values.put(TITLE_COl, title)
        values.put(DATE_COL, date)
        values.put(NOTE_COL, note)
        values.put(BG_COL, bg_color)

        // here we are creating a
        // writable variable of
        // our database as we want to
        // insert value in our database
        val db = this.writableDatabase

        // all values are inserted into database
        db.insert(TABLE_NAME, null, values)

        // at last we are
        // closing our database
        db.close()
    }

    fun deleteNoteById(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "id = ?", arrayOf(id)) // No need for id.toString() since it's already a String

    }


    fun updateNoteById(id: String, title: String, note: String,bg_color: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("note", note)
            put("bg_color", bg_color)
        }
        db.update("notes", values, "id = ?", arrayOf(id.toString()))
    }



    // below method is to get
    // all data from our database
    fun getName(): Cursor? {

        // here we are creating a readable
        // variable of our database
        // as we want to read value from it
        val db = this.readableDatabase

        // below code returns a cursor to
        // read data from the database
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null)

    }
        fun getColorById(id: String):String{
            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT $BG_COL FROM $TABLE_NAME WHERE $ID_COL = ?", arrayOf(id))
            var color = ""
            if (cursor.moveToFirst()) {
                color = cursor.getString(cursor.getColumnIndexOrThrow(BG_COL))
            }
            cursor.close()
            return color

        }
    companion object{
        // here we have defined variables for our database

        // below is variable for database name
        private val DATABASE_NAME = "Sticky notes"

        // below is the variable for database version
        private val DATABASE_VERSION = 1

        // below is the variable for table name
        val TABLE_NAME = "Notes"


        // below is the variable for name column
        val ID_COL = "id"
        val TITLE_COl = "title"
        val DATE_COL = "date"
        val NOTE_COL="note"
        val BG_COL="bg_color"
    }
}
