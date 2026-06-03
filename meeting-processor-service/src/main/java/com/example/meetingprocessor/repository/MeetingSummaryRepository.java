package com.example.meetingprocessor.repository;

import com.example.meetingprocessor.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, UUID> {}
