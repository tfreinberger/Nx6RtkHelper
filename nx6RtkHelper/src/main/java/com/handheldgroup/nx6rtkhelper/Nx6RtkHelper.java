package com.handheldgroup.nx6rtkhelper;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.handheldgroup.serialport.SerialPort;
import com.mmi.IMmiDevice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Nx6RtkHelper {
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    String nmeaSentence;
    public Nx6RtkHelper() {
    }

    public static void powerOn(boolean powerOn) {
        try {
            @SuppressLint("PrivateApi")
            IBinder iBinder = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class)
                    .invoke(null, "mmi_device");
            IMmiDevice mService = IMmiDevice.Stub.asInterface(iBinder);
            //noinspection ConstantConditions
            mService.writeForBackNode("/sys/class/ext_dev/function/ext_dev_5v_enable", powerOn ? "1" : "0");
            mService.writeForBackNode("/sys/class/ext_dev/function/pin10_en", powerOn ? "1" : "0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void openSerialPath(String path, int baudrate, int flag) {
        try {
            serialPort = new SerialPort(new File(path), baudrate, flag);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public InputStream getInputStream() {
        return inputStream;
    }
    public OutputStream getOutputStream() {
        return outputStream;
    }
}
