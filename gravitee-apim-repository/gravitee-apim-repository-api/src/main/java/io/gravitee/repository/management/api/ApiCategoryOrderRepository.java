package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.util.Optional;
import java.util.Set;

public interface ApiCategoryOrderRepository extends FindAllRepository<ApiCategoryOrder> {
    Optional<ApiCategoryOrder> findById(String apiId, String categoryId) throws TechnicalException;
    ApiCategoryOrder create(ApiCategoryOrder apiCategoryOrder) throws TechnicalException;
    ApiCategoryOrder update(ApiCategoryOrder apiCategoryOrder) throws TechnicalException;
    void delete(String apiId, String categoryId) throws TechnicalException;
    Set<ApiCategoryOrder> findAllByCategoryId(String categoryId);
    Set<ApiCategoryOrder> findAllByApiId(String apiId);
    int findMaxOrderByCategoryId(String categoryId) throws TechnicalException;
}
