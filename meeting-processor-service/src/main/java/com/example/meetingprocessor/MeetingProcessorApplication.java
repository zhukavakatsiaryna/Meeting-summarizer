package com.example.meetingprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingProcessorApplication.class, args);
    }
}
