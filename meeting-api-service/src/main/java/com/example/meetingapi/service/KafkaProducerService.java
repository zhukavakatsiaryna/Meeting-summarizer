package com.example.meetingapi.service;

import com.example.meetingapi.dto.MeetingUploadedEvent;
import com.example.meetingapi.entity.Meeting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.meeting-uploaded}")
    private String meetingUploadedTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMeetingUploaded(Meeting meeting) {
        MeetingUploadedEvent event = new MeetingUploadedEvent(
                meeting.getId().toString(),
                meeting.getUser().getId().toString(),
                meeting.getTitle(),
                meeting.getFileUrl(),
                Instant.now().toString()
        );
        kafkaTemplate.send(meetingUploadedTopic, meeting.getId().toString(), event);
    }
}
