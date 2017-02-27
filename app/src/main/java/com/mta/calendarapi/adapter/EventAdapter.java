package com.mta.calendarapi.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.api.services.calendar.model.Event;
import com.mta.calendarapi.R;

import java.util.ArrayList;

/**
 * Created by vanthanhbk on 23/02/2017.
 */

public class EventAdapter extends ArrayAdapter<Event> {
    private ArrayList<Event> arr_event;
    private Context context;

    public EventAdapter(Context context, ArrayList<Event> objects) {
        super(context, 0,objects);
        this.context = context;
        this.arr_event = objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) { // This a new view we inflate the new layout
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_event, parent, false);
        } // Now we can fill the layout with the right values

        Event event = arr_event.get(position);

        TextView tv_sumary = (TextView) convertView.findViewById(R.id.tv_sumary);
        TextView tv_location = (TextView) convertView.findViewById(R.id.tv_location);
        TextView tv_timefrom = (TextView) convertView.findViewById(R.id.tv_timefrom);
        TextView tv_timeeto = (TextView) convertView.findViewById(R.id.tv_timeto);
        TextView tv_date = (TextView) convertView.findViewById(R.id.tv_date);

        tv_sumary.setText(event.getSummary());
        tv_location.setText(event.getLocation());
        try {
            //if (event.getLocation().equals(" ")) tv_location.setText("Viet Nam (default)");
            tv_timefrom.setText(event.getStart().getDateTime().toString());
            tv_timeeto.setText(event.getEnd().getDateTime().toString());

        }catch (Exception e){
            Log.d("log",event.getStart().getDate().toString());
            if (event.getLocation() == "") tv_location.setText("Viet Nam (default)");
            tv_date.setText(event.getStart().getDate().toString());
        }finally {

        }

        return  convertView;
    }


}
