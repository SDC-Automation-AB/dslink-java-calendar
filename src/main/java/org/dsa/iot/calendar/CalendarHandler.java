package org.dsa.iot.calendar;

import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
import org.dsa.iot.calendar.ews.ExchangeCalendar;
import org.dsa.iot.calendar.google.GoogleCalendar;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.osaf.caldav4j.methods.HttpClient;

import java.util.HashMap;
import java.util.Map;

public class CalendarHandler extends DSLinkHandler {
    private final HttpClient httpClient;
    public static final Map<String, BaseCalendar> calendars = new HashMap<>();
    public static Node googleAccessToken;

    public CalendarHandler() {
        httpClient = new HttpClient();
        httpClient.getHostConfiguration().setHost("localhost", 5232, "http");
    }

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
                    if (!calendars.containsKey(calendarNode.getName())) {
                        if (typeAttribute.getString().equals("caldav")) {
                            String host = calendarNode.getRoConfig("host").getString();
                            int port = calendarNode.getRoConfig("port").getNumber().intValue();
                            String path = calendarNode.getRoConfig("path").getString();
                            Node eventsNode = calendarNode.getChild("events");
                            CalDAVCalendar cal = new CalDAVCalendar(host, port, path, eventsNode);
                            calendars.put(calendarNode.getName(), cal);
                        } else if (typeAttribute.getString().equals("google")) {
                            String clientId = calendarNode.getRoConfig("clientId").getString();
                            String clientSecret = calendarNode.getRoConfig("clientSecret").getString();
                            Node eventsNode = calendarNode.getChild("events");
                            GoogleCalendar cal = new GoogleCalendar(calendarNode, clientId, clientSecret);
                            calendars.put(calendarNode.getName(), cal);
                            cal.attemptAuthorize(calendarNode);
                        } else if (typeAttribute.getString().equals("exchange")) {
                        	String vers = getROConfigOrDefault(calendarNode, "version", new Value("2010 SP2")).getString();
                        	ExchangeVersion version = Actions.parseExchangeVersion(vers);
                        	String email = getROConfigOrDefault(calendarNode, "email", new Value("")).getString();
                        	String pass = getPasswordOrDefault(calendarNode, "");
                        	boolean autoDisc = getROConfigOrDefault(calendarNode, "autoDiscoverUrl", new Value(true)).getBool();
                        	String url = getROConfigOrDefault(calendarNode, "url", new Value("")).getString();
                        	ExchangeCalendar cal;
                        	if (autoDisc) {
                            	cal = new ExchangeCalendar(calendarNode, version, email, pass);
                            } else {
                            	cal = new ExchangeCalendar(calendarNode, version, email, pass, url);
                            }
                        	calendars.put(calendarNode.getName(), cal);
                        	cal.init();
                        }
                    }

                    if (calendarNode.hasChild("createAnEvent")) {
                        calendarNode.removeChild("createAnEvent");
                    }
                    if (calendarNode.hasChild("removeAccount")) {
                        calendarNode.removeChild("removeAccount");
                    }
                    if (calendarNode.hasChild("refreshCalendar")) {
                        calendarNode.removeChild("refreshCalendar");
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
