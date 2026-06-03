package com.example.meetingprocessor.service;

import java.util.UUID;

public interface TranscribeService {
    String startJob(UUID meetingId, String s3Url);
    String downloadTranscript(String jobName);
}
