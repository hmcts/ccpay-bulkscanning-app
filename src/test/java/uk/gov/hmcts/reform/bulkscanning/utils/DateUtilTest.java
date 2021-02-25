package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateUtilTest {
    Date date;

    @Before
    public void setup() throws ParseException{
        String startString = "January 10, 2021";
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        date = format.parse(startString);
    }

    @Test
    public void testLocalDateTimeToDate()  {
        LocalDateTime localDateTime = DateUtil.dateToLocalDateTime(date);
        assertEquals("2021-01-10T00:00",localDateTime.toString());
    }

    @Test
    public void testGetDateForReportName(){
        String reportName = DateUtil.getDateForReportName(date);
        assertEquals("100121",reportName);
    }

    @Test
    public void testGetDateTimeForReportName(){
        String reportName = DateUtil.getDateTimeForReportName(date);
        assertEquals("100121_000000",reportName);
    }

    @Test
    public void testAtStartOfDay(){
        Date startOfDay = DateUtil.atStartOfDay(date);
        assertEquals("Sun Jan 10 00:00:00 GMT 2021",startOfDay.toString());
    }


}
