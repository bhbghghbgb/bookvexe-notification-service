package com.bookvexe.mailservice.kafka;

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
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final JobLauncher jobLauncher;
    private final Job sendMailJob;
    private final List<MailKafkaDTO> mailQueue;

    // Rate Limiting State Variables
    private final AtomicInteger mailsSentInMinute = new AtomicInteger(0);
    private long lastJobRunTime = 0;
    private static final int MAX_MAILS_PER_MINUTE = 5;
    private static final long MIN_DELAY_BETWEEN_JOBS_MS = 5000; // 5 seconds

    @KafkaListener(topics = "mail-topic", groupId = "mail_group")
    public void consume(MailKafkaDTO kafkaDTO) {
        log.info("Received mail request from Kafka for recipient: {}", kafkaDTO.getTo());
        synchronized (mailQueue) {
            mailQueue.add(kafkaDTO);
            log.debug("Mail added to internal queue. Current size: {}", mailQueue.size());
        }
    }

    // Reset the mail count every minute
    @Scheduled(cron = "0 * * * * *") // Runs at the start of every minute (e.g., 1:00:00, 1:01:00)
    public void resetMailCounter() {
        int count = mailsSentInMinute.getAndSet(0);
        log.info("Mail count reset. {} mails were processed in the last minute.", count);
    }

    // Check queue and trigger batch job, throttled by time and count
    @Scheduled(fixedRate = 1000) // Check queue every 1 second
    public void triggerMailBatch() {
        if (mailQueue.isEmpty()) {
            log.debug("Queue empty. Skipping batch job trigger.");
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Time-based rate limit (at most 1 job per 5 seconds)
        if (now - lastJobRunTime < MIN_DELAY_BETWEEN_JOBS_MS) {
            log.debug("Throttling active. Next job allowed in {}ms. Skipping batch job trigger.",
                MIN_DELAY_BETWEEN_JOBS_MS - (now - lastJobRunTime));
            return;
        }

        // 2. Mail-per-minute rate limit (at most 5 jobs per minute)
        if (mailsSentInMinute.get() >= MAX_MAILS_PER_MINUTE) {
            log.warn("Minute rate limit reached ({} mails). Skipping batch job trigger.", MAX_MAILS_PER_MINUTE);
            return;
        }

        // --- Rate limits passed, trigger job ---
        try {
            log.info("Launching mail batch job. Current queue size: {}", mailQueue.size());
            jobLauncher.run(sendMailJob, new JobParametersBuilder()
                .addLong("time", now)
                .toJobParameters());

            mailsSentInMinute.incrementAndGet();
            lastJobRunTime = now;
            log.info("Batch job launched. Mail count for this minute: {}", mailsSentInMinute.get());
        } catch (Exception e) {
            log.error("Error launching mail batch job", e);
        }
    }
}