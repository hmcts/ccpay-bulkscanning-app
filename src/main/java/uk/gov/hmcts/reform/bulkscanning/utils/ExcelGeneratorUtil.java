package uk.gov.hmcts.reform.bulkscanning.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.apache.poi.ss.usermodel.IndexedColors.BLACK;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType.DATA_LOSS;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType.UNPROCESSED;

public final class ExcelGeneratorUtil {

    private ExcelGeneratorUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static Workbook exportToExcel(ReportType reportType, List<ReportData> reportDataList) throws IOException {
        String[] colsDataLoss = {"Loss_Resp", "Payment_Asset_dcn", "Resp_Service ID", "Resp_Service Name",
            "Date_Banked", "BGC_Batch", "Payment_Method", "Amount"};
        String[] colsUnprocessed = {"Resp_Service ID", "Resp_Service Name", "Exception_Ref", "CCD_Ref",
            "Date_Banked", "BGC_Batch", "Payment_Asset_dcn", "Payment_Method", "Amount"};
        try (Workbook workbook = new HSSFWorkbook()) {
            final CreationHelper createHelper = workbook.getCreationHelper();

            final Sheet sheet = workbook.createSheet(reportType.toString());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Row for Header
            Row headerRow = sheet.createRow(0);

            // Header
            if (reportType.equals(UNPROCESSED)) {
                buildReportUnprocessed(reportDataList, colsUnprocessed, sheet, headerCellStyle, headerRow);
            } else if (reportType.equals(DATA_LOSS)) {
                buildReportDataLoss(reportDataList, colsDataLoss, sheet, headerCellStyle, headerRow);
            }

            // CellStyle for Age
            CellStyle ageCellStyle = workbook.createCellStyle();
            ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));

            return workbook;
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    private static void buildReportDataLoss(List<ReportData> reportDataList, String[] colsDataLoss, Sheet sheet,
                                            CellStyle headerCellStyle, Row headerRow) {
        for (int col = 0; col < colsDataLoss.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(colsDataLoss[col]);
            cell.setCellStyle(headerCellStyle);
        }
        int rowIdx = 1;
        for (ReportData reportData : reportDataList) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(reportData.getLossResp());
            row.createCell(1).setCellValue(reportData.getPaymentAssetdcn());
            row.createCell(2).setCellValue(reportData.getRespServiceId());
            row.createCell(3).setCellValue(reportData.getRespServiceName());
            row.createCell(4).setCellValue(reportData.getDateBanked());
            row.createCell(5).setCellValue(reportData.getBgcBatch());
            row.createCell(6).setCellValue(reportData.getPaymentMethod());
            if (Optional.ofNullable(reportData.getAmount()).isPresent()) {
                row.createCell(7).setCellValue(reportData.getAmount().toString());
            }
        }
        for (int i = 0; i < colsDataLoss.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void buildReportUnprocessed(List<ReportData> reportDataList, String[] colsUnprocessed, Sheet sheet,
                                               CellStyle headerCellStyle, Row headerRow) {
        for (int col = 0; col < colsUnprocessed.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(colsUnprocessed[col]);
            cell.setCellStyle(headerCellStyle);
        }
        int rowIdx = 1;
        for (ReportData reportData : reportDataList) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(reportData.getRespServiceId());
            row.createCell(1).setCellValue(reportData.getRespServiceName());
            row.createCell(2).setCellValue(reportData.getExceptionRef());
            row.createCell(3).setCellValue(reportData.getCcdRef());
            row.createCell(4).setCellValue(reportData.getDateBanked());
            row.createCell(5).setCellValue(reportData.getBgcBatch());
            row.createCell(6).setCellValue(reportData.getPaymentAssetdcn());
            row.createCell(7).setCellValue(reportData.getPaymentMethod());
            row.createCell(8).setCellValue(reportData.getAmount().toString());
        }
        for (int i = 0; i < colsUnprocessed.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
