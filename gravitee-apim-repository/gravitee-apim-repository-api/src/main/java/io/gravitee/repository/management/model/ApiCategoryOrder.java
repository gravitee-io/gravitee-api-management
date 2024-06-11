package io.gravitee.repository.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCategoryOrder {
    private String apiId;
    private String categoryId;
    private int order;
}
