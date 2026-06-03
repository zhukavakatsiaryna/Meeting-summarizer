package com.example.meetingprocessor.repository;

import com.example.meetingprocessor.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {}
