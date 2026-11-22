package com.shopping.user_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InterestsRequest {
    @NotNull
    private List<String> categories;
}
