package com.shopping.user_service.service;

import com.shopping.user_service.entity.Customer;
import com.shopping.user_service.entity.CustomerInterest;
import com.shopping.user_service.repository.CustomerInterestRepository;
import com.shopping.user_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterestService {

    private static final int MAX_CATEGORIES   = 20;
    private static final int MAX_CATEGORY_LEN = 64;

    private final CustomerInterestRepository interestRepository;
    private final CustomerRepository         customerRepository;

    public List<String> getInterests(Long customerId, String requesterEmail) {
        assertOwnership(customerId, requesterEmail);
        return interestRepository.findByCustomerIdOrderByCategoryAsc(customerId)
                .stream()
                .map(CustomerInterest::getCategory)
                .toList();
    }

    @Transactional
    public List<String> replaceInterests(Long customerId, List<String> categories,
                                         String requesterEmail) {
        assertOwnership(customerId, requesterEmail);

        List<String> normalised = normalise(categories);

        // Full replace: an empty list clears every interest
        interestRepository.deleteByCustomerId(customerId);

        List<CustomerInterest> rows = new ArrayList<>();
        for (String category : normalised) {
            CustomerInterest interest = new CustomerInterest();
            interest.setCustomerId(customerId);
            interest.setCategory(category);
            rows.add(interest);
        }
        interestRepository.saveAll(rows);

        return normalised.stream().sorted().toList();
    }

    // Trim, drop blanks, drop duplicates case-insensitively keeping the first occurrence,
    // then enforce the limits. A bare RuntimeException whose message contains neither
    // "not found" nor "already registered" is mapped to 400 by GlobalExceptionHandler.
    private List<String> normalise(List<String> categories) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (categories != null) {
            for (String raw : categories) {
                if (raw == null) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                unique.putIfAbsent(trimmed.toLowerCase(), trimmed);
            }
        }

        List<String> result = new ArrayList<>(unique.values());

        if (result.size() > MAX_CATEGORIES) {
            throw new RuntimeException("A customer may have at most "
                    + MAX_CATEGORIES + " interests");
        }
        for (String category : result) {
            if (category.length() > MAX_CATEGORY_LEN) {
                throw new RuntimeException("Interest category exceeds "
                        + MAX_CATEGORY_LEN + " characters");
            }
        }
        return result;
    }

    // Ownership is by email: JwtAuthFilter puts the JWT subject (the email) in the
    // Authentication principal and nothing else.
    private void assertOwnership(Long customerId, String requesterEmail) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (requesterEmail == null
                || !requesterEmail.equalsIgnoreCase(customer.getEmail())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
