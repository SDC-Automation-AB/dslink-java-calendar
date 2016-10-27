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

            if (i == 0 && hasEnoughFreeTimeInRange(now, current.start, wantedDuration)) {
                return new TimeRange(now, current.start);
            }

            if (isLastEventOfTable(table, i)) {
                return new TimeRange(current.end, current.end.plus(wantedDuration));
            }

            TimeRange next = table.get(i + 1);
            if (hasEnoughFreeTimeInRange(current.end, next.start, wantedDuration)) {
                return new TimeRange(current.end, current.end.plus(wantedDuration));
            }
        }

        return new TimeRange(now, now.plus(wantedDuration));
    }

    private static boolean isLastEventOfTable(List<TimeRange> table, int i) {
        return i == table.size() - 1;
    }

    private static boolean hasEnoughFreeTimeInRange(Instant from, Instant to, Duration wantedDuration) {
        return from.plus(wantedDuration).compareTo(to) <= 0;
    }

    // TODO: We needn't to assume the timezone from DGLux https://github.com/IOT-DSA/dslink-java-calendar/issues/15
    public static Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
