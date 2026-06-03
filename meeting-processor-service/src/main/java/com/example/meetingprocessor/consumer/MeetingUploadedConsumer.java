package com.example.meetingprocessor.consumer;

import com.example.meetingprocessor.dto.MeetingUploadedEvent;
import com.example.meetingprocessor.service.MeetingProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MeetingUploadedConsumer {

    private static final Logger log = LoggerFactory.getLogger(MeetingUploadedConsumer.class);

    private final MeetingProcessorService processorService;

    public MeetingUploadedConsumer(MeetingProcessorService processorService) {
        this.processorService = processorService;
    }

    @KafkaListener(topics = "meeting.uploaded", groupId = "meeting-processor-group")
    public void consume(MeetingUploadedEvent event) {
        log.info("Received meeting.uploaded for meetingId={}", event.meetingId());
        processorService.process(event);
    }
}
