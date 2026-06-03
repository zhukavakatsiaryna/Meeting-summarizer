package com.example.meetingapi.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SummaryResponse(
        UUID id,
        UUID meetingId,
        String shortSummary,
        String detailedSummary,
        List<String> actionItems,
        List<String> decisions,
        List<String> blockers,
        Instant createdAt
) {}
