package com.example.meetingapi.service.impl;

import com.example.meetingapi.dto.MeetingUploadedEvent;
import com.example.meetingapi.entity.Meeting;
import com.example.meetingapi.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.meeting-uploaded}")
    private String meetingUploadedTopic;

    public KafkaProducerServiceImpl(KafkaTemplate<String, Object> kafkaTemplate) {
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
