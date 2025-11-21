package com.bookvexe.mailservice.dto;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessedMailItem {
    private MailKafkaDTO originalDto;
    private MimeMessage mimeMessage;
    private int retryCount;
    
    public ProcessedMailItem withIncrementedRetry() {
        return new ProcessedMailItem(
            this.originalDto,
            this.mimeMessage,
            this.retryCount + 1
        );
    }
}