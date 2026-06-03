package com.example.meetingprocessor.service;

import com.example.meetingprocessor.dto.MeetingUploadedEvent;

public interface MeetingProcessorService {
    void process(MeetingUploadedEvent event);
    void completeProcessing(String jobName);
    void markFailed(String jobName);
}
