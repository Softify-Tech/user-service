package com.shopping.user_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// user-service owns this table. Hibernate (ddl-auto=update) creates it, the same way
// "customers" is managed. The AI service only ever reads it, with raw SQL, which is why
// customerId is a plain Long column and not a @ManyToOne Customer.
@Entity
@Table(name = "customer_interests",
       uniqueConstraints = @UniqueConstraint(name = "uq_customer_interest",
                                             columnNames = {"customer_id", "category"}))
@Data
public class CustomerInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long interestId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 64)
    private String category;

    private LocalDateTime createdAt = LocalDateTime.now();
}
