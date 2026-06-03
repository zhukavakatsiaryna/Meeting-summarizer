package com.example.meetingprocessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSummaryResult(
        String shortSummary,
        String detailedSummary,
        List<String> actionItems,
        List<String> decisions,
        List<String> blockers
) {}
