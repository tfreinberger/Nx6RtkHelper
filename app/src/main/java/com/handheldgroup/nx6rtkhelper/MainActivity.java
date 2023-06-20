package com.handheldgroup.nx6rtkhelper;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Nx6RtkHelper.powerOn(true);
        RtkSourceTable rtkSourceTable = new RtkSourceTable();
        rtkSourceTable.searchForSourceTable("195.200.70.199", 2101);
        rtkSourceTable.getSourceTypeStream(new RtkSourceTable.OnSourceTypeStream() {
            @Override
            public void onSourceType(String mountPoint) {
                Log.i("Mountpoint: ", mountPoint);

            }
        });
    }
}