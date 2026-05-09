package com.shopping.user_service.dto;

import com.shopping.user_service.entity.Customer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerResponse {

    private Long customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setCustomerId(customer.getCustomerId());
        res.setFirstName(customer.getFirstName());
        res.setLastName(customer.getLastName());
        res.setEmail(customer.getEmail());
        res.setPhone(customer.getPhone());
        res.setRole(customer.getRole());
        res.setCreatedAt(customer.getCreatedAt());
        return res;
    }
}
