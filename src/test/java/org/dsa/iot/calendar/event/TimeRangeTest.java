package org.dsa.iot.calendar.event;

import org.junit.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeRangeTest {
    @Test
    public void areContiguous() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T11:00");
        Instant t2End = getInstantFromString("2016-10-24T12:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areContiguous(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void areNotContiguous() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T11:01");
        Instant t2End = getInstantFromString("2016-10-24T12:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areContiguous(t1, t2);

        assertThat(result).isFalse();
    }

    @Test
    public void doesnt_overlap_when_different_times() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-22T10:15");
        Instant t2End = getInstantFromString("2016-10-22T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isFalse();
    }

    @Test
    public void overlap_when_same_times() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:15");
        Instant t2End = getInstantFromString("2016-10-24T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_when_one_range_is_included_in_the_other() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:20");
        Instant t2End = getInstantFromString("2016-10-24T10:59");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_when_one_range_is_included_in_the_other_same_end_time() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:20");
        Instant t2End = getInstantFromString("2016-10-24T11:00");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_when_one_range_is_included_in_the_other_same_start_time() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:15");
        Instant t2End = getInstantFromString("2016-10-24T10:59");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_when_one_range_starts_before_the_other_ends() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:40");
        Instant t2End = getInstantFromString("2016-10-24T11:30");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t1, t2);

        assertThat(result).isTrue();
    }

    @Test
    public void overlaps_when_one_range_starts_before_the_other_ends_inverted_case() {
        Instant t1Start = getInstantFromString("2016-10-24T10:15");
        Instant t1End = getInstantFromString("2016-10-24T11:00");
        TimeRange t1 = new TimeRange(t1Start, t1End);

        Instant t2Start = getInstantFromString("2016-10-24T10:40");
        Instant t2End = getInstantFromString("2016-10-24T11:30");
        TimeRange t2 = new TimeRange(t2Start, t2End);

        boolean result = TimeRange.areOverlapping(t2, t1);

        assertThat(result).isTrue();
    }

    private Instant getInstantFromString(String dateTimeToParse) {
        return LocalDateTime.parse(dateTimeToParse).atZone(ZoneId.systemDefault()).toInstant();
    }
}
