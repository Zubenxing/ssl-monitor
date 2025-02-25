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

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendExpiryNotification(Domain domain, int daysUntilExpiry) {
        if (!StringUtils.hasText(domain.getNotificationEmail())) {
            log.warn("No notification email set for domain: {}", domain.getDomainName());
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(createEmailVariables(domain, daysUntilExpiry));

            String emailTemplate = daysUntilExpiry <= 7 ? "urgent-expiry-email" : "expiry-notification-email";
            String htmlContent = templateEngine.process(emailTemplate, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(domain.getNotificationEmail());
            helper.setSubject(createEmailSubject(domain, daysUntilExpiry));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent expiry notification email for domain: {} to {}", 
                domain.getDomainName(), domain.getNotificationEmail());
        } catch (MessagingException e) {
            log.error("Failed to send expiry notification email for domain: " + domain.getDomainName(), e);
        }
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
} 