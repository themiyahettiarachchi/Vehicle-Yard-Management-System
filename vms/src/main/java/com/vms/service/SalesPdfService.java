package com.vms.service;

import com.itextpdf.kernel.colors2.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.vms.model.Sale;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating PDF reports for the Sales module.
 */
@Service
public class SalesPdfService {

    private static final DeviceRgb HEADER_BG = new DeviceRgb(30, 41, 59); // Dark blue
    private static final DeviceRgb HEADER_TEXT = new DeviceRgb(255, 255, 255); // White
    private static final DeviceRgb ROW_ALT_BG = new DeviceRgb(241, 245, 249); // Light gray
    private static final DeviceRgb ACCENT_GREEN = new DeviceRgb(34, 197, 94); // Green
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(226, 232, 240); // Border

    /**
     * Generate a PDF report of all sales records.
     */
    public byte[] generateSalesReport(List<Sale> sales, int totalSales,
            BigDecimal totalRevenue, int availableVehicles) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {// err
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate()); // Landscape for wide table
            Document document = new Document(pdfDoc, PageSize.A4.rotate());
            document.setMargins(30, 30, 30, 30);

            // === Title Section ===
            addTitle(document);

            // === Summary Section ===
            addSummary(document, totalSales, totalRevenue, availableVehicles);

            // === Sales Table ===
            addSalesTable(document, sales);

            // === Footer ===
            addFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addTitle(Document document) {
        Paragraph title = new Paragraph("Vehicle Yard Management System")
                .setFontSize(22)
                .setBold()
                .setFontColor(new DeviceRgb(30, 41, 59))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        document.add(title);

        Paragraph subtitle = new Paragraph("Sales Report")
                .setFontSize(16)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(subtitle);

        Paragraph date = new Paragraph("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a")))
                .setFontSize(9)
                .setFontColor(new DeviceRgb(148, 163, 184))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);
    }

    private void addSummary(Document document, int totalSales,
            BigDecimal totalRevenue, int availableVehicles) {
        // Summary in a 3-column table
        Table summaryTable = new Table(UnitValue.createPercentArray(3)).useAllAvailableWidth();
        summaryTable.setMarginBottom(20);

        summaryTable.addCell(createSummaryCell("Total Sales", String.valueOf(totalSales)));
        summaryTable.addCell(createSummaryCell("Total Revenue",
                "$" + String.format("%,.0f", totalRevenue)));
        summaryTable.addCell(createSummaryCell("Available Vehicles", String.valueOf(availableVehicles)));

        document.add(summaryTable);
    }

    private Cell createSummaryCell(String label, String value) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(12)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(label)
                .setFontSize(9)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setMarginBottom(4));

        cell.add(new Paragraph(value)
                .setFontSize(18)
                .setBold()
                .setFontColor(new DeviceRgb(30, 41, 59)));

        return cell;
    }

    private void addSalesTable(Document document, List<Sale> sales) {
        // Column widths: Vehicle, Buyer Type, Customer, Contact, Sale Price, Date,
        // Status
        float[] columnWidths = { 3f, 1.5f, 2.5f, 2f, 2f, 1.8f, 1.2f };
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

        // Table Header
        String[] headers = { "Vehicle", "Buyer Type", "Customer", "Contact", "Sale Price", "Date", "Status" };
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .setBackgroundColor(HEADER_BG)
                    .setPadding(10)
                    .setBorder(Border.NO_BORDER);
            headerCell.add(new Paragraph(header)
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(HEADER_TEXT));
            table.addHeaderCell(headerCell);
        }

        // Table Body
        if (sales == null || sales.isEmpty()) {
            Cell noDataCell = new Cell(1, 7)
                    .setPadding(30)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
            noDataCell.add(new Paragraph("No sales records found.")
                    .setFontSize(11)
                    .setFontColor(new DeviceRgb(148, 163, 184)));
            table.addCell(noDataCell);
        } else {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/dd/yyyy");
            for (int i = 0; i < sales.size(); i++) {
                Sale sale = sales.get(i);
                boolean isAlternate = (i % 2 != 0);

                // Vehicle
                String vehicleName = sale.getVehicle() != null ? sale.getVehicle().getDisplayName() : "N/A";
                String chassisNumber = sale.getVehicle() != null ? sale.getVehicle().getPlateNumber() : "";
                table.addCell(createBodyCell(vehicleName + "\n" + (chassisNumber != null ? chassisNumber : ""),
                        isAlternate, false));

                // Buyer Type
                table.addCell(createBodyCell(
                        sale.getBuyerType() != null ? sale.getBuyerType() : "N/A",
                        isAlternate, false));

                // Customer
                table.addCell(createBodyCell(
                        sale.getCustomerName() != null ? sale.getCustomerName() : "N/A",
                        isAlternate, false));

                // Contact
                table.addCell(createBodyCell(
                        sale.getContactNumber() != null ? sale.getContactNumber() : "N/A",
                        isAlternate, false));

                // Sale Price
                String priceStr = sale.getSalePrice() != null
                        ? "$" + String.format("%,.0f", sale.getSalePrice())
                        : "$0";
                table.addCell(createBodyCell(priceStr, isAlternate, true));

                // Date
                String dateStr = sale.getSaleDate() != null
                        ? sale.getSaleDate().format(dtf)
                        : "N/A";
                table.addCell(createBodyCell(dateStr, isAlternate, false));

                // Status
                table.addCell(createBodyCell(
                        sale.getStatus() != null ? sale.getStatus() : "N/A",
                        isAlternate, false));
            }
        }

        document.add(table);
    }

    private Cell createBodyCell(String text, boolean isAlternate, boolean isGreen) {
        Cell cell = new Cell()
                .setPadding(8)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));

        if (isAlternate) {
            cell.setBackgroundColor(ROW_ALT_BG);
        }

        Paragraph para = new Paragraph(text).setFontSize(9);
        if (isGreen) {
            para.setFontColor(ACCENT_GREEN).setBold();
        } else {
            para.setFontColor(new DeviceRgb(51, 65, 85));
        }

        cell.add(para);
        return cell;
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph(
                "This report was auto-generated by Vehicle Yard Management System. " +
                        "For questions, contact the Sales Department.")
                .setFontSize(8)
                .setFontColor(new DeviceRgb(148, 163, 184))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footer);
    }
}
