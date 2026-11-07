package com.forkmyfolio.controller.guest;

import com.forkmyfolio.dto.response.SettingDto;
import com.forkmyfolio.mapper.SettingMapper;
import com.forkmyfolio.service.SettingService;
import com.forkmyfolio.service.impl.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Endpoints for retrieving public settings")
@Slf4j
public class PublicSettingController {
    private final SettingService settingService;
    private final SettingMapper settingMapper;
    private final PdfGenerationService pdfGenerationService;

    @Operation(summary = "Get all public settings as a list of objects")
    @GetMapping
    public ResponseEntity<List<SettingDto>> getPublicSettings() {
        log.info("Request received for public settings list");
        var settings = settingService.getAllSettings();
        return ResponseEntity.ok(settingMapper.toDtoList(settings));
    }

    @GetMapping("/pdf-templates")
    @Operation(summary = "Get the list of available PDF template names")
    public ResponseEntity<List<String>> getAvailablePdfTemplates() {
        log.info("Request received for available PDF templates");
        List<String> templateNames = pdfGenerationService.getAvailableTemplateNames();
        return ResponseEntity.ok(templateNames);
    }
}