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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApiCategoryOrderRepository extends JdbcAbstractFindAllRepository<ApiCategoryOrder> implements ApiCategoryOrderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiCategoryOrderRepository.class);

    JdbcApiCategoryOrderRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "api_category_orders");
    }

    @Override
    protected JdbcObjectMapper<ApiCategoryOrder> buildOrm() {
        return JdbcObjectMapper
            .builder(ApiCategoryOrder.class, this.tableName)
            .updateSql(
                "update " +
                this.tableName +
                " set " +
                " api_id = ?" +
                " , category_id = ?" +
                " , " +
                escapeReservedWord("order") +
                " = ? " +
                WHERE_CLAUSE +
                " api_id = ? " +
                " and category_id = ? "
            )
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("category_id", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .build();
    }

    @Override
    public ApiCategoryOrder create(ApiCategoryOrder apiCategoryOrder) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryOrderRepository.create({}, {})", apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId());

        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiCategoryOrder));
            return findById(apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId()).orElse(null);
        } catch (Exception e) {
            LOGGER.error("Failed to create api category order", e);
            throw new TechnicalException("Failed to create api category order", e);
        }
    }

    @Override
    public ApiCategoryOrder update(ApiCategoryOrder apiCategoryOrder) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryOrderRepository.update({})", apiCategoryOrder);

        if (Objects.isNull(apiCategoryOrder)) {
            throw new IllegalStateException("ApiCategoryOrder must not be null for update");
        }

        try {
            jdbcTemplate.update(
                getOrm()
                    .buildUpdatePreparedStatementCreator(apiCategoryOrder, apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId())
            );
            return findById(apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        format("No api category found with id [%s, %s]", apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId())
                            .toString()
                    )
                );
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to update api category order", e);
            throw new IllegalStateException("Failed to update api category order", e);
        } catch (Exception e) {
            LOGGER.error("Failed to update api category order", e);
            throw new TechnicalException("Failed to update api category order", e);
        }
    }

    @Override
    public void delete(String apiId, Collection<String> categoriesIds) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryRepository.delete({}, {})", apiId, categoriesIds);
        try {
            jdbcTemplate.update(
                "delete from " + this.tableName + " where api_id = ? and category_id in ( " + getOrm().buildInClause(categoriesIds) + " ) ",
                (PreparedStatement ps) -> {
                    ps.setString(1, apiId);
                    getOrm().setArguments(ps, categoriesIds, 2);
                }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to delete api category order", e);
            throw new TechnicalException("Failed to delete api category order", e);
        }
    }

    @Override
    public Set<ApiCategoryOrder> findAllByCategoryId(String categoryId) {
        LOGGER.debug("JdbcApiCategoryOrderRepository.findAllByCategoryId({})", categoryId);

        try {
            final List<ApiCategoryOrder> apiCategories = jdbcTemplate.query(
                new StringBuilder()
                    .append("select")
                    .append(" api_id, category_id")
                    .append(", ")
                    .append(escapeReservedWord("order"))
                    .append(" from ")
                    .append(this.tableName)
                    .append(" where category_id = ?")
                    .toString(),
                getOrm().getRowMapper(),
                categoryId
            );
            return new HashSet<>(apiCategories);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ApiCategoryOrder by category id [{}]", categoryId, ex);
            return Set.of();
        }
    }

    @Override
    public Set<ApiCategoryOrder> findAllByApiId(String apiId) {
        LOGGER.debug("JdbcApiCategoryOrderRepository.findAllByApiId({})", apiId);

        try {
            final List<ApiCategoryOrder> apiCategories = jdbcTemplate.query(
                new StringBuilder()
                    .append("select")
                    .append(" api_id, category_id")
                    .append(", ")
                    .append(escapeReservedWord("order"))
                    .append(" from ")
                    .append(this.tableName)
                    .append(" where api_id = ?")
                    .toString(),
                getOrm().getRowMapper(),
                apiId
            );
            return new HashSet<>(apiCategories);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ApiCategoryOrder by api id [{}]", apiId, ex);
            return Set.of();
        }
    }

    @Override
    public List<String> deleteByApiId(String apiId) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryOrderRepository.deleteByApiId({})", apiId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select category_id from " + this.tableName + " where api_id = ?",
                String.class,
                apiId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + this.tableName + " where api_id = ?", apiId);
            }

            LOGGER.debug("JdbcApiCategoryOrderRepository.deleteByApiId({}) - Done", apiId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete ApiCategoryOrder by apiId: {}", apiId, ex);
            throw new TechnicalException("Failed to delete ApiCategoryOrder by apiId", ex);
        }
    }

    @Override
    public Optional<ApiCategoryOrder> findById(String apiId, String categoryId) {
        LOGGER.debug("JdbcApiCategoryOrderRepository.findById({}, {})", apiId, categoryId);

        try {
            final List<ApiCategoryOrder> apiCategories = jdbcTemplate.query(
                new StringBuilder()
                    .append("select")
                    .append(" api_id, category_id")
                    .append(", ")
                    .append(escapeReservedWord("order"))
                    .append(" from ")
                    .append(this.tableName)
                    .append(" where api_id = ? and category_id = ?")
                    .toString(),
                getOrm().getRowMapper(),
                apiId,
                categoryId
            );
            return apiCategories.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find ApiCategoryOrder by id", ex);
            return Optional.empty();
        }
    }
}
