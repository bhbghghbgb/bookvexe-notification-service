package com.bookvexe.mailservice.batch;

import com.bookvexe.mailservice.dto.MailKafkaDTO;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SpringBatchConfig {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    // Inject the mailQueue bean
    private final List<MailKafkaDTO> mailQueue;

    @Value("${mail.dry-run:false}")
    private boolean dryRun;

    @Value("${spring.mail.username}") // Inject the configured username
    private String mailFrom;

    @Bean
    public Job sendMailJob(JobRepository jobRepository, Step sendMailStep) {
        log.debug("Configuring Spring Batch Job: sendMailJob");
        return new JobBuilder("sendMailJob", jobRepository).flow(sendMailStep).end().build();
    }

    @Bean
    public Step sendMailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.debug("Configuring Spring Batch Step: sendMailStep with chunk size 10");
        return new StepBuilder("sendMailStep", jobRepository).<MailKafkaDTO, MimeMessage>chunk(10, transactionManager) // Process 10 emails per batch
            .reader(mailItemReader()).processor(mailItemProcessor()).writer(mailItemWriter()).build();
    }

    @Bean
    public ItemReader<MailKafkaDTO> mailItemReader() {
        return () -> {
            synchronized (mailQueue) {
                if (!mailQueue.isEmpty()) {
                    MailKafkaDTO mailDto = mailQueue.remove(0);
                    log.debug("Reading mail from queue for recipient: {}", mailDto.getTo());
                    return mailDto;
                }
            }
            log.debug("Mail queue is empty, ending batch reading.");
            return null; // End of reading
        };
    }

    @Bean
    public ItemProcessor<MailKafkaDTO, MimeMessage> mailItemProcessor() {
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
            return mimeMessage;
        };
    }

    @Bean
    public ItemWriter<MimeMessage> mailItemWriter() {
        return items -> {
            for (MimeMessage message : items) {
                String subject = message.getSubject();
                if (dryRun) {
                    log.warn("[Dry-Run] Skipping send for subject: {}", subject);
                } else {
                    mailSender.send(message);
                    log.info("Successfully sent email with subject: {}", subject);
                }
            }
        };
    }
}