package com.example.meetingapi.repository;

import com.example.meetingapi.entity.Meeting;
import com.example.meetingapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    List<Meeting> findByUserOrderByCreatedAtDesc(User user);
}
