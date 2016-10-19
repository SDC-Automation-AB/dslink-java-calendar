package org.dsa.iot.calendar.caldav;

import com.fasterxml.uuid.Generators;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Uid;
import org.apache.commons.httpclient.HostConfiguration;
import org.dsa.iot.calendar.abstractions.DSAEvent;
import org.dsa.iot.calendar.abstractions.BaseCalendar;
import org.dsa.iot.dslink.node.Node;
import org.osaf.caldav4j.CalDAVCollection;
import org.osaf.caldav4j.CalDAVConstants;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.model.request.CalendarQuery;
import org.osaf.caldav4j.util.GenerateQuery;

import java.util.*;

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
        VEvent vEvent = new VEvent(new Date(event.getStart().getTime()), new Date(event.getEnd().getTime()), event.getTitle());
        if (event.getUniqueId() != null) {
            // Add existing unique identifier.
            vEvent.getProperties().add(new Uid(event.getUniqueId()));
        } else {
            // Generate a new unique identifier.
            vEvent.getProperties().add(new Uid(Generators.timeBasedGenerator().generate().toString()));
        }
        vEvent.getProperties().add(new Description(event.getDescription()));
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
                    DSAEvent event = new DSAEvent(
                            vEvent.getSummary().getValue()
                    );
                    event.setUniqueId(vEvent.getUid().getValue());
                    if (vEvent.getDescription() != null) {
                        event.setDescription(vEvent.getDescription().getValue());
                    }
                    if (vEvent.getStartDate() != null) {
                        event.setStart(vEvent.getStartDate().getDate());
                    }
                    if (vEvent.getEndDate() != null) {
                        event.setEnd(vEvent.getEndDate().getDate());
                    }
                    if (vEvent.getLocation() != null) {
                        event.setLocation(vEvent.getLocation().getValue());
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
