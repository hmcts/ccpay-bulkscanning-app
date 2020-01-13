package uk.gov.hmcts.reform.bulkscanning.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public final class DateUtil {

    private DateUtil() {

    }

    public static Date localDateTimeToDate(LocalDateTime ldt) {
        return ldt == null ? null : Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static String getDateForReportName(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("ddMMyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    public static String getDateTimeForReportName(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("ddMMyy_HHmmss");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }
}
