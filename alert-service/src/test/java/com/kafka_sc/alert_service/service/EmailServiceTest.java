package com.kafka_sc.alert_service.service;

import com.kafka_sc.alert_service.entity.Alert;
import com.kafka_sc.alert_service.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<Alert> alertArgumentCaptor;

    @Test
    void sendEmail_success_recordsSendAlert() {
        emailService.sendEmail("user@example.com", "Alert", "Body", 1L);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(alertRepository).save(alertArgumentCaptor.capture());
        assertThat(alertArgumentCaptor.getValue().isSent()).isTrue();
        assertThat(alertArgumentCaptor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void sendEmail_mailFailure_recordUnsentEmail() {
        willThrow(new MailSendException("SMPT unavailable"))
                .given(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail("user@example.com", "Alert", "Body", 1L);

        verify(alertRepository).save(alertArgumentCaptor.capture());
        assertThat(alertArgumentCaptor.getValue().isSent()).isFalse();
    }

    @Test
    void sendEmail_databaseFailure_propagatesAndRecordsOnlyOnce() {
        given(alertRepository.save(any(Alert.class)))
                .willThrow(new DataAccessResourceFailureException("DB unavailable"));

        assertThatThrownBy(
                () -> emailService.sendEmail(
                        "user@example.com", "Alert", "Body", 1L)
        )
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }
}
