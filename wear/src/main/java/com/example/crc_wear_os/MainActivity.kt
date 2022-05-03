package com.example.crc_wear_os

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.example.crc_wear_os.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var wearableRecyclerView : WearableRecyclerView = findViewById(R.id.recycler_view)
        wearableRecyclerView.apply {
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(this@MainActivity)

            val transportationModes : ArrayList<MenuItem> = listOfNotNull(
                MenuItem("still"),
                MenuItem("walking"),
                MenuItem("manual"),
                MenuItem("motorized"),
                MenuItem("walker"),
                MenuItem("crutches")
            ) as ArrayList<MenuItem>

            adapter = MenuAdapter() {
                public override fun onItemClicked(position: Int) {
                    switch (position) P
                            case 0:
                }
            }
        }

    }


}

private class MenuAdapter() : Adapter<MenuAdapter.ViewHolder>() {
    var list : ArrayList<MenuItem> = ArrayList()

    public AdapterCallback :
    interface  {
        fun onItemClicked
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItem {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

}

private class MenuItem(mode : String) {
    val transportationModes = hashMapOf(
        "still" to R.drawable.still,
        "walking" to R.drawable.walking,
        "manual" to R.drawable.manual,
        "motorized" to R.drawable.motorized,
        "walker" to R.drawable.walker,
        "crutches" to R.drawable.crutches
    )
    val icon = transportationModes[mode]!!

}