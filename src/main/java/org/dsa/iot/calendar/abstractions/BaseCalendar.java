package org.dsa.iot.calendar.abstractions;

import org.dsa.iot.calendar.Actions;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.provider.LoopProvider;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class BaseCalendar {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

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
     * @param start Start time
     * @param end End time
     * @return List of strings to unique ids.
     */
    public abstract List<DSAEvent> getEvents(Date start, Date end);

    public void startUpdateLoop() {
        LoopProvider.getProvider().schedulePeriodic(new Runnable() {
            @Override
            public void run() {
                updateCalendar();
            }
        }, 0, UPDATE_LOOP_DELAY, TimeUnit.SECONDS);
    }

    public boolean supportsMultipleCalendars() {
        return false;
    }

    public List<DSAIdentifier> getCalendars() {
        return new ArrayList<>();
    }

    public void createEventNode(DSAEvent event) {
        Node eventNode;
        if (eventsNode.hasChild(event.getUniqueId())) {
            eventNode = eventsNode.getChild(event.getUniqueId());
        } else {
            // Add event node.
            NodeBuilder eventBuilder = eventsNode.createChild(event.getUniqueId());
            eventNode = eventBuilder.build();
            eventBuilder.setDisplayName(event.getTitle());
            eventBuilder.setAttribute("type", new Value("event"));
            eventNode.createChild("description")
                    .setDisplayName("Description")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("start")
                    .setDisplayName("Start")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("end")
                    .setDisplayName("End")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("timeZone")
                    .setDisplayName("Time Zone")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("calendar")
                    .setDisplayName("Calendar")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("calendarId")
                    .setDisplayName("Calendar ID")
                    .setValueType(ValueType.STRING)
                    .build();
            eventNode.createChild("location")
                    .setDisplayName("Location")
                    .setValueType(ValueType.STRING)
                    .build();
        }
        String title = event.getTitle();
        String description = event.getDescription();
        Date startString = event.getStart();
        Date endString = event.getEnd();
        String timeZone = event.getTimeZone();
        String location = event.getLocation();
        DSAIdentifier calendarIdentifier = event.getCalendar();
        if (title != null) {
            eventNode.setDisplayName(title);
        }
        if (description != null) {
            eventNode.getChild("description").setValue(new Value(description));
        }
        if (startString != null) {
            eventNode.getChild("start").setValue(new Value(new SimpleDateFormat(DATE_PATTERN).format(startString)));
        }
        if (endString != null) {
            eventNode.getChild("end").setValue(new Value(new SimpleDateFormat(DATE_PATTERN).format(endString)));
        }
        if (timeZone != null) {
            eventNode.getChild("timeZone").setValue(new Value(timeZone));
        }
        if (location != null) {
            eventNode.getChild("location").setValue(new Value(location));
        }
        if (calendarIdentifier != null) {
            eventNode.getChild("calendar").setValue(new Value(calendarIdentifier.getTitle()));
            eventNode.getChild("calendarId").setValue(new Value(calendarIdentifier.getUid()));
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
                    eventsNode.removeChild(eventNode.getValue());
                }
            }
        }
    }
}
