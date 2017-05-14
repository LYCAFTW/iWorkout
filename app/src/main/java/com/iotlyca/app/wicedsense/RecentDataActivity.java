package com.iotlyca.app.wicedsense;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

import com.iotlyca.util.RecentDataAdaptor;

/**
 * Created by guangyang on 17/5/8.
 */

public class RecentDataActivity extends ListActivity {
    RecentDataAdaptor adapter;
    final String[] values = new String[]{"chest press", "seated row", "leg press", "abdominal", "bicep curl", "counter balance smith", "tricep press", "leg extension", "hyperextension"};
    final int[] picSource = new int[]{R.drawable.chest_press, R.drawable.seated_row, R.drawable.leg_press, R.drawable.abdominal,
            R.drawable.bicep_curl, R.drawable.counter_balance_smith, R.drawable.tricep_press, R.drawable.leg_extension, R.drawable.hyperextension};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String historyData = intent.getStringExtra("history");
        String [] value = new String[10];
        int [] picS = new int[10];
        int i = 0;
        for (String items : historyData.split(",")) {
            String [] item = items.split("/");
            for (int index = 0; index < 10; index++) {
                if (values[index].equals(item[0])) {
                    value[i] = items;
                    picS[i] = picSource[index];
                }
            }
            i = i + 1;
        }

        adapter = new RecentDataAdaptor(this, value, picS);

        setListAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        return;
    }

}

