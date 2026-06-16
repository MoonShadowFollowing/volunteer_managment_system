package edu.scau.vms.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一 LocalDateTime → "yyyy-MM-dd HH:mm:ss"，LocalDate → "yyyy-MM-dd"。
 * 覆盖 JSR310 默认的 ISO-8601 输出。
 */
@Configuration
public class JacksonConfig {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeCustomizer() {
        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern(DATE_PATTERN);

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFmt));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFmt));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFmt));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFmt));

        return builder -> builder.modulesToInstall(module);
    }
}
