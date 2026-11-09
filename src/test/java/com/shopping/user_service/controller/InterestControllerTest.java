package com.shopping.user_service.controller;

import com.shopping.user_service.service.InterestService;
import com.shopping.user_service.service.JwtService;
import com.shopping.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-value-long-enough-for-hs256-signing-0123456789",
        "jwt.expiration=3600000"
})
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterestService interestService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "a@b.com")
    void getReturnsCustomerIdAndCategories() throws Exception {
        when(interestService.getInterests(7L, "a@b.com"))
                .thenReturn(List.of("Books", "Electronics"));

        mockMvc.perform(get("/api/users/7/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.categories[0]").value("Books"))
                .andExpect(jsonPath("$.categories[1]").value("Electronics"))
                .andExpect(jsonPath("$.categories.length()").value(2));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void putStoresTheParsedListAndReturnsTheSameShape() throws Exception {
        when(interestService.replaceInterests(eq(7L), anyList(), eq("a@b.com")))
                .thenReturn(List.of("Books", "Toys"));

        mockMvc.perform(put("/api/users/7/interests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categories\":[\"Toys\",\"Books\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.categories[0]").value("Books"))
                .andExpect(jsonPath("$.categories[1]").value("Toys"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(interestService).replaceInterests(eq(7L), captor.capture(), eq("a@b.com"));
        assertEquals(List.of("Toys", "Books"), captor.getValue());
    }

    @Test
    @WithMockUser(username = "someone.else@example.com")
    void getForAnotherCustomerIsForbidden() throws Exception {
        when(interestService.getInterests(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/users/7/interests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "someone.else@example.com")
    void putForAnotherCustomerIsForbidden() throws Exception {
        when(interestService.replaceInterests(anyLong(), anyList(), anyString()))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(put("/api/users/7/interests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categories\":[\"Books\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetIsRejected() throws Exception {
        // The default entry point may answer 401 or 403 depending on configuration
        mockMvc.perform(get("/api/users/7/interests"))
                .andExpect(status().is4xxClientError());

        verify(interestService, never()).getInterests(anyLong(), anyString());
    }
}
