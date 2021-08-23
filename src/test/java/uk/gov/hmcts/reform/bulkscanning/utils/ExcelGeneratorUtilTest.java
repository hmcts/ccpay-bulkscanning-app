package uk.gov.hmcts.reform.bulkscanning.utils;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExcelGeneratorUtilTest {

    @Test
    public void testUtilityClassWithOneConstructor() {
        Constructor[] constructors = ExcelGeneratorUtil.class.getDeclaredConstructors();
        assertEquals("Utility class should only have one constructor",
                     1, constructors.length);
    }

    @Test
    public  void  testUtilityClassConstructorToBeInaccessible() {
        Constructor[] constructors = ExcelGeneratorUtil.class.getDeclaredConstructors();
        assertFalse("Utility class constructor should be inaccessible",
                    constructors[0].isAccessible());
    }

    @Test
    public void testExportUnprocessedToExcel() throws IOException {
        List<ReportData> reportDataList = new ArrayList<>();
        ReportData reportData = ReportData.recordWith()
                                    .lossResp("loss-resp")
                                     .paymentAssetDcn("payment-asset-dcn")
                                    .respServiceId("resp-serv-id")
                                    .respServiceName("resp-serv-name")
                                    .dateBanked("2020-01-02")
                                    .bgcBatch("bgc-batch")
                                    .paymentMethod("payment-method")
                                    .exceptionRef("exception-ref")
                                    .ccdRef("ccd-ref")
                                    .amount(BigDecimal.valueOf(100)).build();
        reportDataList.add(reportData);
        Workbook actual = ExcelGeneratorUtil.exportToExcel(ReportType.UNPROCESSED,reportDataList);
        String actualResp = actual.getSheet("UNPROCESSED").getRow(1).getCell(0).getStringCellValue();
        String actualResServName = actual.getSheet("UNPROCESSED").getRow(1).getCell(1).getStringCellValue();
        assertEquals("should be equal to resp-serv-id","resp-serv-id",actualResp);
        assertEquals("should be equal to resp-serv-name","resp-serv-name",actualResServName);
    }

    @Test
    public void testExportDataLossToExcel() throws IOException {
        List<ReportData> reportDataList = new ArrayList<>();
        ReportData reportData = ReportData.recordWith()
            .lossResp("loss-resp")
            .paymentAssetDcn("payment-asset-dcn")
            .respServiceId("resp-serv-id")
            .respServiceName("resp-serv-name")
            .dateBanked("2020-01-02")
            .bgcBatch("bgc-batch")
            .paymentMethod("payment-method")
            .exceptionRef("exception-ref")
            .ccdRef("ccd-ref")
            .amount(BigDecimal.valueOf(100)).build();
        reportDataList.add(reportData);
        Workbook actual = ExcelGeneratorUtil.exportToExcel(ReportType.DATA_LOSS,reportDataList);
        String actuallossResp = actual.getSheet("DATA_LOSS").getRow(1).getCell(0).getStringCellValue();
        String actualPaymentAssetDcn = actual.getSheet("DATA_LOSS").getRow(1).getCell(1).getStringCellValue();
        assertEquals("should be equal to loss-resp","loss-resp",actuallossResp);
        assertEquals("should be equal to payment-asset-dcn","payment-asset-dcn",actualPaymentAssetDcn);
    }



}
