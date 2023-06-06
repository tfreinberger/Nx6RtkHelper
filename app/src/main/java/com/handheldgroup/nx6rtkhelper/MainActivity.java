package com.handheldgroup.nx6rtkhelper;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Nx6RtkHelper.powerOn(true);
        Nx6RtkHelper nx6RtkHelper = new Nx6RtkHelper();
        nx6RtkHelper.openSerialPath("/dev/ttyHSL1", Baudrate.BAUD_115200, 0);
        nx6RtkHelper.getInputStream();
        nx6RtkHelper.getOutputStream();
    }
}