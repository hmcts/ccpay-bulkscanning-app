package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class DateUtilTest {
    Date date;

    @Before
    public void setUp() throws ParseException {
        String startString = "January 10, 2021";
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        date = format.parse(startString);
    }

    @Test
    public void testLocalDateTimeToDate()  {
        LocalDateTime localDateTime = DateUtil.dateToLocalDateTime(date);
        assertThat(localDateTime.toString()).isEqualTo("2021-01-10T00:00");
    }

    @Test
    public void testGetDateForReportName() {
        String reportName = DateUtil.getDateForReportName(date);
        assertThat(reportName).isEqualTo("100121");
    }

    @Test
    public void testGetDateTimeForReportName() {
        String reportName = DateUtil.getDateTimeForReportName(date);
        assertThat(reportName).isEqualTo("100121_000000");
    }

}
