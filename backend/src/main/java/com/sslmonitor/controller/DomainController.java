package com.sslmonitor.controller;

import com.sslmonitor.model.Domain;
import com.sslmonitor.service.CertificateService;
import com.sslmonitor.service.EmailService;
import com.sslmonitor.repository.DomainRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RestController
@RequestMapping("/api/domains")
@CrossOrigin(origins = "*")
@Validated
public class DomainController {

    private final DomainRepository domainRepository;
    private final CertificateService certificateService;
    private final EmailService emailService;

    public DomainController(DomainRepository domainRepository, CertificateService certificateService, EmailService emailService) {
        this.domainRepository = domainRepository;
        this.certificateService = certificateService;
        this.emailService = emailService;
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

            // 清理域名格式
            String cleanDomainName = cleanDomainName(domain.getDomainName());
            domain.setDomainName(cleanDomainName);

            // 检查域名是否已存在
            if (domainRepository.findByDomainName(cleanDomainName).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Domain already exists"));
            }

            Domain savedDomain = domainRepository.save(domain);
            Domain checkedDomain = certificateService.checkCertificate(cleanDomainName);
            return ResponseEntity.ok(checkedDomain);
        } catch (Exception e) {
            log.error("Error adding domain: " + domain.getDomainName(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to add domain: " + e.getMessage()));
        }
    }

    private String cleanDomainName(String domainName) {
        if (domainName == null) return null;
        // 移除前后空格
        domainName = domainName.trim();
        // 移除协议前缀
        domainName = domainName.replaceAll("^(https?://)", "");
        // 移除路径后缀
        domainName = domainName.replaceAll("/.*$", "");
        // 移除端口号
        domainName = domainName.replaceAll(":\\d+$", "");
        return domainName;
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

    @PostMapping("/{id}/send-notification")
    public ResponseEntity<?> sendNotification(@PathVariable Long id) {
        try {
            return domainRepository.findById(id)
                .map(domain -> {
                    if (domain.getCertificateExpiryDate() == null) {
                        return ResponseEntity.badRequest()
                            .body(createErrorResponse("Certificate expiry date not available"));
                    }
                    int daysUntilExpiry = (int) ChronoUnit.DAYS.between(
                        LocalDateTime.now(), 
                        domain.getCertificateExpiryDate()
                    );
                    emailService.sendExpiryNotification(domain, daysUntilExpiry);
                    return ResponseEntity.ok()
                        .body(createSuccessResponse("Notification sent successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error sending notification for domain id: " + id, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to send notification: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDomain(@PathVariable Long id, @Valid @RequestBody Domain domain) {
        try {
            return domainRepository.findById(id)
                .map(existingDomain -> {
                    String cleanDomainName = cleanDomainName(domain.getDomainName());
                    
                    // 如果域名变更，检查新域名是否已存在
                    if (!existingDomain.getDomainName().equals(cleanDomainName) &&
                        domainRepository.findByDomainName(cleanDomainName).isPresent()) {
                        return ResponseEntity.badRequest()
                            .body(createErrorResponse("Domain already exists"));
                    }
                    
                    existingDomain.setDomainName(cleanDomainName);
                    existingDomain.setNotificationEmail(domain.getNotificationEmail());
                    existingDomain.setAutoRenewal(domain.isAutoRenewal());
                    
                    Domain savedDomain = domainRepository.save(existingDomain);
                    Domain checkedDomain = certificateService.checkCertificate(cleanDomainName);
                    return ResponseEntity.ok(checkedDomain);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating domain with id: " + id, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to update domain: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
} 