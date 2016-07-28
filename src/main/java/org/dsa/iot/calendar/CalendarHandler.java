package org.dsa.iot.calendar;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
import org.dsa.iot.calendar.google.GoogleCalendar;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;

import java.util.HashMap;
import java.util.Map;

public class CalendarHandler extends DSLinkHandler {
    public static final Map<String, BaseCalendar> calendars = new HashMap<>();

    @Override
    public boolean isResponder() {
        return true;
    }

    @Override
    public void onResponderInitialized(final DSLink link) {
        super.onResponderInitialized(link);

        final Node superRoot = link.getNodeManager().getSuperRoot();

        Actions.addAddCalDavCalendarNode(superRoot);
        Actions.addAddGoogleCalendarNode(superRoot);

        for (Map.Entry<String, Node> entry : superRoot.getChildren().entrySet()) {
            try {
                Value typeAttribute = entry.getValue().getAttribute("type");
                if (typeAttribute != null) {
                    Node calendarNode = entry.getValue();

                    // Set up calendar
                    if (!calendars.containsKey(calendarNode.getName())) {
                        BaseCalendar cal;
                        switch (typeAttribute.getString()) {
                            case "caldav":
                                String host = calendarNode.getRoConfig("host").getString();
                                int port = calendarNode.getRoConfig("port").getNumber().intValue();
                                String path = calendarNode.getRoConfig("path").getString();
                                Node eventsNode = calendarNode.getChild("events");
                                cal = new CalDAVCalendar(host, port, path, eventsNode);
                                break;
                            case "google":
                                String clientId = calendarNode.getRoConfig("clientId").getString();
                                String clientSecret = calendarNode.getRoConfig("clientSecret").getString();
                                cal = new GoogleCalendar(calendarNode, clientId, clientSecret);
                                calendars.put(calendarNode.getName(), cal);
                                ((GoogleCalendar)cal).attemptAuthorize(calendarNode);
                                Actions.addGetEventsRange(calendarNode);
                                Actions.addGetCalendars(calendarNode);
                                break;
                            default:
                                throw new Exception("Unknown calendar type");
                        }
                        calendars.put(calendarNode.getName(), cal);
                    }

                    Actions.addRemoveCalendarNode(calendarNode);
                    Actions.addRefreshCalendarNode(calendarNode);
                    Actions.addCreateEventNode(calendarNode);
                }
            } catch(Exception e) {
                System.err.println("Error restoring:");
                e.printStackTrace();
            }
        }
    }
}
