package com.example.meetingapi.repository;

import com.example.meetingapi.entity.Meeting;
import com.example.meetingapi.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, UUID> {
    Optional<MeetingSummary> findByMeeting(Meeting meeting);
}
