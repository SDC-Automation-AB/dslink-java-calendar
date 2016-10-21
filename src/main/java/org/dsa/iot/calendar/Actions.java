package org.dsa.iot.calendar;

import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.abstractions.DSAEvent;
import org.dsa.iot.calendar.abstractions.DSAIdentifier;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
import org.dsa.iot.calendar.ews.ExchangeCalendar;
import org.dsa.iot.calendar.google.GoogleCalendar;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.*;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.dsa.iot.calendar.CalendarHandler.CALENDARS;

public class Actions {
    private Actions() {
    }

    static Node addAddCalDavCalendarNode(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("addCalDavCalendar");
        builder.setDisplayName("Add CalDAV Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddCalDAVCalendar(superRoot));
        return builder.build();
    }

    static Node addAddGoogleCalendarNode(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("addGoogleCalendar");
        builder.setDisplayName("Add Google Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddGoogleCalendar(superRoot));
        return builder.build();
    }

    static Node addAddExchangeCalendarNode(Node superRoot) {
        NodeBuilder builder = superRoot.createChild("addExchangeCalendar");
        builder.setDisplayName("Add Exchange Calendar");
        builder.setSerializable(false);
        builder.setAction(new AddExchangeCalendar(superRoot));
        return builder.build();
    }

    static Node addRemoveCalendarNode(Node calendarNode) {
        NodeBuilder rmBuilder = calendarNode.createChild("removeAccount");
        rmBuilder.setDisplayName("Remove Account");
        rmBuilder.setSerializable(false);
        rmBuilder.setAction(new RemoveAccount());
        return rmBuilder.build();
    }

    static Node addRefreshCalendarNode(Node calendarNode) {
        NodeBuilder refreshBuilder = calendarNode.createChild("refreshCalendar");
        refreshBuilder.setDisplayName("Refresh Calendar");
        refreshBuilder.setSerializable(false);
        refreshBuilder.setAction(new RefreshBuilder(CALENDARS.get(calendarNode.getName())));
        return refreshBuilder.build();
    }

    static Node addCreateEventNode(Node calendarNode) {
        NodeBuilder createEventNode = calendarNode.createChild("createAnEvent");
        createEventNode.setDisplayName("Create Event");
        createEventNode.setSerializable(false);
        createEventNode.setAction(new CreateEvent(CALENDARS.get(calendarNode.getName())));
        return createEventNode.build();
    }

    public static Node addEditEventNode(Node eventNode) {
        NodeBuilder editEventNode = eventNode.createChild("editEvent");
        editEventNode.setDisplayName("Edit Event");
        editEventNode.setSerializable(false);
        editEventNode.setAction(new EditEvent(CALENDARS.get(eventNode.getParent().getParent().getName())));
        return editEventNode.build();
    }

    public static Node addDeleteEventNode(Node eventNode) {
        NodeBuilder deleteEventNode = eventNode.createChild("deleteEvent");
        deleteEventNode.setDisplayName("Delete Event");
        deleteEventNode.setSerializable(false);
        deleteEventNode.setAction(new RemoveEvent(CALENDARS.get(eventNode.getParent().getParent().getName()), eventNode.getName()));
        return deleteEventNode.build();
    }

    static Node addGetEventsRange(Node calendarNode) {
        NodeBuilder getEventsRange = calendarNode.createChild("getEventsRange");
        getEventsRange.setDisplayName("Get Events Range");
        getEventsRange.setSerializable(false);
        getEventsRange.setAction(new GetEvents(CALENDARS.get(calendarNode.getName())));
        return getEventsRange.build();
    }

    static Node addGetCalendars(Node calendarNode) {
        NodeBuilder getCalendars = calendarNode.createChild("getCalendars");
        getCalendars.setDisplayName("Get Calendars");
        getCalendars.setSerializable(false);
        getCalendars.setAction(new GetCalendars(CALENDARS.get(calendarNode.getName())));
        return getCalendars.build();
    }

    private static class AddCalDAVCalendar extends Action {
        static final int DEFAULT_PORT = 80;

        AddCalDAVCalendar(final Node superRoot) {
            super(Permission.CONFIG, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    String desc = "";
                    String username = "";
                    String password = "";
                    String host = "";
                    int port = DEFAULT_PORT;
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
                    CALENDARS.put(desc, cal);

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
        AddGoogleCalendar(final Node superRoot) {
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
                    eventsBuilder.build();

                    try {
                        GoogleCalendar cal = new GoogleCalendar(calendarNode, clientId, clientSecret);
                        CALENDARS.put(desc, cal);

                        Actions.addCreateEventNode(calendarNode);
                        Actions.addRemoveCalendarNode(calendarNode);
                        Actions.addRefreshCalendarNode(calendarNode);
                        Actions.addGetEventsRange(calendarNode);
                        Actions.addGetCalendars(calendarNode);
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
        AddExchangeCalendar(final Node superRoot) {
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

                    CALENDARS.put(desc, cal);

                    Actions.addCreateEventNode(calendarNode);
                    Actions.addRemoveCalendarNode(calendarNode);
                    Actions.addRefreshCalendarNode(calendarNode);

                    cal.startUpdateLoop();
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
        switch (str) {
            case "2007 SP1":
                return ExchangeVersion.Exchange2007_SP1;
            case "2010":
                return ExchangeVersion.Exchange2010;
            case "2010 SP1":
                return ExchangeVersion.Exchange2010_SP1;
            default:
                return ExchangeVersion.Exchange2010_SP2;
        }
    }

    private static class RemoveAccount extends Action {
        RemoveAccount() {
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
        RefreshBuilder(final BaseCalendar calendar) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    calendar.updateCalendar();
                }
            });
        }
    }

    private static class CreateEvent extends Action {
        CreateEvent(final BaseCalendar calendar) {
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
                            System.out.println(timeRange);
                            Date startDate = BaseCalendar.DATE_FORMAT.parse(dates[0]);
                            Date endDate = BaseCalendar.DATE_FORMAT.parse(dates[1]);
                            String location = actionResult.getParameter("location", new Value("")).getString();
                            event.setStart(startDate);
                            event.setEnd(endDate);
                            event.setLocation(location);
                            if (calendar.supportsMultipleCalendars()) {
                                String calendar = actionResult.getParameter("calendar").getString();
                                int indexOfPipe = calendar.lastIndexOf('|');
                                String calTitle = calendar.substring(0, indexOfPipe);
                                String calUid = calendar.substring(indexOfPipe + 1);
                                event.setCalendar(new DSAIdentifier(calUid, calTitle));
                            }
                            calendar.createEvent(event);
                            actionResult.getTable().addRow(Row.make(new Value("Event created.")));
                        } catch (Exception e) {
                            e.printStackTrace();
                            actionResult.getTable().addRow(Row.make(new Value("Error occurred: " + e.getMessage())));
                        }
                    }
                }
            });
            addParameter(new Parameter("title", ValueType.STRING));
            addParameter(new Parameter("desc", ValueType.STRING));
            addParameter(new Parameter("location", ValueType.STRING));
            Parameter parameter = new Parameter("timeRange", ValueType.TIME);
            parameter.setEditorType(EditorType.DATE_RANGE);
            addParameter(parameter);
            if (calendar.supportsMultipleCalendars()) {
                List<String> calendars = new ArrayList<>();
                for (DSAIdentifier id : calendar.getCalendars()) {
                    calendars.add(id.getTitle() + "|" + id.getUid());
                }
                addParameter(new Parameter("calendar", ValueType.makeEnum(calendars)));
            }

            addResult(new Parameter("success", ValueType.STRING));
        }
    }

    private static class EditEvent extends Action {
        EditEvent(final BaseCalendar calendar) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult actionResult) {
                    String title = actionResult.getParameter("title").getString();
                    if (title != null && !title.isEmpty()) {
                        String desc = actionResult.getParameter("desc", new Value("")).getString();
                        String timeRange = actionResult.getParameter("timeRange").getString();
                        String location = actionResult.getParameter("location", new Value("")).getString();
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
                            event.setLocation(location);
                            Node editEventNode = actionResult.getNode().getParent();
                            editEventNode.setDisplayName(title);
                            editEventNode.getChild("description").setValue(new Value(desc));
                            editEventNode.getChild("start").setValue(new Value(dates[0]));
                            editEventNode.getChild("end").setValue(new Value(dates[1]));
                            editEventNode.getChild("location").setValue(new Value(location));
                            if (calendar.supportsMultipleCalendars()) {
                                String calendarId = editEventNode.getChild("calendarId").getValue().getString();
                                event.setCalendar(new DSAIdentifier(calendarId,
                                        editEventNode.getChild("calendar").getValue().getString()));
                            }
                            calendar.deleteEvent(editEventNode.getName(), false);
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
            addParameter(new Parameter("location", ValueType.STRING));
            Parameter parameter = new Parameter("timeRange", ValueType.TIME);
            parameter.setEditorType(EditorType.DATE_RANGE);
            addParameter(parameter);

            addResult(new Parameter("success", ValueType.BOOL));
        }
    }

    private static class RemoveEvent extends Action {
        RemoveEvent(final BaseCalendar calendar, final String uid) {
            super(Permission.WRITE, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    calendar.deleteEvent(uid, true);
                }
            });
        }
    }

    private static class GetEvents extends Action {
        GetEvents(final BaseCalendar calendar) {
            super(Permission.READ, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult actionResult) {
                    try {
                        String timeRange = actionResult.getParameter("timeRange").getString();
                        String[] dates = timeRange.split("/", 2);
                        Date startDate = BaseCalendar.DATE_FORMAT.parse(dates[0]);
                        Date endDate = BaseCalendar.DATE_FORMAT.parse(dates[1]);
                        List<DSAEvent> events = calendar.getEventsInRange(startDate, endDate);
                        actionResult.getTable().setMode(Table.Mode.APPEND);
                        for (DSAEvent event : events) {
                            actionResult
                                    .getTable()
                                    .addRow(Row.make(
                                            new Value(event.getUniqueId()),
                                            new Value(event.getTitle()),
                                            new Value(event.getDescription()),
                                            new Value(BaseCalendar.DATE_FORMAT.format(event.getStart())),
                                            new Value(BaseCalendar.DATE_FORMAT.format(event.getEnd())),
                                            new Value(event.getTimeZone()),
                                            new Value(event.getCalendar().getTitle()),
                                            new Value(event.getCalendar().getUid()),
                                            new Value(event.getLocation())));
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
            Parameter parameter = new Parameter("timeRange", ValueType.TIME);
            parameter.setEditorType(EditorType.DATE_RANGE);
            addParameter(parameter);

            addResult(new Parameter("ID", ValueType.STRING));
            addResult(new Parameter("Title", ValueType.STRING));
            addResult(new Parameter("Description", ValueType.STRING));
            addResult(new Parameter("Start", ValueType.STRING));
            addResult(new Parameter("End", ValueType.STRING));
            addResult(new Parameter("TimeZone", ValueType.STRING));
            addResult(new Parameter("CalendarID", ValueType.STRING));
            addResult(new Parameter("CalendarTitle", ValueType.STRING));
            addResult(new Parameter("Location", ValueType.STRING));
            setResultType(ResultType.TABLE);
        }
    }

    private static class GetCalendars extends Action {
        GetCalendars(final BaseCalendar calendar) {
            super(Permission.READ, new Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    List<DSAIdentifier> calendars = calendar.getCalendars();
                    event.getTable().setMode(Table.Mode.APPEND);
                    for (DSAIdentifier cal : calendars) {
                        event.getTable().addRow(Row.make(new Value(cal.getUid()), new Value(cal.getTitle())));
                    }
                }
            });
            addResult(new Parameter("ID", ValueType.STRING));
            addResult(new Parameter("Title", ValueType.STRING));
            setResultType(ResultType.TABLE);
        }
    }
}
