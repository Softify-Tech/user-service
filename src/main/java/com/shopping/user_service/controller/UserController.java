package com.shopping.user_service.controller;

import com.shopping.user_service.dto.*;
import com.shopping.user_service.service.InterestService;
import com.shopping.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService     userService;
    private final InterestService interestService;

    // POST /api/users/register
    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.from(userService.register(request)));
    }

    // POST /api/users/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(CustomerResponse.from(userService.getById(id)));
    }

    // GET /api/users  (admin only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getAll() {
        List<CustomerResponse> customers = userService.getAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(customers);
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(CustomerResponse.from(userService.update(id, request)));
    }

    // POST /api/users/forgot-password — request a reset link (public)
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        // Always return success to prevent email enumeration
        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    // POST /api/users/reset-password — submit new password with token (public)
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully. You can now log in.");
    }

    // ── Interests ─────────────────────────────────────────────────────────────
    // SecurityConfig needs NO change for these two routes: its permitAll rule for
    // GET /api/users/* uses a single '*', which matches one path segment only, so
    // /api/users/{id}/interests already falls through to anyRequest().authenticated().

    // GET /api/users/{id}/interests
    @GetMapping("/{id}/interests")
    public ResponseEntity<InterestsResponse> getInterests(@PathVariable Long id,
                                                          Authentication authentication) {
        List<String> categories = interestService.getInterests(id, authentication.getName());
        return ResponseEntity.ok(InterestsResponse.of(id, categories));
    }

    // PUT /api/users/{id}/interests — full replace
    @PutMapping("/{id}/interests")
    public ResponseEntity<InterestsResponse> replaceInterests(
            @PathVariable Long id,
            @Valid @RequestBody InterestsRequest request,
            Authentication authentication) {
        List<String> categories = interestService.replaceInterests(
                id, request.getCategories(), authentication.getName());
        return ResponseEntity.ok(InterestsResponse.of(id, categories));
    }
}
