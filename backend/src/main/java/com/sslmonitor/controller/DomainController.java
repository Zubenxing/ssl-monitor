package com.sslmonitor.controller;

import com.sslmonitor.model.Domain;
import com.sslmonitor.service.CertificateService;
import com.sslmonitor.repository.DomainRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/domains")
@CrossOrigin(origins = "*")
@Validated
public class DomainController {

    private final DomainRepository domainRepository;
    private final CertificateService certificateService;

    public DomainController(DomainRepository domainRepository, CertificateService certificateService) {
        this.domainRepository = domainRepository;
        this.certificateService = certificateService;
    }

    @GetMapping
    public List<Domain> getAllDomains() {
        return domainRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> addDomain(@Valid @RequestBody Domain domain) {
        try {
            log.info("Adding new domain: {}", domain);
            if (domain.getDomainName() == null || domain.getDomainName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Domain name cannot be empty"));
            }

            // 检查域名是否已存在
            if (domainRepository.findByDomainName(domain.getDomainName()).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Domain already exists"));
            }

            Domain savedDomain = domainRepository.save(domain);
            Domain checkedDomain = certificateService.checkCertificate(domain.getDomainName());
            return ResponseEntity.ok(checkedDomain);
        } catch (Exception e) {
            log.error("Error adding domain: " + domain.getDomainName(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to add domain: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id) {
        try {
            if (domainRepository.existsById(id)) {
                domainRepository.deleteById(id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting domain with id: " + id, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to delete domain: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/auto-renewal")
    public ResponseEntity<?> toggleAutoRenewal(@PathVariable Long id) {
        try {
            return domainRepository.findById(id)
                .map(domain -> {
                    domain.setAutoRenewal(!domain.isAutoRenewal());
                    return ResponseEntity.ok(domainRepository.save(domain));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error toggling auto-renewal for domain id: " + id, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to update auto-renewal setting: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<?> checkCertificate(@PathVariable Long id) {
        try {
            return domainRepository.findById(id)
                .map(domain -> ResponseEntity.ok(certificateService.checkCertificate(domain.getDomainName())))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error checking certificate for domain id: " + id, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to check certificate: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
} 