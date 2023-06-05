package com.handheldgroup.nx6rtkhelper;

import android.annotation.SuppressLint;
import android.os.IBinder;

import com.mmi.IMmiDevice;

public class Nx6RtkHelper {

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
}
