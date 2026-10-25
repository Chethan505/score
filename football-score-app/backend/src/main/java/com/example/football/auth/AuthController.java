package com.example.football.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WhatsAppService whatsAppService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, WhatsAppService whatsAppService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        User user = userService.findOrCreateByPhoneNumber(request.phoneNumber());

        String otp = generateOtp();
        // In production, store OTP with expiry in Redis
        // For now, we'll simulate sending via WhatsApp
        
        // whatsAppService.sendTemplateMessage(
        //     WhatsAppTemplateMessage.builder()
        //         .to(user.getPhoneNumber())
        //         .templateName("auth_otp")
        //         .variables(Map.of("otp", otp))
        //         .build()
        // );

        return ResponseEntity.ok(Map.of("message", "OTP sent to " + maskPhone(request.phoneNumber())));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        User user = userService.findOrCreateByPhoneNumber(request.phoneNumber());

        // In production, verify OTP from Redis
        // For now, accept any 6-digit code for development
        if (request.otp().length() != 6 || !request.otp().matches("\\d{6}")) {
            return ResponseEntity.badRequest().build();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, user.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken()) || !jwtTokenProvider.isRefreshToken(request.refreshToken())) {
            return ResponseEntity.status(401).build();
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        User user = userService.getUser(userId);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getPhoneNumber());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return ResponseEntity.ok(new AuthResponse(accessToken, newRefreshToken, user.getId()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser(@CurrentUser UUID userId) {
        User user = userService.getUser(userId);
        return ResponseEntity.ok(new UserProfile(
                user.getId(),
                user.getPhoneNumber(),
                user.getWhatsappOptIn(),
                user.getTimezone()
        ));
    }

    @PutMapping("/me/opt-in")
    public ResponseEntity<Void> updateOptIn(@CurrentUser UUID userId, @Valid @RequestBody OptInRequest request) {
        userService.updateOptIn(userId, request.optIn());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/timezone")
    public ResponseEntity<Void> updateTimezone(@CurrentUser UUID userId, @Valid @RequestBody TimezoneRequest request) {
        userService.updateTimezone(userId, request.timezone());
        return ResponseEntity.ok().build();
    }

    private String generateOtp() {
        return String.format("%06d", new java.util.Random().nextInt(999999));
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) return phone;
        return phone.substring(0, phone.length() - 4).replaceAll("\\d", "*") + phone.substring(phone.length() - 4);
    }

    public record SendOtpRequest(
            @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phoneNumber
    ) {}

    public record VerifyOtpRequest(
            @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phoneNumber,
            @NotBlank @Pattern(regexp = "^\\d{6}$") String otp
    ) {}

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}

    public record OptInRequest(
            boolean optIn
    ) {}

    public record TimezoneRequest(
            @NotBlank String timezone
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UUID userId
    ) {}

    public record UserProfile(
            UUID id,
            String phoneNumber,
            boolean whatsappOptIn,
            String timezone
    ) {}
}