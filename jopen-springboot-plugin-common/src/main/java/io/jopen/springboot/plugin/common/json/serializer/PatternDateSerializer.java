package io.jopen.springboot.plugin.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Date;

/**
 * @author maxuefeng
 */
@Deprecated
public class PatternDateSerializer extends JsonSerializer<Date> {

    /**
     * @param date
     * @param generator
     * @param provider
     * @throws IOException
     */
    @Override
    public void serialize(Date date, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeNumber(date.getTime());
    }
}
