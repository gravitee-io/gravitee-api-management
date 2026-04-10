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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.search.ApiProductCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiProduct;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
@CustomLog
public class JdbcApiProductRepository extends JdbcAbstractCrudRepository<ApiProduct, String> implements ApiProductsRepository {

    private final String API_PRODUCT_APIS;
    private final String API_PRODUCT_GROUPS;

    JdbcApiProductRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "api_products");
        API_PRODUCT_APIS = getTableNameFor("api_product_apis");
        API_PRODUCT_GROUPS = getTableNameFor("api_product_groups");
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
            storeGroupIds(apiProduct, false);
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
            storeGroupIds(apiProduct, true);
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
            List<String> groupIds = jdbcTemplate.query(
                "SELECT group_id FROM " + API_PRODUCT_GROUPS + " WHERE api_product_id = ?",
                (ResultSet rs, int rowNum) -> rs.getString("group_id"),
                id
            );
            apiProduct.setGroups(groupIds.isEmpty() ? null : new HashSet<>(groupIds));
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
            List<ApiProduct> aggregated = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregated);
            return new HashSet<>(aggregated);
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
            List<ApiProduct> aggregated = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregated);
            return aggregated.isEmpty() ? Optional.empty() : Optional.of(aggregated.get(0));
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
            List<ApiProduct> aggregatedByEnv = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregatedByEnv);
            return new HashSet<>(aggregatedByEnv);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by environment", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("JdbcApiProductRepository.delete({})", id);
        try {
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_APIS + " WHERE api_product_id = ?", id);
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_GROUPS + " WHERE api_product_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete api product", ex);
        }
    }

    @Override
    public List<ApiProduct> search(ApiProductCriteria criteria) throws TechnicalException {
        log.debug("JdbcApiProductRepository.search({})", criteria);
        if (criteria == null) {
            return new ArrayList<>(findAll());
        }
        try {
            List<Object> args = new ArrayList<>();
            boolean needApiJoin = criteria.getApiIds() != null && !criteria.getApiIds().isEmpty();
            String clause = buildSearchIdsClause(criteria, args);

            String sql = getOrm().getSelectAllSql() + " ap ";
            if (needApiJoin) {
                sql += "LEFT JOIN " + API_PRODUCT_APIS + " apa ON ap.id = apa.api_product_id ";
            }
            if (clause != null) {
                sql += "WHERE " + clause + " ";
            }
            sql += "ORDER BY ap.id";

            final boolean joinApiIds = needApiJoin;
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                sql,
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    if (joinApiIds) addApiId(apiProduct, rs);
                    return apiProduct;
                },
                args.toArray()
            );
            List<ApiProduct> aggregated = needApiJoin ? aggregateApiProducts(apiProducts) : apiProducts;
            enrichWithGroupIds(aggregated);
            return aggregated;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to search api products", ex);
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
                    "WHERE ap.id IN (SELECT api_product_id FROM " +
                    API_PRODUCT_APIS +
                    " WHERE api_id = ?) " +
                    "ORDER BY ap.id",
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                apiId
            );
            List<ApiProduct> aggregatedByApiId = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregatedByApiId);
            return new HashSet<>(aggregatedByApiId);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by apiId", ex);
        }
    }

    @Override
    public void removeApiFromAllApiProducts(String apiId) throws TechnicalException {
        log.debug("JdbcApiProductRepository.removeApiFromAllApiProducts({})", apiId);
        try {
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_APIS + " WHERE api_id = ?", apiId);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to remove api from all api products", ex);
        }
    }

    @Override
    public Set<ApiProduct> findApiProductsByApiIds(Collection<String> apiIds) throws TechnicalException {
        if (CollectionUtils.isEmpty(apiIds)) {
            return Set.of();
        }
        log.debug("JdbcApiProductRepository.findApiProductsByApiIds({})", apiIds);
        try {
            List<String> idList = new ArrayList<>(apiIds);
            String sql =
                getOrm().getSelectAllSql() +
                " ap " +
                "LEFT JOIN " +
                API_PRODUCT_APIS +
                " apa ON ap.id = apa.api_product_id " +
                "WHERE ap.id IN (SELECT api_product_id FROM " +
                API_PRODUCT_APIS +
                " WHERE api_id IN (" +
                getOrm().buildInClause(idList) +
                ")) ORDER BY ap.id";
            List<ApiProduct> apiProducts = jdbcTemplate.query(
                sql,
                (ResultSet rs, int rowNum) -> {
                    ApiProduct apiProduct = getOrm().getRowMapper().mapRow(rs, rowNum);
                    addApiId(apiProduct, rs);
                    return apiProduct;
                },
                idList.toArray()
            );
            List<ApiProduct> aggregatedByApiIds = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregatedByApiIds);
            return new HashSet<>(aggregatedByApiIds);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by api ids", ex);
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
            List<ApiProduct> aggregatedByIds = aggregateApiProducts(apiProducts);
            enrichWithGroupIds(aggregatedByIds);
            return new HashSet<>(aggregatedByIds);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api products by ids", ex);
        }
    }

    @Override
    public Page<ApiProduct> search(ApiProductCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        log.debug("JdbcApiProductRepository.search({}, pageable)", criteria);
        Page<String> idPage = searchIds(criteria != null ? List.of(criteria) : List.of(), pageable, sortable);
        if (idPage.getContent().isEmpty()) {
            return new Page<>(List.of(), pageable.pageNumber(), 0, idPage.getTotalElements());
        }
        Map<String, ApiProduct> byId = new LinkedHashMap<>();
        findByIds(idPage.getContent()).forEach(p -> byId.put(p.getId(), p));
        List<ApiProduct> ordered = idPage.getContent().stream().filter(byId::containsKey).map(byId::get).toList();
        return new Page<>(ordered, pageable.pageNumber(), ordered.size(), idPage.getTotalElements());
    }

    @Override
    public Page<String> searchIds(List<ApiProductCriteria> apiProductCriteriaList, Pageable pageable, Sortable sortable)
        throws TechnicalException {
        log.debug("JdbcApiProductRepository.searchIds({})", apiProductCriteriaList);
        if (apiProductCriteriaList == null || apiProductCriteriaList.isEmpty()) {
            return JdbcAbstractPageableRepository.getResultAsPage(pageable, Collections.emptyList());
        }
        try {
            List<String> clauses = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            boolean needApiJoin = apiProductCriteriaList
                .stream()
                .anyMatch(criteria -> criteria.getApiIds() != null && !criteria.getApiIds().isEmpty());
            for (ApiProductCriteria criteria : apiProductCriteriaList) {
                String clause = buildSearchIdsClause(criteria, args);
                if (clause != null) {
                    clauses.add(clause);
                }
            }
            if (clauses.isEmpty()) {
                return JdbcAbstractPageableRepository.getResultAsPage(pageable, Collections.emptyList());
            }
            String fromClause = " FROM " + tableName + " ap ";
            if (needApiJoin) {
                fromClause += "JOIN " + API_PRODUCT_APIS + " apa ON ap.id = apa.api_product_id ";
            }
            String whereClause = "(" + String.join(") OR (", clauses) + ") ";

            // Count total (database-side) for Page totalElements
            String countSelect = needApiJoin ? "COUNT(DISTINCT ap.id)" : "COUNT(ap.id)";
            String countSql = "SELECT " + countSelect + fromClause + "WHERE " + whereClause;
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class, args.toArray());
            if (totalCount == null || totalCount == 0) {
                return JdbcAbstractPageableRepository.getResultAsPage(pageable, Collections.emptyList());
            }

            // Data query with SQL-level pagination (LIMIT/OFFSET) to avoid loading all IDs into memory
            String dataSql = "SELECT " + (needApiJoin ? "DISTINCT " : "") + "ap.id" + fromClause + "WHERE " + whereClause;
            if (sortable != null && sortable.field() != null && !sortable.field().isEmpty()) {
                String field = "name".equals(sortable.field()) ? "ap.name" : "ap.id";
                dataSql +=
                    "ORDER BY " + field + (sortable.order() == io.gravitee.repository.management.api.search.Order.DESC ? " DESC" : " ASC");
            } else {
                dataSql += "ORDER BY ap.name ASC";
            }
            dataSql += " " + AbstractJdbcRepositoryConfiguration.createPagingClause(pageable.pageSize(), pageable.from());

            List<String> ids = jdbcTemplate.query(dataSql, (ResultSet rs, int rowNum) -> rs.getString("id"), args.toArray());
            return new Page<>(ids, pageable.pageNumber(), ids.size(), totalCount);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to search API Product IDs by criteria", ex);
        }
    }

    private String buildSearchIdsClause(ApiProductCriteria criteria, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
            List<String> idList = new ArrayList<>(criteria.getIds());
            clauses.add("ap.id IN (" + getOrm().buildInClause(idList) + ")");
            args.addAll(idList);
        }
        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            clauses.add("ap.name = ?");
            args.add(criteria.getName());
        }
        if (criteria.getVersion() != null && !criteria.getVersion().isEmpty()) {
            clauses.add("ap.version = ?");
            args.add(criteria.getVersion());
        }
        if (criteria.getEnvironmentId() != null && !criteria.getEnvironmentId().isEmpty()) {
            clauses.add("ap.environment_id = ?");
            args.add(criteria.getEnvironmentId());
        }
        if (criteria.getEnvironments() != null && !criteria.getEnvironments().isEmpty()) {
            List<String> envList = new ArrayList<>(criteria.getEnvironments());
            clauses.add("ap.environment_id IN (" + getOrm().buildInClause(envList) + ")");
            args.addAll(envList);
        }
        if (criteria.getApiIds() != null && !criteria.getApiIds().isEmpty()) {
            clauses.add("apa.api_id IN (" + getOrm().buildInClause(new ArrayList<>(criteria.getApiIds())) + ")");
            args.addAll(criteria.getApiIds());
        }
        if (criteria.getGroups() != null && !criteria.getGroups().isEmpty()) {
            List<String> groupList = new ArrayList<>(criteria.getGroups());
            clauses.add(
                "ap.id IN (SELECT api_product_id FROM " +
                    API_PRODUCT_GROUPS +
                    " WHERE group_id IN (" +
                    getOrm().buildInClause(groupList) +
                    "))"
            );
            args.addAll(groupList);
        }
        return clauses.isEmpty() ? null : String.join(" AND ", clauses);
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

    private void storeGroupIds(ApiProduct apiProduct, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("DELETE FROM " + API_PRODUCT_GROUPS + " WHERE api_product_id = ?", apiProduct.getId());
        }
        if (apiProduct.getGroups() != null && !apiProduct.getGroups().isEmpty()) {
            List<String> groupIds = new ArrayList<>(apiProduct.getGroups());
            jdbcTemplate.batchUpdate(
                "INSERT INTO " + API_PRODUCT_GROUPS + " (api_product_id, group_id) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, apiProduct.getId());
                        ps.setString(2, groupIds.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return groupIds.size();
                    }
                }
            );
        }
    }

    private void enrichWithGroupIds(List<ApiProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<String> productIds = products.stream().map(ApiProduct::getId).toList();
        Map<String, Set<String>> groupsByProductId = new HashMap<>();
        jdbcTemplate.query(
            "SELECT api_product_id, group_id FROM " +
                API_PRODUCT_GROUPS +
                " WHERE api_product_id IN (" +
                getOrm().buildInClause(productIds) +
                ")",
            rs -> {
                groupsByProductId.computeIfAbsent(rs.getString("api_product_id"), k -> new HashSet<>()).add(rs.getString("group_id"));
            },
            productIds.toArray()
        );
        products.forEach(p -> p.setGroups(groupsByProductId.getOrDefault(p.getId(), null)));
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
