package com.bookvexe.mailservice.config;

import com.bookvexe.mailservice.dto.MailKafkaDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
public class MailQueueConfig {

    @Bean
    public Queue<MailKafkaDTO> mailQueue() {
        return new ConcurrentLinkedQueue<>();
    }
}