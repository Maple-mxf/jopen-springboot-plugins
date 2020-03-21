package io.jopen.springboot.plugin.mongo.quartz.trigger;

import io.jopen.springboot.plugin.mongo.quartz.trigger.properties.CalendarIntervalTriggerPropertiesConverter;
import io.jopen.springboot.plugin.mongo.quartz.trigger.properties.CronTriggerPropertiesConverter;
import io.jopen.springboot.plugin.mongo.quartz.trigger.properties.DailyTimeIntervalTriggerPropertiesConverter;
import io.jopen.springboot.plugin.mongo.quartz.trigger.properties.SimpleTriggerPropertiesConverter;
import org.bson.Document;
import org.quartz.spi.OperableTrigger;

import java.util.Arrays;
import java.util.List;

/**
 * Converts trigger type specific properties.
 */
public abstract class TriggerPropertiesConverter {

    private static final List<TriggerPropertiesConverter> propertiesConverters = Arrays.asList(
            new SimpleTriggerPropertiesConverter(),
            new CalendarIntervalTriggerPropertiesConverter(),
            new CronTriggerPropertiesConverter(),
            new DailyTimeIntervalTriggerPropertiesConverter());

    /**
     * Returns properties converter for given trigger or null when not found.
     * @param trigger    a trigger instance
     * @return converter or null
     */
    public static TriggerPropertiesConverter getConverterFor(OperableTrigger trigger) {
        for (TriggerPropertiesConverter converter : propertiesConverters) {
            if (converter.canHandle(trigger)) {
                return converter;
            }
        }
        return null;
    }

    protected abstract boolean canHandle(OperableTrigger trigger);

    public abstract Document injectExtraPropertiesForInsert(OperableTrigger trigger, Document original);

    public abstract void setExtraPropertiesAfterInstantiation(OperableTrigger trigger, Document stored);
}
