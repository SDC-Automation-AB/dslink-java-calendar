package org.dsa.iot.calendar.event;

import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class EventUtilsTest {
    @Test
    public void returns_now_when_no_events() {
        TimeRange timeRange = EventUtils.findNextFreeTimeRange(new ArrayList<>(), Duration.of(10, ChronoUnit.HOURS));
    }

}