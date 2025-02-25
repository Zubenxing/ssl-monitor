package com.sslmonitor.service;

import com.sslmonitor.model.Domain;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private static final int MAX_RETRIES = 1;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.notification.enabled:false}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendExpiryNotification(Domain domain, int daysUntilExpiry) {
        if (!mailEnabled) {
            log.info("Email notifications are disabled. Skipping notification for domain: {}", domain.getDomainName());
            return;
        }

        if (!StringUtils.hasText(domain.getNotificationEmail())) {
            log.warn("No notification email set for domain: {}", domain.getDomainName());
            return;
        }

        if (!StringUtils.hasText(fromEmail)) {
            log.error("Sender email not configured in application.properties");
            return;
        }

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Starting email sending process for domain {} (attempt {}/{})", 
                    domain.getDomainName(), retryCount + 1, MAX_RETRIES);
                
                // 创建邮件上下文
                log.debug("Creating email context and variables...");
                long startTime = System.currentTimeMillis();
                Context context = new Context();
                Map<String, Object> variables = createEmailVariables(domain, daysUntilExpiry);
                context.setVariables(variables);
                log.debug("Email context created in {} ms", System.currentTimeMillis() - startTime);

                // 选择并处理模板
                log.debug("Processing email template...");
                startTime = System.currentTimeMillis();
                String emailTemplate = daysUntilExpiry <= 7 ? "urgent-expiry-email" : "expiry-notification-email";
                String htmlContent = templateEngine.process(emailTemplate, context);
                log.debug("Template processed in {} ms", System.currentTimeMillis() - startTime);
                
                if (htmlContent == null || htmlContent.trim().isEmpty()) {
                    throw new RuntimeException("Failed to generate email content from template: " + emailTemplate);
                }

                // 创建邮件消息
                log.debug("Creating MIME message...");
                startTime = System.currentTimeMillis();
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                log.debug("MIME message created in {} ms", System.currentTimeMillis() - startTime);
                
                // 设置邮件属性
                log.debug("Setting email properties...");
                startTime = System.currentTimeMillis();
                helper.setFrom(fromEmail);
                helper.setTo(domain.getNotificationEmail());
                String subject = createEmailSubject(domain, daysUntilExpiry);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                log.debug("Email properties set in {} ms", System.currentTimeMillis() - startTime);

                log.info("Attempting to send email: From={}, To={}, Subject={}", 
                    fromEmail, domain.getNotificationEmail(), subject);

                // 发送邮件
                startTime = System.currentTimeMillis();
                mailSender.send(message);
                log.info("Email sent successfully in {} ms", System.currentTimeMillis() - startTime);
                
                log.info("Successfully sent notification email for domain: {} to {}", 
                    domain.getDomainName(), domain.getNotificationEmail());
                return;
                
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                log.error("Failed to send email (attempt {}/{}). Error details: {}", 
                    retryCount, MAX_RETRIES, e.toString(), e);
                
                if (e instanceof MessagingException) {
                    log.error("MessagingException details - Root cause: {}", 
                        getRootCause(e).getMessage());
                }
                
                if (retryCount < MAX_RETRIES) {
                    try {
                        log.info("Waiting {} ms before retry...", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Email sending interrupted", ie);
                    }
                }
            }
        }

        String errorMsg = String.format("Failed to send notification after %d attempts for domain %s: %s",
            MAX_RETRIES, domain.getDomainName(), lastException != null ? lastException.getMessage() : "Unknown error");
        log.error(errorMsg, lastException);
        throw new RuntimeException(errorMsg, lastException);
    }

    private Map<String, Object> createEmailVariables(Domain domain, int daysUntilExpiry) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("domain", domain);
        variables.put("daysUntilExpiry", daysUntilExpiry);
        variables.put("expiryDate", domain.getCertificateExpiryDate());
        variables.put("isUrgent", daysUntilExpiry <= 7);
        return variables;
    }

    private String createEmailSubject(Domain domain, int daysUntilExpiry) {
        String urgencyPrefix = daysUntilExpiry <= 7 ? "【紧急】" : "【提醒】";
        return String.format("%s域名证书即将过期 - %s（剩余%d天）", 
            urgencyPrefix, domain.getDomainName(), daysUntilExpiry);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
} 