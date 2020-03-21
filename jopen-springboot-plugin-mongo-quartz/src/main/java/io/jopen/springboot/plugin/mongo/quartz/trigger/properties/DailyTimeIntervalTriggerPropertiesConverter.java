package io.jopen.springboot.plugin.mongo.quartz.trigger.properties;

import io.jopen.springboot.plugin.mongo.quartz.trigger.TriggerPropertiesConverter;
import org.bson.Document;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.TimeOfDay;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.DateBuilder;

public class DailyTimeIntervalTriggerPropertiesConverter extends TriggerPropertiesConverter {

    private static final String TRIGGER_REPEAT_INTERVAL_UNIT = "repeatIntervalUnit";
    private static final String TRIGGER_REPEAT_INTERVAL = "repeatInterval";
    private static final String TRIGGER_TIMES_TRIGGERED = "timesTriggered";
    private static final String TRIGGER_START_TIME_OF_DAY = "startTimeOfDay";
    private static final String TRIGGER_END_TIME_OF_DAY = "endTimeOfDay";

    @Override
    protected boolean canHandle(OperableTrigger trigger) {
        return ((trigger instanceof DailyTimeIntervalTrigger)
                && !((DailyTimeIntervalTriggerImpl) trigger).hasAdditionalProperties());
    }

    @Override
    public Document injectExtraPropertiesForInsert(OperableTrigger trigger, Document original) {
        DailyTimeIntervalTriggerImpl t = (DailyTimeIntervalTriggerImpl) trigger;

        return new Document(original)
                .append(TRIGGER_REPEAT_INTERVAL_UNIT, t.getRepeatIntervalUnit().name())
                .append(TRIGGER_REPEAT_INTERVAL, t.getRepeatInterval())
                .append(TRIGGER_TIMES_TRIGGERED, t.getTimesTriggered())
                .append(TRIGGER_START_TIME_OF_DAY, toDocument(t.getStartTimeOfDay()))
                .append(TRIGGER_END_TIME_OF_DAY, toDocument(t.getEndTimeOfDay()));
    }

    private Document toDocument(TimeOfDay tod) {
        return new Document().
                append("hour", tod.getHour()).
                append("minute", tod.getMinute()).
                append("second", tod.getSecond());
    }

    @Override
    public void setExtraPropertiesAfterInstantiation(OperableTrigger trigger, Document stored) {
        DailyTimeIntervalTriggerImpl t = (DailyTimeIntervalTriggerImpl) trigger;

        String interval_unit = stored.getString(TRIGGER_REPEAT_INTERVAL_UNIT);
        if (interval_unit != null) {
            t.setRepeatIntervalUnit(DateBuilder.IntervalUnit.valueOf(interval_unit));
        }
        Integer repeatInterval = stored.getInteger(TRIGGER_REPEAT_INTERVAL);
        if (repeatInterval != null) {
            t.setRepeatInterval(repeatInterval);
        }
        Integer timesTriggered = stored.getInteger(TRIGGER_TIMES_TRIGGERED);
        if (timesTriggered != null) {
            t.setTimesTriggered(timesTriggered);
        }

        Document startTOD = (Document) stored.get(TRIGGER_START_TIME_OF_DAY);
        if (startTOD != null) {
            t.setStartTimeOfDay(fromDocument(startTOD));
        }
        Document endTOD = (Document) stored.get(TRIGGER_END_TIME_OF_DAY);
        if (endTOD != null) {
            t.setEndTimeOfDay(fromDocument(endTOD));
        }
    }

    private TimeOfDay fromDocument(Document tod) {
        return new TimeOfDay(tod.getInteger("hour"), tod.getInteger("minute"), tod.getInteger("second"));
    }
}
