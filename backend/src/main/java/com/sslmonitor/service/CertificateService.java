package com.sslmonitor.service;

import com.sslmonitor.model.Domain;
import com.sslmonitor.repository.DomainRepository;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CertificateService {
    
    private final DomainRepository domainRepository;
    private final EmailService emailService;
    private static final String LETS_ENCRYPT_STAGING_URL = "acme://letsencrypt.org/staging";
    private static final String LETS_ENCRYPT_PRODUCTION_URL = "acme://letsencrypt.org";
    private static final int MAX_RETRIES = 2;  // 增加重试次数
    private static final int RETRY_DELAY_SECONDS = 2;
    private static final int CONNECT_TIMEOUT = 10000; // 增加到10秒
    private static final int READ_TIMEOUT = 10000; // 增加到10秒

    public CertificateService(DomainRepository domainRepository, EmailService emailService) {
        this.domainRepository = domainRepository;
        this.emailService = emailService;
    }

    public Domain checkCertificate(String domainName, boolean isManualCheck) {
        int retryCount = 0;
        Exception lastException = null;
        Domain domain = null;

        try {
            // 清理域名格式并创建域名对象
            domainName = cleanDomainName(domainName);
            domain = domainRepository.findByDomainName(domainName)
                .orElse(new Domain());
            domain.setDomainName(domainName);
            domain.setLastChecked(LocalDateTime.now());
            
            while (retryCount < MAX_RETRIES) {
                try {
                    log.info("Attempting certificate check for domain {} (attempt {}/{})", 
                        domainName, retryCount + 1, MAX_RETRIES);
                    Domain checkedDomain = doCheckCertificate(domainName);
                    if (checkedDomain != null) {
                        return checkedDomain;
                    }
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
        } catch (Exception e) {
            log.error("Unexpected error during certificate check for domain: " + domainName, e);
            lastException = e;
        }

        String errorMsg = String.format("Certificate check failed after %d attempts for domain %s: %s",
            MAX_RETRIES, domainName, lastException != null ? lastException.getMessage() : "Unknown error");
        log.error(errorMsg, lastException);
        
        if (domain == null) {
            domain = new Domain();
            domain.setDomainName(domainName);
        }
        
        return handleCertificateError(domain, errorMsg);
    }

    private String cleanDomainName(String domainName) {
        if (domainName == null) return null;
        
        // 移除前后空格
        domainName = domainName.trim().toLowerCase();
        
        // 移除协议前缀
        if (domainName.startsWith("https://")) {
            domainName = domainName.substring(8);
        } else if (domainName.startsWith("http://")) {
            domainName = domainName.substring(7);
        }
        
        // 移除路径部分
        if (domainName.contains("/")) {
            domainName = domainName.substring(0, domainName.indexOf("/"));
        }
        
        // 移除端口号
        if (domainName.contains(":")) {
            domainName = domainName.substring(0, domainName.indexOf(":"));
        }
        
        log.debug("Cleaned domain name from input: {}", domainName);
        return domainName;
    }

    private Domain doCheckCertificate(String domainName) throws Exception {
        log.info("Starting certificate check for domain: {}", domainName);
        
        // 清理域名格式
        domainName = cleanDomainName(domainName);
        log.debug("Cleaned domain name: {}", domainName);

        // 获取或创建域名记录
        Domain domain = domainRepository.findByDomainName(domainName)
            .orElse(new Domain());
        domain.setDomainName(domainName);
        domain.setLastChecked(LocalDateTime.now());

        // 检查域名可访问性和证书状态
        CertificateCheckResult checkResult = checkCertificateAccessibility(domainName);
        
        // 如果无法访问，设置错误状态并返回
        if (!checkResult.isAccessible()) {
            domain.setCertificateStatus("ERROR");
            domain.setCertificateDetails(checkResult.getErrorMessage());
            return domainRepository.save(domain);
        }

        // 更新证书信息
        domain.setCertificateExpiryDate(checkResult.getExpiryDate());
        domain.setCertificateDetails(checkResult.getCertificateDetails());
        
        // 简化状态判断：只要证书在有效期内就是VALID，否则就是ERROR
        LocalDateTime now = LocalDateTime.now();
        if (checkResult.getExpiryDate() != null && now.isBefore(checkResult.getExpiryDate())) {
            domain.setCertificateStatus("VALID");
        } else {
            domain.setCertificateStatus("ERROR");
        }

        log.info("Successfully checked certificate for domain: {}, status: {}, expires in {} days", 
            domainName, domain.getCertificateStatus(), checkResult.getDaysUntilExpiry());

        // 保存域名信息
        Domain savedDomain = domainRepository.save(domain);
        
        // 检查是否需要发送邮件通知（证书有效期小于30天）
        if (savedDomain.getCertificateExpiryDate() != null && 
            StringUtils.hasText(savedDomain.getNotificationEmail())) {
            
            long daysUntilExpiry = ChronoUnit.DAYS.between(
                LocalDateTime.now(), 
                savedDomain.getCertificateExpiryDate()
            );
            
            // 只在证书有效期小于30天时发送邮件
            if (daysUntilExpiry <= 30) {
                CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendExpiryNotification(savedDomain, (int)daysUntilExpiry);
                    } catch (Exception e) {
                        log.error("Failed to send notification email for domain: {}", savedDomain.getDomainName(), e);
                    }
                });
            }
        }
        
        return savedDomain;
    }

    private static class CertificateCheckResult {
        private boolean accessible;
        private String errorMessage;
        private LocalDateTime expiryDate;
        private String certificateDetails;
        private long daysUntilExpiry;

        public CertificateCheckResult() {
            this.accessible = false;
        }

        public boolean isAccessible() { return accessible; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getExpiryDate() { return expiryDate; }
        public String getCertificateDetails() { return certificateDetails; }
        public long getDaysUntilExpiry() { return daysUntilExpiry; }

        public void setAccessible(boolean accessible) { this.accessible = accessible; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
        public void setCertificateDetails(String certificateDetails) { this.certificateDetails = certificateDetails; }
        public void setDaysUntilExpiry(long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }
    }

    private CertificateCheckResult checkCertificateAccessibility(String domainName) {
        CertificateCheckResult result = new CertificateCheckResult();
        
        try {
            SSLContext sslContext = createTrustAllSSLContext();
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(READ_TIMEOUT);
                InetSocketAddress address = new InetSocketAddress(domainName, 443);
                socket.connect(address, CONNECT_TIMEOUT);
                
                try (SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, domainName, 443, true)) {
                    sslSocket.setSoTimeout(READ_TIMEOUT);
                    sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"}); // 添加TLS协议支持
                    sslSocket.startHandshake();
                    
                    X509Certificate[] certs = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
                    
                    if (certs != null && certs.length > 0) {
                        X509Certificate cert = certs[0];
                        LocalDateTime expiryDate = cert.getNotAfter().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                        LocalDateTime startDate = cert.getNotBefore().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                        LocalDateTime now = LocalDateTime.now();
                        
                        // 检查证书是否在有效期内
                        if (now.isBefore(startDate)) {
                            result.setErrorMessage("Certificate is not yet valid");
                            return result;
                        }
                        
                        if (now.isAfter(expiryDate)) {
                            result.setErrorMessage("Certificate has expired");
                            return result;
                        }
                        
                        // 构建证书详情
                        StringBuilder details = new StringBuilder();
                        details.append("Subject: ").append(cert.getSubjectX500Principal().getName()).append("\n");
                        details.append("Issuer: ").append(cert.getIssuerX500Principal().getName()).append("\n");
                        details.append("Valid From: ").append(cert.getNotBefore()).append("\n");
                        details.append("Valid Until: ").append(cert.getNotAfter()).append("\n");
                        details.append("Serial Number: ").append(cert.getSerialNumber()).append("\n");
                        
                        long daysUntilExpiry = ChronoUnit.DAYS.between(now, expiryDate);
                        details.append("Days until expiry: ").append(daysUntilExpiry);
                        
                        result.setAccessible(true);
                        result.setExpiryDate(expiryDate);
                        result.setCertificateDetails(details.toString());
                        result.setDaysUntilExpiry(daysUntilExpiry);
                        
                        log.debug("Certificate check successful for domain: {}, expires in {} days", 
                            domainName, daysUntilExpiry);
                        
                        return result;
                    } else {
                        result.setErrorMessage("No certificates found for domain: " + domainName);
                        return result;
                    }
                }
            }
        } catch (UnknownHostException e) {
            result.setErrorMessage("DNS resolution failed: " + e.getMessage());
            log.error("DNS resolution failed for domain {}: {}", domainName, e.getMessage());
        } catch (SSLHandshakeException e) {
            result.setErrorMessage("SSL handshake failed: " + e.getMessage());
            log.error("SSL handshake failed for domain {}: {}", domainName, e.getMessage());
        } catch (SocketTimeoutException e) {
            result.setErrorMessage("Connection timed out: " + e.getMessage());
            log.error("Connection timed out for domain {}: {}", domainName, e.getMessage());
        } catch (Exception e) {
            result.setErrorMessage("Certificate check failed: " + e.getMessage());
            log.error("Certificate check failed for domain {}: {}", domainName, e.getMessage(), e);
        }
        
        return result;
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(READ_TIMEOUT);
            InetSocketAddress address = new InetSocketAddress(host, port);
            log.debug("Checking if port {} is open on host {}", port, host);
            socket.connect(address, CONNECT_TIMEOUT);
            log.debug("Port {} is open on host {}", port, host);
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

    private Domain handleCertificateError(Domain domain, String errorMessage) {
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
                checkCertificate(domain.getDomainName(), false);
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

    private void renewCertificate(Domain domain) {
        log.info("Starting certificate renewal for domain: {}", domain.getDomainName());
        
        try {
            // 创建ACME会话
            Session session = new Session(LETS_ENCRYPT_PRODUCTION_URL);
            
            // 创建账户（如果是第一次）或加载现有账户
            KeyPair accountKeyPair = KeyPairUtils.createKeyPair(2048);
            Account account = new AccountBuilder()
                    .agreeToTermsOfService()
                    .useKeyPair(accountKeyPair)
                    .create(session);
            
            // 创建新的订单
            Order order = account.newOrder()
                    .domains(domain.getDomainName())
                    .create();
            
            // 获取授权
            for (Authorization auth : order.getAuthorizations()) {
                // 处理HTTP-01验证
                Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
                if (challenge != null) {
                    // 这里需要实现将challenge token放到域名的/.well-known/acme-challenge/目录下
                    String token = challenge.getToken();
                    String content = challenge.getAuthorization();
                    
                    // TODO: 实现将token和content保存到域名的/.well-known/acme-challenge/目录
                    log.info("Token: {}", token);
                    log.info("Content: {}", content);
                    log.info("Token URL: http://{}/.well-known/acme-challenge/{}", 
                        domain.getDomainName(), token);
                    
                    // 触发验证
                    challenge.trigger();
                    
                    // 等待验证完成
                    while (challenge.getStatus() != Status.VALID) {
                        if (challenge.getStatus() == Status.INVALID) {
                            throw new AcmeException("Challenge failed... Giving up.");
                        }
                        Thread.sleep(3000L);
                        challenge.update();
                    }
                }
            }
            
            // 生成CSR
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(2048);
            CSRBuilder csrBuilder = new CSRBuilder();
            csrBuilder.addDomain(domain.getDomainName());
            csrBuilder.sign(domainKeyPair);
            byte[] csr = csrBuilder.getEncoded();
            
            // 完成订单
            order.execute(csr);
            
            // 获取证书链
            Certificate certificate = order.getCertificate();
            X509Certificate cert = certificate.getCertificate();
            
            // 更新域名信息
            domain.setLastRenewal(LocalDateTime.now());
            domain.setCertificateExpiryDate(cert.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
            domain.setCertificateDetails("Renewed certificate: " + cert.getSubjectX500Principal().getName());
            domainRepository.save(domain);
            
            log.info("Certificate renewal completed for domain: {}", domain.getDomainName());
        } catch (AcmeException e) {
            log.error("Failed to renew certificate for domain: " + domain.getDomainName(), e);
            throw new RuntimeException("Certificate renewal failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Certificate renewal was interrupted", e);
        } catch (Exception e) {
            log.error("Unexpected error during certificate renewal for domain: " + domain.getDomainName(), e);
            throw new RuntimeException("Certificate renewal failed", e);
        }
    }

    public void doCheckCertificate(Domain domain) {
        try {
            doCheckCertificate(domain.getDomainName());
        } catch (Exception e) {
            log.error("Failed to check certificate for domain: " + domain.getDomainName(), e);
        }
    }

    // 默认的checkCertificate方法设为手动检查
    public Domain checkCertificate(String domainName) {
        return checkCertificate(domainName, true);
    }
} 