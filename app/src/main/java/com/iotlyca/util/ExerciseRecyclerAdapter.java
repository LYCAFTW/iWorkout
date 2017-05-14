package com.iotlyca.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iotlyca.app.wicedsense.R;

import java.util.ArrayList;

/**
 * Created by guangyang on 17/5/7.
 */

public class ExerciseRecyclerAdapter extends RecyclerView.Adapter<ExerciseRecyclerAdapter.ViewHolder> {

    private ArrayList<Exercise> mData;

    public ExerciseRecyclerAdapter(ArrayList<Exercise> data) {
        this.mData = data;
    }

    public void updateData(ArrayList<Exercise> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 实例化展示的view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.exercise_item2, parent, false);
        // 实例化viewholder
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Exercise data = mData.get(position);
        holder.name.setText(data.getName());
        holder.image.setImageResource(data.getImage());
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView name;
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.exercise_text);
            image = (ImageView) itemView.findViewById(R.id.exercise_image);
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), String.valueOf(getAdapterPosition()), Toast.LENGTH_SHORT).show();
        }
    }
}

