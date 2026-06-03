package com.example.meetingapi.service;

import com.example.meetingapi.entity.Meeting;
import com.example.meetingapi.entity.User;
import com.example.meetingapi.service.impl.KafkaProducerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private KafkaProducerServiceImpl kafkaProducerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaProducerService, "meetingUploadedTopic", "meeting.uploaded");
    }

    @Test
    void givenMeeting_whenPublishMeetingUploaded_thenSendsToCorrectTopicWithMeetingId() {
        UUID meetingId = UUID.randomUUID();
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail("user@example.com");

        Meeting meeting = new Meeting();
        ReflectionTestUtils.setField(meeting, "id", meetingId);
        meeting.setUser(user);
        meeting.setTitle("Sprint Review");
        meeting.setFileUrl("s3://bucket/meetings/file.mp4");

        kafkaProducerService.publishMeetingUploaded(meeting);

        verify(kafkaTemplate).send(eq("meeting.uploaded"), eq(meetingId.toString()), any());
    }

    @Test
    void givenMeeting_whenPublishMeetingUploaded_thenEventContainsCorrectMeetingId() {
        UUID meetingId = UUID.randomUUID();
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail("user@example.com");

        Meeting meeting = new Meeting();
        ReflectionTestUtils.setField(meeting, "id", meetingId);
        meeting.setUser(user);
        meeting.setTitle("Sprint Review");
        meeting.setFileUrl("s3://bucket/meetings/file.mp4");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        kafkaProducerService.publishMeetingUploaded(meeting);

        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().toString()).contains(meetingId.toString());
    }
}
