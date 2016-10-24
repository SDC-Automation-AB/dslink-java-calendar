package org.dsa.iot.calendar.event;

import org.junit.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeRangeTest {

    @Test
    public void overlaps_returns_false_when_different_times() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-22T10:15");
        Instant t2End = getInstantFromString("2016-10-22T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.isOverlap(t1, t2);

        assertThat(result).isFalse();
    }

    @Test
    public void overlaps_returns_true_when_same_times() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:15");
        Instant t2End = getInstantFromString("2016-10-24T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.isOverlap(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_returns_true_when_one_range_is_included_in_the_other() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:20");
        Instant t2End = getInstantFromString("2016-10-24T10:59");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.isOverlap(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_returns_true_when_one_range_is_included_in_the_other_edge_case1() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:20");
        Instant t2End = getInstantFromString("2016-10-24T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.isOverlap(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_returns_true_when_one_range_is_included_in_the_other_edge_case2() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:15");
        Instant t2End = getInstantFromString("2016-10-24T10:59");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.isOverlap(t1, t2);

        assertThat(result).isTrue();
    }

    private Instant getInstantFromString(String dateTimeToParse) {
        return LocalDateTime.parse(dateTimeToParse).atZone(ZoneId.systemDefault()).toInstant();
    }
}
