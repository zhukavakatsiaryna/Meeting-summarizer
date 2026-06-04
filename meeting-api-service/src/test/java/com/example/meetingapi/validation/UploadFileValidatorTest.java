package com.example.meetingapi.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadFileValidatorTest {

    private final UploadFileValidator validator = new UploadFileValidator();

    @ParameterizedTest
    @ValueSource(strings = {"mp4", "mov", "m4a", "mp3", "wav", "flac", "ogg", "webm", "amr"})
    void validate_acceptsAllowedExtensions(String ext) {
        MockMultipartFile file = new MockMultipartFile("file", "meeting." + ext, "video/*", new byte[]{1});
        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    @Test
    void validate_throwsOnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "meeting.mp4", "video/mp4", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File must not be empty");
    }

    @Test
    void validate_throwsWhenFileTooLarge() {
        byte[] content = new byte[1];
        MockMultipartFile file = new MockMultipartFile("file", "meeting.mp4", "video/mp4", content) {
            @Override
            public long getSize() {
                return UploadFileValidator.MAX_FILE_SIZE_BYTES + 1;
            }
        };
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File exceeds the 500 MB size limit");
    }

    @Test
    void validate_throwsWhenNoExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "meeting", "video/mp4", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File must have an extension");
    }

    @Test
    void validate_throwsWhenNullFilename() {
        MockMultipartFile file = new MockMultipartFile("file", null, "video/mp4", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File must have an extension");
    }

    @Test
    void validate_throwsOnUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "meeting.avi", "video/avi", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type: avi");
    }

    @Test
    void validate_isCaseInsensitiveForExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "meeting.MP4", "video/mp4", new byte[]{1});
        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }
}
