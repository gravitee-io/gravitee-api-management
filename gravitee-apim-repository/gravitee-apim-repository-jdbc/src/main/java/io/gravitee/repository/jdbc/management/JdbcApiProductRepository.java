/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.apiproducts.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
@Slf4j
public class JdbcApiProductRepository extends JdbcAbstractCrudRepository<ApiProduct, String> implements ApiProductsRepository {

    private final String API_PRODUCT_APIS;

    JdbcApiProductRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "api_products");
        API_PRODUCT_APIS = getTableNameFor("api_product_apis");
    }

    @Override
    protected JdbcObjectMapper<ApiProduct> buildOrm() {
        return JdbcObjectMapper.builder(ApiProduct.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("version", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final ApiProduct item) {
        return item.getId();
    }

    @Override
    public ApiProduct create(ApiProduct apiProduct) throws TechnicalException {
        log.debug("JdbcApiProductRepository.create({})", apiProduct.getName());
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiProduct));
            storeApiIds(apiProduct, false);
            return findById(apiProduct.getId()).orElse(null);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to create api product", ex);
        }
    }

    @Override
    public ApiProduct update(final ApiProduct apiProduct) throws TechnicalException {
        log.debug("JdbcApiProductRepository.update({})", apiProduct.getName());
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(apiProduct, apiProduct.getId()));
            storeApiIds(apiProduct, true);
            return findById(apiProduct.getId()).orElseThrow(() ->
                new IllegalStateException(String.format("No api product found with id [%s]", apiProduct.getId()))
            );
        } catch (final IllegalStateException ex) {
            throw new TechnicalException(String.format("Failed to update api product with id [%s]", apiProduct.getId()), ex);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to update api product", ex);
        }
    }

    @Override
    public Optional<ApiProduct> findById(String id) throws TechnicalException {
        Optional<ApiProduct> opt = super.findById(id);
        if (opt.isPresent()) {
            ApiProduct apiProduct = opt.get();
            List<String> apiIds = jdbcTemplate.query(
                "SELECT api_id FROM " + API_PRODUCT_APIS + " WHERE api_product_id = ?",
                (ResultSet rs, int rowNum) -> rs.getString("api_id"),
                id
            );
            apiProduct.setApiIds(apiIds);
            return Optional.of(apiProduct);
        }
        return opt;
    }

    @Override
    public Set<ApiProduct> findAll() throws TechnicalException {
        log.debug("JdbcApiProductRepository.findAll()");
        try {
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " ap " +
                    "LEFT JOIN " +
                    API_PRODUCT_APIS +
                    " apa ON ap.id = apa.api_product_id " +
                    "ORDER BY ap.id",
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                }
            );
            return new HashSet<>(aggregateApiProducts(apiProducts));
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find all api products", ex);
        }
    }

    @Override
    public Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name) throws TechnicalException {
        log.debug("JdbcApiProductRepository.findByEnvironmentIdAndName({}, {})", environmentId, name);
        try {
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " ap " +
                    "LEFT JOIN " +
                    API_PRODUCT_APIS +
                    " apa ON ap.id = apa.api_product_id " +
                    "WHERE ap.environment_id = ? AND ap.name = ?",
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                environmentId,
                name
            );
            return apiProducts.isEmpty() ? Optional.empty() : Optional.of(aggregateApiProducts(apiProducts).get(0));
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api product by environment and name", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcApiProductRepository.findByEnvironmentId({})", environmentId);
        try {
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " ap " +
                    "LEFT JOIN " +
                    API_PRODUCT_APIS +
                    " apa ON ap.id = apa.api_product_id " +
                    "WHERE ap.environment_id = ? " +
                    "ORDER BY ap.id",
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                environmentId
            );
            return new HashSet<>(aggregateApiProducts(apiProducts));
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by environment", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("JdbcApiProductRepository.delete({})", id);
        try {
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_APIS + " WHERE api_product_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete api product", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByApiId(String apiId) throws TechnicalException {
        log.debug("JdbcApiProductRepository.findByApiId({})", apiId);
        try {
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " ap " +
                    "LEFT JOIN " +
                    API_PRODUCT_APIS +
                    " apa ON ap.id = apa.api_product_id " +
                    "WHERE apa.api_id = ? " +
                    "ORDER BY ap.id",
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                apiId
            );
            return new HashSet<>(aggregateApiProducts(apiProducts));
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by apiId", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByIds(Collection<String> ids) throws TechnicalException {
        if (CollectionUtils.isEmpty(ids)) {
            return Set.of();
        }
        log.debug("JdbcApiProductRepository.findByIds({})", ids);
        try {
            List<String> idList = new ArrayList<>(ids);
            String sql =
                getOrm().getSelectAllSql() +
                " ap " +
                "LEFT JOIN " +
                API_PRODUCT_APIS +
                " apa ON ap.id = apa.api_product_id " +
                "WHERE ap.id IN (" +
                getOrm().buildInClause(idList) +
                ") ORDER BY ap.id";
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                sql,
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                idList.toArray()
            );
            return new HashSet<>(aggregateApiProducts(apiProducts));
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by ids", ex);
        }
    }

    private void storeApiIds(ApiProduct apiProduct, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_APIS + " WHERE api_product_id = ?", apiProduct.getId());
        }
        if (apiProduct.getApiIds() != null && !apiProduct.getApiIds().isEmpty()) {
            List<String> apiIds = new ArrayList<>(apiProduct.getApiIds());
            jdbcTemplate.batchUpdate(
                "INSERT INTO " + API_PRODUCT_APIS + " (api_product_id, api_id) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, apiProduct.getId());
                        ps.setString(2, apiIds.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return apiIds.size();
                    }
                }
            );
        }
    }

    private void addApiId(ApiProduct apiProduct, ResultSet rs) throws SQLException {
        try {
            String apiId = rs.getString("api_id");
            if (apiId != null && !rs.wasNull()) {
                if (apiProduct.getApiIds() == null) {
                    apiProduct.setApiIds(new ArrayList<>());
                }
                apiProduct.getApiIds().add(apiId);
            }
        } catch (SQLException ex) {
            log.debug("Field api_id is not part of the result set; {}", ex.getMessage());
        }
    }

    private List<ApiProduct> aggregateApiProducts(List<ApiProduct> apiProducts) {
        if (apiProducts == null || apiProducts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, ApiProduct> aggregated = new LinkedHashMap<>();
        for (ApiProduct product : apiProducts) {
            String id = product.getId();
            if (!aggregated.containsKey(id)) {
                if (product.getApiIds() == null) {
                    product.setApiIds(new ArrayList<>());
                }
                aggregated.put(id, product);
            } else {
                ApiProduct existing = aggregated.get(id);
                if (product.getApiIds() != null) {
                    if (existing.getApiIds() == null) {
                        existing.setApiIds(new ArrayList<>());
                    }
                    existing.getApiIds().addAll(product.getApiIds());
                }
            }
        }
        return new ArrayList<>(aggregated.values());
    }
}
