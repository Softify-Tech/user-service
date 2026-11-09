package com.shopping.user_service.service;

import com.shopping.user_service.entity.Customer;
import com.shopping.user_service.entity.CustomerInterest;
import com.shopping.user_service.repository.CustomerInterestRepository;
import com.shopping.user_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    private static final Long   CUSTOMER_ID = 7L;
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private CustomerInterestRepository interestRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private InterestService interestService;

    private Customer owner;

    @BeforeEach
    void setUp() {
        owner = new Customer();
        owner.setCustomerId(CUSTOMER_ID);
        owner.setEmail(OWNER_EMAIL);
    }

    private void ownerExists() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(owner));
    }

    private List<String> savedCategories() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerInterest>> captor = ArgumentCaptor.forClass(List.class);
        verify(interestRepository).saveAll(captor.capture());
        return captor.getValue().stream().map(CustomerInterest::getCategory).toList();
    }

    @Test
    void replaceThenReadReturnsStoredListSorted() {
        ownerExists();

        List<String> stored = interestService.replaceInterests(
                CUSTOMER_ID, List.of("Electronics", "Books"), OWNER_EMAIL);

        assertEquals(List.of("Books", "Electronics"), stored);
        assertEquals(List.of("Electronics", "Books"), savedCategories());

        when(interestRepository.findByCustomerIdOrderByCategoryAsc(CUSTOMER_ID))
                .thenReturn(List.of(interest("Books"), interest("Electronics")));

        assertEquals(List.of("Books", "Electronics"),
                interestService.getInterests(CUSTOMER_ID, OWNER_EMAIL));
    }

    @Test
    void secondReplaceReplacesRatherThanAppends() {
        ownerExists();

        interestService.replaceInterests(CUSTOMER_ID, List.of("Books"), OWNER_EMAIL);
        List<String> stored = interestService.replaceInterests(
                CUSTOMER_ID, List.of("Toys", "Sports"), OWNER_EMAIL);

        verify(interestRepository, times(2)).deleteByCustomerId(CUSTOMER_ID);
        assertEquals(List.of("Sports", "Toys"), stored);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerInterest>> captor = ArgumentCaptor.forClass(List.class);
        verify(interestRepository, times(2)).saveAll(captor.capture());
        List<String> secondSave = captor.getAllValues().get(1).stream()
                .map(CustomerInterest::getCategory).toList();
        assertEquals(List.of("Toys", "Sports"), secondSave);
    }

    @Test
    void emptyListClearsEveryInterest() {
        ownerExists();

        List<String> stored = interestService.replaceInterests(
                CUSTOMER_ID, Collections.emptyList(), OWNER_EMAIL);

        assertTrue(stored.isEmpty());
        verify(interestRepository).deleteByCustomerId(CUSTOMER_ID);
        assertTrue(savedCategories().isEmpty());
    }

    @Test
    void blanksAndCaseInsensitiveDuplicatesAreNormalisedAway() {
        ownerExists();

        List<String> stored = interestService.replaceInterests(
                CUSTOMER_ID,
                List.of(" Books ", "books", "   ", "BOOKS", "Toys"),
                OWNER_EMAIL);

        assertEquals(List.of("Books", "Toys"), stored);
        assertEquals(List.of("Books", "Toys"), savedCategories());
    }

    @Test
    void getInterestsRejectsAnotherCustomersEmail() {
        ownerExists();

        assertThrows(AccessDeniedException.class,
                () -> interestService.getInterests(CUSTOMER_ID, "someone.else@example.com"));

        verify(interestRepository, never()).deleteByCustomerId(any());
        verify(interestRepository, never()).findByCustomerIdOrderByCategoryAsc(anyLong());
    }

    @Test
    void replaceInterestsRejectsAnotherCustomersEmail() {
        ownerExists();

        assertThrows(AccessDeniedException.class,
                () -> interestService.replaceInterests(
                        CUSTOMER_ID, List.of("Books"), "someone.else@example.com"));

        verify(interestRepository, never()).deleteByCustomerId(any());
        verify(interestRepository, never()).saveAll(any());
    }

    @Test
    void unknownCustomerIsNotFound() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> interestService.getInterests(CUSTOMER_ID, OWNER_EMAIL));

        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void moreThanTwentyCategoriesIsRejected() {
        ownerExists();

        List<String> tooMany = new ArrayList<>(
                IntStream.rangeClosed(1, 21).mapToObj(i -> "Category" + i).toList());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> interestService.replaceInterests(CUSTOMER_ID, tooMany, OWNER_EMAIL));

        assertFalse(ex.getMessage().toLowerCase().contains("not found"));
        assertFalse(ex.getMessage().toLowerCase().contains("already registered"));
        verify(interestRepository, never()).deleteByCustomerId(any());
        verify(interestRepository, never()).saveAll(any());
    }

    @Test
    void categoryLongerThanSixtyFourCharactersIsRejected() {
        ownerExists();

        String tooLong = "x".repeat(65);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> interestService.replaceInterests(
                        CUSTOMER_ID, List.of(tooLong), OWNER_EMAIL));

        assertFalse(ex.getMessage().toLowerCase().contains("not found"));
        assertFalse(ex.getMessage().toLowerCase().contains("already registered"));
        verify(interestRepository, never()).deleteByCustomerId(any());
        verify(interestRepository, never()).saveAll(any());
    }

    private CustomerInterest interest(String category) {
        CustomerInterest interest = new CustomerInterest();
        interest.setCustomerId(CUSTOMER_ID);
        interest.setCategory(category);
        return interest;
    }
}
