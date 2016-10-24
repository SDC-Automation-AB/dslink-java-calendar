package org.dsa.iot.calendar.event;

import java.time.*;
import java.util.Date;
import java.util.List;

public class EventUtils {
    protected static Clock clock = Clock.systemDefaultZone();

    public static TimeRange findNextFreeTimeRange(List<DSAEvent> events, Duration duration) {
        if (events.isEmpty()) {
            Instant now = Instant.now(clock);
            return new TimeRange(now, now.plus(duration));
        }
        Instant end = events.get(0).getEnd();
        return new TimeRange(end, end.plus(duration));
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // TODO: We needn't to assume the timezone from DGLux https://github.com/IOT-DSA/dslink-java-calendar/issues/15
    public static Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
