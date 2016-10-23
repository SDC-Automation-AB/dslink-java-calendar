package org.dsa.iot.calendar.google;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import org.dsa.iot.calendar.BaseCalendar;
import org.dsa.iot.calendar.guest.DSAGuest;
import org.dsa.iot.calendar.DSAIdentifier;
import org.dsa.iot.calendar.event.DSAEvent;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.AUTHORIZATION_SERVER_URL;
import static com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants.TOKEN_SERVER_URL;

public class GoogleCalendar extends BaseCalendar {
    private static final int CREDENTIALS_EXPIRATION_TIMEOUT = 60;
    private String clientId;
    private String clientSecret;
    private HttpTransport httpTransport;
    private JsonFactory jsonGenerator;
    private Calendar calendar;
    private Credential credential;
    private final String userId;

    public GoogleCalendar(Node calendarNode, String clientId, String clientSecret) throws GeneralSecurityException, IOException {
        super(calendarNode.getChild("events"));
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        httpTransport = new NetHttpTransport();
        jsonGenerator = JacksonFactory.getDefaultInstance();
        userId = calendarNode.getName();
    }

    public void attemptAuthorize(final Node calendarNode) throws IOException {
        final AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                httpTransport,
                jsonGenerator,
                new GenericUrl(TOKEN_SERVER_URL),
                new ClientParametersAuthentication(clientId, clientSecret),
                clientId,
                AUTHORIZATION_SERVER_URL)
                .setScopes(Collections.singletonList(CalendarScopes.CALENDAR))
                .setDataStoreFactory(new FileDataStoreFactory(new File(userId))).build();
        authorizeExistingCredential(flow, userId);
        if (credential == null) {
            AuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
            url.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI);
            url.set("accessType", "offline");
            url.set("approvalPrompt", "force");
            final Node urlNode = calendarNode.createChild("googleLoginUrl")
                    .setDisplayName("Google Login URL")
                    .setSerializable(false)
                    .setValueType(ValueType.STRING)
                    .setValue(new Value(url.build()))
                    .build();
            final Node codeNode = calendarNode.createChild("googleLoginCode")
                    .setDisplayName("Google Login Code")
                    .setSerializable(false)
                    .setValueType(ValueType.STRING)
                    .setWritable(Writable.CONFIG)
                    .build();
            codeNode.getListener().setValueHandler(new Handler<ValuePair>() {
                @Override
                public void handle(ValuePair event) {
                    Value value = event.getCurrent();
                    if (value != null && value.getString() != null) {
                        try {
                            authorize(flow, value.getString());
                            calendarNode.removeChild(urlNode);
                            calendarNode.removeChild(codeNode);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void authorizeExistingCredential(AuthorizationCodeFlow flow, String userId) throws IOException {
        Credential newCredential = flow.loadCredential(userId);
        if (newCredential != null
                && (newCredential.getRefreshToken() != null
                || newCredential.getExpiresInSeconds() > CREDENTIALS_EXPIRATION_TIMEOUT)) {
            credential = newCredential;
            createCalendarConnection();
            startUpdateLoop();
        }
    }

    private void authorize(AuthorizationCodeFlow flow, String code) throws IOException {
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).execute();
        credential = flow.createAndStoreCredential(response, userId);
        createCalendarConnection();
        startUpdateLoop();
    }

    private void createCalendarConnection() {
        calendar = new Calendar.Builder(httpTransport,
                jsonGenerator,
                credential)
                .setApplicationName("dslink-java-calendar")
                .build();
    }

    @Override
    public void createEvent(DSAEvent event) {
        Event googleEvent = new Event();
        EventDateTime startEventDateTime = new EventDateTime();
        EventDateTime endEventDateTime = new EventDateTime();
        startEventDateTime.setDateTime(new DateTime(event.getStart()));
        startEventDateTime.setTimeZone("UTC");
        endEventDateTime.setDateTime(new DateTime(event.getEnd()));
        endEventDateTime.setTimeZone("UTC");
        googleEvent.setSummary(event.getTitle());
        googleEvent.setDescription(event.getDescription());
        googleEvent.setStart(startEventDateTime);
        googleEvent.setEnd(endEventDateTime);
        googleEvent.setLocation(event.getLocation());
        List<EventAttendee> attendees = new ArrayList<>();
        for (DSAGuest guest : event.getGuests()) {
            EventAttendee attendee = new EventAttendee();
            attendee.setId(guest.getUniqueId());
            attendee.setDisplayName(guest.getDisplayName());
            attendee.setEmail(guest.getEmail());
            attendee.setOrganizer(guest.isOrganizer());
            attendees.add(attendee);
        }
        googleEvent.setAttendees(attendees);
        try {
            Event submittedEvent = calendar.events().insert(event.getCalendar().getUid(), googleEvent).execute();
            event.setUniqueId(submittedEvent.getId());
            createEventNode(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEvent(String uid, boolean destroyNode) {
        try {
            String calendarId = eventsNode.getChild(uid).getChild("calendarId").getValue().getString();
            calendar.events().delete(calendarId, uid).execute();
            if (destroyNode) {
                eventsNode.removeChild(uid);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DSAEvent> getEvents() {
        List<DSAEvent> events = new ArrayList<>();
        try {
            CalendarList calendarList = calendar.calendarList().list().execute();
            for (CalendarListEntry listEntry : calendarList.getItems()) {
                for (Event event : calendar.events().list(listEntry.getId()).execute().getItems()) {
                    DSAEvent dsaEvent = new DSAEvent(event.getSummary());
                    dsaEvent.setUniqueId(event.getId());
                    dsaEvent.setDescription(event.getDescription());
                    dsaEvent.setLocation(event.getLocation());
                    dsaEvent.setCalendar(new DSAIdentifier(listEntry.getId(), listEntry.getSummary()));
                    if (event.getStart() != null) {
                        if (event.getStart().getDate() != null) {
                            dsaEvent.setStart(new Date(event.getStart().getDate().getValue()));
                        } else if (event.getStart().getDateTime() != null) {
                            dsaEvent.setStart(new Date(event.getStart().getDateTime().getValue()));
                        }
                    }
                    if (event.getEnd() != null) {
                        if (event.getEnd().getDate() != null) {
                            dsaEvent.setEnd(new Date(event.getEnd().getDate().getValue()));
                        } else if (event.getEnd().getDateTime() != null) {
                            dsaEvent.setEnd(new Date(event.getEnd().getDateTime().getValue()));
                        }
                    }
                    if (event.getAttendees() != null) {
                        for (EventAttendee attendee : event.getAttendees()) {
                            DSAGuest guest = new DSAGuest();
                            if (attendee.getId() != null) {
                                guest.setUniqueId(attendee.getId());
                            }
                            if (attendee.getDisplayName() != null) {
                                guest.setDisplayName(attendee.getDisplayName());
                            }
                            if (attendee.getEmail() != null) {
                                guest.setEmail(attendee.getEmail());
                            }
                            if (attendee.getOrganizer() != null) {
                                guest.setOrganizer(attendee.getOrganizer());
                            }
                            dsaEvent.getGuests().add(guest);
                        }
                    }
                    events.add(dsaEvent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }

    @Override
    public boolean supportsMultipleCalendars() {
        return true;
    }

    @Override
    public List<DSAIdentifier> getCalendars() {
        List<DSAIdentifier> calendars = new ArrayList<>();
        try {
            CalendarList calendarList = calendar.calendarList().list().execute();
            for (CalendarListEntry listEntry : calendarList.getItems()) {
                calendars.add(new DSAIdentifier(listEntry.getId(), listEntry.getSummary()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return calendars;
    }
}
