package uk.gov.hmcts.reform.bulkscanning.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ExcelGeneratorUtil {
    public static ByteArrayInputStream exportToExcel(ReportType reportType, List<ReportData> reportDataList) throws IOException {
        String[] cols_Data_Loss = {"Loss_Resp", "Payment_Asset_DCN", "Resp_Service ID", "Resp_Service Name", "Date_Banked", "BGC_Batch", "Payment_Method", "Amount"};
        String[] cols_Unprocessed = {"Resp_Service ID", "Resp_Service Name", "Exception_Ref", "CCD_Ref", "Date_Banked", "BGC_Batch", "Payment_Asset_DCN", "Payment_Method", "Amount"};
        try(
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
        ){
            CreationHelper createHelper = workbook.getCreationHelper();

            Sheet sheet = workbook.createSheet(reportType.toString());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Row for Header
            Row headerRow = sheet.createRow(0);

            // Header
            if(reportType.equals(ReportType.UNPROCESSED)){
                for (int col = 0; col < cols_Unprocessed.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(cols_Unprocessed[col]);
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
                    row.createCell(6).setCellValue(reportData.getPaymentAssetDcn());
                    row.createCell(7).setCellValue(reportData.getPaymentMethod());
                    row.createCell(8).setCellValue(reportData.getAmount().toString());
                }
            }
            else if(reportType.equals(ReportType.DATA_LOSS)){
                for (int col = 0; col < cols_Data_Loss.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(cols_Data_Loss[col]);
                    cell.setCellStyle(headerCellStyle);
                }
                int rowIdx = 1;
                for (ReportData reportData : reportDataList) {
                    Row row = sheet.createRow(rowIdx++);

                    row.createCell(0).setCellValue(reportData.getLossResp());
                    row.createCell(1).setCellValue(reportData.getPaymentAssetDcn());
                    row.createCell(2).setCellValue(reportData.getRespServiceId());
                    row.createCell(3).setCellValue(reportData.getRespServiceName());
                    row.createCell(4).setCellValue(reportData.getDateBanked());
                    row.createCell(5).setCellValue(reportData.getBgcBatch());
                    row.createCell(6).setCellValue(reportData.getPaymentMethod());
                    if(Optional.ofNullable(reportData.getAmount()).isPresent()){
                        row.createCell(7).setCellValue(reportData.getAmount().toString());
                    }
                }
            }

            // CellStyle for Age
            CellStyle ageCellStyle = workbook.createCellStyle();
            ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));



            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
