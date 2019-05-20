package com.trien.dnmap;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

// custom adapter for marker's info windows
public class CustomInfoWindowGoogleMap implements InfoWindowAdapter {

    private Context context;

    public CustomInfoWindowGoogleMap(Context ctx){
        context = ctx;
    }

    // put the implementation inside this method if we want to keep the default layout and overlay our custom layout on top (white padding drawn around)
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    // put the implementation inside this method if we want to use the completely layout of our own
    @Override
    public View getInfoWindow(Marker marker) {
        View view = ((Activity)context).getLayoutInflater()
                .inflate(R.layout.info_window, null);

        LinearLayout container = view.findViewById(R.id.container);
        TextView text = view.findViewById(R.id.textView);
        ImageView img = view.findViewById(R.id.imageView);
        ImageView arrowDown = view.findViewById(R.id.arrowDown);

        // get and set appropriate text content from snippets
        String snippet = marker.getSnippet();
        text.setText(snippet);

        // depending on the content of snippet, we set text color, frame color and image resources accordingly
        if(snippet.equals(context.getString(R.string.snippet_zone1))){
            img.setImageResource(R.drawable.ic_info_blue);
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineBlue));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_blue);
            container.setBackgroundResource(R.drawable.rectangle_blue_light);
        }
        else if(snippet.equals(context.getString(R.string.snippet_zone2))){
            img.setImageResource(R.drawable.ic_info_orange);
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineOrange));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_orange);
            container.setBackgroundResource(R.drawable.rectangle_orange);
        }
        else if(snippet.equals(context.getString(R.string.snippet_zone3))){
            img.setImageResource(R.drawable.ic_info_violet);
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineViolet));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_violet);
            container.setBackgroundResource(R.drawable.rectangle_violet);
        }
        else if(snippet.equals(context.getString(R.string.snippet_zone4))){
            img.setImageResource(R.drawable.ic_info_green);
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLineGreen));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_green);
            container.setBackgroundResource(R.drawable.rectangle_green);
        }
        else if(snippet.equals(context.getString(R.string.snippet_zone5))){
            img.setImageResource(R.drawable.ic_info_pink);
            text.setTextColor(context.getResources().getColor(R.color.colorPolyLinePink));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_pink);
            container.setBackgroundResource(R.drawable.rectangle_pink);
        }
        else if(snippet.equals(context.getString(R.string.snippet_warland_reserve))){
            img.setImageResource(R.drawable.marker_blue_dark);
            text.setTextColor(context.getResources().getColor(R.color.colorMainMarker));
            arrowDown.setImageResource(R.drawable.ic_arrow_down_blue_dark);
            container.setBackgroundResource(R.drawable.rectangle_blue_dark);
        }

        return view;
    }
}