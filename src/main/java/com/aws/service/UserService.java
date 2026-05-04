package com.aws.service;

import java.util.List;
import java.util.UUID;

import com.aws.dto.ChangePasswordRequest;
import com.aws.dto.ForgotPasswordRequest;
import com.aws.dto.LoginRequest;
import com.aws.dto.LoginResponse;
import com.aws.dto.ResetPasswordRequest;
import com.aws.dto.SignupDto;
import com.aws.dto.UserResponseDto;




public interface UserService {

	LoginResponse processLogin(LoginRequest loginRequest, String accessToken, String refreshToken);
	 String register(SignupDto dto);
	 String registerAdmin(SignupDto signupDto);
	 UserResponseDto findByEmail(String email);
	 List<UserResponseDto> listUsers(String role, String status);
	 public String changePassword(String email, ChangePasswordRequest request);
	 void deleteUser(UUID id);
	 void activateUser(UUID id);
	 public String forgotPassword(ForgotPasswordRequest request);
	 public String resetPassword(ResetPasswordRequest request);
	 
	   UserResponseDto getProfile(String email);
	 
  

}
