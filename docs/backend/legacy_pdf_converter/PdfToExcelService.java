package com.g9latam.team62.fintech_api.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfToExcelService {

    private static final Pattern ACCOUNT_LINE_PATTERN = Pattern.compile("^CARTOLA CUENTARUT\\s+N°\\s+(\\d+)$");
    private static final Pattern CLIENT_LINE_PATTERN = Pattern.compile(
            "^(?<name>.+?)\\s+(?<rut>\\d{1,2}\\.\\d{3}\\.\\d{3}-[\\dkK])\\s+(?<date>\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})$");
    private static final Pattern CARTOLA_SELECTED_PATTERN = Pattern
            .compile("^Cartola Seleccionada\\s+(?<cartola>\\d+)\\s+(?<date>\\d{2}/\\d{2}/\\d{4})$");
    private static final Pattern CARTOLA_DETAIL_LINE_PATTERN = Pattern
            .compile("^N° Cartola\\s+(?<cartola>\\d+)\\s+Fecha Emisión\\s+(?<date>\\d{2}/\\d{2}/\\d{4})$");
    private static final Pattern MOVEMENTS_COUNT_PATTERN = Pattern
            .compile("^N° de Movimientos\\s+(?<count>\\d+)\\s+Saldo Anterior \\$\\s+(?<balance>[\\d.]+)$");
    private static final Pattern DATE_RANGE_PATTERN = Pattern
            .compile("^Fecha Inicio\\s+(?<start>\\d{2}/\\d{2}/\\d{4})\\s+Fecha Final\\s+(?<end>\\d{2}/\\d{2}/\\d{4})$");
    private static final Pattern BALANCE_ONLY_PATTERN = Pattern
            .compile("^\\$?(?<amount>[\\d.]+)\\s+\\$?(?<balance>[\\d.]+)$");
    private static final Pattern DATE_OPERATION_PATTERN = Pattern
            .compile("^(?<date>\\d{2}/[A-Za-z]{3})\\s+(?<operation>\\d{6,7})\\s+(?<rest>.*)$");
    private static final Pattern INLINE_MOVEMENT_PATTERN = Pattern
            .compile("^(?<description>.*?)(?:\\s+\\$?(?<amount>[\\d.]+))\\s+\\$?(?<balance>[\\d.]+)$");

    public byte[] convertPdfToExcel(MultipartFile pdfFile) throws IOException {
        validatePdfFile(pdfFile);

        ParsedCartola parsedCartola = extractCartolaFromPdf(pdfFile);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(parsedCartola.sheetName);
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setWrapText(true);
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);

            int maxColumns = 0;
            for (int rowIndex = 0; rowIndex < parsedCartola.rows.size(); rowIndex++) {
                List<Object> rowValues = parsedCartola.rows.get(rowIndex);
                maxColumns = Math.max(maxColumns, rowValues.size());

                Row row = sheet.createRow(rowIndex);
                for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
                    Cell cell = row.createCell(columnIndex);
                    writeCellValue(cell, rowValues.get(columnIndex));
                    cell.setCellStyle(cellStyle);
                }
            }

            for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                int currentWidth = sheet.getColumnWidth(columnIndex);
                sheet.setColumnWidth(columnIndex, Math.min(currentWidth + 1024, 12000));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void validatePdfFile(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("El archivo PDF no puede estar vacío");
        }

        String contentType = pdfFile.getContentType();
        if (!org.springframework.http.MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
            throw new IllegalArgumentException("El archivo debe tener formato PDF");
        }
    }

    private ParsedCartola extractCartolaFromPdf(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String pdfText = stripper.getText(document);

            ParsedCartola parsedCartola = new ParsedCartola();
            parsedCartola.sheetName = "Cartola";
            String[] lines = pdfText.split("\\r?\\n");
            MovementBuffer pendingMovement = null;
            boolean movementHeaderAdded = false;

            for (String line : lines) {
                String normalizedLine = normalizeLine(line);
                if (normalizedLine.isBlank()) {
                    continue;
                }

                String collapsedLine = collapseSpaces(normalizedLine);

                if (isMovementHeaderFragment(collapsedLine)) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    continue;
                }

                if (isStructuralNoise(collapsedLine)) {
                    continue;
                }

                if (handleHeaderLine(parsedCartola, collapsedLine)) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    continue;
                }

                if (ACCOUNT_LINE_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher accountMatcher = ACCOUNT_LINE_PATTERN.matcher(collapsedLine);
                    if (accountMatcher.matches()) {
                        parsedCartola.rows.add(row("N° " + accountMatcher.group(1)));
                    }
                    continue;
                }

                if (CLIENT_LINE_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher clientMatcher = CLIENT_LINE_PATTERN.matcher(collapsedLine);
                    if (clientMatcher.matches()) {
                        parsedCartola.rows.add(row(clientMatcher.group("name"), null, clientMatcher.group("rut"),
                                clientMatcher.group("date")));
                    }
                    continue;
                }

                if (CARTOLA_SELECTED_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher matcher = CARTOLA_SELECTED_PATTERN.matcher(collapsedLine);
                    if (matcher.matches()) {
                        parsedCartola.rows.add(row("Cartola Seleccionada", null,
                                matcher.group("cartola") + " " + matcher.group("date")));
                    }
                    parsedCartola.sheetName = updateSheetName(parsedCartola.sheetName, collapsedLine);
                    continue;
                }

                if (CARTOLA_DETAIL_LINE_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher matcher = CARTOLA_DETAIL_LINE_PATTERN.matcher(collapsedLine);
                    if (matcher.matches()) {
                        parsedCartola.rows.add(
                                row("N° Cartola", matcher.group("cartola"), "Fecha Emisión", matcher.group("date")));
                        parsedCartola.sheetName = updateSheetName("Cartola N° " + matcher.group("cartola"),
                                collapsedLine);
                    }
                    continue;
                }

                if (MOVEMENTS_COUNT_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher matcher = MOVEMENTS_COUNT_PATTERN.matcher(collapsedLine);
                    if (matcher.matches()) {
                        parsedCartola.rows.add(List.of("N° de Movimientos", Integer.parseInt(matcher.group("count")),
                                "Saldo Anterior $", matcher.group("balance")));
                    }
                    continue;
                }

                if (DATE_RANGE_PATTERN.matcher(collapsedLine).matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    Matcher matcher = DATE_RANGE_PATTERN.matcher(collapsedLine);
                    if (matcher.matches()) {
                        parsedCartola.rows.add(
                                List.of("Fecha Inicio", matcher.group("start"), "Fecha Final", matcher.group("end")));
                    }
                    continue;
                }

                if (collapsedLine.startsWith("Total Giros $") || collapsedLine.startsWith("Total Depósitos $")
                        || collapsedLine.startsWith("Total Cargos $") || collapsedLine.startsWith("Total Abonos $")
                        || collapsedLine.startsWith("Saldo Final $")) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    parsedCartola.rows.add(parseTotalLine(collapsedLine));
                    continue;
                }

                if (collapsedLine.equalsIgnoreCase("Cliente")
                        || collapsedLine.equalsIgnoreCase("Movimientos")
                        || collapsedLine.equalsIgnoreCase("Saldo")
                        || collapsedLine.equalsIgnoreCase("Detalle de movimientos:")
                        || collapsedLine.equalsIgnoreCase("Detalle de Movimientos")) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    if (collapsedLine.toLowerCase(Locale.ROOT).startsWith("detalle de movimientos")) {
                        parsedCartola.rows.add(List.of("Detalle de Movimientos"));
                        if (!movementHeaderAdded) {
                            parsedCartola.rows
                                    .add(List.of("Fecha", "N° Operación", "Descripción", "Abonos", "Cargos", "Saldo"));
                            movementHeaderAdded = true;
                        }
                    } else {
                        parsedCartola.rows.add(List.of(toTitleCaseIfNeeded(collapsedLine)));
                    }
                    continue;
                }

                if (collapsedLine.startsWith("Subtotales $")) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    parsedCartola.rows.add(parseSubtotalLine(collapsedLine));
                    continue;
                }

                if (collapsedLine.startsWith("Notas:")) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }
                    parsedCartola.rows.add(List.of("Notas:"));
                    continue;
                }

                Matcher movementMatcher = DATE_OPERATION_PATTERN.matcher(collapsedLine);
                if (movementMatcher.matches()) {
                    if (pendingMovement != null) {
                        flushPendingMovement(parsedCartola.rows, pendingMovement);
                        pendingMovement = null;
                    }

                    if (!movementHeaderAdded) {
                        parsedCartola.rows
                                .add(List.of("Fecha", "N° Operación", "Descripción", "Abonos", "Cargos", "Saldo"));
                        movementHeaderAdded = true;
                    }

                    String date = movementMatcher.group("date");
                    String operation = movementMatcher.group("operation");
                    String rest = movementMatcher.group("rest");

                    pendingMovement = new MovementBuffer(date, operation);

                    Matcher inlineMatcher = INLINE_MOVEMENT_PATTERN.matcher(rest);
                    if (inlineMatcher.matches()) {
                        pendingMovement.appendDescription(inlineMatcher.group("description"));
                        pendingMovement.amount = inlineMatcher.group("amount");
                        pendingMovement.balance = inlineMatcher.group("balance");
                    } else {
                        pendingMovement.appendDescription(rest);
                    }
                    continue;
                }

                if (pendingMovement != null) {
                    Matcher balanceOnlyMatcher = BALANCE_ONLY_PATTERN.matcher(collapsedLine);
                    Matcher inlineMatcher = INLINE_MOVEMENT_PATTERN.matcher(collapsedLine);

                    if (pendingMovement.amount == null && balanceOnlyMatcher.matches()) {
                        pendingMovement.amount = balanceOnlyMatcher.group("amount");
                        pendingMovement.balance = balanceOnlyMatcher.group("balance");
                    } else if (pendingMovement.amount == null && inlineMatcher.matches()) {
                        pendingMovement.appendDescription(inlineMatcher.group("description"));
                        pendingMovement.amount = inlineMatcher.group("amount");
                        pendingMovement.balance = inlineMatcher.group("balance");
                    } else {
                        pendingMovement.appendDescription(collapsedLine);
                    }
                    continue;
                }

                parsedCartola.rows.add(List.of(collapsedLine));
            }

            if (pendingMovement != null) {
                flushPendingMovement(parsedCartola.rows, pendingMovement);
                pendingMovement = null;
            }

            if (parsedCartola.rows.isEmpty()) {
                parsedCartola.rows.add(List.of("No se encontró texto legible en el PDF"));
            }

            return parsedCartola;
        }
    }

    private String normalizeLine(String line) {
        return line == null ? "" : line.replace('\u00A0', ' ').trim();
    }

    private String collapseSpaces(String line) {
        return line == null ? "" : line.trim().replaceAll("\\s+", " ");
    }

    private boolean handleHeaderLine(ParsedCartola parsedCartola, String collapsedLine) {
        Matcher accountMatcher = ACCOUNT_LINE_PATTERN.matcher(collapsedLine);
        if (accountMatcher.matches()) {
            parsedCartola.rows.add(row("CARTOLA CUENTARUT "));
            parsedCartola.rows.add(row("N° " + accountMatcher.group(1)));
            return true;
        }

        if (collapsedLine.equalsIgnoreCase("CARTOLA CUENTARUT")) {
            parsedCartola.rows.add(row("CARTOLA CUENTARUT "));
            return true;
        }

        if (collapsedLine.equalsIgnoreCase("Nombre RUT Fecha y Hora")) {
            parsedCartola.rows.add(row("Nombre", null, "RUT", "Fecha Hora"));
            return true;
        }

        return false;
    }

    private List<Object> parseTotalLine(String collapsedLine) {
        String[] parts = collapsedLine.split(" ");
        if (parts.length < 4) {
            return row(collapsedLine);
        }

        String labelLeft = parts[0] + " " + parts[1] + " $";
        String value = parts[parts.length - 1];

        if (collapsedLine.startsWith("Saldo Final $")) {
            return row("Saldo Final $", value);
        }

        String secondLabel = collapsedLine.contains("Cargos") ? "Total Cargos $" : "Total Abonos $";
        if (collapsedLine.startsWith("Total Giros $")) {
            return row("Total Giros $", "", "Total Cargos $", value);
        }
        if (collapsedLine.startsWith("Total Depósitos $")) {
            return row("Total Depósitos $", "", "Total Abonos $", value);
        }

        return row(labelLeft, value, secondLabel, "");
    }

    private List<Object> parseSubtotalLine(String collapsedLine) {
        Matcher matcher = Pattern.compile("^Subtotales \\$?(?<abonos>[\\d.]+)\\s+\\$?(?<cargos>[\\d.]+)$")
                .matcher(collapsedLine);
        if (matcher.matches()) {
            return row(null, null, "Subtotales $", matcher.group("abonos"), matcher.group("cargos"), null);
        }

        return row(collapsedLine);
    }

    private void flushPendingMovement(List<List<Object>> rows, MovementBuffer pendingMovement) {
        if (pendingMovement == null) {
            return;
        }
        rows.add(buildMovementRow(pendingMovement.date, pendingMovement.operation, pendingMovement.getDescription(),
                pendingMovement.amount, pendingMovement.balance));
    }

    private boolean isMovementHeaderFragment(String collapsedLine) {
        String lowerLine = collapsedLine.toLowerCase(Locale.ROOT);
        if (lowerLine.equals("fecha n°")
                || lowerLine.equals("fecha")
                || lowerLine.equals("n°")
                || lowerLine.equals("operación")
                || lowerLine.equals("operacion")
                || lowerLine.equals("descripción")
                || lowerLine.equals("descripcion")
                || lowerLine.equals("abonos")
                || lowerLine.equals("cargos")
                || lowerLine.equals("saldo")
                || lowerLine.equals("descripción abonos cargos saldo")
                || lowerLine.equals("descripcion abonos cargos saldo")
                || lowerLine.equals("fecha n° descripción abonos cargos saldo")
                || lowerLine.equals("fecha n° descripcion abonos cargos saldo")
                || lowerLine.equals("fecha n° operación")
                || lowerLine.equals("fecha n° operacion")
                || lowerLine.equals("fecha n° operación descripción abonos cargos saldo")
                || lowerLine.equals("fecha n° operacion descripcion abonos cargos saldo")) {
            return true;
        }

        return (lowerLine.contains("fecha") || lowerLine.contains("descripci"))
                && (lowerLine.contains("abono") || lowerLine.contains("cargo") || lowerLine.contains("saldo"));
    }

    private boolean isStructuralNoise(String collapsedLine) {
        String lowerLine = collapsedLine.toLowerCase(Locale.ROOT);
        return lowerLine.equals("movimientos")
                || lowerLine.equals("saldo")
                || lowerLine.equals("cliente")
                || lowerLine.equals("detalle de movimientos:")
                || lowerLine.equals("detalle de movimientos")
                || lowerLine.equals("nombre")
                || lowerLine.equals("rut")
                || lowerLine.equals("fecha hora");
    }

    private List<Object> buildMovementRow(String date, String operation, String description, String amount,
            String balance) {
        String normalizedDescription = collapseSpaces(description);
        boolean isCredit = isCreditMovement(normalizedDescription);
        Object abonos = isCredit ? parseNumericValue(amount) : "";
        Object cargos = isCredit ? "" : parseNumericValue(amount);

        List<Object> row = new ArrayList<>(6);
        row.add(date);
        row.add(operation);
        row.add(normalizedDescription);
        row.add(abonos);
        row.add(cargos);
        row.add(balance == null ? "" : balance);
        return row;
    }

    private boolean isCreditMovement(String description) {
        String upperDescription = description.toUpperCase(Locale.ROOT);
        return upperDescription.startsWith("TEF DE")
                || upperDescription.startsWith("ABONO")
                || upperDescription.startsWith("DEPÓSITO")
                || upperDescription.startsWith("DEPOSITO");
    }

    private Object parseNumericValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.replace(".", "");
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= Integer.MAX_VALUE) {
                return (int) parsed;
            }

            return parsed;
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        cell.setCellValue(value.toString());
    }

    private String toTitleCaseIfNeeded(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        if (value.equalsIgnoreCase("detalle de movimientos:")) {
            return "Detalle de Movimientos";
        }

        return value;
    }

    private List<Object> row(Object... values) {
        List<Object> row = new ArrayList<>(values.length);
        for (Object value : values) {
            row.add(value);
        }

        return row;
    }

    private String updateSheetName(String currentSheetName, String collapsedLine) {
        Matcher matcher = CARTOLA_DETAIL_LINE_PATTERN.matcher(collapsedLine);
        if (matcher.matches()) {
            return "Cartola N° " + matcher.group("cartola");
        }

        return currentSheetName;
    }

    private static class ParsedCartola {
        private String sheetName;
        private final List<List<Object>> rows = new ArrayList<>();
    }

    private static class MovementBuffer {
        private final String date;
        private final String operation;
        private final StringBuilder descriptionBuilder = new StringBuilder();
        private String amount;
        private String balance;

        private MovementBuffer(String date, String operation) {
            this.date = date;
            this.operation = operation;
        }

        private void appendDescription(String text) {
            if (text == null || text.isBlank()) {
                return;
            }
            if (descriptionBuilder.length() > 0) {
                descriptionBuilder.append(" ");
            }
            descriptionBuilder.append(text.trim());
        }

        private String getDescription() {
            return descriptionBuilder.toString();
        }
    }
}
