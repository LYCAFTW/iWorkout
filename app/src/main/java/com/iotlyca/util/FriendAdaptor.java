package com.iotlyca.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iotlyca.app.wicedsense.R;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by guangyang on 17/5/6.
 */

public class FriendAdaptor
        extends RecyclerView.Adapter<FriendAdaptor.ViewHolder>{
    private ArrayList<Friend> mData;
    final String[] values = new String[]{"chest press", "seated row", "leg press", "abdominal", "bicep curl", "counter balance smith", "tricep press", "leg extension", "hyperextension"};

    public FriendAdaptor(ArrayList<Friend> data) {
        this.mData = data;
    }

    public void updateData(ArrayList<Friend> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 实例化展示的view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        // 实例化viewholder
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Friend data = mData.get(position);
        holder.name.setText(data.getName());
        // this funciton used to set the image
        new DownloadImageTask(holder.image).execute(data.getImage());

        if (data.getMessage() == null) return;
        String[] stringList = data.getMessage().split(" ");
        StringBuilder rez = new StringBuilder();
        for (int i = 0; i < stringList.length; i++) {
            rez.append(values[i]);
            rez.append(":");
            rez.append(stringList[i]);
            rez.append("Kcal");
            rez.append("\n");
        }
        holder.message.setText(rez.toString());
    }

    //Info section end
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        ImageView image;
        TextView message;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.friend_name);
            image = (ImageView) itemView.findViewById(R.id.friend_image);
            message = (TextView) itemView.findViewById(R.id.friend_message);
        }
    }
}
