package com.example.meetingprocessor.dto;

public record MeetingUploadedEvent(
        String meetingId,
        String userId,
        String title,
        String fileUrl,
        String timestamp
) {}
