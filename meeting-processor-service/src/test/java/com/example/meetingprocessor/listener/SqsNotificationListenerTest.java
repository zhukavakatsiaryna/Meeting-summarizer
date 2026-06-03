package com.example.meetingprocessor.listener;

import com.example.meetingprocessor.service.MeetingProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsNotificationListenerTest {

    @Mock private SqsClient sqsClient;
    @Mock private MeetingProcessorService processorService;
    @InjectMocks private SqsNotificationListener listener;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "queueUrl", QUEUE_URL);
    }

    private String buildSqsBody(String jobName, String status) {
        String innerJson = String.format(
                "{\\\"TranscriptionJobName\\\":\\\"%s\\\",\\\"TranscriptionJobStatus\\\":\\\"%s\\\"}",
                jobName, status);
        return String.format("{\"Message\":\"%s\"}", innerJson);
    }

    @Test
    void givenBlankQueueUrl_whenPoll_thenNoSqsCallMade() {
        ReflectionTestUtils.setField(listener, "queueUrl", "");

        listener.poll();

        verifyNoInteractions(sqsClient);
    }

    @Test
    void givenCompletedTranscribeJob_whenPoll_thenCompleteProcessingIsCalled() {
        String jobName = "meeting-" + UUID.randomUUID();
        Message message = Message.builder()
                .body(buildSqsBody(jobName, "COMPLETED"))
                .receiptHandle("receipt-1")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        listener.poll();

        verify(processorService).completeProcessing(jobName);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void givenFailedTranscribeJob_whenPoll_thenMarkFailedIsCalled() {
        String jobName = "meeting-" + UUID.randomUUID();
        Message message = Message.builder()
                .body(buildSqsBody(jobName, "FAILED"))
                .receiptHandle("receipt-2")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        listener.poll();

        verify(processorService).markFailed(jobName);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void givenUnrelatedJobName_whenPoll_thenNoProcessorMethodCalled() {
        Message message = Message.builder()
                .body(buildSqsBody("unrelated-job-name", "COMPLETED"))
                .receiptHandle("receipt-3")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        listener.poll();

        verifyNoInteractions(processorService);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void givenEmptyQueue_whenPoll_thenNoProcessorMethodCalled() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of()).build());

        listener.poll();

        verifyNoInteractions(processorService);
    }

    @Test
    void givenProcessingThrowsException_whenPoll_thenMessageIsNotDeleted() {
        String jobName = "meeting-" + UUID.randomUUID();
        Message message = Message.builder()
                .body(buildSqsBody(jobName, "COMPLETED"))
                .receiptHandle("receipt-4")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        doThrow(new RuntimeException("DB error")).when(processorService).completeProcessing(jobName);

        listener.poll();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
