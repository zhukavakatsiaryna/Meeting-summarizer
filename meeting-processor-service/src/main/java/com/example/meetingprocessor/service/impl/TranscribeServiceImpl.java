package com.example.meetingprocessor.service.impl;

import com.example.meetingprocessor.service.TranscribeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;

import java.util.Map;
import java.util.UUID;

@Service
public class TranscribeServiceImpl implements TranscribeService {

    private static final Logger log = LoggerFactory.getLogger(TranscribeService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, MediaFormat> EXTENSION_TO_FORMAT = Map.of(
            "mp4", MediaFormat.MP4,
            "m4a", MediaFormat.MP4,
            "mov", MediaFormat.MP4,
            "mp3", MediaFormat.MP3,
            "wav", MediaFormat.WAV,
            "flac", MediaFormat.FLAC,
            "ogg", MediaFormat.OGG,
            "webm", MediaFormat.WEBM,
            "amr", MediaFormat.AMR
    );

    private final TranscribeClient transcribeClient;
    private final S3Client s3Client;

    public TranscribeServiceImpl(TranscribeClient transcribeClient, S3Client s3Client) {
        this.transcribeClient = transcribeClient;
        this.s3Client = s3Client;
    }

    public String startJob(UUID meetingId, String s3Url) {
        String jobName = "meeting-" + meetingId;
        String[] parts = parseS3Url(s3Url);
        String bucket = parts[0];
        String sourceKey = parts[1];

        transcribeClient.startTranscriptionJob(StartTranscriptionJobRequest.builder()
                .transcriptionJobName(jobName)
                .media(Media.builder().mediaFileUri(s3Url).build())
                .mediaFormat(detectFormat(sourceKey))
                .outputBucketName(bucket)
                .outputKey("transcripts/" + jobName + ".json")
                .identifyLanguage(true)
                .build());

        log.info("Started Transcribe job {} for meeting {}", jobName, meetingId);
        return jobName;
    }

    public String downloadTranscript(String jobName) {
        String transcriptUri = transcribeClient.getTranscriptionJob(
                GetTranscriptionJobRequest.builder().transcriptionJobName(jobName).build()
        ).transcriptionJob().transcript().transcriptFileUri();

        String[] parts = parseS3Url(transcriptUri);
        byte[] resultJson = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(parts[0]).key(parts[1]).build()
        ).asByteArray();

        try {
            JsonNode root = MAPPER.readTree(resultJson);
            return root.path("results").path("transcripts").get(0).path("transcript").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Transcribe output for job " + jobName, e);
        }
    }

    private MediaFormat detectFormat(String key) {
        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        return EXTENSION_TO_FORMAT.getOrDefault(ext, MediaFormat.MP4);
    }

    private String[] parseS3Url(String s3Url) {
        if (s3Url.startsWith("s3://")) {
            String withoutScheme = s3Url.substring("s3://".length());
            int slash = withoutScheme.indexOf('/');
            return new String[]{withoutScheme.substring(0, slash), withoutScheme.substring(slash + 1)};
        }
        String path = s3Url.replaceFirst("https?://", "");
        if (path.contains(".s3.amazonaws.com/")) {
            String[] hostAndKey = path.split("\\.s3\\.amazonaws\\.com/", 2);
            return new String[]{hostAndKey[0], hostAndKey[1]};
        }
        String withoutHost = path.substring(path.indexOf('/') + 1);
        int slash = withoutHost.indexOf('/');
        return new String[]{withoutHost.substring(0, slash), withoutHost.substring(slash + 1)};
    }
}
