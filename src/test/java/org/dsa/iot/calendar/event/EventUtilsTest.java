package org.dsa.iot.calendar.event;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EventUtilsTest {
    public static final Duration wantedDuration = Duration.of(10, ChronoUnit.HOURS);
    public static final String TITLE = "jlkfdsjalk";
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private Instant startOfEvent;
    private final Duration eventDuration = Duration.of(1, ChronoUnit.HOURS);

    @Before
    public void setUp() throws Exception {
        EventUtils.clock = clock;
        startOfEvent = Instant.now(clock);
    }

    @Test
    public void now_when_no_events() {
        TimeRange expectedResult = new TimeRange(startOfEvent, startOfEvent.plus(wantedDuration));

        TimeRange timeRange = EventUtils.findNextFreeTimeRange(new ArrayList<>(), wantedDuration);

        assertThat(timeRange).isEqualTo(expectedResult);
    }

    @Test
    public void end_of_current_event() {
        Instant endOfEvent = startOfEvent.plus(eventDuration);
        TimeRange expectedResult = new TimeRange(endOfEvent, endOfEvent.plus(wantedDuration));
        List<DSAEvent> events = Lists.newArrayList(new DSAEvent(TITLE, startOfEvent, startOfEvent.plus(eventDuration)));

        TimeRange timeRange = EventUtils.findNextFreeTimeRange(events, wantedDuration);

        assertThat(timeRange).isEqualTo(expectedResult);
    }
}
