package org.dsa.iot.calendar;

import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.abstractions.DSAEvent;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
import org.dsa.iot.calendar.ews.ExchangeCalendar;
import org.dsa.iot.calendar.google.GoogleCalendar;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.EditorType;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

import static org.dsa.iot.calendar.CalendarHandler.calendars;

public class Actions {
    public static Node addAddCalDavCalendarNode(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("addCalDavCalendar");
        builder.setDisplayName("Add CalDAV Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddCalDAVCalendar(superRoot));
        return builder.build();
    }

    public static Node addAddGoogleCalendarNode(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("addGoogleCalendar");
        builder.setDisplayName("Add Google Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddGoogleCalendar(superRoot));
        return builder.build();
    }
    
    public static Node addAddExchangeCalendarNode(Node superRoot)  {
    	NodeBuilder builder = superRoot.createChild("addExchangeCalendar");
        builder.setDisplayName("Add Exchange Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddExchangeCalendar(superRoot));
        return builder.build();
    }

    public static Node addRemoveCalendarNode(Node calendarNode) {
        NodeBuilder rmBuilder = calendarNode.createChild("removeAccount");
        rmBuilder.setDisplayName("Remove Account");
        rmBuilder.setSerializable(false);
        rmBuilder.setAction(new RemoveAccount());
        return rmBuilder.build();
    }

    public static Node addRefreshCalendarNode(Node calendarNode) {
        NodeBuilder refreshBuilder = calendarNode.createChild("refreshCalendar");
        refreshBuilder.setDisplayName("Refresh Calendar");
        refreshBuilder.setSerializable(false);
        refreshBuilder.setAction(new RefreshBuilder(calendars.get(calendarNode.getName())));
        return refreshBuilder.build();
    }

    public static Node addCreateEventNode(Node calendarNode) {
        NodeBuilder createEventNode = calendarNode.createChild("createAnEvent");
        createEventNode.setDisplayName("Create Event");
        createEventNode.setSerializable(false);
        createEventNode.setAction(new CreateEvent(calendars.get(calendarNode.getName())));
        return createEventNode.build();
    }

    public static Node addEditEventNode(Node eventNode) {
        NodeBuilder editEventNode = eventNode.createChild("editEvent");
        editEventNode.setDisplayName("Edit Event");
        editEventNode.setSerializable(false);
        editEventNode.setAction(new EditEvent(calendars.get(eventNode.getParent().getParent().getName())));
        return editEventNode.build();
    }

    public static Node addDeleteEventNode(Node eventNode) {
        NodeBuilder deleteEventNode = eventNode.createChild("deleteEvent");
        deleteEventNode.setDisplayName("Delete Event");
        deleteEventNode.setSerializable(false);
        deleteEventNode.setAction(new RemoveEvent(calendars.get(eventNode.getParent().getParent().getName()), eventNode.getName()));
        return deleteEventNode.build();
    }

    private static class AddCalDAVCalendar extends Action {
        public AddCalDAVCalendar(final Node superRoot) {
            super(Permission.CONFIG, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    String desc = "";
                    String username = "";
                    String password = "";
                    String host = "";
                    int port = 80;
                    String path = "";
                    if (event.getParameter("desc") != null) {
                        desc = event.getParameter("desc").getString();
                    }
                    if (event.getParameter("username") != null) {
                        username = event.getParameter("username").getString();
                    }
                    if (event.getParameter("password") != null) {
                        password = event.getParameter("password").getString();
                    }
                    if (event.getParameter("host") != null) {
                        host = event.getParameter("host").getString();
                    }
                    if (event.getParameter("port") != null) {
                        port = event.getParameter("port").getNumber().intValue();
                    }
                    if (event.getParameter("calendarPath") != null) {
                        path = event.getParameter("calendarPath").getString();
                    }

                    NodeBuilder calendarBuilder = superRoot.createChild(desc);
                    calendarBuilder.setAttribute("type", new Value("caldav"));
                    calendarBuilder.setRoConfig("username", new Value(username));
                    calendarBuilder.setPassword(password.toCharArray());
                    calendarBuilder.setRoConfig("host", new Value(host));
                    calendarBuilder.setRoConfig("port", new Value(port));
                    calendarBuilder.setRoConfig("path", new Value(path));
                    Node calendarNode = calendarBuilder.build();

                    NodeBuilder eventsBuilder = calendarNode.createChild("events");
                    eventsBuilder.setDisplayName("Events");
                    Node events = eventsBuilder.build();

                    CalDAVCalendar cal = new CalDAVCalendar(host, port, path, events);
                    calendars.put(desc, cal);

                    Actions.addCreateEventNode(calendarNode);
                    Actions.addRemoveCalendarNode(calendarNode);
                    Actions.addRefreshCalendarNode(calendarNode);
                }
            });
            addParameter(new Parameter("desc", ValueType.STRING));
            addParameter(new Parameter("username", ValueType.STRING));
            addParameter(new Parameter("password", ValueType.STRING));
            addParameter(new Parameter("host", ValueType.STRING));
            addParameter(new Parameter("port", ValueType.NUMBER));
            addParameter(new Parameter("calendarPath", ValueType.STRING));
        }
    }

    private static class AddGoogleCalendar extends Action {
        public AddGoogleCalendar(final Node superRoot) {
            super(Permission.CONFIG, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    String desc = "";
                    String clientId = "";
                    String clientSecret = "";
                    if (event.getParameter("desc") != null) {
                        desc = event.getParameter("desc").getString();
                    }
                    if (event.getParameter("clientId") != null) {
                        clientId = event.getParameter("clientId").getString();
                    }
                    if (event.getParameter("clientSecret") != null) {
                        clientSecret = event.getParameter("clientSecret").getString();
                    }

                    NodeBuilder calendarBuilder = superRoot.createChild(desc);
                    calendarBuilder.setAttribute("type", new Value("google"));
                    calendarBuilder.setRoConfig("clientId", new Value(clientId));
                    calendarBuilder.setRoConfig("clientSecret", new Value(clientSecret));
                    Node calendarNode = calendarBuilder.build();

                    NodeBuilder eventsBuilder = calendarNode.createChild("events");
                    eventsBuilder.setDisplayName("Events");
                    Node events = eventsBuilder.build();

                    try {
                        GoogleCalendar cal = new GoogleCalendar(calendarNode, clientId, clientSecret);
                        calendars.put(desc, cal);

                        Actions.addCreateEventNode(calendarNode);
                        Actions.addRemoveCalendarNode(calendarNode);
                        Actions.addRefreshCalendarNode(calendarNode);
                        cal.attemptAuthorize(calendarNode);
                    } catch (GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            addParameter(new Parameter("desc", ValueType.STRING));
            addParameter(new Parameter("clientId", ValueType.STRING));
            addParameter(new Parameter("clientSecret", ValueType.STRING));
        }
    }
    
    public static class AddExchangeCalendar extends Action {
    	public AddExchangeCalendar(final Node superRoot) {
    		super(Permission.CONFIG, new Handler<ActionResult>() {
				@Override
				public void handle(ActionResult event) {
					String desc = "";
                    String email = "";
                    String password = "";
                    boolean autoDiscover = true;
                    String url = "";
                    String vers = "2010 SP2";
                    ExchangeVersion version = ExchangeVersion.Exchange2010_SP2;

                    if (event.getParameter("desc") != null) {
                        desc = event.getParameter("desc").getString();
                    }
                    if (event.getParameter("version") != null) {
                        vers = event.getParameter("version").getString();
                        version = parseExchangeVersion(vers);
                    }
                    if (event.getParameter("email") != null) {
                        email = event.getParameter("email").getString();
                    }
                    if (event.getParameter("password") != null) {
                        password = event.getParameter("password").getString();
                    }
                    if (event.getParameter("autoDiscoverUrl") != null) {
                        autoDiscover = event.getParameter("autoDiscoverUrl").getBool();
                    }
                    if (event.getParameter("url") != null) {
                        url = event.getParameter("url").getString();
                    }
                    
                    NodeBuilder calendarBuilder = superRoot.createChild(desc);
                    calendarBuilder.setAttribute("type", new Value("exchange"));
                    calendarBuilder.setRoConfig("version", new Value(vers));
                    calendarBuilder.setRoConfig("email", new Value(email));
                    calendarBuilder.setPassword(password.toCharArray());
                    calendarBuilder.setRoConfig("autoDiscoverUrl", new Value(autoDiscover));
                    calendarBuilder.setRoConfig("url", new Value(url));
                    Node calendarNode = calendarBuilder.build();

                    NodeBuilder eventsBuilder = calendarNode.createChild("events");
                    eventsBuilder.setDisplayName("Events");
                    eventsBuilder.build();
                    
                    ExchangeCalendar cal;
                    if (autoDiscover) {
                    	cal = new ExchangeCalendar(calendarNode, version, email, password);
                    } else {
                    	cal = new ExchangeCalendar(calendarNode, version, email, password, url);
                    }
                    
                    calendars.put(desc, cal);

                    Actions.addCreateEventNode(calendarNode);
                    Actions.addRemoveCalendarNode(calendarNode);
                    Actions.addRefreshCalendarNode(calendarNode);
					
                    cal.init();
				}
    		});
    		addParameter(new Parameter("desc", ValueType.STRING));
    		addParameter(new Parameter("version", ValueType.makeEnum("2007 SP1", "2010", "2010 SP1", "2010 SP2"), new Value("2010 SP2")));
            addParameter(new Parameter("email", ValueType.STRING));
            addParameter(new Parameter("password", ValueType.STRING).setEditorType(EditorType.PASSWORD));
            addParameter(new Parameter("autoDiscoverUrl", ValueType.BOOL, new Value(true)));
            addParameter(new Parameter("url", ValueType.STRING));
    	}
    }
    
    public static ExchangeVersion parseExchangeVersion(String str) {
    	if (str.equals("2007 SP1")) return ExchangeVersion.Exchange2007_SP1;
        else if (str.equals("2010")) return ExchangeVersion.Exchange2010;
        else if (str.equals("2010 SP1")) return ExchangeVersion.Exchange2010_SP1;
        else return ExchangeVersion.Exchange2010_SP2;
    }

    private static class RemoveAccount extends Action {
        public RemoveAccount() {
            super(Permission.CONFIG, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Node calendar = event.getNode().getParent();
                    event.getNode().getParent().getParent().removeChild(calendar);
                }
            });
        }
    }

    private static class RefreshBuilder extends Action {
        public RefreshBuilder(final BaseCalendar calendar) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    calendar.updateCalendar();
                }
            });
        }
    }

    private static class CreateEvent extends Action {
        public CreateEvent(final BaseCalendar calendar) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult actionResult) {
                    String title = actionResult.getParameter("title").getString();
                    if (title != null && !title.isEmpty()) {
                        String desc = actionResult.getParameter("desc", new Value("")).getString();
                        String timeRange = actionResult.getParameter("timeRange").getString();
                        DSAEvent event = new DSAEvent(title);
                        event.setDescription(desc);
                        try {
                            String[] dates = timeRange.split("/", 2);
                            if (dates.length != 2) {
                                throw new Exception("Unexpected dates length");
                            }
                            Date startDate = BaseCalendar.DATE_FORMAT.parse(dates[0]);
                            Date endDate = BaseCalendar.DATE_FORMAT.parse(dates[1]);
                            event.setStart(startDate);
                            event.setEnd(endDate);
                            calendar.createEvent(event);
                            calendar.updateCalendar();
                            actionResult.getTable().addRow(Row.make(new Value(true)));
                        } catch (Exception e) {
                            e.printStackTrace();
                            actionResult.getTable().addRow(Row.make(new Value(false)));
                        }
                    }
                }
            });
            addParameter(new Parameter("title", ValueType.STRING));
            addParameter(new Parameter("desc", ValueType.STRING));
            Parameter parameter = new Parameter("timeRange", ValueType.TIME);
            parameter.setEditorType(EditorType.DATE_RANGE);
            addParameter(parameter);

            addResult(new Parameter("success", ValueType.BOOL));
        }
    }

    private static class EditEvent extends Action {
        public EditEvent(final BaseCalendar calendar) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult actionResult) {
                    String title = actionResult.getParameter("title").getString();
                    if (title != null && !title.isEmpty()) {
                        String desc = actionResult.getParameter("desc", new Value("")).getString();
                        String timeRange = actionResult.getParameter("timeRange").getString();
                        DSAEvent event = new DSAEvent(title);
                        event.setDescription(desc);
                        try {
                            String[] dates = timeRange.split("/", 2);
                            if (dates.length != 2) {
                                throw new Exception("Unexpected dates length");
                            }
                            Date startDate = BaseCalendar.DATE_FORMAT.parse(dates[0]);
                            Date endDate = BaseCalendar.DATE_FORMAT.parse(dates[1]);
                            event.setStart(startDate);
                            event.setEnd(endDate);
                            actionResult.getNode().getParent().setDisplayName(title);
                            actionResult.getNode().getParent().getChild("description").setValue(new Value(desc));
                            actionResult.getNode().getParent().getChild("start").setValue(new Value(dates[0]));
                            actionResult.getNode().getParent().getChild("end").setValue(new Value(dates[1]));
                            calendar.deleteEvent(actionResult.getNode().getParent().getName(), false);
                            calendar.createEvent(event);
                            actionResult.getTable().addRow(Row.make(new Value(true)));
                        } catch (Exception e) {
                            e.printStackTrace();
                            actionResult.getTable().addRow(Row.make(new Value(false)));
                        }
                    }
                }
            });
            addParameter(new Parameter("title", ValueType.STRING));
            addParameter(new Parameter("desc", ValueType.STRING));
            Parameter parameter = new Parameter("timeRange", ValueType.TIME);
            parameter.setEditorType(EditorType.DATE_RANGE);
            addParameter(parameter);

            addResult(new Parameter("success", ValueType.BOOL));
        }
    }

    private static class RemoveEvent extends Action {
        public RemoveEvent(final BaseCalendar calendar, final String uid) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    calendar.deleteEvent(uid, false);
                }
            });
        }
    }
}
