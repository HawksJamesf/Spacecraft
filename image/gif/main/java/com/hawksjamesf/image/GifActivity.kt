package com.hawksjamesf.image

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_gif.*

class GifActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif)
//        iv_gif.setSource("tenor.gif")
        iv_gif.setSource("tenor.gif",assets)

        bt_start.setOnClickListener {
            iv_gif.start()

        }

    }
}