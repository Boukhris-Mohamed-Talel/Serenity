package com.example.appointment.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends reminder emails via Gmail (or any SMTP) when {@code spring.mail.username} is set.
 */
@Slf4j
@Service
public class AppointmentMailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public AppointmentMailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminderEmail(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(toEmail) || !StringUtils.hasText(fromAddress)) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.debug("JavaMailSender not available; skip email");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.debug("Sent reminder email to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send reminder email to {}: {}", toEmail, e.getMessage());
        }
    }
}
