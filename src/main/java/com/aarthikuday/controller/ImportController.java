package com.aarthikuday.controller;

import com.aarthikuday.service.ImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty. Please upload a valid Excel spreadsheet.");
                return ResponseEntity.badRequest().body(response);
            }
            importService.importExcel(file);
            response.put("success", true);
            response.put("message", "Successfully imported Excel data!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Import failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/google-sheets")
    public ResponseEntity<Map<String, Object>> syncGoogleSheets(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String url = request.get("url");
        try {
            if (url == null || url.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "URL parameter is missing.");
                return ResponseEntity.badRequest().body(response);
            }
            importService.importGoogleSheets(url);
            response.put("success", true);
            response.put("message", "Successfully synchronized Google Sheet data!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
