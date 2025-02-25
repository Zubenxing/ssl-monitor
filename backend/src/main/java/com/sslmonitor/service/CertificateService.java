package com.sslmonitor.service;

import com.sslmonitor.model.Domain;
import com.sslmonitor.repository.DomainRepository;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class CertificateService {
    
    private final DomainRepository domainRepository;
    private static final String LETS_ENCRYPT_STAGING_URL = "acme://letsencrypt.org/staging";
    private static final String LETS_ENCRYPT_PRODUCTION_URL = "acme://letsencrypt.org";

    public CertificateService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    public Domain checkCertificate(String domainName) {
        try {
            Domain domain = domainRepository.findByDomainName(domainName)
                .orElse(new Domain());
            domain.setDomainName(domainName);

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(domainName, 443)) {
                socket.startHandshake();
                X509Certificate[] certs = (X509Certificate[]) socket.getSession().getPeerCertificates();
                X509Certificate cert = certs[0];

                LocalDateTime expiryDate = cert.getNotAfter().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                domain.setCertificateExpiryDate(expiryDate);
                domain.setLastChecked(LocalDateTime.now());
                domain.setCertificateStatus("VALID");
                domain.setCertificateDetails(cert.getSubjectX500Principal().getName());

                return domainRepository.save(domain);
            }
        } catch (Exception e) {
            log.error("Error checking certificate for domain: " + domainName, e);
            Domain domain = new Domain();
            domain.setDomainName(domainName);
            domain.setCertificateStatus("ERROR");
            domain.setLastChecked(LocalDateTime.now());
            return domainRepository.save(domain);
        }
    }

    @Scheduled(cron = "0 0 */12 * * *") // Run every 12 hours
    public void checkAllCertificates() {
        List<Domain> domains = domainRepository.findAll();
        for (Domain domain : domains) {
            checkCertificate(domain.getDomainName());
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // Run at 1 AM daily
    public void autoRenewCertificates() {
        List<Domain> domains = domainRepository.findByAutoRenewalTrue();
        LocalDateTime renewalThreshold = LocalDateTime.now().plusDays(30);

        for (Domain domain : domains) {
            if (domain.getCertificateExpiryDate() != null && 
                domain.getCertificateExpiryDate().isBefore(renewalThreshold)) {
                try {
                    renewCertificate(domain);
                } catch (Exception e) {
                    log.error("Failed to auto-renew certificate for domain: " + domain.getDomainName(), e);
                }
            }
        }
    }

    private void renewCertificate(Domain domain) {
        // TODO: Implement actual Let's Encrypt certificate renewal logic
        // This would involve:
        // 1. Creating an ACME session
        // 2. Proving domain ownership
        // 3. Generating and installing the new certificate
        log.info("Certificate renewal initiated for domain: " + domain.getDomainName());
    }
} 