package org.dsa.iot.calendar.event;

import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class EventUtilsTest {
    public static final Duration expectedDuration = Duration.of(10, ChronoUnit.HOURS);
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Before
    public void setUp() throws Exception {
        EventUtils.clock = clock;
    }

    @Test
    public void returns_now_when_no_events() {
        Instant now = Instant.now(clock);
        TimeRange expected = new TimeRange(now, now.plus(10, ChronoUnit.HOURS));

        TimeRange timeRange = EventUtils.findNextFreeTimeRange(new ArrayList<>(), EventUtilsTest.expectedDuration);

        assertThat(timeRange).isEqualTo(expected);
    }
}
