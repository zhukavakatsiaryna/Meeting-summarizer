package com.example.meetingprocessor.service;

import com.example.meetingprocessor.dto.AiSummaryResult;

public interface AiSummarizerService {
    AiSummaryResult summarize(String transcript);
}
