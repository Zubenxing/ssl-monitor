package com.sslmonitor.service;

import com.sslmonitor.model.Domain;
import com.sslmonitor.repository.DomainRepository;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CertificateService {
    
    private final DomainRepository domainRepository;
    private final EmailService emailService;
    private static final String LETS_ENCRYPT_STAGING_URL = "acme://letsencrypt.org/staging";
    private static final String LETS_ENCRYPT_PRODUCTION_URL = "acme://letsencrypt.org";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 2;

    public CertificateService(DomainRepository domainRepository, EmailService emailService) {
        this.domainRepository = domainRepository;
        this.emailService = emailService;
    }

    public Domain checkCertificate(String domainName) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                return doCheckCertificate(domainName);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed for domain {}: {}", 
                    retryCount + 1, domainName, e.getMessage());
                retryCount++;
                
                if (retryCount < MAX_RETRIES) {
                    try {
                        TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All attempts failed for domain: " + domainName, lastException);
        return handleCertificateError(domainName, "Certificate check failed after " + MAX_RETRIES + " attempts");
    }

    private Domain doCheckCertificate(String domainName) throws Exception {
        // 首先检查端口是否开放
        if (!isPortOpen(domainName, 443)) {
            return handleCertificateError(domainName, "Port 443 is not accessible");
        }

        Domain domain = domainRepository.findByDomainName(domainName)
            .orElse(new Domain());
        domain.setDomainName(domainName);

        SSLContext sslContext = createTrustAllSSLContext();
        SSLSocketFactory factory = sslContext.getSocketFactory();
        
        try (SSLSocket socket = (SSLSocket) factory.createSocket(domainName, 443)) {
            socket.setSoTimeout(10000); // 10秒超时
            socket.startHandshake();
            X509Certificate[] certs = (X509Certificate[]) socket.getSession().getPeerCertificates();
            
            if (certs != null && certs.length > 0) {
                X509Certificate cert = certs[0];
                LocalDateTime expiryDate = cert.getNotAfter().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                domain.setCertificateExpiryDate(expiryDate);
                domain.setLastChecked(LocalDateTime.now());
                domain.setCertificateStatus("VALID");
                domain.setCertificateDetails(cert.getSubjectX500Principal().getName());

                checkAndSendNotification(domain);

                log.info("Successfully checked certificate for domain: {}, expires: {}", 
                    domainName, expiryDate);
                return domainRepository.save(domain);
            } else {
                throw new CertificateException("No certificates found");
            }
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000); // 5秒连接超时
            return true;
        } catch (Exception e) {
            log.warn("Port {} is not accessible on host {}: {}", port, host, e.getMessage());
            return false;
        }
    }

    private SSLContext createTrustAllSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    private Domain handleCertificateError(String domainName, String errorMessage) {
        Domain domain = domainRepository.findByDomainName(domainName)
            .orElse(new Domain());
        domain.setDomainName(domainName);
        domain.setCertificateStatus("ERROR");
        domain.setLastChecked(LocalDateTime.now());
        domain.setCertificateDetails(errorMessage);
        return domainRepository.save(domain);
    }

    @Scheduled(cron = "0 0 */12 * * *") // Run every 12 hours
    public void checkAllCertificates() {
        List<Domain> domains = domainRepository.findAll();
        for (Domain domain : domains) {
            try {
                checkCertificate(domain.getDomainName());
            } catch (Exception e) {
                log.error("Failed to check certificate for domain: " + domain.getDomainName(), e);
            }
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

    private void checkAndSendNotification(Domain domain) {
        if (domain.getCertificateExpiryDate() == null) return;

        LocalDateTime now = LocalDateTime.now();
        long daysUntilExpiry = ChronoUnit.DAYS.between(now, domain.getCertificateExpiryDate());

        if (daysUntilExpiry <= 7) {
            emailService.sendExpiryNotification(domain, (int) daysUntilExpiry);
        } else if (daysUntilExpiry <= 30) {
            emailService.sendExpiryNotification(domain, (int) daysUntilExpiry);
        }
    }

    private void renewCertificate(Domain domain) {
        // TODO: Implement actual Let's Encrypt certificate renewal logic
        log.info("Certificate renewal initiated for domain: " + domain.getDomainName());
    }
} 