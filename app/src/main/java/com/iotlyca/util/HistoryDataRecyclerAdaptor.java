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
 * Created by guangyang on 17/5/9.
 */

public class HistoryDataRecyclerAdaptor extends RecyclerView.Adapter<HistoryDataRecyclerAdaptor.ViewHolder> {

    private ArrayList<String> mData;

    public HistoryDataRecyclerAdaptor(ArrayList<String> data) {
        this.mData = data;
    }

    public void updateData(ArrayList<String> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @Override
    public HistoryDataRecyclerAdaptor.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_data, parent, false);
        HistoryDataRecyclerAdaptor.ViewHolder viewHolder = new HistoryDataRecyclerAdaptor.ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(HistoryDataRecyclerAdaptor.ViewHolder holder, int position) {
        String data = mData.get(position);
        StringBuilder rez = new StringBuilder();
        if (data != null) {
            String[] stringList = data.split("/");
            if (stringList.length != 3) {
                holder.text.setText(data);
                return;
            }
            rez.append(stringList[0]);
            rez.append("/");
            rez.append(stringList[1]);
            rez.append(" times/");
            rez.append(stringList[2]);
            rez.append(" lb");
            holder.text.setText(rez.toString());
            return;
        }
        holder.text.setText(data);
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder  {

        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.history_data);
        }

    }
}
