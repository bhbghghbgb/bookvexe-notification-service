package com.bookvexe.mailservice.kafka;

import com.bookvexe.mailservice.config.RateLimiter;
import com.bookvexe.mailservice.dto.MailKafkaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final JobLauncher jobLauncher;
    private final Job sendMailJob;
    private final List<MailKafkaDTO> mailQueue;
    private final RateLimiter rateLimiter;

    private final AtomicBoolean jobRunning = new AtomicBoolean(false);

    @KafkaListener(topics = "mail-topic", groupId = "mail_group")
    public void consume(MailKafkaDTO kafkaDTO) {
        log.info("Received mail request from Kafka for recipient: {}", kafkaDTO.getTo());
        synchronized (mailQueue) {
            mailQueue.add(kafkaDTO);
            log.debug("Mail added to internal queue. Current size: {}", mailQueue.size());
        }
    }

    // Check queue and trigger batch job, throttled by time and count
    @Scheduled(fixedDelay = 1000)
    public void triggerMailBatch() {
        if (mailQueue.isEmpty()) return;

        if (!jobRunning.compareAndSet(false, true)) {
            log.debug("Previous batch still running, skipping this cycle.");
            return;
        }

        try {
            if (!rateLimiter.canSend()) {
                log.debug("Rate limiter blocked this batch.");
                return;
            }

            long now = System.currentTimeMillis();
            jobLauncher.run(sendMailJob, new JobParametersBuilder().addLong("time", now).toJobParameters());

        } catch (Exception e) {
            log.error("Error launching mail batch job", e);
        } finally {
            jobRunning.set(false);
        }
    }
}