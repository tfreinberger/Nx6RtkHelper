package com.handheldgroup.nx6rtkhelper;

public enum Status {
    UNPREPARED,
    PARTLY_PREPARED,
    PREPARED,
    CONNECTING,
    DATA_LOSS,
    CONNECTED,
    RTK_DATA_SUCCESS,
    CONNECT_FAILED,
    SOCKET_IO_ERROR,
    DISCONNECTING,
    DISCONNECTED,
    TIME_OUT,
    UNAUTHORIZED
}
