package com.example.meetingapi.security;

import com.example.meetingapi.entity.User;
import com.example.meetingapi.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtTokenProvider jwtTokenProvider;
    private User user;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
    }

    @Test
    void givenUser_whenGenerateToken_thenTokenIsNotBlank() {
        String token = jwtTokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
    }

    @Test
    void givenValidToken_whenIsValid_thenReturnsTrue() {
        String token = jwtTokenProvider.generateToken(user);

        assertThat(jwtTokenProvider.isValid(token)).isTrue();
    }

    @Test
    void givenTamperedToken_whenIsValid_thenReturnsFalse() {
        assertThat(jwtTokenProvider.isValid("not.a.valid.token")).isFalse();
    }

    @Test
    void givenExpiredToken_whenIsValid_thenReturnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, -1L);
        String token = shortLived.generateToken(user);

        assertThat(jwtTokenProvider.isValid(token)).isFalse();
    }

    @Test
    void givenValidToken_whenGetEmail_thenReturnsUserEmail() {
        String token = jwtTokenProvider.generateToken(user);

        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("user@example.com");
    }
}
