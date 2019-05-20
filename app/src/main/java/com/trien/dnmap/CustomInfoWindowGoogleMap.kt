package com.trien.dnmap

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker

// custom adapter for marker's info windows
class CustomInfoWindowGoogleMap(private val context: Context) : InfoWindowAdapter {

    // put the implementation inside this method if we want to keep the default layout and overlay our custom layout on top (white padding drawn around)
    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    // put the implementation inside this method if we want to use the completely layout of our own
    override fun getInfoWindow(marker: Marker): View {
        val view = (context as Activity).layoutInflater
                .inflate(R.layout.info_window, null)

        val container = view.findViewById<LinearLayout>(R.id.container)
        val text = view.findViewById<TextView>(R.id.textView)
        val img = view.findViewById<ImageView>(R.id.imageView)
        val arrowDown = view.findViewById<ImageView>(R.id.arrowDown)

        // get and set appropriate text content from snippets
        val snippet = marker.snippet
        text.text = snippet

        // depending on the content of snippet, we set text color, frame color and image resources accordingly
        if (snippet == context.getString(R.string.snippet_zone1)) {
            img.setImageResource(R.drawable.ic_info_blue)
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineBlue))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_blue)
            container.setBackgroundResource(R.drawable.rectangle_blue_light)
        } else if (snippet == context.getString(R.string.snippet_zone2)) {
            img.setImageResource(R.drawable.ic_info_orange)
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineOrange))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_orange)
            container.setBackgroundResource(R.drawable.rectangle_orange)
        } else if (snippet == context.getString(R.string.snippet_zone3)) {
            img.setImageResource(R.drawable.ic_info_violet)
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineViolet))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_violet)
            container.setBackgroundResource(R.drawable.rectangle_violet)
        } else if (snippet == context.getString(R.string.snippet_zone4)) {
            img.setImageResource(R.drawable.ic_info_green)
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineGreen))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_green)
            container.setBackgroundResource(R.drawable.rectangle_green)
        } else if (snippet == context.getString(R.string.snippet_zone5)) {
            img.setImageResource(R.drawable.ic_info_pink)
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLinePink))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_pink)
            container.setBackgroundResource(R.drawable.rectangle_pink)
        } else if (snippet == context.getString(R.string.snippet_warland_reserve)) {
            img.setImageResource(R.drawable.marker_blue_dark)
            text.setTextColor(context.getResources().getColor(R.color.colorMainMarker))
            arrowDown.setImageResource(R.drawable.ic_arrow_down_blue_dark)
            container.setBackgroundResource(R.drawable.rectangle_blue_dark)
        }

        return view
    }
}