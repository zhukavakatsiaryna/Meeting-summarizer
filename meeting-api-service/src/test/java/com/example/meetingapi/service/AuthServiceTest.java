package com.example.meetingapi.service;

import com.example.meetingapi.dto.LoginRequest;
import com.example.meetingapi.dto.LoginResponse;
import com.example.meetingapi.dto.RegisterRequest;
import com.example.meetingapi.entity.User;
import com.example.meetingapi.entity.UserRole;
import com.example.meetingapi.repository.UserRepository;
import com.example.meetingapi.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.meetingapi.service.impl.AuthServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void givenNewEmail_whenRegister_thenUserIsSavedWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("new@example.com", "secret");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-secret");
    }

    @Test
    void givenExistingEmail_whenRegister_thenThrowsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "secret");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenValidCredentials_whenLogin_thenReturnsTokenAndUserInfo() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo("USER");
    }
}
