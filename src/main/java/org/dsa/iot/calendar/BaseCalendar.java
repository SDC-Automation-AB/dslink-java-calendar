package org.dsa.iot.calendar;

import org.dsa.iot.calendar.event.DSAEvent;
import org.dsa.iot.calendar.guest.DSAGuest;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.provider.LoopProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class BaseCalendar {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
                                                                         .withZone(ZoneId.systemDefault());

    private static final int UPDATE_LOOP_DELAY = 30;

    protected final Node eventsNode;

    public BaseCalendar(Node eventsNode) {
        this.eventsNode = eventsNode;
    }

    public abstract void createEvent(DSAEvent event);

    public abstract void deleteEvent(String uid, boolean destroyNode);

    public abstract List<DSAEvent> getEvents();

    /**
     * This filters for events by start and end time.
     *
     * @param start Start time
     * @param end   End time
     * @return List of strings to unique ids.
     */
    public List<DSAEvent> getEventsInRange(Instant start, Instant end) {
        List<DSAEvent> events = getEvents();
        List<DSAEvent> newEvents = new ArrayList<>();

        for (DSAEvent event : events) {
            if (event.isInRange(start, end)) {
                newEvents.add(event);
            }
        }

        return newEvents;
    }

    public void startUpdateLoop() {
        LoopProvider.getProvider().schedulePeriodic(this::updateCalendar, 0, UPDATE_LOOP_DELAY, TimeUnit.SECONDS);
    }

    public boolean supportsMultipleCalendars() {
        return false;
    }

    public List<DSAIdentifier> getCalendars() {
        return new ArrayList<>();
    }

    protected void createEventNode(DSAEvent event) {
        // Add event node.
        NodeBuilder eventBuilder = eventsNode.createChild(event.getUniqueId(), false);
        Node eventNode = eventBuilder.build();
        eventBuilder.setDisplayName(event.getTitle());
        eventBuilder.setAttribute("type", new Value("event"));
        eventNode.createChild("description", false)
                .setDisplayName("Description")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("start", false)
                .setDisplayName("Start")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("end", false)
                .setDisplayName("End")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("timeZone", false)
                .setDisplayName("Time Zone")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("calendar", false)
                .setDisplayName("Calendar")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("calendarId", false)
                .setDisplayName("Calendar ID")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("location", false)
                .setDisplayName("Location")
                .setValueType(ValueType.STRING)
                .build();
        eventNode.createChild("guests", false)
                .setDisplayName("Guests")
                .setValueType(ValueType.ARRAY)
                .build();
        String title = event.getTitle();
        String description = event.getDescription();
        Instant start = event.getStart();
        Instant end = event.getEnd();
        String timeZone = event.getTimeZone();
        String location = event.getLocation();
        DSAIdentifier calendarIdentifier = event.getCalendar();
        List<DSAGuest> guests = event.getGuests();
        if (title != null) {
            eventNode.setDisplayName(title);
        }
        if (description != null && eventNode.hasChild("description", false)) {
            eventNode.getChild("description", false).setValue(new Value(description));
        }
        if (eventNode.hasChild("start", false)) {
            eventNode.getChild("start", false).setValue(new Value(dateTimeFormatter.format(start)));
        }
        if (eventNode.hasChild("end", false)) {
            eventNode.getChild("end", false).setValue(new Value(dateTimeFormatter.format(end)));
        }
        if (timeZone != null && eventNode.hasChild("timeZone", false)) {
            eventNode.getChild("timeZone", false).setValue(new Value(timeZone));
        }
        if (location != null && eventNode.hasChild("location", false)) {
            eventNode.getChild("location", false).setValue(new Value(location));
        }
        if (guests != null && !guests.isEmpty() && eventNode.hasChild("guests", false)) {
            eventNode.getChild("guests", false).setValue(new Value(event.serializeGuests()));
        }
        if (calendarIdentifier != null
                && eventNode.hasChild("calendar", false)
                && eventNode.hasChild("calendarId", false)) {
            eventNode.getChild("calendar", false).setValue(new Value(calendarIdentifier.getTitle()));
            eventNode.getChild("calendarId", false).setValue(new Value(calendarIdentifier.getUid()));
        }
        Actions.addEditEventNode(eventNode);
        Actions.addDeleteEventNode(eventNode);
    }

    public void updateCalendar() {
        List<DSAEvent> events = getEvents();
        List<String> touchedEvents = new ArrayList<>();

        for (DSAEvent event : events) {
            if (event.getUniqueId() == null) {
                continue;
            }
            touchedEvents.add(event.getUniqueId());
            createEventNode(event);
        }

        if (eventsNode.getChildren() != null) {
            for (Map.Entry<String, Node> eventNode : eventsNode.getChildren().entrySet()) {
                if (!touchedEvents.contains(eventNode.getValue().getName())) {
                    eventsNode.removeChild(eventNode.getValue(), false);
                }
            }
        }
    }
}
