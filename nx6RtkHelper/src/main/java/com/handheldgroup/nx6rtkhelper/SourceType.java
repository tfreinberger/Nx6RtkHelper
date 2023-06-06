package com.handheldgroup.nx6rtkhelper;

public enum SourceType {
    UNKNOWN,
    STR,
    CAS,
    NET;

    public static SourceType getSourceType(String id) {
        SourceType rc = UNKNOWN;
        SourceType[] values = SourceType.values();
        for (SourceType v : values) {
            if (v.name().equals(id)) {
                rc = v;
                break;
            }
        }
        return rc;
    }
}
