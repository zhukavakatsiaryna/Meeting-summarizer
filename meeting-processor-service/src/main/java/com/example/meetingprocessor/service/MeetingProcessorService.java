package com.example.meetingprocessor.service;

import com.example.meetingprocessor.dto.AiSummaryResult;
import com.example.meetingprocessor.dto.MeetingUploadedEvent;
import com.example.meetingprocessor.entity.Meeting;
import com.example.meetingprocessor.entity.MeetingStatus;
import com.example.meetingprocessor.entity.MeetingSummary;
import com.example.meetingprocessor.repository.MeetingRepository;
import com.example.meetingprocessor.repository.MeetingSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MeetingProcessorService {

    private static final Logger log = LoggerFactory.getLogger(MeetingProcessorService.class);

    private final MeetingRepository meetingRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final AiSummarizerService aiSummarizerService;
    private final TranscribeService transcribeService;

    public MeetingProcessorService(MeetingRepository meetingRepository,
                                   MeetingSummaryRepository summaryRepository,
                                   AiSummarizerService aiSummarizerService,
                                   TranscribeService transcribeService) {
        this.meetingRepository = meetingRepository;
        this.summaryRepository = summaryRepository;
        this.aiSummarizerService = aiSummarizerService;
        this.transcribeService = transcribeService;
    }

    /** Called by the Kafka consumer. Starts the Transcribe job and returns immediately. */
    @Transactional
    public void process(MeetingUploadedEvent event) {
        UUID meetingId = UUID.fromString(event.meetingId());
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        try {
            meeting.setStatus(MeetingStatus.PROCESSING);
            meetingRepository.save(meeting);
            transcribeService.startJob(meetingId, event.fileUrl());
        } catch (Exception e) {
            log.error("Failed to start Transcribe job for meeting {}", meetingId, e);
            meeting.setStatus(MeetingStatus.FAILED);
            meetingRepository.save(meeting);
        }
    }

    /** Called by the SQS listener when Transcribe reports COMPLETED. */
    @Transactional
    public void completeProcessing(String jobName) {
        UUID meetingId = jobNameToMeetingId(jobName);
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));

        try {
            log.info("Downloading transcript for job {}", jobName);
            String transcript = transcribeService.downloadTranscript(jobName);

            log.info("Summarizing transcript for meeting {}", meetingId);
            AiSummaryResult result = aiSummarizerService.summarize(transcript);

            MeetingSummary summary = new MeetingSummary();
            summary.setMeetingId(meetingId);
            summary.setShortSummary(result.shortSummary());
            summary.setDetailedSummary(result.detailedSummary());
            summary.setActionItems(result.actionItems());
            summary.setDecisions(result.decisions());
            summary.setBlockers(result.blockers());
            summary.setTranscript(transcript);
            summaryRepository.save(summary);

            meeting.setStatus(MeetingStatus.COMPLETED);
            meetingRepository.save(meeting);

            log.info("Successfully completed meeting {}", meetingId);
        } catch (Exception e) {
            log.error("Failed to complete processing for meeting {}", meetingId, e);
            meeting.setStatus(MeetingStatus.FAILED);
            meetingRepository.save(meeting);
            throw e;
        }
    }

    /** Called by the SQS listener when Transcribe reports FAILED. */
    @Transactional
    public void markFailed(String jobName) {
        UUID meetingId = jobNameToMeetingId(jobName);
        meetingRepository.findById(meetingId).ifPresent(meeting -> {
            meeting.setStatus(MeetingStatus.FAILED);
            meetingRepository.save(meeting);
            log.error("Transcribe job {} failed for meeting {}", jobName, meetingId);
        });
    }

    private UUID jobNameToMeetingId(String jobName) {
        // job names are "meeting-<uuid>"
        return UUID.fromString(jobName.substring("meeting-".length()));
    }
}
