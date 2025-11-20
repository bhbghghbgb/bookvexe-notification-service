package com.bookvexe.mailservice.controller;

import com.bookvexe.mailservice.dto.MailKafkaDTO;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class MailTestController {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final List<MailKafkaDTO> mailQueue;

    @Value("${mail.dry-run:false}")
    private boolean dryRun;

    @Value("${spring.mail.username}") // Inject the configured username
    private String mailFrom;

    @GetMapping
    public String showTestPage() {
        return "mail-test"; // Points to src/main/resources/templates/mail-test.html
    }

    /**
     * Sends mail directly without using the batch queue.
     */
    @PostMapping("/send/direct")
    public ResponseEntity<String> sendDirectMail(@RequestParam String recipient) {
        log.info("Attempting to send direct mail to: {}", recipient);

        if (dryRun) {
            log.warn("[Dry-Run] Direct send skipped for recipient: {}", recipient);
            return ResponseEntity.ok("[Dry-Run] Direct mail attempt logged.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            Context context = new Context();
            context.setVariable("name", "Test User");
            context.setVariable("message", "This is a direct test email.");

            String htmlContent = templateEngine.process("example-template", context);

            helper.setFrom(mailFrom);
            helper.setTo(recipient);
            helper.setSubject("Direct Test Email from Mail Service");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Successfully sent direct email to: {}", recipient);
            return ResponseEntity.ok("Direct mail sent successfully to " + recipient);
        } catch (Exception e) {
            log.error("Failed to send direct mail to: {}", recipient, e);
            return ResponseEntity.internalServerError().body("Failed to send direct mail: " + e.getMessage());
        }
    }

    /**
     * Sends mail via the batch queue, simulating a Kafka message.
     */
    @PostMapping("/send/batch")
    public ResponseEntity<String> sendBatchMail(@RequestParam String recipient) {
        log.info("Queueing mail for batch processing to: {}", recipient);

        MailKafkaDTO mailDto = new MailKafkaDTO(recipient, "Batch Queued Test Email", "This email was queued via the test endpoint for batch processing.", "example-template", Map.of("name", "Batch User"));

        synchronized (mailQueue) {
            mailQueue.add(mailDto);
        }

        log.info("Mail queued for batch. Current queue size: {}", mailQueue.size());
        return ResponseEntity.ok("Mail queued for batch processing to " + recipient + ". It will be processed on the next scheduled run.");
    }
}