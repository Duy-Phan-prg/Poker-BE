package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateCategoryRequest;

public interface CreateCategoryValidator {
    void validate(CreateCategoryRequest request);
}
