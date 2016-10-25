package org.dsa.iot.calendar.event;

import java.time.*;
import java.util.Date;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;

public class EventUtils {
    protected static Clock clock = Clock.systemDefaultZone();

    private EventUtils() {
    }

    public static TimeRange findNextFreeTimeRange(List<DSAEvent> events, Duration wantedDuration) {
        Instant now = Instant.now(clock);
        if (events.isEmpty()) {
            return new TimeRange(now, now.plus(wantedDuration));
        }

        List<DSAEvent> eventsNotOver = events
                .stream()
                .filter(event -> event.getEnd().isAfter(Instant.now(clock)))
                .sorted((o1, o2) -> {
                    if (MINUTES.between(o1.getStart(), now) > MINUTES.between(o2.getStart(), now)) {
                        return 1;
                    } else if (MINUTES.between(o1.getStart(), now) < MINUTES.between(o2.getStart(), now)) {
                        return -1;
                    } else {
                        return 0;
                    }
                }).collect(toList());

        for (int i = 0; i < eventsNotOver.size() - 1; ++i) {
            DSAEvent event = events.get(i);

            Duration freeRange = Duration.between(now, event.getStart());

            if (wantedDuration.compareTo(freeRange) <= 0) {
                return new TimeRange(now, now.plus(wantedDuration));
            }
        }

        return new TimeRange(now, now.plus(wantedDuration));
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // TODO: We needn't to assume the timezone from DGLux https://github.com/IOT-DSA/dslink-java-calendar/issues/15
    public static Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
