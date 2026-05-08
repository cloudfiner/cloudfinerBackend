package com.aws.controller;

import com.aws.dto.AuthResponse;
import org.springframework.security.core.GrantedAuthority;
import com.aws.dto.ChangePasswordRequest;
import com.aws.dto.ForgotPasswordRequest;
import com.aws.dto.LoginRequest;
import com.aws.dto.LoginResponse;
import com.aws.dto.ResetPasswordRequest;
import com.aws.dto.SignupDto;
import com.aws.dto.UserResponseDto;
import com.aws.entity.RefreshToken;
import com.aws.entity.User;
import com.aws.repository.RefreshTokenRepository;
import com.aws.security.CustomUserDetails;
import com.aws.security.CustomUserDetailsService;
import com.aws.security.JwtFilter;
import com.aws.security.JwtUtil;
import com.aws.service.ActivityLogService;
import com.aws.service.UserService;
import com.aws.serviceImpl.TokenBlacklistServiceImpl;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {
	

	    private final UserService userService;
	    private final AuthenticationManager authenticationManager;
	    private final JwtUtil jwtUtil;
	    private final ModelMapper modelMapper;
	    private final ActivityLogService activityLogService;
	    private final CustomUserDetailsService customUserDetailsService;
	    private final RefreshTokenRepository refreshTokenRepository;
	    private final JwtFilter jwtfiler;
	    private final  TokenBlacklistServiceImpl tokenBlacklistService;

	    public UserController(UserService userService,
	                          AuthenticationManager authenticationManager,
	                          JwtUtil jwtUtil,
	                          ModelMapper modelMapper,
	                          ActivityLogService activityLogService,
	                          CustomUserDetailsService customUserDetailsService,
	                          RefreshTokenRepository refreshTokenRepository,
	                          JwtFilter jwtfiler,
	                          TokenBlacklistServiceImpl tokenBlacklistService
	    		) {
	        this.userService = userService;
	        this.authenticationManager = authenticationManager;
	        this.jwtUtil = jwtUtil;
	        this.modelMapper = modelMapper;
	        this.activityLogService = activityLogService;
	        this.customUserDetailsService = customUserDetailsService;
	        this.refreshTokenRepository = refreshTokenRepository;
	        this.jwtfiler=jwtfiler;
	        this.tokenBlacklistService=tokenBlacklistService;
	    }

	        
    //=====================login============== 
	    @PostMapping("/login")
	    public ResponseEntity<?> login( @Valid  @RequestBody LoginRequest loginRequest,
	                                   HttpServletRequest request,
	                                   HttpServletResponse response) {
	        try {
	            Authentication authentication = authenticationManager.authenticate(
	                    new UsernamePasswordAuthenticationToken(
	                            loginRequest.email(),
	                            loginRequest.password()
	                    )
	            );

	            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

	            String accessToken = jwtUtil.generateAccessToken(userDetails);
	            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

	            // Roles as LIST (correct)
	            List<String> roles = userDetails.getAuthorities().stream()
	                    .map(GrantedAuthority::getAuthority)
	                    .toList();

	            // Upsert refresh token (single device)
	            Optional<RefreshToken> existing =
	                    refreshTokenRepository.findByUsername(userDetails.getUsername());

	            if (existing.isPresent()) {
	                RefreshToken token = existing.get();
	                token.setToken(refreshToken);
	                token.setExpiryDate(Instant.now().plusSeconds(60 * 60 * 24 * 30));
	                token.setRevoked(false);
	                token.setIpAddress(request.getRemoteAddr());
	                token.setDevice(request.getHeader("User-Agent"));
	                refreshTokenRepository.save(token);
	            } else {
	                RefreshToken token = new RefreshToken();
	                token.setUsername(userDetails.getUsername());
	                token.setToken(refreshToken);
	                token.setExpiryDate(Instant.now().plusSeconds(60 * 60 * 24 * 30));
	                token.setRevoked(false);
	                token.setIpAddress(request.getRemoteAddr());
	                token.setDevice(request.getHeader("User-Agent"));
	                refreshTokenRepository.save(token);
	            }

	            // Cookie (dev config)
	            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
	                    .httpOnly(true)
	                    .secure(true)
	                    .sameSite("None")
	                    .path("/")
	                    .maxAge(60 * 60 * 24 * 30)
	                    .build();

	            response.addHeader("Set-Cookie", cookie.toString());

	            // IMPORTANT: send roles as LIST (not String)
	            AuthResponse authResponse = new AuthResponse(
	            	     accessToken,
	            	        refreshToken,
	            	        userDetails.getUsername(),
	            	        userDetails.getUsername(),
	            	        roles,
	            	        86400000L
	            );

	            activityLogService.log("LOGIN", userDetails.getUsername(), request.getRemoteAddr());

	            return ResponseEntity.ok(authResponse);

	        } catch (AuthenticationException e) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(Map.of("error", "Invalid email or password"));
	        }
	    }
    //================LOGOUT===================
	    @PostMapping("/logout")
	    public ResponseEntity<?> logout(
	            @CookieValue(value = "refreshToken", required = false) String refreshToken,
	            HttpServletRequest request,
	            HttpServletResponse response) {

	        // 🔐 1. Access Token nikaalo
	        String authHeader = request.getHeader("Authorization");
	        String accessToken = null;

	        if (authHeader != null && authHeader.startsWith("Bearer ")) {
	            accessToken = authHeader.substring(7);
	        }

	        // 🚫 2. Access Token → Redis blacklist
	        if (accessToken != null) {
	            long expiry = jwtUtil.getRemainingTime(accessToken);
	            tokenBlacklistService.blacklistToken(accessToken, expiry);
	        }

	        // 🔁 3. Refresh Token → DB revoke
	        if (refreshToken != null) {
	            refreshTokenRepository.findByToken(refreshToken)
	                    .ifPresent(token -> {
	                        token.setRevoked(true);
	                        refreshTokenRepository.save(token);
	                    });
	        }

	        // 🍪 4. Cookie delete (IMPORTANT)
	        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
	                .httpOnly(true)
	                .secure(false) // dev: false | prod: true
	                .path("/")
	                .maxAge(0)
	                .sameSite("Lax")
	                .build();

	        response.addHeader("Set-Cookie", cookie.toString());

	        // ✅ 5. Always success (idempotent)
	        return ResponseEntity.ok(
	                Map.of("message", "Logged out successfully")
	        );
	    }
  
 // ==================== REGISTER USER ====================
    @PostMapping("/register")
    public ResponseEntity<?> register( @RequestBody SignupDto signupDto,
                                      HttpServletRequest request) {

        String message = userService.register(signupDto);

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        String userAgentStr = request.getHeader("User-Agent");
        UserAgent agent = UserAgent.parseUserAgentString(userAgentStr);

        String browser = agent.getBrowser().getName();
        String os = agent.getOperatingSystem().getName();

        activityLogService.log(
            "CREATE_USER",
            signupDto.getEmail(),
            "IP=" + ip + ", Browser=" + browser + ", OS=" + os
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
 // ==================== REGISTER ADMIN ====================
    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody SignupDto signupDto,
                                            HttpServletRequest request) {

        String message = userService.registerAdmin(signupDto);
        
        

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        String userAgentStr = request.getHeader("User-Agent");
        UserAgent agent = UserAgent.parseUserAgentString(userAgentStr);

        String browser = agent.getBrowser().getName();
        String os = agent.getOperatingSystem().getName();

        activityLogService.log(
            "CREATE_ADMIN",
            signupDto.getEmail(),
            "IP=" + ip + ", Browser=" + browser + ", OS=" + os
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    

    //===============api for refresh token============================
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token missing"));
        }

        try {
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Token not found"));

            if (storedToken.isRevoked() ||
                    storedToken.getExpiryDate().isBefore(java.time.Instant.now())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token expired or revoked"));
            }

            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            String newAccessToken = jwtUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 🔥 Rotate token (update existing)
            storedToken.setToken(newRefreshToken);
            storedToken.setExpiryDate(java.time.Instant.now().plusSeconds(60 * 60 * 24 * 30));
            refreshTokenRepository.save(storedToken);

            // 🔥 Cookie fix
            ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(false) // dev
                    .path("/")
                    .maxAge(60 * 60 * 24 * 30)
                    .sameSite("Lax")
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid request"));
        }
    }
 // ==================== GET USER BY EMAIL ====================
   

    @GetMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserByEmail(@Valid  @PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
    
    
    
    // ==================== ✅  List All Users API (Admin Only)  ====================
    
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")  // Only ADMIN can access
    public ResponseEntity<List<UserResponseDto>> listUsers(
    		     @Valid 
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        List<UserResponseDto> users = userService.listUsers(role, status);
        return ResponseEntity.ok(users);
    }
    
 // ==================== DELETE USER ====================
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("User deleted successfully");
    }
    
    
    //================activate user
    
    @PutMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> activateUser( @Valid  @PathVariable UUID id) {

        userService.activateUser(id);

        return ResponseEntity.ok("User activated successfully");
    }
    
 // ================= CHANGE PASSWORD =================
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {

        String res = userService.changePassword(user.getUsername(), request);
        return ResponseEntity.ok(res);
    }
    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String res = userService.forgotPassword(request);
        return ResponseEntity.ok(res);
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        String res = userService.resetPassword(request);
        return ResponseEntity.ok(res);
    }
    
    //====================Check profile=============
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal CustomUserDetails user) {

        UserResponseDto profile = userService.getProfile(user.getUsername());

        return ResponseEntity.ok(profile);
    }
}