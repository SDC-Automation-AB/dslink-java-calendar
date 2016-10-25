package org.dsa.iot.calendar.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TimeSlotTable {
    private List<TimeRange> table = new ArrayList<>();

    public void mergeSlot(TimeRange toMerge) {
        if (table.isEmpty()) {
            table.add(toMerge);

            return;
        }

        if (table.contains(toMerge)) {
            return;
        }

        boolean wasAdded = false;
        for (TimeRange slot : table) {
            if (TimeRange.areOverlapping(toMerge, slot)) {
                Instant start = toMerge.start.isBefore(slot.start) ? toMerge.start : slot.start;
                Instant end = toMerge.end.isAfter(slot.end) ? toMerge.end : slot.end;

                slot.start = start;
                slot.end = end;
                wasAdded = true;
                break;
            } else if (TimeRange.areContiguous(toMerge, slot)) {
                Instant start = slot.start.isBefore(toMerge.start) ? slot.start : toMerge.start;
                Instant end = slot.start.isBefore(toMerge.start) ? toMerge.end : slot.end;

                slot.start = start;
                slot.end = end;
                wasAdded = true;
                break;
            }
        }

        if (!wasAdded) {
            table.add(toMerge);
        }

        table.sort(Comparator.comparing(o -> o.start));
        stabilizeTable();
    }

    /* This is meant to re-merge every slots until the stable is stabilized */
    private void stabilizeTable() {
        int previousTableSize;
        do {
            previousTableSize = table.size();
            for (Iterator<TimeRange> it1 = table.iterator(); it1.hasNext(); ) {
                TimeRange range = it1.next();
                for (TimeRange slot : table) {
                    if (range == slot) {
                        continue;
                    }

                    if (TimeRange.areOverlapping(range, slot)) {
                        Instant start = range.start.isBefore(slot.start) ? range.start : slot.start;
                        Instant end = range.end.isAfter(slot.end) ? range.end : slot.end;

                        slot.start = start;
                        slot.end = end;
                        it1.remove();
                        break;
                    } else if (TimeRange.areContiguous(range, slot)) {
                        Instant start = slot.start.isBefore(range.start) ? slot.start : range.start;
                        Instant end = slot.start.isBefore(range.start) ? range.end : slot.end;

                        slot.start = start;
                        slot.end = end;
                        it1.remove();
                        break;
                    }
                }
            }
        } while (previousTableSize != table.size());

        table.sort(Comparator.comparing(o -> o.start));
    }

    public List<TimeRange> getTable() {
        return table;
    }
}
