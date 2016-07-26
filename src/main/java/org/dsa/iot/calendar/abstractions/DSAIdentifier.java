package org.dsa.iot.calendar.abstractions;

public class DSAIdentifier {
    private final String uid;
    private final String title;

    public DSAIdentifier(String uid, String title) {
        this.uid = uid;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getUid() {
        return uid;
    }
}
