package org.dsa.iot.calendar;

import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
import org.dsa.iot.calendar.ews.ExchangeCalendar;
import org.dsa.iot.calendar.google.GoogleCalendar;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;

import java.util.HashMap;
import java.util.Map;

public class CalendarHandler extends DSLinkHandler {
    static final Map<String, BaseCalendar> CALENDARS = new HashMap<>();

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
        Actions.addAddExchangeCalendarNode(superRoot);

        for (Map.Entry<String, Node> entry : superRoot.getChildren().entrySet()) {
            try {
                Value typeAttribute = entry.getValue().getAttribute("type");
                if (typeAttribute != null) {
                    Node calendarNode = entry.getValue();

                    // Set up calendar
                    if (!CALENDARS.containsKey(calendarNode.getName())) {
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
                                CALENDARS.put(calendarNode.getName(), cal);
                                ((GoogleCalendar) cal).attemptAuthorize(calendarNode);
                                Actions.addGetEventsRange(calendarNode);
                                Actions.addGetCalendars(calendarNode);
                                break;
                            case "exchange":
                                String vers = getROConfigOrDefault(calendarNode, "version", new Value("2010 SP2")).getString();
                                ExchangeVersion version = Actions.parseExchangeVersion(vers);
                                String email = getROConfigOrDefault(calendarNode, "email", new Value("")).getString();
                                String pass = getPasswordOrDefault(calendarNode, "");
                                boolean autoDisc = getROConfigOrDefault(calendarNode, "autoDiscoverUrl", new Value(true)).getBool();
                                String url = getROConfigOrDefault(calendarNode, "url", new Value("")).getString();
                                if (autoDisc) {
                                    cal = new ExchangeCalendar(calendarNode, version, email, pass);
                                } else {
                                    cal = new ExchangeCalendar(calendarNode, version, email, pass, url);
                                }
                                CALENDARS.put(calendarNode.getName(), cal);
                                cal.startUpdateLoop();
                            default:
                                throw new Exception("Unknown calendar type");
                        }
                        CALENDARS.put(calendarNode.getName(), cal);
                    }

                    Actions.addRemoveCalendarNode(calendarNode);
                    Actions.addRefreshCalendarNode(calendarNode);
                    Actions.addCreateEventNode(calendarNode);
                }
            } catch (Exception e) {
                System.err.println("Error restoring:");
                e.printStackTrace();
            }
        }
    }

    private static Value getROConfigOrDefault(Node n, String name, Value def) {
        Value val = n.getRoConfig(name);
        if (val == null) {
            n.setRoConfig(name, def);
            return def;
        }
        return val;
    }

    private static String getPasswordOrDefault(Node n, String def) {
        char[] parr = n.getPassword();
        if (parr == null) {
            n.setPassword(def.toCharArray());
            return def;
        }
        return String.valueOf(parr);
    }
}
