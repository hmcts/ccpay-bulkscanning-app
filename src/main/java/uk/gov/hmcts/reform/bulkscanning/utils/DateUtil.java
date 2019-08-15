package uk.gov.hmcts.reform.bulkscanning.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public final class DateUtil {

    private DateUtil() {

    }

    public static Date localDateTimeToDate(LocalDateTime ldt) {
        return ldt != null ? Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return date != null ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()) : null;
    }
}
