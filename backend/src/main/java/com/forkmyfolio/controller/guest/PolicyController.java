package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.PolicyDto;
import com.forkmyfolio.mapper.PolicyMapper;
import com.forkmyfolio.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/policies", "/api/v1/policy"})
@Tag(name = "Policies", description = "Endpoints for retrieving legal documents like Terms of Service and Privacy Policy.")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyMapper policyMapper;

    @GetMapping({"/terms-of-service", "/terms"})
    @Operation(summary = "Get the current Terms of Service", description = "Retrieves the latest version of the Terms of Service.")
    public ResponseEntity<ApiResponseWrapper<PolicyDto>> getTermsOfService() {
        log.info("Request received for Terms of Service");
        PolicyDto dto = policyMapper.toDto(policyService.getTermsOfService());
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }

    @GetMapping({"/privacy-policy", "/privacy"})
    @Operation(summary = "Get the current Privacy Policy", description = "Retrieves the latest version of the Privacy Policy.")
    public ResponseEntity<ApiResponseWrapper<PolicyDto>> getPrivacyPolicy() {
        log.info("Request received for Privacy Policy");
        PolicyDto dto = policyMapper.toDto(policyService.getPrivacyPolicy());
        return ResponseEntity.ok(new ApiResponseWrapper<>(dto));
    }
}