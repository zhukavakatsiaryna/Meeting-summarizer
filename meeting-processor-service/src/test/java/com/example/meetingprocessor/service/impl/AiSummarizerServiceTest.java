package com.example.meetingprocessor.service.impl;

import com.example.meetingprocessor.dto.AiSummaryResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSummarizerServiceTest {

    private static final String VALID_API_RESPONSE = """
            {
              "choices": [{
                "message": {
                  "content": "{\\"shortSummary\\":\\"Short overview\\",\\"detailedSummary\\":\\"Full details\\",\\"actionItems\\":[\\"Do X\\"],\\"decisions\\":[\\"Use Y\\"],\\"blockers\\":[]}"
                }
              }]
            }
            """;

    @Test
    void givenBlankApiKey_whenSummarize_thenReturnsPlaceholderSummary() {
        AiSummarizerServiceImpl service = new AiSummarizerServiceImpl("", "gpt-4o-mini", mock(RestClient.class));

        AiSummaryResult result = service.summarize("Some transcript");

        assertThat(result.shortSummary()).isEqualTo("AI summarization not configured");
        assertThat(result.actionItems()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenValidApiKeyAndSuccessfulResponse_whenSummarize_thenReturnsAiResult() {
        AiSummarizerServiceImpl service = spy(new AiSummarizerServiceImpl("sk-real-key", "gpt-4o-mini", mock(RestClient.class)));
        doReturn(VALID_API_RESPONSE).when(service).callOpenAi(any(Map.class));

        AiSummaryResult result = service.summarize("Meeting transcript content");

        assertThat(result.shortSummary()).isEqualTo("Short overview");
        assertThat(result.detailedSummary()).isEqualTo("Full details");
        assertThat(result.actionItems()).containsExactly("Do X");
        assertThat(result.decisions()).containsExactly("Use Y");
        assertThat(result.blockers()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenApiCallThrowsException_whenSummarize_thenReturnsFailureSummary() {
        AiSummarizerServiceImpl service = spy(new AiSummarizerServiceImpl("sk-real-key", "gpt-4o-mini", mock(RestClient.class)));
        doThrow(new RuntimeException("Connection refused")).when(service).callOpenAi(any(Map.class));

        AiSummaryResult result = service.summarize("Meeting transcript content");

        assertThat(result.shortSummary()).isEqualTo("Summarization failed");
        assertThat(result.actionItems()).isEmpty();
    }
}
