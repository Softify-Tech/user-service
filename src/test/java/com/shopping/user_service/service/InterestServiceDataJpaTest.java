package com.shopping.user_service.service;

import com.shopping.user_service.entity.Customer;
import com.shopping.user_service.entity.CustomerInterest;
import com.shopping.user_service.repository.CustomerInterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Datasource-backed cover for the full-replace path. The mock-based
 * {@link InterestServiceTest} cannot see it: the delete/insert ordering and the
 * uq_customer_interest unique constraint on (customer_id, category) only exist in a
 * real database, and an overlapping replace is exactly what the Angular edit flow sends.
 */
@DataJpaTest
@ActiveProfiles("h2test")
@Import(InterestService.class)
class InterestServiceDataJpaTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerInterestRepository interestRepository;

    @Autowired
    private InterestService interestService;

    private Long customerId;

    @BeforeEach
    void setUp() {
        Customer owner = new Customer();
        owner.setFirstName("Ode");
        owner.setLastName("Tester");
        owner.setEmail(OWNER_EMAIL);
        owner.setPasswordHash("hashed");
        customerId = entityManager.persistAndFlush(owner).getCustomerId();
    }

    @Test
    void replacingWithAnOverlappingListKeepsOnlyTheNewRows() {
        interestService.replaceInterests(customerId, List.of("Books", "Toys"), OWNER_EMAIL);
        entityManager.flush();
        entityManager.clear();

        // "Books" is in both lists, so this is the insert-before-delete collision.
        List<String> stored =
                interestService.replaceInterests(customerId, List.of("Books"), OWNER_EMAIL);

        entityManager.flush();
        entityManager.clear();

        assertThat(stored).containsExactly("Books");
        assertThat(interestRepository.findByCustomerIdOrderByCategoryAsc(customerId))
                .extracting(CustomerInterest::getCategory)
                .containsExactly("Books");
    }

    @Test
    void replacingWithAnEmptyListClearsEveryStoredRow() {
        interestService.replaceInterests(customerId, List.of("Books", "Toys"), OWNER_EMAIL);
        entityManager.flush();
        entityManager.clear();

        List<String> stored = interestService.replaceInterests(customerId, List.of(), OWNER_EMAIL);

        entityManager.flush();
        entityManager.clear();

        assertThat(stored).isEmpty();
        assertThat(interestRepository.findByCustomerIdOrderByCategoryAsc(customerId)).isEmpty();
    }
}
