package com.veely.account.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelDictionaryImporter implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Environment env;

    @Override
    public void run(String... args) throws Exception {
        boolean enabled = Boolean.parseBoolean(env.getProperty("veely.import.enabled", "false"));
        if (!enabled) return;

        Path base = Path.of(env.getProperty("veely.import.baseDir", "import"));

        importAccounts(base.resolve(env.getProperty("veely.import.accounts", "Piano dei conti.XLSX")));
        importVatCodes(base.resolve(env.getProperty("veely.import.vat", "Codici IVA.XLSX")));
        importWithholdingTypes(base.resolve(env.getProperty("veely.import.withholding", "Tabella Ritenute.XLSX")));

        log.info("✅ Import dizionari completato.");
    }

    private void importAccounts(Path file) throws Exception {
        log.info("📥 Import Piano dei conti: {}", file.toAbsolutePath());
        try (InputStream is = Files.newInputStream(file); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sh = wb.getSheetAt(0);
            int inserted = 0;

            for (int r = 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String code = s(row, 0);
                String desc = s(row, 1);
                if (blank(code) || blank(desc)) continue;

                Integer level = calcLevel(code);

                jdbc.update("""
                    INSERT INTO accounts(code, description, level, active)
                    VALUES(?,?,?,true)
                    ON DUPLICATE KEY UPDATE description=VALUES(description), level=VALUES(level), active=true
                """, code, desc, level);

                inserted++;
            }
            log.info("✅ Piano dei conti importato (righe processate: {}).", inserted);
        }
    }

    private void importVatCodes(Path file) throws Exception {
        log.info("📥 Import Codici IVA: {}", file.toAbsolutePath());
        try (InputStream is = Files.newInputStream(file); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sh = wb.getSheetAt(0);
            int processed = 0;

            for (int r = 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String code = s(row, 0);
                if (blank(code)) continue;

                BigDecimal rate = bd(row, 1);
                String regDesc = s(row, 2);
                String longDesc = s(row, 3);
                String opType = s(row, 4);
                String category = s(row, 5);

                boolean usePurch = boolLike(row, 6);
                boolean useSales = boolLike(row, 7);
                boolean useReceipts = boolLike(row, 8);

                String vatEdf = s(row, 9);
                String grouping = s(row, 10);
                String natPurch = s(row, 11);
                String natSales = s(row, 12);

                boolean stampDuty = boolLike(row, 13);
                boolean reverse = boolLike(row, 14);

                BigDecimal agriComp = bd(row, 15);
                String notes = s(row, 16);
                String externalCode = s(row, 17);
                String validity = s(row, 18);

                jdbc.update("""
                    INSERT INTO vat_codes(
                      code, rate, registry_description, long_description, operation_type, category,
                      use_purchases, use_sales, use_receipts,
                      vat_edf_code, vat_grouping,
                      custom_nature_purchases, custom_nature_sales,
                      stamp_duty_applicable, reverse_charge_relevant,
                      agri_comp_rate, notes, external_code, validity
                    )
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE
                      rate=VALUES(rate),
                      registry_description=VALUES(registry_description),
                      long_description=VALUES(long_description),
                      operation_type=VALUES(operation_type),
                      category=VALUES(category),
                      use_purchases=VALUES(use_purchases),
                      use_sales=VALUES(use_sales),
                      use_receipts=VALUES(use_receipts),
                      vat_edf_code=VALUES(vat_edf_code),
                      vat_grouping=VALUES(vat_grouping),
                      custom_nature_purchases=VALUES(custom_nature_purchases),
                      custom_nature_sales=VALUES(custom_nature_sales),
                      stamp_duty_applicable=VALUES(stamp_duty_applicable),
                      reverse_charge_relevant=VALUES(reverse_charge_relevant),
                      agri_comp_rate=VALUES(agri_comp_rate),
                      notes=VALUES(notes),
                      external_code=VALUES(external_code),
                      validity=VALUES(validity)
                """,
                        code, rate, regDesc, longDesc, opType, category,
                        usePurch, useSales, useReceipts,
                        vatEdf, grouping,
                        natPurch, natSales,
                        stampDuty, reverse,
                        agriComp, notes, externalCode, validity);

                processed++;
            }
            log.info("✅ Codici IVA importati (righe processate: {}).", processed);
        }
    }

    private void importWithholdingTypes(Path file) throws Exception {
        log.info("📥 Import Tabella Ritenute: {}", file.toAbsolutePath());
        try (InputStream is = Files.newInputStream(file); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sh = wb.getSheetAt(0);
            int processed = 0;

            for (int r = 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String code = s(row, 0);
                if (blank(code)) continue;

                String description = s(row, 1);
                String category = s(row, 2);
                // colonna 3 "Descrizione" (nel tuo file) -> la usiamo come tribute_description se utile
                String tributeDescription = s(row, 3);

                boolean shortRent = boolLike(row, 4);
                LocalDate effectiveFrom = date(row, 5);

                BigDecimal rate = bd(row, 6);
                BigDecimal taxablePercent = bd(row, 7);

                String tributeCode = s(row, 8);
                // colonna 9 "Descrizione" (nel tuo file) -> descrizione codice tributo/scadenza
                String dueDateDescription = s(row, 9);

                LocalDate dueDate = date(row, 10);
                String longDesc = s(row, 11);

                jdbc.update("""
                    INSERT INTO withholding_types(
                      code, description, category, effective_from,
                      rate, taxable_percent,
                      tribute_code, tribute_description,
                      due_date, due_date_description,
                      short_rent, long_description
                    )
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE
                      description=VALUES(description),
                      category=VALUES(category),
                      effective_from=VALUES(effective_from),
                      rate=VALUES(rate),
                      taxable_percent=VALUES(taxable_percent),
                      tribute_code=VALUES(tribute_code),
                      tribute_description=VALUES(tribute_description),
                      due_date=VALUES(due_date),
                      due_date_description=VALUES(due_date_description),
                      short_rent=VALUES(short_rent),
                      long_description=VALUES(long_description)
                """,
                        code, description, category, effectiveFrom,
                        rate, taxablePercent,
                        tributeCode, tributeDescription,
                        dueDate, dueDateDescription,
                        shortRent, longDesc);

                processed++;
            }
            log.info("✅ Ritenute importate (righe processate: {}).", processed);
        }
    }

    // ---------------- helpers ----------------

    private static String s(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return trimToNull(c.getStringCellValue());
        if (c.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(c)) {
                LocalDate d = c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return d.toString();
            }
            return trimToNull(BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString());
        }
        if (c.getCellType() == CellType.BOOLEAN) return c.getBooleanCellValue() ? "true" : "false";
        if (c.getCellType() == CellType.FORMULA) {
            try {
                return trimToNull(c.getStringCellValue());
            } catch (Exception ignore) {
                try {
                    return trimToNull(BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString());
                } catch (Exception ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    private static BigDecimal bd(Row row, int idx) {
        String v = s(row, idx);
        if (blank(v)) return null;
        // gestisce decimali con virgola
        v = v.trim().replace(",", ".");
        try {
            return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean boolLike(Row row, int idx) {
        String v = s(row, idx);
        if (blank(v)) return false;
        v = v.trim().toLowerCase(Locale.ROOT);
        return v.equals("1") || v.equals("true") || v.equals("si") || v.equals("sì") || v.equals("yes") || v.equals("y") || v.equals("x");
    }

    private static LocalDate date(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;

        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String v = s(row, idx);
        if (blank(v)) return null;

        // prova dd/MM/yyyy
        try {
            return LocalDate.parse(v.trim(), DateTimeFormatter.ofPattern("d/M/uuuu"));
        } catch (Exception ignore) { }

        // prova yyyy-MM-dd
        try {
            return LocalDate.parse(v.trim());
        } catch (Exception ignore) { }

        return null;
    }

    private static Integer calcLevel(String code) {
        if (blank(code)) return null;
        // livello = numero di segmenti separati da "."
        String[] parts = code.trim().split("\\.");
        return parts.length;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

