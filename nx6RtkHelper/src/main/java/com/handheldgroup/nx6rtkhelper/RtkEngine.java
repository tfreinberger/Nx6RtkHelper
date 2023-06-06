package com.handheldgroup.nx6rtkhelper;

import static com.handheldgroup.nx6rtkhelper.Chars.NTRIP_PROTOCOL_SEPARATOR_CRLF;

import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class RtkEngine {

    private static final String TAG = "RTKEngine";

    private OnStatusChangeListener statusChangeListener;

    private OnDataReceivedListener dataReceivedListener;

    private static NtripParameter ntripParameter = null;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ReadThread readThread;
    private SocketAddress socketAddress;

    long lastGGATime = 0;
    String lastGGA;

    boolean isRestarting = false;

    public interface OnStatusChangeListener {
        void onChange(Status status, String... msg);
    }

    public interface OnDataReceivedListener {
        void onRTKData(byte[] data, int size);
    }

    public void setNtripParameter(NtripParameter parameter) {
        ntripParameter = parameter;
    }

    public void start(@NonNull OnDataReceivedListener dataReceivedListener, @NonNull OnStatusChangeListener statusChangeListener) {
        this.dataReceivedListener = dataReceivedListener;
        this.statusChangeListener = statusChangeListener;
        getRTK();
    }

    private void getRTK() {
        new Thread(() -> {
            stopGetRTK();
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
            }
            socketAddress = new InetSocketAddress(ntripParameter.getHost(), ntripParameter.getPort());
            connectToHost(socket, socketAddress, (status, msg) -> {
                switch (status) {
                    case CONNECTED:
                        Timber.tag(TAG).i("Connect to Host succeed");
                        byte[] data = getAuthorizationSentence(ntripParameter.getMountPoint(),
                                ntripParameter.getUsername(), ntripParameter.getPassword()).getBytes(StandardCharsets.ISO_8859_1);
                        writeToHost(socket, data, data.length);
                        break;
                }
                isRestarting = false;
            });
        }).start();
    }

    private String getAuthorizationSentence(@NonNull String mountPoint, @NonNull String username, @NonNull String password) {
        final String authRequest = "GET /" + mountPoint + " HTTP/1.0" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "User-Agent: NTRIP Client" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "Accept: */*" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "Authorization: Basic " + ToBase64(username + ":" + password) + NTRIP_PROTOCOL_SEPARATOR_CRLF;
        Timber.tag(TAG).i("Authorization: %s", authRequest);
        return authRequest;
    }

    private String ToBase64(String authorization) {
        return Base64.encodeToString(authorization.getBytes(), Base64.CRLF);
    }

    public void restart() {
        if (dataReceivedListener != null && !isRestarting) {
            isRestarting = true;
            Timber.tag(TAG).i("Restart...");
            stopGetRTK();
            getRTK();
        }
    }

    public void stop(OnStatusChangeListener statusChangeListener) {
        this.statusChangeListener = statusChangeListener;
        Timber.tag(TAG).i("Disconnecting");
        stopGetRTK();
        dataReceivedListener = null;
    }

    private void stopGetRTK() {
        Timber.tag(TAG).i("stop");
        if (socket != null) {
            disconnectFromHost();
            Timber.tag(TAG).i("disconnect");
            socket = null;
        } else {
            Timber.tag(TAG).i("Already disconnected");
            if (readThread != null) {
                readThread.interrupt();
            }
        }
    }

    private synchronized void disconnectFromHost() {
        if (socket == null || socket.isClosed()) {
            Timber.tag(TAG).i("Socket is null or closed");
            return;
        }
        Timber.tag(TAG).i("Disconnect...");
        try {
            readThread.interrupt();
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            Timber.tag(TAG).i("Disconnecting: %s", e.getMessage());
        } finally {
            Timber.tag(TAG).i("Disconnect from Host!");
            statusChangeListener.onChange(Status.DISCONNECTED);
        }
    }

    private synchronized void connectToHost(Socket socket, @NonNull SocketAddress address, @Nullable OnStatusChangeListener listener) {
        if (socket.isConnected()) {
            Timber.tag(TAG).i("Already connected to Host");
            return;
        }
        Timber.tag(TAG).i("Connecting...");
        if (listener == null) {
            listener = statusChangeListener;
        }
        try {
            if (socket.isClosed()) {
                listener.onChange(Status.DISCONNECTED, "Socket is closed");
            } else if (!socket.isConnected()) {
                socket.connect(address, 10 * 1000); // timeout to connect
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                readThread = new ReadThread();
                readThread.start();
                listener.onChange(Status.CONNECTED, "Connected");
                Timber.tag(TAG).i("Connect to Host succeed");
            }
            if (socket.isConnected()) {
                socket.setSoTimeout(5 * 1000); // timeout
            }
        } catch (ConnectException connectException) {
            Timber.tag(TAG).e("Failed%s", connectException.getMessage());
            listener.onChange(Status.CONNECT_FAILED, connectException.getMessage());
        } catch (SocketException socketException) {
            Timber.tag(TAG).e("Socket is closed%s", socketException.getMessage());
            listener.onChange(Status.DISCONNECTED, socketException.getMessage());
        } catch (SocketTimeoutException timeoutException) {
            Timber.tag(TAG).e("Timeout%s", timeoutException.getMessage());
            listener.onChange(Status.TIME_OUT, timeoutException.getMessage());
        } catch (IOException ioException) {
            Timber.tag(TAG).e("Error%s", ioException.getMessage());
            listener.onChange(Status.CONNECT_FAILED, ioException.getMessage());
        }
    }

 /*   private synchronized void writeToHost(Socket socket, byte[] data, int length) {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            Log.i(TAG, "Cannot write to Host");
            return;
        }
        Log.i(TAG, "Write to host...");
        try {
            outputStream.write(data, 0, length);
            outputStream.flush();
            Log.i(TAG, "Write to Host succeed");
            statusChangeListener.onChange(Status.CONNECTED);
        } catch (IOException e) {
            Log.i(TAG, "Write to Host failed: " + e.getMessage());
            statusChangeListener.onChange(Status.SOCKET_IO_ERROR);
            statusChangeListener.onChange(Status.TIME_OUT);
        }
    }*/

    public synchronized int readFromHost(Socket socket, byte[] data) {
        if (socket == null) {
            Timber.tag(TAG).i("Read from server. Socket is null");
            return 0;
        }
        Timber.tag(TAG).i("Start to read from server..");
        if (!socket.isConnected() || socket.isClosed()) {
            Timber.tag(TAG).i("Start to read from server. Check socket is OK");
            return 0;
        }
        int length = 0;
        try {
            InputStream inputStream = socket.getInputStream();
            if (inputStream == null) {
                return 0;
            }
            length = socket.getInputStream().read(data, 0, data.length);
            Timber.tag(TAG).i(String.valueOf(length));
        } catch (IOException e) {
            Timber.tag(TAG).i("Read from Server failed");
            //statusChangeListener.onChange(Status.SOCKET_IO_ERROR, e.getMessage());
        }
        return length;
    }

    public void sendGGA(String gga) {
        lastGGA = gga;
        if (socket != null) {
            long diff = SystemClock.elapsedRealtime() - lastGGATime;
            if (diff > 5000) {
                if (Build.MODEL.equals("NAUTIZ_X6P")) {
                    writeToHost(socket, gga.getBytes(), gga.length());
                } else {
                    SendGGA sendGGA = new SendGGA();
                    sendGGA.execute(gga);
                }
                Timber.tag("Send GGA").i(gga);
                lastGGATime = SystemClock.elapsedRealtime();
            }
        }
    }

    class SendGGA extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            writeToHost(socket, strings[0].getBytes(), strings[0].length());
            return null;
        }
    }

    private synchronized void writeToHost(Socket socket, byte[] data, int length) {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            Timber.tag("RTKDataRunnable").i("Cannot write to Host");
            return;
        }
        Timber.tag("RTKDataRunnable").i("Write to host...");
        try {
            outputStream.write(data, 0, length);
            outputStream.flush();
            Timber.tag("RTKDataRunnable").i("Write to Host succeed");
            statusChangeListener.onChange(Status.CONNECTED);
        } catch (IOException e) {
            Timber.tag("RTKDataRunnable").i("Write to Host failed: %s", e.getMessage());
            //statusChangeListener.onChange(Status.SOCKET_IO_ERROR);
            //statusChangeListener.onChange(Status.TIME_OUT);
        }
    }

    class ReadThread extends Thread {

        private static final String TAG = "RTKDataRunnable";
        byte[] receiveBuffer = new byte[8192];
        int length = 0;

        @Override
        public void run() {
            Timber.tag(TAG).i("Data runnable start to get rtk data...");
            while (!isInterrupted()) {
                try {
                    length = inputStream.read(receiveBuffer, 0, receiveBuffer.length);
                } catch (IOException e) {
                    Timber.tag(TAG).i(e, "Read error");
                    length = 0;
                }
                Timber.tag(TAG).i("getting rtk data... length >> %s", length);
                if (length == -1) {
                    getRTK();
                }
                if (length > 0) {
                    Timber.tag(TAG).i(new String(receiveBuffer, 0, length, StandardCharsets.UTF_8));
                    Timber.tag(TAG).i("Data received(" + length + "): " + new String(receiveBuffer, 0, length, StandardCharsets.ISO_8859_1));
                    String result = new String(receiveBuffer, 0, length, StandardCharsets.ISO_8859_1);
                    Timber.tag("Result").i(result.substring(0, Math.min(20, length)));
                    Timber.tag("ReceiveDataHex").i("HexData: %s", bytesToHex(receiveBuffer));
                    if (result.substring(0, Math.min(20, length)).contains("SOURCETABLE 200 OK")) {
                        statusChangeListener.onChange(Status.PARTLY_PREPARED);
                    } else if (result.substring(0, Math.min(20, length)).contains("ICY 200 OK")) {
                        if (lastGGA != null) {
                            Timber.tag(TAG).i("sending gga%s", lastGGA);
                            sendGGA(lastGGA);
                            lastGGATime = 0;
                        }
                        //statusChangeListener.onChange(Status.PARTLY_PREPARED);
                    } else {
                        dataReceivedListener.onRTKData(receiveBuffer, length);
                        statusChangeListener.onChange(Status.RTK_DATA_SUCCESS);
                    }
                    if (result.contains("401 Unauthorized")) {
                        statusChangeListener.onChange(Status.UNAUTHORIZED);
                    }
                } else {
                    if (!isRestarting) {
                        statusChangeListener.onChange(Status.DATA_LOSS);
                        SystemClock.sleep(100);
                        Timber.tag(TAG).i("No data received, sleep 100ms");
                    }
                }
            }
            Timber.tag(TAG).i("Stop to get rtk data");
        }
    }

    final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
