package com.example.meetingapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Service {
    String upload(String key, MultipartFile file) throws IOException;
}
