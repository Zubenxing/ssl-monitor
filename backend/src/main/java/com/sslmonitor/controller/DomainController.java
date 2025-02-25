package com.sslmonitor.controller;

import com.sslmonitor.model.Domain;
import com.sslmonitor.service.CertificateService;
import com.sslmonitor.repository.DomainRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domains")
@CrossOrigin(origins = "*")
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
    public Domain addDomain(@RequestBody Domain domain) {
        Domain savedDomain = domainRepository.save(domain);
        return certificateService.checkCertificate(domain.getDomainName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id) {
        if (domainRepository.existsById(id)) {
            domainRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/auto-renewal")
    public ResponseEntity<Domain> toggleAutoRenewal(@PathVariable Long id) {
        return domainRepository.findById(id)
            .map(domain -> {
                domain.setAutoRenewal(!domain.isAutoRenewal());
                return ResponseEntity.ok(domainRepository.save(domain));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<Domain> checkCertificate(@PathVariable Long id) {
        return domainRepository.findById(id)
            .map(domain -> ResponseEntity.ok(certificateService.checkCertificate(domain.getDomainName())))
            .orElse(ResponseEntity.notFound().build());
    }
} 