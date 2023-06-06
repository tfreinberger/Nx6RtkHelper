package com.handheldgroup.nx6rtkhelper;

import timber.log.Timber;

public class NtripParameter {
    private static final String TAG = "NTRIPParameter";

    private String host;
    private int port;
    private String username;
    private String password;
    private String mountPoint;

    public NtripParameter(String host, int port, String mountPoint, String username, String password) {
        Timber.tag(TAG).d("IP = " + host + ", port=" + port + ", mountpoint=" + mountPoint + ", username=" + username + ", password=" + password);
        this.host =host;
        this.port = port;
        this.mountPoint = mountPoint;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }
}
