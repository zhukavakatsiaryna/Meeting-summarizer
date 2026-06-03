package com.example.meetingapi.service;

import com.example.meetingapi.dto.MeetingResponse;
import com.example.meetingapi.dto.SummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface MeetingService {
    MeetingResponse uploadVideo(String title, MultipartFile file, String userEmail) throws IOException;
    List<MeetingResponse> getUserMeetings(String userEmail);
    MeetingResponse getMeeting(UUID id, String userEmail);
    SummaryResponse getSummary(UUID id, String userEmail);
}
