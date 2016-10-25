package org.dsa.iot.calendar.event;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSlotTableTest {
    private final TimeSlotTable timeSlotTable = new TimeSlotTable();

    @Test
    public void mergeSlot_addsSingleTimeRange() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T13:00"), getInstantFromString("2016-10-24T14:00"));

        timeSlotTable.mergeSlot(timeRange);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(13);
        assertThat(getHour(result.get(0).end)).isEqualTo(14);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_mergesOverlappingTimeRangesAsOne() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T13:00"), getInstantFromString("2016-10-24T14:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T13:30"), getInstantFromString("2016-10-24T15:00"));

        timeSlotTable.mergeSlot(timeRange);
        timeSlotTable.mergeSlot(timeRange2);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(13);
        assertThat(getHour(result.get(0).end)).isEqualTo(15);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_mergesContiguousRanges() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T13:00"), getInstantFromString("2016-10-24T14:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T14:00"), getInstantFromString("2016-10-24T15:00"));

        timeSlotTable.mergeSlot(timeRange);
        timeSlotTable.mergeSlot(timeRange2);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(13);
        assertThat(getHour(result.get(0).end)).isEqualTo(15);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_mergesContiguousRanges_inverted_case() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T13:00"), getInstantFromString("2016-10-24T14:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T14:00"), getInstantFromString("2016-10-24T15:00"));

        timeSlotTable.mergeSlot(timeRange2);
        timeSlotTable.mergeSlot(timeRange);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(13);
        assertThat(getHour(result.get(0).end)).isEqualTo(15);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_doesntMerge_whenTimesArentContiguousOrOverlapping() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T13:00"), getInstantFromString("2016-10-24T14:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T15:00"), getInstantFromString("2016-10-24T16:00"));

        timeSlotTable.mergeSlot(timeRange);
        timeSlotTable.mergeSlot(timeRange2);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(13);
        assertThat(getHour(result.get(0).end)).isEqualTo(14);
        assertThat(getHour(result.get(1).start)).isEqualTo(15);
        assertThat(getHour(result.get(1).end)).isEqualTo(16);
        assertThat(result).hasSize(2);
    }

    /*
    This is needed because if we have this specific case:
    T1 - 10:00-11:00
    T2 - 12:00-13:00

    ---

    T3 - 11:00-12:00

    It needs to be represented as one time range: 10:00-13:00
     */
    @Test
    public void mergeSlot_needsToMergeUntilTableIsStable() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T10:00"), getInstantFromString("2016-10-24T11:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T12:00"), getInstantFromString("2016-10-24T13:00"));
        TimeRange timeRange3 = new TimeRange(getInstantFromString("2016-10-24T11:00"), getInstantFromString("2016-10-24T12:00"));

        timeSlotTable.mergeSlot(timeRange);
        timeSlotTable.mergeSlot(timeRange2);
        timeSlotTable.mergeSlot(timeRange3);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(10);
        assertThat(getHour(result.get(0).end)).isEqualTo(13);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_multipleMerge_afterInsert() {
        TimeRange timeRange1 = new TimeRange(getInstantFromString("2016-10-24T10:00"), getInstantFromString("2016-10-24T11:00"));
        TimeRange timeRange2 = new TimeRange(getInstantFromString("2016-10-24T12:00"), getInstantFromString("2016-10-24T13:00"));
        TimeRange timeRange3 = new TimeRange(getInstantFromString("2016-10-24T14:00"), getInstantFromString("2016-10-24T15:00"));
        TimeRange timeRange4 = new TimeRange(getInstantFromString("2016-10-24T10:30"), getInstantFromString("2016-10-24T14:30"));

        timeSlotTable.mergeSlot(timeRange1);
        timeSlotTable.mergeSlot(timeRange2);
        timeSlotTable.mergeSlot(timeRange3);
        timeSlotTable.mergeSlot(timeRange4);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(10);
        assertThat(getHour(result.get(0).end)).isEqualTo(15);
        assertThat(result).hasSize(1);
    }

    @Test
    public void mergeSlot_doesntMergeAnything_whenTimeRangeAlreadyExists() {
        TimeRange timeRange = new TimeRange(getInstantFromString("2016-10-24T10:00"), getInstantFromString("2016-10-24T11:00"));

        timeSlotTable.mergeSlot(timeRange);
        timeSlotTable.mergeSlot(timeRange);

        List<TimeRange> result = timeSlotTable.getTable();
        assertThat(getHour(result.get(0).start)).isEqualTo(10);
        assertThat(getHour(result.get(0).end)).isEqualTo(11);
        assertThat(result).hasSize(1);

    }

    private int getHour(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).getHour();
    }

    private Instant getInstantFromString(String dateTimeToParse) {
        return LocalDateTime.parse(dateTimeToParse).atZone(ZoneId.systemDefault()).toInstant();
    }
}
