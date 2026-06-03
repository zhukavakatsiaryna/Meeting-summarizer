package com.example.meetingapi.service;

import com.example.meetingapi.entity.Meeting;

public interface KafkaProducerService {
    void publishMeetingUploaded(Meeting meeting);
}
