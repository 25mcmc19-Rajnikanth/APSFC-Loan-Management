package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.LoginRequest;
import com.apsfc.loanmanagement.dto.request.RegisterRequest;
import com.apsfc.loanmanagement.dto.response.AuthResponse;
import com.apsfc.loanmanagement.dto.response.UserProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse currentProfile();
}