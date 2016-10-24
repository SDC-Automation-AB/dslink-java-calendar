package org.dsa.iot.calendar.ews;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.Attendee;
import microsoft.exchange.webservices.data.property.complex.ItemId;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.search.CalendarView;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import org.dsa.iot.calendar.Actions;
import org.dsa.iot.calendar.BaseCalendar;
import org.dsa.iot.calendar.event.DSAEvent;
import org.dsa.iot.calendar.guest.DSAGuest;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.EditorType;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode.HardDelete;
import static microsoft.exchange.webservices.data.core.enumeration.service.SendCancellationsMode.SendToNone;
import static microsoft.exchange.webservices.data.core.enumeration.service.calendar.AffectedTaskOccurrence.SpecifiedOccurrenceOnly;

public class ExchangeCalendar extends BaseCalendar {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeCalendar.class);

    private String email;
    private String password;
    private ExchangeVersion version;
    private String url;
    private boolean autoDiscover;

    private Node node;

    private ExchangeService service;
    private static final long ONE_YEAR_IN_MILLISECONDS = 31556952000L;

    public ExchangeCalendar(Node calendarNode, ExchangeVersion exchangeVersion, String email, String password) {
        super(calendarNode.getChild("events"));
        this.node = calendarNode;
        this.email = email;
        this.password = password;
        this.version = exchangeVersion;
        this.autoDiscover = true;

        setupService();
    }

    public ExchangeCalendar(Node calendarNode, ExchangeVersion exchangeVersion, String email, String password, String url) {
        this(calendarNode, exchangeVersion, email, password);
        this.url = url;
        this.autoDiscover = false;

        setupService();
    }

    private void setupService() {
        service = new ExchangeService(version);
        ExchangeCredentials credentials = new WebCredentials(email, password);
        service.setCredentials(credentials);

        if (!autoDiscover) {
            URI uri = null;
            if (url != null) {
                try {
                    uri = new URI(url);
                } catch (URISyntaxException e) {
                    uri = null;
                }
            }
            if (uri != null) {
                service.setUrl(uri);
            } else {
                LOGGER.error("Invalid URL");
            }
        } else {
            try {
                service.autodiscoverUrl(email, new IAutodiscoverRedirectionUrl() {

                    @Override
                    public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
                        try {
                            new URI(redirectionUrl);
                            return true;
                        } catch (URISyntaxException e) {
                            return false;
                        }
                    }

                });
                if (service.getUrl() != null) {
                    url = service.getUrl().toString();
                    node.setRoConfig("url", new Value(url));
                } else {
                    LOGGER.error("URL Autodiscovery Failed");
                }
            } catch (Exception e) {
                LOGGER.debug("Silented exception:", e);
            }
        }

        makeEditAction();
    }

    @Override
    public void createEvent(DSAEvent event) {
        Appointment appointment;
        try {
            appointment = new Appointment(service);
            appointment.setSubject(event.getTitle());
            appointment.setBody(MessageBody.getMessageBodyFromText(event.getDescription()));
            appointment.setStart(Date.from(event.getStart()));
            appointment.setEnd(Date.from(event.getEnd()));
            appointment.setLocation(event.getLocation());
            for (DSAGuest guest : event.getGuests()) {
                Attendee attendee = new Attendee();
                attendee.setName(guest.getDisplayName());
                attendee.setAddress(guest.getEmail());
                appointment.getRequiredAttendees().add(attendee);
            }
            appointment.save();
            String uid = appointment.getId().getUniqueId();
            event.setUniqueId(uid);
        } catch (Exception e) {
            LOGGER.debug("", e);
        }
    }

    @Override
    public void deleteEvent(String uid, boolean destroyNode) {
        try {
            ItemId itemId = ItemId.getItemIdFromString(uid);
            service.deleteItem(itemId, HardDelete, SendToNone, SpecifiedOccurrenceOnly);
        } catch (Exception e) {
            LOGGER.debug("", e);
        }

        if (destroyNode) {
            eventsNode.removeChild(uid);
        }

    }

    @Override
    public List<DSAEvent> getEvents() {
        List<DSAEvent> events = new ArrayList<>();
        FindItemsResults<Appointment> results = null;
        try {
            Date now = new Date();
            Date nextYear = new Date(System.currentTimeMillis() + ONE_YEAR_IN_MILLISECONDS);
            results = service.findAppointments(WellKnownFolderName.Calendar, new CalendarView(now, nextYear));
        } catch (Exception e) {
            LOGGER.debug("Silenced exception:", e);
        }

        if (results == null) {
            return events;
        }

        for (Appointment appointment : results) {
            try {
                DSAEvent event = new DSAEvent(appointment.getSubject(), appointment.getStart().toInstant(), appointment.getEnd().toInstant());
                try {
                    event.setDescription(appointment.getBody().toString());
                } catch (ServiceLocalException e) {
                    event.setDescription("");
                }
                event.setLocation(appointment.getLocation());
                for (Attendee attendee : appointment.getRequiredAttendees()) {
                    event.getGuests().add(exchangeToDSAGuest(attendee));
                }
                for (Attendee attendee : appointment.getOptionalAttendees()) {
                    event.getGuests().add(exchangeToDSAGuest(attendee));
                }
                event.setTimeZone("UTC");
                event.setUniqueId(appointment.getId().getUniqueId());
                events.add(event);
            } catch (ServiceLocalException e) {
                LOGGER.debug("", e);
            }
        }
        return events;
    }

    private DSAGuest exchangeToDSAGuest(Attendee attendee) {
        DSAGuest guest = new DSAGuest();
        if (attendee.getName() != null) {
            guest.setDisplayName(attendee.getName());
        }
        if (attendee.getAddress() != null) {
            guest.setEmail(attendee.getAddress());
        }
        return guest;
    }

    private void makeEditAction() {
        Action act = new Action(Permission.CONFIG, new Handler<ActionResult>() {
            @Override
            public void handle(ActionResult event) {
                handleEdit(event);
            }
        });

        Value version = node.getRoConfig("version");
        ValueType possibleVersions = ValueType.makeEnum("2007 SP1", "2010", "2010 SP1", "2010 SP2");
        act.addParameter(new Parameter("version", possibleVersions, version));

        act.addParameter(new Parameter("email", ValueType.STRING, new Value(email)));

        act.addParameter(new Parameter("password", ValueType.STRING, new Value(password))
                .setEditorType(EditorType.PASSWORD));

        act.addParameter(new Parameter("autoDiscoverUrl", ValueType.BOOL, new Value(autoDiscover)));
        act.addParameter(new Parameter("url", ValueType.STRING, new Value(url)));

        Node editNode = node.getChild("edit");
        if (editNode == null) {
            node.createChild("edit")
                    .setDisplayName("Edit")
                    .setAction(act)
                    .setSerializable(false)
                    .build();
        } else {
            editNode.setAction(act);
        }
    }

    private void handleEdit(ActionResult event) {
        String vers = node.getRoConfig("version").getString();
        if (event.getParameter("version") != null) {
            vers = event.getParameter("version").getString();
            version = Actions.parseExchangeVersion(vers);
        }
        if (event.getParameter("email") != null) {
            email = event.getParameter("email").getString();
        }
        if (event.getParameter("password") != null) {
            password = event.getParameter("password").getString();
        }
        if (event.getParameter("autoDiscoverUrl") != null) {
            autoDiscover = event.getParameter("host").getBool();
        }
        if (event.getParameter("url") != null) {
            url = event.getParameter("url").getString();
        }

        node.setRoConfig("version", new Value(vers));
        node.setRoConfig("email", new Value(email));
        node.setPassword(password.toCharArray());
        node.setRoConfig("autoDiscoverUrl", new Value(autoDiscover));
        node.setRoConfig("url", new Value(url));

        setupService();
    }
}
