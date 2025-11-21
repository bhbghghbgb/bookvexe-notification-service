package com.bookvexe.mailservice.batch;

import com.bookvexe.mailservice.dto.MailKafkaDTO;
import com.bookvexe.mailservice.dto.ProcessedMailItem;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Queue;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SpringBatchConfig {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Queue<MailKafkaDTO> mailQueue;

    @Value("${mail.dry-run:false}")
    private boolean dryRun;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${mail.max-retries:3}")
    private int maxRetries;

    @Bean
    public Job sendMailJob(JobRepository jobRepository, Step sendMailStep) {
        log.debug("Configuring Spring Batch Job: sendMailJob");
        return new JobBuilder("sendMailJob", jobRepository).flow(sendMailStep).end().build();
    }

    @Bean
    public Step sendMailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.debug("Configuring Spring Batch Step: sendMailStep with chunk size 2");
        return new StepBuilder("sendMailStep", jobRepository)
            .<MailKafkaDTO, ProcessedMailItem>chunk(2, transactionManager)
            .reader(mailItemReader())
            .processor(mailItemProcessor())
            .writer(mailItemWriter())
            .build();
    }

    @Bean
    public ItemReader<MailKafkaDTO> mailItemReader() {
        return () -> {
            MailKafkaDTO mailDto = mailQueue.poll();
            if (mailDto != null) {
                log.debug("Reading mail from queue for recipient: {}", mailDto.getTo());
                return mailDto;
            }
            log.debug("Mail queue is empty, ending batch reading.");
            return null;
        };
    }

    @Bean
    public ItemProcessor<MailKafkaDTO, ProcessedMailItem> mailItemProcessor() {
        return item -> {
            log.info("Processing mail for recipient: {}", item.getTo());
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            Context context = new Context();
            if (item.getTemplateModel() != null) {
                context.setVariables(item.getTemplateModel());
                log.debug("Set {} template variables for recipient: {}", item.getTemplateModel().size(), item.getTo());
            }
            if (item.getBody() != null) {
                context.setVariable("message", item.getBody());
            }

            String templateName = item.getTemplateName() != null ? item.getTemplateName() : "example-template";
            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(mailFrom);
            helper.setTo(item.getTo());
            helper.setSubject(item.getSubject());
            helper.setText(htmlContent, true);

            log.debug("Successfully created MimeMessage for recipient: {}", item.getTo());
            return new ProcessedMailItem(item, mimeMessage, 0);
        };
    }

    @Bean
    public ItemWriter<ProcessedMailItem> mailItemWriter() {
        return items -> {
            for (ProcessedMailItem mailItem : items) {
                MailKafkaDTO mailDto = mailItem.getOriginalDto();
                MimeMessage message = mailItem.getMimeMessage();

                String subject = mailDto.getSubject();
                String recipient = mailDto.getTo();

                if (dryRun) {
                    log.warn("[Dry-Run] Skipping send for subject: {} to: {}", subject, recipient);
                } else {
                    try {
                        mailSender.send(message);
                        log.info("Successfully sent email with subject: {} to: {}", subject, recipient);
                    } catch (MailException e) {
                        log.error("Failed to send email with subject: {} to: {}. Error: {}",
                            subject, recipient, e.getMessage());

                        if (mailItem.getRetryCount() < maxRetries) {
                            ProcessedMailItem retryItem = mailItem.withIncrementedRetry();
                            // Re-queue the original DTO (not the processed item)
                            mailQueue.offer(retryItem.getOriginalDto());
                            log.warn("Re-queued failed email for retry {}/{} for recipient: {}",
                                retryItem.getRetryCount(), maxRetries, recipient);
                        } else {
                            log.error("Max retries exceeded ({}) for email to: {}. Moving to dead letter.",
                                maxRetries, recipient);
                            // Here you could move to a dead letter queue
                            // For now, we just log and don't re-queue
                        }

                        throw e; // Re-throw to mark the chunk as failed
                    }
                }
            }
        };
    }
}