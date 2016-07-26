package org.dsa.iot.calendar;

import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.calendar.abstractions.DSAEvent;
import org.dsa.iot.calendar.abstractions.DSAIdentifier;
import org.dsa.iot.calendar.caldav.CalDAVCalendar;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                            if (calendar.supportsMultipleCalendars()) {
                                String calendar = actionResult.getParameter("calendar").getString();
                                int indexOfPipe = calendar.lastIndexOf('|');
                                String calTitle = calendar.substring(0, indexOfPipe);
                                String calUid = calendar.substring(indexOfPipe + 1);
                                event.setCalendar(new DSAIdentifier(calUid, calTitle));
                            }
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
            if (calendar.supportsMultipleCalendars()) {
                List<String> calendars = new ArrayList<>();
                for (DSAIdentifier id : calendar.getCalendars()) {
                    calendars.add(id.getTitle() + "|" + id.getUid());
                }
                addParameter(new Parameter("calendar", ValueType.makeEnum(calendars)));
            }

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
                            if (calendar.supportsMultipleCalendars()) {
                                event.setCalendar(new DSAIdentifier(actionResult.getNode().getParent().getChild("calendarId").getValue().getString(),
                                        actionResult.getNode().getParent().getChild("calendar").getValue().getString()));
                            }
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
                    calendar.updateCalendar();
                }
            });
        }
    }
}
