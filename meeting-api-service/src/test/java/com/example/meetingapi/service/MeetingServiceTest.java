package com.example.meetingapi.service;

import com.example.meetingapi.dto.MeetingResponse;
import com.example.meetingapi.dto.SummaryResponse;
import com.example.meetingapi.entity.*;
import com.example.meetingapi.exception.ForbiddenException;
import com.example.meetingapi.exception.ResourceNotFoundException;
import com.example.meetingapi.repository.MeetingRepository;
import com.example.meetingapi.repository.MeetingSummaryRepository;
import com.example.meetingapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingSummaryRepository summaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private S3Service s3Service;
    @InjectMocks private MeetingService meetingService;

    private User owner;
    private UUID meetingId;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());
        owner.setEmail("owner@example.com");

        meetingId = UUID.randomUUID();
        meeting = new Meeting();
        ReflectionTestUtils.setField(meeting, "id", meetingId);
        meeting.setUser(owner);
        meeting.setTitle("Quarterly Review");
        meeting.setFileUrl("s3://bucket/meetings/file.mp4");
        meeting.setStatus(MeetingStatus.UPLOADED);
    }

    @Test
    void givenValidUserAndFile_whenUploadVideo_thenMeetingSavedAndKafkaEventPublished() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "meeting.mp4", "video/mp4", "data".getBytes());
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(s3Service.upload(any(), any())).thenReturn("s3://bucket/meetings/meeting.mp4");
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> {
            Meeting m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });

        MeetingResponse response = meetingService.uploadVideo("Quarterly Review", file, "owner@example.com");

        verify(meetingRepository).save(any(Meeting.class));
        verify(kafkaProducerService).publishMeetingUploaded(any(Meeting.class));
        assertThat(response.title()).isEqualTo("Quarterly Review");
        assertThat(response.status()).isEqualTo("UPLOADED");
    }

    @Test
    void givenUnknownUser_whenUploadVideo_thenThrowsResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile("file", "meeting.mp4", "video/mp4", "data".getBytes());
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.uploadVideo("Title", file, "ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenUserWithMeetings_whenGetUserMeetings_thenReturnsAllMeetings() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(meetingRepository.findByUserOrderByCreatedAtDesc(owner)).thenReturn(List.of(meeting));

        List<MeetingResponse> result = meetingService.getUserMeetings("owner@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Quarterly Review");
    }

    @Test
    void givenMeetingOwner_whenGetMeeting_thenReturnsMeetingResponse() {
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        MeetingResponse response = meetingService.getMeeting(meetingId, "owner@example.com");

        assertThat(response.id()).isEqualTo(meetingId);
        assertThat(response.status()).isEqualTo("UPLOADED");
    }

    @Test
    void givenDifferentUser_whenGetMeeting_thenThrowsForbiddenException() {
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.getMeeting(meetingId, "other@example.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void givenUnknownMeetingId_whenGetMeeting_thenThrowsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(meetingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeeting(unknownId, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenCompletedMeetingWithSummary_whenGetSummary_thenReturnsSummaryResponse() {
        MeetingSummary summary = new MeetingSummary();
        ReflectionTestUtils.setField(summary, "id", UUID.randomUUID());
        summary.setMeeting(meeting);
        summary.setShortSummary("Short summary");
        summary.setDetailedSummary("Detailed summary");
        summary.setActionItems(List.of("Action 1"));
        summary.setDecisions(List.of("Decision 1"));
        summary.setBlockers(List.of());

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(summaryRepository.findByMeeting(meeting)).thenReturn(Optional.of(summary));

        SummaryResponse response = meetingService.getSummary(meetingId, "owner@example.com");

        assertThat(response.shortSummary()).isEqualTo("Short summary");
        assertThat(response.actionItems()).containsExactly("Action 1");
    }

    @Test
    void givenMeetingWithNoSummaryYet_whenGetSummary_thenThrowsResourceNotFoundException() {
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(summaryRepository.findByMeeting(meeting)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getSummary(meetingId, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
