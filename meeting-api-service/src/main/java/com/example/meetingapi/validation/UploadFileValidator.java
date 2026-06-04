package com.example.meetingapi.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class UploadFileValidator {

    static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("mp4", "mov", "m4a", "mp3", "wav", "flac", "ogg", "webm", "amr");
    static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 500 MB size limit");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + ext + ". Allowed: " + ALLOWED_EXTENSIONS);
        }
    }
}
