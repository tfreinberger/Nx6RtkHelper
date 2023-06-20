package com.handheldgroup.nx6rtkhelper;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.handheldgroup.serialport.SerialPort;
import com.mmi.IMmiDevice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import timber.log.Timber;

public class Nx6RtkHelper {
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    String nmeaSentence;
    ReadNmea readNmea;

    public Nx6RtkHelper() {
    }

    private OnNmeaSentenceListener onNmeaSentenceListener;

    public interface OnNmeaSentenceListener {
        void onNmeaSentence(String nmeaSentence);
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

    public void openSerialPath(int baudrate, int flag) {
        try {
            serialPort = new SerialPort(new File("/dev/ttyHSL1"), baudrate, flag);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            Timber.tag("Serial").i("Can not open Path!");
            e.printStackTrace();
        }
    }

    public void closeSerialPath() {
        try {
            outputStream.close();
            inputStream.close();
            serialPort.close();
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

    public void sendRTKData(byte[] data, int size) {
        try {
            outputStream.write(data, 0, size);
            Timber.tag("Sent Data").i(String.valueOf(size));
            outputStream.flush();
        } catch (IOException e) {
            Timber.tag("Sent Data").e("Send Data to serial port failed!");
            e.printStackTrace();
        }
    }

    public void sendUbxCommand(byte[] data) {
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            Timber.tag("Sent Data").e("Send Data to serial port failed!");
            e.printStackTrace();
        }
    }

    public void startNmeaReading() {
        if (readNmea == null) {
            readNmea = new ReadNmea();
            readNmea.start();
        }
    }

    public void saveUbxConfig() {
        try {
            outputStream.write(UbxCommands.saveUbloxConfig());
            outputStream.flush();
        } catch (IOException e) {
            Timber.tag("Sent Data").e("Send Data to serial port failed!");
            e.printStackTrace();
        }
    }

    public void stopNmeaReading() {
        if (readNmea != null) {
            readNmea.interrupt();
        }
    }

    public void receiveNmeaSentence(OnNmeaSentenceListener onNmeaSentenceListener) {
        this.onNmeaSentenceListener = onNmeaSentenceListener;
    }

    class ReadNmea extends Thread {

        byte[] buffer = new byte[4096];
        int index = 0;

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    int b = inputStream.read();
                    buffer[index] = (byte) b;
                    if (buffer[index] == '\n') {
                        nmeaSentence = new String(buffer, 0, index, StandardCharsets.ISO_8859_1);
                        onNmeaSentenceListener.onNmeaSentence(nmeaSentence);
                        Arrays.fill(buffer, 0, index, (byte) 0);
                        index = 0;
                    } else {
                        index++;
                    }
                } catch (IOException | ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
