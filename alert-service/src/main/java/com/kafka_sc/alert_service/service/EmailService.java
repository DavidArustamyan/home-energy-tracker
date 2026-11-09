package com.kafka_sc.alert_service.service;

import com.kafka_sc.alert_service.entity.Alert;
import com.kafka_sc.alert_service.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AlertRepository alertRepository;

    public EmailService(JavaMailSender mailSender, AlertRepository alertRepository) {
        this.mailSender = mailSender;
        this.alertRepository = alertRepository;
    }

    public void sendEmail(String to, String subject, String body, Long userId) {
        log.info("Sending email to: {}, subject: {}", to, subject);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("noreply@kafka-sc.com");
        message.setSubject(subject);
        message.setText(body);

        boolean sent;
        try {
            mailSender.send(message);
            sent = true;
            log.info("Email sent to: {}", to);
        } catch (MailException e) {
            sent = false;
            log.error("Failed to send email to: {}", to, e);
        }

        alertRepository.save(Alert.builder()
                .sent(sent)
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .build());
    }
}
