package com.example.meetingprocessor.listener;

import com.example.meetingprocessor.service.MeetingProcessorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Component
public class SqsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationListener.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SqsClient sqsClient;
    private final MeetingProcessorService processorService;
    private final String queueUrl;

    public SqsNotificationListener(SqsClient sqsClient,
                                   MeetingProcessorService processorService,
                                   @Value("${aws.sqs.queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.processorService = processorService;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (queueUrl == null || queueUrl.isBlank()) return;

        List<Message> messages = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20) // long polling
                        .build()
        ).messages();

        for (Message message : messages) {
            try {
                handle(message.body());
                delete(message.receiptHandle());
            } catch (Exception e) {
                log.error("Failed to handle SQS message — will retry after visibility timeout", e);
            }
        }
    }

    private void handle(String sqsBody) throws Exception {
        // SQS body is an SNS envelope: { "Message": "<transcribe-notification-json>", ... }
        JsonNode envelope = MAPPER.readTree(sqsBody);
        JsonNode notification = MAPPER.readTree(envelope.path("Message").asText());

        String jobName = notification.path("TranscriptionJobName").asText();
        String status = notification.path("TranscriptionJobStatus").asText();

        if (jobName.isBlank() || !jobName.startsWith("meeting-")) {
            log.debug("Ignoring unrelated Transcribe notification for job '{}'", jobName);
            return;
        }

        log.info("Received Transcribe notification: job={} status={}", jobName, status);

        switch (status) {
            case "COMPLETED" -> processorService.completeProcessing(jobName);
            case "FAILED" -> processorService.markFailed(jobName);
            default -> log.warn("Unexpected Transcribe job status '{}' for job {}", status, jobName);
        }
    }

    private void delete(String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build());
    }
}
