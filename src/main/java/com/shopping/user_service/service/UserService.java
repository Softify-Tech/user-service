package com.shopping.user_service.service;

import com.shopping.user_service.dto.*;
import com.shopping.user_service.entity.Customer;
import com.shopping.user_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;
    private final RestTemplate       restTemplate;

    @Value("${services.email-service:http://localhost:8086}")
    private String emailServiceUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public Customer register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setPhone(request.getPhone());
        return customerRepository.save(customer);
    }

    public LoginResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        String token = jwtService.generateToken(customer.getEmail(), customer.getRole(), customer.getCustomerId());
        return new LoginResponse(token, customer.getCustomerId(),
                customer.getEmail(), customer.getRole());
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer update(Long id, RegisterRequest request) {
        Customer customer = getById(id);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        return customerRepository.save(customer);
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    public void forgotPassword(String email) {
        // Always return success to prevent email enumeration attacks
        customerRepository.findByEmail(email).ifPresent(customer -> {
            String token  = UUID.randomUUID().toString();
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            customer.setResetToken(token);
            customer.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            customerRepository.save(customer);

            // Send reset email via email-service
            try {
                Map<String, Object> emailRequest = new HashMap<>();
                emailRequest.put("to",           email);
                emailRequest.put("subject",      "Password Reset Request");
                emailRequest.put("type",         "PASSWORD_RESET");
                emailRequest.put("customerName", customer.getFirstName());
                emailRequest.put("body",
                    "Click the link below to reset your password. " +
                    "This link expires in 1 hour.\n\n" + resetUrl +
                    "\n\nIf you did not request this, please ignore this email.");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                restTemplate.postForEntity(
                        emailServiceUrl + "/api/email/send",
                        new HttpEntity<>(emailRequest, headers),
                        String.class
                );
            } catch (Exception e) {
                // Log but don't expose email-service errors to the caller
                System.err.println("Failed to send reset email: " + e.getMessage());
            }
        });
    }

    public void resetPassword(String token, String newPassword) {
        Customer customer = customerRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (customer.getResetTokenExpiry() == null ||
                customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);
    }
}
