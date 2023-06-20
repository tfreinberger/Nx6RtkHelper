package com.handheldgroup.nx6rtkhelper;

import static com.handheldgroup.nx6rtkhelper.Chars.NTRIP_PROTOCOL_SEPARATOR_CRLF;

import android.os.AsyncTask;
import android.util.Base64;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RtkSourceTable {

    public RtkSourceTable() {
    }

    private OnSourceTableList onSourceTableList;
    private OnSourceTypeStream onSourceTypeStream;

    ArrayList<String> mpList;

    public interface OnSourceTableList {
        void onSourceTableListToString(String sourceTable);
    }

    public interface OnSourceTypeStream {
        void onSourceType(String mountPoint);
    }

    public void searchForSourceTable(String address, int port) {
        new RequestSourceTask().execute(address, String.valueOf(port));
    }

    public void getSourceTableList(OnSourceTableList onSourceTableList) {
        this.onSourceTableList = onSourceTableList;
    }

    public void getSourceTypeStream(OnSourceTypeStream onSourceTypeStream) {
        this.onSourceTypeStream = onSourceTypeStream;
    }

    public ArrayList<String> getMpList() {
        return mpList;
    }

    class RequestSourceTask extends AsyncTask<String, Integer, String> {

        String line;

        @Override
        protected String doInBackground(String... strings) {

            Socket socket = new Socket();
            InetAddressValidator validator = InetAddressValidator.getInstance();
            InetSocketAddress socketAddress = null;

            mpList = new ArrayList<>();

            // make sure to use IPv4 address
            try {
                InetAddress inetAddress = InetAddress.getByName(strings[0]);
                InetAddress[] inetAddresses = InetAddress.getAllByName(inetAddress.getCanonicalHostName());
                for (int i = 0; i < inetAddresses.length; i++) {
                    if (validator.isValidInet4Address(inetAddresses[i].getHostAddress())) {
                        socketAddress = new InetSocketAddress(inetAddresses[i].getHostAddress(), Integer.parseInt(strings[1]));
                    } else {
                        line = "No valid IPv4 Address!";
                    }
                }
            } catch (UnknownHostException e) {
                line = e.getMessage();
            }

            try {
                if (socket.isClosed()) {
                    line = "Socket is closed!";
                } else if (!socket.isConnected() && socketAddress != null) {
                    socket.connect(socketAddress, 10 * 1000); // timeout to connect
                }
                if (socket.isConnected()) {
                    socket.setSoTimeout(5 * 1000);
                    byte[] data = getRequestSentence().getBytes(StandardCharsets.ISO_8859_1);
                    try {
                        OutputStream ous = socket.getOutputStream();
                        if (ous != null) {
                            ous.write(data, 0 , data.length);
                            ous.flush();
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int readBytes;

                        while ((readBytes = readFromHost(socket, buffer)) != -1) {
                            baos.write(buffer, 0, readBytes);
                            if (new String(buffer, 0, readBytes).contains("ENDSOURCETABLE")) {
                                break;
                            }
                        }
                        String[] stringLines = baos.toString().split(NTRIP_PROTOCOL_SEPARATOR_CRLF);
                        if (onSourceTableList != null) {
                            onSourceTableList.onSourceTableListToString(baos.toString());
                        }
                        baos.close();
                        for (String line : stringLines) {
                            if (line.length() < 3) {
                                continue;
                            }
                            String typeString = line.substring(0, 3);
                            SourceType sourceType = SourceType.getSourceType(typeString);
                            switch (sourceType) {
                                case CAS:
                                case NET:
                                    break;
                                case STR:
                                    String[] parts = line.split(";", 19);
                                    String mountPoint = parts[1];
                                    if (onSourceTypeStream != null) {
                                        onSourceTypeStream.onSourceType(mountPoint);
                                        mpList.add(mountPoint);
                                    }
                            }
                        }
                    } catch (IOException e) {
                        line = e.getMessage();
                    }
                }

            } catch (IOException connectException) {
                line = connectException.getMessage();
            } finally {
                try {
                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return line;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Log.i("SourceTable: ", s);
        }
    }

    private synchronized int readFromHost(Socket socket, byte[] data) {
        int length = 0;
        try {
            InputStream inputStream = socket.getInputStream();
            if (inputStream == null) {
                return 0;
            }
            length = socket.getInputStream().read(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return length;
    }

    private String getRequestSentence() {
        return "GET / HTTP/1.0" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "User-Agent: NTRIP Client" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "Accept: */*" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "Connection: close" + NTRIP_PROTOCOL_SEPARATOR_CRLF +
                "Authorization: Basic " + ToBase64() + NTRIP_PROTOCOL_SEPARATOR_CRLF;
    }

    private String ToBase64() {
        return Base64.encodeToString(":".getBytes(), Base64.CRLF);
    }
}
