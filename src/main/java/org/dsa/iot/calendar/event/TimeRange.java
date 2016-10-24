package org.dsa.iot.calendar.event;

import java.time.Instant;

public class TimeRange {
    public Instant start;
    public Instant end;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeRange timeRange = (TimeRange) o;

        if (start != null ? !start.equals(timeRange.start) : timeRange.start != null) return false;
        return end != null ? end.equals(timeRange.end) : timeRange.end == null;
    }

    @Override
    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }

    public TimeRange(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

    public static boolean isOverlap(TimeRange t1, TimeRange t2) {
        if (rangeIsSame(t1, t2)) {
            return true;
        }

        return t1.isIncludedIn(t2) || t2.isIncludedIn(t1);

    }

    private boolean isIncludedIn(TimeRange other) {
        return (start.equals(other.start) || start.isAfter(other.start))
                && (end.equals(other.end) || end.isBefore(other.end));
    }

    private static boolean rangeIsSame(TimeRange t1, TimeRange t2) {
        return t1.start.equals(t2.start) && t1.end.equals(t2.end);
    }
}
