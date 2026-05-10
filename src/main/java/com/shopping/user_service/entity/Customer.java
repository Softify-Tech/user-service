package com.shopping.user_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    private String phone;

    private String role = "CUSTOMER";

    private LocalDateTime createdAt = LocalDateTime.now();

    // Password reset token — null when no reset is pending
    @Column(unique = true)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;
}
