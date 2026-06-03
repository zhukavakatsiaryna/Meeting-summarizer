package com.example.meetingapi.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meeting_summaries")
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "short_summary", columnDefinition = "TEXT")
    private String shortSummary;

    @Column(name = "detailed_summary", columnDefinition = "TEXT")
    private String detailedSummary;

    @Convert(converter = StringListConverter.class)
    @Column(name = "action_items", columnDefinition = "TEXT")
    private List<String> actionItems;

    @Convert(converter = StringListConverter.class)
    @Column(name = "decisions", columnDefinition = "TEXT")
    private List<String> decisions;

    @Convert(converter = StringListConverter.class)
    @Column(name = "blockers", columnDefinition = "TEXT")
    private List<String> blockers;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }
    public String getShortSummary() { return shortSummary; }
    public void setShortSummary(String shortSummary) { this.shortSummary = shortSummary; }
    public String getDetailedSummary() { return detailedSummary; }
    public void setDetailedSummary(String detailedSummary) { this.detailedSummary = detailedSummary; }
    public List<String> getActionItems() { return actionItems; }
    public void setActionItems(List<String> actionItems) { this.actionItems = actionItems; }
    public List<String> getDecisions() { return decisions; }
    public void setDecisions(List<String> decisions) { this.decisions = decisions; }
    public List<String> getBlockers() { return blockers; }
    public void setBlockers(List<String> blockers) { this.blockers = blockers; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public Instant getCreatedAt() { return createdAt; }
}
