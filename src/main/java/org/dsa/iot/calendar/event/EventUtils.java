package org.dsa.iot.calendar.event;

import java.time.*;
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

        TimeSlotTable timeSlots = new TimeSlotTable();
        for (DSAEvent event : eventsNotOver) {
            timeSlots.mergeSlot(new TimeRange(event.getStart(), event.getEnd()));
        }

        List<TimeRange> table = timeSlots.getTable();

        for (int i = 0; i < table.size(); ++i) {
            TimeRange current = table.get(i);

            if (i == 0 && now.plus(wantedDuration).compareTo(current.start) <= 0) {
                    return new TimeRange(now, current.start);
            }

            if (i == table.size() - 1) {
                return new TimeRange(current.end, current.end.plus(wantedDuration));
            }
            TimeRange next = table.get(i + 1);

            Instant endOfFreeSpace = current.end.plus(wantedDuration);
            if (endOfFreeSpace.compareTo(next.start) > 0) {
                continue;
            } else {
                return new TimeRange(current.end, endOfFreeSpace);
            }
        }

        return new TimeRange(now, now.plus(wantedDuration));
    }

    // TODO: We needn't to assume the timezone from DGLux https://github.com/IOT-DSA/dslink-java-calendar/issues/15
    public static Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
