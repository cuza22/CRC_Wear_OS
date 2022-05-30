package com.example.crc_wear_os

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

class MainActivity : Activity() {

    private val TAG = "MainActivity"

    private val transportationModes : ArrayList<String> = arrayListOf("still", "walking", "manual", "motorized", "crutches", "walker")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_wear)

        var wearableRecyclerView : WearableRecyclerView = findViewById(R.id.recycler_view)
        wearableRecyclerView.apply {
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(this@MainActivity, CustomScrollingLayoutCallback())
            adapter = MenuAdapter(context, transportationModes)
        }

    }

    private class MenuAdapter(val context: Context, dataList : ArrayList<String>) :
            RecyclerView.Adapter<MenuAdapter.ViewHolder>()  {

        var modeList : ArrayList<String> = dataList
        val iconList : ArrayList<Int> = arrayListOf(
            R.drawable.still_white,
            R.drawable.walking_white,
            R.drawable.manual_white,
            R.drawable.motorized_white,
            R.drawable.crutches_white,
            R.drawable.walker_white
        )

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val mode : TextView = itemView.findViewById(R.id.mode)
            val icon : ImageView = itemView.findViewById(R.id.icon)

            init {
                itemView.setOnClickListener {
                    val intent = Intent(context, Collecting::class.java).apply {
                        putExtra("mode", mode.text.toString())
                    }
                    context.startActivity(intent)
                    val intent_background = Intent(context, BackGroundCollecting::class.java)
                    context.startActivity(intent_background)
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: MenuAdapter.ViewHolder, position: Int) {
            holder.mode.text = modeList[position]
            holder.icon.setImageResource(iconList[position])
        }

        override fun getItemCount(): Int {
            return modeList.count()
        }

    }

    private class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

        private var progressToCenter : Float = 0f

        override fun onLayoutFinished(child: View, parent: RecyclerView) {
            child.apply {
                val centerOffset = height.toFloat() / 2.0f / parent.height.toFloat()
                val yRelativeToCenterOffset = y / parent.height + centerOffset

                progressToCenter = Math.abs(0.5f - yRelativeToCenterOffset)
                progressToCenter = Math.min(progressToCenter, 0.65f)

                scaleX = 1 - progressToCenter
                scaleY = 1 - progressToCenter
            }
        }

    }
}