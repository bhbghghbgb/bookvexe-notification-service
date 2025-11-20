package com.bookvexe.mailservice.config;

import com.bookvexe.mailservice.dto.MailKafkaDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class MailQueueConfig {

    @Bean
    public List<MailKafkaDTO> mailQueue() {
        return Collections.synchronizedList(new ArrayList<>());
    }
}
