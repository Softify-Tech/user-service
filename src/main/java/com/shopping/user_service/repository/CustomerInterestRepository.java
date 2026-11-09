package com.shopping.user_service.repository;

import com.shopping.user_service.entity.CustomerInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerInterestRepository extends JpaRepository<CustomerInterest, Long> {
    List<CustomerInterest> findByCustomerIdOrderByCategoryAsc(Long customerId);
    void deleteByCustomerId(Long customerId);
}
