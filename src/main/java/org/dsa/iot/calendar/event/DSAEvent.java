package org.dsa.iot.calendar.event;

import org.dsa.iot.calendar.DSAIdentifier;
import org.dsa.iot.calendar.guest.DSAGuest;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class DSAEvent {
    private String uniqueId;
    private String title;
    private String description;
    private Instant start;
    private Instant end;
    private String timeZone;
    private DSAIdentifier calendarIdentifier;
    private boolean readOnly;
    private String location;
    private List<DSAGuest> guests;

    protected Clock clock;

    public DSAEvent(String title, Instant start, Instant end) {
        this.title = title;
        this.start = start;
        this.end = end;
        timeZone = TimeZone.getDefault().getID();
        guests = new ArrayList<>();
        clock = Clock.systemDefaultZone();
    }

    /**
     * Check whether the provided datetime is within the range of the
     * event's start and end datetime.
     * @param start Start datetime range.
     * @param end End datetime range.
     * @return True if the datetime is within this event's range.
     */
    public final boolean isInRange(Instant start, Instant end) {
        TimeRange eventRange = new TimeRange(this.start, this.end);
        TimeRange requestedRange = new TimeRange(start, end);
        return TimeRange.areOverlapping(eventRange, requestedRange);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public DSAIdentifier getCalendar() {
        return calendarIdentifier;
    }

    public void setCalendar(DSAIdentifier calendarIdentifier) {
        this.calendarIdentifier = calendarIdentifier;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<DSAGuest> getGuests() {
        return guests;
    }

    public JsonArray serializeGuests() {
        JsonArray guestsJson = new JsonArray();
        for (DSAGuest guest : guests) {
            JsonObject guestJson = new JsonObject();
            guestJson.put("uid", guest.getUniqueId());
            guestJson.put("name", guest.getDisplayName());
            guestJson.put("email", guest.getEmail());
            guestJson.put("organizer", guest.isOrganizer());
            guestsJson.add(guestJson);
        }
        return guestsJson;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }
}
