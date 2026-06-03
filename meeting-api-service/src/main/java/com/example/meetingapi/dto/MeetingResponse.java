package com.example.meetingapi.dto;

import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(UUID id, String title, String status, Instant createdAt) {}
