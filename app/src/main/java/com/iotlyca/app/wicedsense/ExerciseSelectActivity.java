package com.iotlyca.app.wicedsense;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.iotlyca.util.ExerciseAdaptor;

public class ExerciseSelectActivity extends ListActivity {
    ExerciseAdaptor adapter;
    final String[] values = new String[]{"chest press", "seated row", "leg press", "abdominal", "bicep curl", "counter balance smith", "tricep press", "leg extension", "hyperextension"};
    final int[] picSource = new int[]{R.drawable.chest_press, R.drawable.seated_row, R.drawable.leg_press, R.drawable.abdominal,
            R.drawable.bicep_curl, R.drawable.counter_balance_smith, R.drawable.tricep_press, R.drawable.leg_extension, R.drawable.hyperextension};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent.hasExtra("start")) {
            String recommendation = intent.getStringExtra("start");
            char[] charArray = recommendation.toCharArray();
            String[] value = new String[3];
            int[] picS = new int[3];
            for (int i = 0; i < 3; i++) {
                int index = charArray[i] - '0';
                value[i] = values[index];
                picS[i] = picSource[index];
            }
            adapter = new ExerciseAdaptor(this, value, picS);
        } else {
            adapter = new ExerciseAdaptor(this, values, picSource);
        }

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
        String item = (String) getListAdapter().getItem(position);
        Toast.makeText(this, item + " selected", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ExerciseSelectActivity.this, MainActivity.class);
        intent.putExtra("exercise", item);
        startActivity(intent);
    }

}


