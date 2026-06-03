package com.example.meetingprocessor.service;

import com.example.meetingprocessor.dto.AiSummaryResult;
import com.example.meetingprocessor.dto.MeetingUploadedEvent;
import com.example.meetingprocessor.entity.Meeting;
import com.example.meetingprocessor.entity.MeetingStatus;
import com.example.meetingprocessor.entity.MeetingSummary;
import com.example.meetingprocessor.repository.MeetingRepository;
import com.example.meetingprocessor.repository.MeetingSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingProcessorServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingSummaryRepository summaryRepository;
    @Mock private AiSummarizerService aiSummarizerService;
    @Mock private TranscribeService transcribeService;
    @InjectMocks private MeetingProcessorService processorService;

    private UUID meetingId;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        meetingId = UUID.randomUUID();
        meeting = new Meeting();
        meeting.setId(meetingId);
        meeting.setStatus(MeetingStatus.UPLOADED);
    }

    @Test
    void givenExistingMeeting_whenProcess_thenStatusSetToProcessingAndTranscribeJobStarted() {
        MeetingUploadedEvent event = new MeetingUploadedEvent(
                meetingId.toString(), UUID.randomUUID().toString(),
                "Sprint Review", "s3://bucket/file.mp4", "2026-06-02T00:00:00Z");
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenReturn(meeting);

        processorService.process(event);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(MeetingStatus.PROCESSING);
        verify(transcribeService).startJob(meetingId, "s3://bucket/file.mp4");
    }

    @Test
    void givenNonExistentMeeting_whenProcess_thenThrowsIllegalArgumentException() {
        MeetingUploadedEvent event = new MeetingUploadedEvent(
                meetingId.toString(), UUID.randomUUID().toString(),
                "Sprint Review", "s3://bucket/file.mp4", "2026-06-02T00:00:00Z");
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processorService.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(meetingId.toString());
    }

    @Test
    void givenTranscribeJobFails_whenProcess_thenStatusSetToFailed() {
        MeetingUploadedEvent event = new MeetingUploadedEvent(
                meetingId.toString(), UUID.randomUUID().toString(),
                "Sprint Review", "s3://bucket/file.mp4", "2026-06-02T00:00:00Z");
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenReturn(meeting);
        doThrow(new RuntimeException("AWS error")).when(transcribeService).startJob(any(), any());

        processorService.process(event);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(m ->
                assertThat(m.getStatus()).isEqualTo(MeetingStatus.FAILED));
    }

    @Test
    void givenCompletedTranscribeJob_whenCompleteProcessing_thenSummaryIsSavedAndStatusSetToCompleted() {
        String jobName = "meeting-" + meetingId;
        String transcript = "We discussed the roadmap.";
        AiSummaryResult aiResult = new AiSummaryResult(
                "Short summary", "Detailed summary",
                List.of("Action 1"), List.of("Decision 1"), List.of());

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenReturn(meeting);
        when(transcribeService.downloadTranscript(jobName)).thenReturn(transcript);
        when(aiSummarizerService.summarize(transcript)).thenReturn(aiResult);

        processorService.completeProcessing(jobName);

        ArgumentCaptor<MeetingSummary> summaryCaptor = ArgumentCaptor.forClass(MeetingSummary.class);
        verify(summaryRepository).save(summaryCaptor.capture());
        assertThat(summaryCaptor.getValue().getShortSummary()).isEqualTo("Short summary");
        assertThat(summaryCaptor.getValue().getTranscript()).isEqualTo(transcript);

        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository, atLeastOnce()).save(meetingCaptor.capture());
        assertThat(meetingCaptor.getAllValues()).anySatisfy(m ->
                assertThat(m.getStatus()).isEqualTo(MeetingStatus.COMPLETED));
    }

    @Test
    void givenSummarizationFails_whenCompleteProcessing_thenStatusSetToFailedAndExceptionRethrown() {
        String jobName = "meeting-" + meetingId;
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenReturn(meeting);
        when(transcribeService.downloadTranscript(jobName)).thenReturn("transcript");
        when(aiSummarizerService.summarize(any())).thenThrow(new RuntimeException("AI error"));

        assertThatThrownBy(() -> processorService.completeProcessing(jobName))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(m ->
                assertThat(m.getStatus()).isEqualTo(MeetingStatus.FAILED));
    }

    @Test
    void givenExistingMeeting_whenMarkFailed_thenStatusSetToFailed() {
        String jobName = "meeting-" + meetingId;
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        processorService.markFailed(jobName);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingStatus.FAILED);
    }

    @Test
    void givenUnknownMeeting_whenMarkFailed_thenNoSaveOccurs() {
        String jobName = "meeting-" + meetingId;
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        processorService.markFailed(jobName);

        verify(meetingRepository, never()).save(any());
    }
}
