package com.example.meetingapi.controller;

import com.example.meetingapi.dto.MeetingResponse;
import com.example.meetingapi.dto.SummaryResponse;
import com.example.meetingapi.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse upload(
            @RequestParam String title,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        return meetingService.uploadVideo(title, file, userDetails.getUsername());
    }

    @GetMapping
    public List<MeetingResponse> list(@AuthenticationPrincipal UserDetails userDetails) {
        return meetingService.getUserMeetings(userDetails.getUsername());
    }

    @GetMapping("/{id}")
    public MeetingResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        return meetingService.getMeeting(id, userDetails.getUsername());
    }

    @GetMapping("/{id}/summary")
    public SummaryResponse getSummary(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        return meetingService.getSummary(id, userDetails.getUsername());
    }
}
