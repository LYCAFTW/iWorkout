package com.iotlyca.util;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.iotlyca.app.wicedsense.R;

public class ExerciseAdaptor extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int [] picSource;

    public ExerciseAdaptor(Context context, String[] values, int[] picSource) {
        super(context, R.layout.exercise_item, values);
        this.picSource = picSource;
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.exercise_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.exercise_text);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.exercise_image);

        textView.setText(values[position]);
        imageView.setImageResource(picSource[position]);

        return rowView;
    }
}