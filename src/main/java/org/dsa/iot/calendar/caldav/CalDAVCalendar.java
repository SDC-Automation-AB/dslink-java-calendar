package org.dsa.iot.calendar.caldav;

import com.fasterxml.uuid.Generators;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Uid;
import org.apache.commons.httpclient.HostConfiguration;
import org.dsa.iot.calendar.BaseCalendar;
import org.dsa.iot.calendar.event.DSAEvent;
import org.dsa.iot.calendar.guest.DSAGuest;
import org.dsa.iot.dslink.node.Node;
import org.osaf.caldav4j.CalDAVCollection;
import org.osaf.caldav4j.CalDAVConstants;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.model.request.CalendarQuery;
import org.osaf.caldav4j.util.GenerateQuery;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CalDAVCalendar extends BaseCalendar {
    private final HttpClient httpClient;
    private final CalDAVCollection caldavCollection;

    public CalDAVCalendar(String host, int port, String path, Node eventsNode) {
        super(eventsNode);
        httpClient = new HttpClient();
        httpClient.getHostConfiguration().setHost(host, port, "http");
        caldavCollection = new CalDAVCollection(
                path,
                (HostConfiguration) httpClient.getHostConfiguration().clone(),
                new CalDAV4JMethodFactory(),
                CalDAVConstants.PROC_ID_DEFAULT
        );
        startUpdateLoop();
    }

    @Override
    public void createEvent(DSAEvent event) {
        java.util.Date start = Date.from(event.getStart());
        java.util.Date end = Date.from(event.getEnd());
        VEvent vEvent = new VEvent(new Date(start), new Date(end), event.getTitle());
        if (event.getUniqueId() != null) {
            // Add existing unique identifier.
            vEvent.getProperties().add(new Uid(event.getUniqueId()));
        } else {
            // Generate a new unique identifier.
            vEvent.getProperties().add(new Uid(Generators.timeBasedGenerator().generate().toString()));
        }
        vEvent.getProperties().add(new Description(event.getDescription()));
        vEvent.getProperties().add(new Location(event.getLocation()));
        for (DSAGuest guest : event.getGuests()) {
            Attendee attendee = new Attendee(URI.create("mailto:" + guest.getEmail()));
            attendee.getParameters().add(new Cn(guest.getDisplayName()));
            vEvent.getProperties().add(attendee);
        }
        VTimeZone vTimeZone = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(event.getTimeZone()).getVTimeZone();
        try {
            caldavCollection.add(httpClient, vEvent, vTimeZone);
        } catch (CalDAV4JException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEvent(String uid, boolean destroyNode) {
        try {
            // This throws an exception, but actually works.
            caldavCollection.delete(httpClient, Component.VEVENT, uid);
        } catch (CalDAV4JException ignored) {
        }
        if (destroyNode) {
            eventsNode.removeChild(uid);
        }
    }

    @Override
    public List<DSAEvent> getEvents() {
        List<DSAEvent> events = new ArrayList<>();
        try {
            GenerateQuery genQuery = new GenerateQuery();
            CalendarQuery query = genQuery.generate();
            List<Calendar> calendars = caldavCollection.queryCalendars(httpClient, query);
            for (Calendar calendar : calendars) {
                String timeZone;
                VTimeZone vTimeZone = (VTimeZone) calendar.getComponent(Component.VTIMEZONE);
                if (vTimeZone != null) {
                    timeZone = vTimeZone.getTimeZoneId().getValue();
                } else {
                    timeZone = TimeZone.getDefault().getID();
                }
                ComponentList componentList = calendar.getComponents().getComponents(Component.VEVENT);
                for (VEvent vEvent : (Iterable<VEvent>) componentList) {
                    if (vEvent.getUid() == null || vEvent.getSummary() == null) {
                        continue;
                    }
                    if (vEvent.getStartDate() == null || vEvent.getEndDate() == null) {
                        throw new IllegalArgumentException("Start or end date can not be null.");
                    }
                    DSAEvent event = new DSAEvent(
                            vEvent.getSummary().getValue(),
                            vEvent.getStartDate().getDate().toInstant(),
                            vEvent.getEndDate().getDate().toInstant()
                    );
                    event.setUniqueId(vEvent.getUid().getValue());
                    if (vEvent.getDescription() != null) {
                        event.setDescription(vEvent.getDescription().getValue());
                    }
                    if (vEvent.getLocation() != null) {
                        event.setLocation(vEvent.getLocation().getValue());
                    }
                    for (Object prop : vEvent.getProperties()) {
                        if (prop instanceof Attendee) {
                            DSAGuest guest = new DSAGuest();
                            guest.setDisplayName(((Attendee) prop).getName());
                            guest.setEmail(((Attendee) prop).getValue());
                            event.getGuests().add(guest);
                        }
                    }
                    event.setTimeZone(timeZone);
                    events.add(event);
                }
            }
        } catch (CalDAV4JException e) {
            e.printStackTrace();
        }

        return events;
    }
}
