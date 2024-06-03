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
import io.gravitee.repository.management.api.ApiCategoryRepository;
import io.gravitee.repository.management.model.ApiCategory;
import io.gravitee.repository.management.model.ApiQualityRule;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcApiCategoryRepository extends JdbcAbstractCrudRepository<ApiCategory, ApiCategory.Id> implements ApiCategoryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiCategoryRepository.class);

    JdbcApiCategoryRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "api_categories");
    }

    @Override
    protected JdbcObjectMapper<ApiCategory> buildOrm() {
        return JdbcObjectMapper
            .builder(ApiCategory.class, this.tableName)
            .updateSql(
                "update " +
                this.tableName +
                " set " +
                " api_id = ?" +
                " , category_id = ?" +
                " , category_key = ?" +
                " , " +
                escapeReservedWord("order") +
                " = ? " +
                WHERE_CLAUSE +
                " api_id = ? " +
                " and category_id = ? "
            )
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("category_id", Types.NVARCHAR, String.class)
            .addColumn("category_key", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .build();
    }

    @Override
    protected ApiCategory.Id getId(ApiCategory item) {
        return item.getId();
    }

    @Override
    public Optional<ApiCategory> findById(ApiCategory.Id id) throws TechnicalException {
        try {
            final List<ApiCategory> apiCategories = jdbcTemplate.query(
                new StringBuilder()
                    .append("select")
                    .append(" api_id, category_id, category_key")
                    .append(", ")
                    .append(escapeReservedWord("order"))
                    .append(" from ")
                    .append(this.tableName)
                    .append(" where api_id = ? and category_id = ?")
                    .append(" limit 1")
                    .toString(),
                getOrm().getRowMapper(),
                id.getApiId(),
                id.getCategoryId()
            );
            return apiCategories.stream().findFirst();
        } catch (final Exception ex) {
            final String error = "Failed to find api category by id";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void delete(ApiCategory.Id id) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryRepository.delete({})", id);
        try {
            jdbcTemplate.update(
                "delete from " + this.tableName + " where api_id = ? and category_id = ? ",
                id.getApiId(),
                id.getCategoryId()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to delete api category", e);
            throw new TechnicalException("Failed to delete api category", e);
        }
    }

    @Override
    public ApiCategory create(ApiCategory apiCategory) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryRepository.create({})", apiCategory.getId());

        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiCategory));
            return findById(apiCategory.getId()).orElse(null);
        } catch (Exception e) {
            LOGGER.error("Failed to create api category", e);
            throw new TechnicalException("Failed to create api category", e);
        }
    }

    @Override
    public ApiCategory update(ApiCategory apiCategory) throws TechnicalException {
        LOGGER.debug("JdbcApiCategoryRepository.update({})", apiCategory.getId());

        try {
            jdbcTemplate.update(
                getOrm()
                    .buildUpdatePreparedStatementCreator(apiCategory, apiCategory.getId().getApiId(), apiCategory.getId().getCategoryId())
            );
            return findById(apiCategory.getId())
                .orElseThrow(() -> new IllegalStateException(format("No api category found with id [%s]", apiCategory.getId().toString())));
        } catch (Exception e) {
            LOGGER.error("Failed to update api category", e);
            throw new TechnicalException("Failed to update api category", e);
        }
    }
}
