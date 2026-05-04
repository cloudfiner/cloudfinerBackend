package com.aws.serviceImpl;
import com.aws.dto.ChangePasswordRequest;
import com.aws.dto.ForgotPasswordRequest;
import com.aws.dto.LoginRequest;
import com.aws.dto.LoginResponse;
import com.aws.dto.ResetPasswordRequest;
import com.aws.dto.SignupDto;
import com.aws.dto.UserResponseDto;
import com.aws.entity.PasswordResetToken;
import com.aws.entity.Role;
import com.aws.entity.User;
import com.aws.exception.EmailAlreadyExistsException;
import com.aws.exception.InvalidOtpException;
import com.aws.exception.InvalidPasswordException;
import com.aws.exception.UserNotFoundException;
import com.aws.repository.PasswordResetTokenRepository;
import com.aws.repository.RoleRepository;
import com.aws.repository.UserRepository;
import com.aws.service.ActivityLogService;
import com.aws.service.UserService;


import jakarta.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final ActivityLogService activityLogService;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;


    public UserServiceImpl(UserRepository userRepository,  
    		PasswordEncoder passwordEncoder,
    		RoleRepository roleRepository,
    		ModelMapper modelMapper,
    		ActivityLogService activityLogService,
    		PasswordResetTokenRepository tokenRepository,
    		 JavaMailSender mailSender
    		) {
        this.userRepository = userRepository;
        this.passwordEncoder=passwordEncoder;
        this.roleRepository=roleRepository;
        this.modelMapper = modelMapper;
        this.activityLogService=activityLogService;
        this.tokenRepository=tokenRepository;
        this.mailSender=mailSender;
    }

    @Override
    public LoginResponse processLogin(LoginRequest loginRequest,
                                      String accessToken,
                                      String refreshToken) {

        //  Get user from DB
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() ->
                        new RuntimeException("User not found with email: " + loginRequest.email())
                );

        //  Convert roles to String list
        List<String> roles = user.getRoles().stream()
                .map(Role::getName) // ROLE_ADMIN, ROLE_USER
                .collect(Collectors.toList());
        
       

        //  Return response
        return new LoginResponse(
        		    
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName(),
                roles
        );
    }

    @Transactional
    @Override
    public String register(SignupDto signupDto) {

        // 1. Email check
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // 2. Get USER role (fixed role 🔐)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // 3. Create user
        User user = new User();
        user.setName(signupDto.getName());
        user.setEmail(signupDto.getEmail());
        user.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));

        
        userRepository.save(user);

        // 4. Save
       

        return "User registered successfully ";
    }

    @Override
    @Transactional
    public String registerAdmin(SignupDto signupDto) {

        // 1. Email check
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // 2. Get ADMIN role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        // 3. Create admin user
        User admin = new User();
        admin.setName(signupDto.getName());
        admin.setEmail(signupDto.getEmail());
        admin.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole));

        // 4. Save
        userRepository.save(admin);
        
     

        return "Admin registered successfully ";
    }
    
    @Override
    public List<UserResponseDto> listUsers(String role, String status) {

        List<User> users;

        if (role != null && status != null) {
            boolean isActive = status.equalsIgnoreCase("ACTIVE");
            String formattedRole = "ROLE_" + role.toUpperCase();

            users = userRepository.findByRoles_NameAndEnabled(formattedRole, isActive);

        } else if (role != null) {
            String formattedRole = "ROLE_" + role.toUpperCase();

            users = userRepository.findByRoles_Name(formattedRole);

        } else if (status != null) {
            boolean isActive = status.equalsIgnoreCase("ACTIVE");

            users = userRepository.findByEnabled(isActive);

        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(user -> {
                    UserResponseDto dto = modelMapper.map(user, UserResponseDto.class);
                    List<String> roles = user.getRoles()
                            .stream()
                            .map(r -> r.getName())
                            .toList();

                    dto.setRoles(roles);

                    dto.setStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");

                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    
    @Override
    public UserResponseDto findByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDto(user);
    }
    
    public UserResponseDto mapToDto(User user) {

        UserResponseDto dto = modelMapper.map(user, UserResponseDto.class);

        List<String> roles = user.getRoles()
                .stream()
                .map(r -> r.getName())
                .toList();

        dto.setRoles(roles);


        dto.setStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");

        return dto;
    }
    
    @Override
    public void deleteUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        //  Already inactive check (optional but professional)
        if (!user.isEnabled()) {
            throw new IllegalStateException("User is already inactive");
        }

        // ✅ Soft delete
        user.setEnabled(false);

        userRepository.save(user);
        
        activityLogService.log(
                "DEACTIVATE_USER",
                user.getEmail(),
                "User deactivated"
            );
    }
    
    @Override
    public void activateUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalStateException("User already active");
        }

        user.setEnabled(true);

        userRepository.save(user);
    }
    
    
    //===========change password =========
    @Override
    public String changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be same as old");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully";
    }
    
    @Override
    public String forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // OTP generate
        SecureRandom rnd = new SecureRandom();
        String otp = String.format("%06d", rnd.nextInt(1_000_000));

        String hashedOtp = passwordEncoder.encode(otp);

        Instant expiry = Instant.now().plus(5, ChronoUnit.MINUTES);

        PasswordResetToken token = tokenRepository.findByEmail(email)
                .orElse(new PasswordResetToken());

        token.setEmail(email);
        token.setOtpHash(hashedOtp);
        token.setExpiryTime(expiry);
        token.setUsed(false);

        tokenRepository.save(token);

        // send mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("CloudFiner - Password Reset OTP");
        message.setText("Your OTP is: " + otp + "\nValid for 5 minutes.");

        mailSender.send(message);

        return "OTP sent to email";
    }
    
    @Override
    public String resetPassword(ResetPasswordRequest request) {

        PasswordResetToken token = tokenRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidOtpException("Invalid email or OTP"));

        if (token.isUsed()) {
            throw new InvalidOtpException("OTP already used");
        }

        if (token.getExpiryTime().isBefore(Instant.now())) {
            throw new InvalidOtpException("OTP expired");
        }

        if (!passwordEncoder.matches(request.getOtp(), token.getOtpHash())) {
            throw new InvalidOtpException("Invalid OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be same as old");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // ✅ mark OTP used
        token.setUsed(true);
        tokenRepository.save(token);

        return "Password reset successful";
    }
    
    @Override
    public UserResponseDto getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponseDto dto = new UserResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());

        //  MULTIPLE ROLES → LIST
        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .toList();

        dto.setRoles(roles);

        //  STATUS MAPPING
        dto.setStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");

        return dto;
    }
}