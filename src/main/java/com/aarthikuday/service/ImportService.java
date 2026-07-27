package com.aarthikuday.service;

import com.aarthikuday.model.Client;
import com.aarthikuday.model.User;
import com.aarthikuday.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Iterator;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ClientService clientService;
    private final UserRepository userRepository;
    private final InvestmentService investmentService;

    public ImportService(ClientService clientService, UserRepository userRepository, InvestmentService investmentService) {
        this.clientService = clientService;
        this.userRepository = userRepository;
        this.investmentService = investmentService;
    }

    @Transactional
    public void importExcel(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }

            int count = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Read username (cell 0)
                String username = getCellValueAsString(currentRow.getCell(0));
                if (username == null || username.trim().isEmpty()) {
                    continue; // Skip empty rows
                }

                String clientName = getCellValueAsString(currentRow.getCell(1));
                String email = getCellValueAsString(currentRow.getCell(2));
                String phone = getCellValueAsString(currentRow.getCell(3));
                String pan = getCellValueAsString(currentRow.getCell(4));
                String address = getCellValueAsString(currentRow.getCell(5));
                String partner = getCellValueAsString(currentRow.getCell(6));
                String assetType = getCellValueAsString(currentRow.getCell(7));
                String schemeName = getCellValueAsString(currentRow.getCell(8));
                
                BigDecimal units = getCellValueAsBigDecimal(currentRow.getCell(9));
                BigDecimal buyPrice = getCellValueAsBigDecimal(currentRow.getCell(10));

                processRow(username, clientName, email, phone, pan, address, partner, assetType, schemeName, units, buyPrice);
                count++;
            }
            log.info("Successfully imported {} rows from Excel.", count);
        }
    }

    @Transactional
    public void importGoogleSheets(String spreadsheetUrl) throws Exception {
        // Translate standard view link to CSV export link if necessary
        String csvUrl = spreadsheetUrl;
        if (spreadsheetUrl.contains("/edit")) {
            csvUrl = spreadsheetUrl.substring(0, spreadsheetUrl.indexOf("/edit")) + "/export?format=csv";
        } else if (!spreadsheetUrl.endsWith("/export?format=csv") && spreadsheetUrl.contains("/d/")) {
            csvUrl = spreadsheetUrl + "/export?format=csv";
        }

        log.info("Downloading Google Sheet CSV from export URL: {}", csvUrl);

        try (InputStream is = new URL(csvUrl).openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            boolean isHeader = true;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // skip header row
                    continue;
                }

                // Parse CSV (supporting basic comma split, wait, let's use safe quote split)
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (columns.length < 11) {
                    continue;
                }

                String username = cleanCsvValue(columns[0]);
                if (username.isEmpty()) {
                    continue;
                }

                String clientName = cleanCsvValue(columns[1]);
                String email = cleanCsvValue(columns[2]);
                String phone = cleanCsvValue(columns[3]);
                String pan = cleanCsvValue(columns[4]);
                String address = cleanCsvValue(columns[5]);
                String partner = cleanCsvValue(columns[6]);
                String assetType = cleanCsvValue(columns[7]);
                String schemeName = cleanCsvValue(columns[8]);

                BigDecimal units = new BigDecimal(cleanCsvValue(columns[9]).replaceAll("[^\\d.]", ""));
                BigDecimal buyPrice = new BigDecimal(cleanCsvValue(columns[10]).replaceAll("[^\\d.]", ""));

                processRow(username, clientName, email, phone, pan, address, partner, assetType, schemeName, units, buyPrice);
                count++;
            }
            log.info("Successfully imported {} rows from Google Sheets CSV.", count);
        }
    }

    private void processRow(String username, String clientName, String email, String phone, String pan, 
                            String address, String partner, String assetType, String schemeName, 
                            BigDecimal units, BigDecimal buyPrice) {
        
        username = username.trim().toLowerCase();
        clientName = clientName != null ? clientName.trim() : "";
        email = email != null ? email.trim() : (username + "@aarthikuday.com");
        phone = phone != null ? phone.trim() : "0000000000";
        pan = pan != null ? pan.trim().toUpperCase() : "PANPENDING";
        address = address != null ? address.trim() : "N/A";
        partner = partner != null ? partner.trim().toUpperCase() : "NJ_WEALTH";
        assetType = assetType != null ? assetType.trim().toUpperCase() : "SIP";
        schemeName = schemeName != null ? schemeName.trim() : "";

        // Check if user already exists
        Client client;
        if (userRepository.existsByUsername(username)) {
            client = clientService.getClientByUsername(username);
        } else {
            // Create user + client
            client = clientService.createClient(
                    username,
                    username + "123", // default password e.g. rahul123
                    clientName,
                    email,
                    phone,
                    pan,
                    address
            );
        }

        // Add investment and update aggregates
        investmentService.addInvestment(client.getId(), partner, assetType, schemeName, units, buyPrice);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            }
            // Format double nicely to prevent scientific notation for long numbers
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.format("%d", (long) val);
            }
            return String.format("%s", val);
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (type == CellType.FORMULA) {
            try {
                return cell.getStringCellValue();
            } catch (Exception e) {
                return String.valueOf(cell.getNumericCellValue());
            }
        }
        return "";
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        CellType type = cell.getCellType();
        if (type == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (type == CellType.STRING) {
            try {
                String str = cell.getStringCellValue().replaceAll("[^\\d.]", "");
                return new BigDecimal(str);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        } else if (type == CellType.FORMULA) {
            try {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private String cleanCsvValue(String val) {
        if (val == null) {
            return "";
        }
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        return val.trim();
    }
}
