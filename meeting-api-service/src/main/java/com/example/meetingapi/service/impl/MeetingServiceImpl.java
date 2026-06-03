package com.example.meetingapi.service.impl;

import com.example.meetingapi.dto.MeetingResponse;
import com.example.meetingapi.dto.SummaryResponse;
import com.example.meetingapi.entity.Meeting;
import com.example.meetingapi.entity.MeetingSummary;
import com.example.meetingapi.entity.MeetingStatus;
import com.example.meetingapi.entity.User;
import com.example.meetingapi.exception.ForbiddenException;
import com.example.meetingapi.exception.ResourceNotFoundException;
import com.example.meetingapi.repository.MeetingRepository;
import com.example.meetingapi.repository.MeetingSummaryRepository;
import com.example.meetingapi.repository.UserRepository;
import com.example.meetingapi.service.KafkaProducerService;
import com.example.meetingapi.service.MeetingService;
import com.example.meetingapi.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final S3Service s3Service;

    public MeetingServiceImpl(MeetingRepository meetingRepository, MeetingSummaryRepository summaryRepository,
                              UserRepository userRepository, KafkaProducerService kafkaProducerService,
                              S3Service s3Service) {
        this.meetingRepository = meetingRepository;
        this.summaryRepository = summaryRepository;
        this.userRepository = userRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.s3Service = s3Service;
    }

    public MeetingResponse uploadVideo(String title, MultipartFile file, String userEmail) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String key = "meetings/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        String fileUrl = s3Service.upload(key, file);

        Meeting meeting = new Meeting();
        meeting.setUser(user);
        meeting.setTitle(title);
        meeting.setFileUrl(fileUrl);
        meeting.setStatus(MeetingStatus.UPLOADED);
        meeting = meetingRepository.save(meeting);

        kafkaProducerService.publishMeetingUploaded(meeting);
        return toResponse(meeting);
    }

    public List<MeetingResponse> getUserMeetings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return meetingRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public MeetingResponse getMeeting(UUID id, String userEmail) {
        Meeting meeting = findAndAuthorize(id, userEmail);
        return toResponse(meeting);
    }

    public SummaryResponse getSummary(UUID id, String userEmail) {
        Meeting meeting = findAndAuthorize(id, userEmail);
        MeetingSummary summary = summaryRepository.findByMeeting(meeting)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Summary not ready yet. Meeting status: " + meeting.getStatus()));
        return toSummaryResponse(summary);
    }

    private Meeting findAndAuthorize(UUID id, String userEmail) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + id));
        if (!meeting.getUser().getEmail().equals(userEmail)) {
            throw new ForbiddenException("Access denied");
        }
        return meeting;
    }

    private MeetingResponse toResponse(Meeting m) {
        return new MeetingResponse(m.getId(), m.getTitle(), m.getStatus().name(), m.getCreatedAt());
    }

    private SummaryResponse toSummaryResponse(MeetingSummary s) {
        return new SummaryResponse(
                s.getId(), s.getMeeting().getId(),
                s.getShortSummary(), s.getDetailedSummary(),
                s.getActionItems(), s.getDecisions(), s.getBlockers(),
                s.getCreatedAt()
        );
    }
}
