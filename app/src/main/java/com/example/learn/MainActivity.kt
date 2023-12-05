package com.example.learn

import android.content.Context
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
//
//            val fontDir = this@MainActivity.resources.getResourceEntryName(R.font.)
//            val fontList = context.assets.list(fontDir)
//
//            val res = isFontFileExists(this@MainActivity, "gilroy_black.otf")
//            Snackbar.make(view, "${fontFiles?.size}", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()

        }
    }

    fun isFontFileExists(context: Context, fontFileName: String): Boolean {
        val assetManager = context.assets
        val fontFilePath = "font/$fontFileName"


        return try {
            val inputStream = assetManager.open(fontFilePath)
            inputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }


}