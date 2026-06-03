package com.example.meetingapi.service;

import com.example.meetingapi.dto.LoginRequest;
import com.example.meetingapi.dto.LoginResponse;
import com.example.meetingapi.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
