package com.shopping.user_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class InterestsResponse {

    private Long customerId;
    private List<String> categories;

    public static InterestsResponse of(Long customerId, List<String> categories) {
        InterestsResponse res = new InterestsResponse();
        res.setCustomerId(customerId);
        res.setCategories(categories);
        return res;
    }
}
