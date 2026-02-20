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

import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PromotionCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionStatus;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPromotionRepository extends JdbcAbstractCrudRepository<Promotion, String> implements PromotionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPromotionRepository.class);

    JdbcPromotionRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "promotions");
    }

    @Override
    protected JdbcObjectMapper<Promotion> buildOrm() {
        return JdbcObjectMapper.builder(Promotion.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("api_definition", Types.NCLOB, String.class)
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, PromotionStatus.class)
            .addColumn("target_env_cockpit_id", Types.NVARCHAR, String.class)
            .addColumn("target_env_name", Types.NVARCHAR, String.class)
            .addColumn("source_env_cockpit_id", Types.NVARCHAR, String.class)
            .addColumn("source_env_name", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("author_user_id", Types.NVARCHAR, String.class)
            .addColumn("author_display_name", Types.NVARCHAR, String.class)
            .addColumn("author_email", Types.NVARCHAR, String.class)
            .addColumn("author_source", Types.NVARCHAR, String.class)
            .addColumn("author_source_id", Types.NVARCHAR, String.class)
            .addColumn("target_api_id", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(Promotion item) {
        return item.getId();
    }

    @Override
    public Page<Promotion> search(PromotionCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcPromotionRepository.search() - {}", getOrm().getTableName());

        try {
            List<Promotion> result;
            final StringBuilder query = new StringBuilder(getOrm().getSelectAllSql());

            if (criteria == null) {
                applySortable(sortable, query);
                result = jdbcTemplate.query(query.toString(), getRowMapper());
            } else {
                query.append(" where 1=1 ");

                if (!isEmpty(criteria.getTargetEnvCockpitIds())) {
                    query
                        .append(" and target_env_cockpit_id in ( ")
                        .append(getOrm().buildInClause(criteria.getTargetEnvCockpitIds()))
                        .append(" ) ");
                }

                if (!isEmpty(criteria.getStatuses())) {
                    query.append(" and status in ( ").append(getOrm().buildInClause(criteria.getStatuses())).append(" ) ");
                }

                if (criteria.getTargetApiExists() != null) {
                    query.append(" and target_api_id");
                    if (criteria.getTargetApiExists()) {
                        query.append(" IS NOT NULL");
                    } else {
                        query.append(" IS NULL");
                    }
                }

                if (!isEmpty(criteria.getApiId())) {
                    query.append(" and api_id = ?");
                }

                applySortable(sortable, query);

                result = jdbcTemplate.query(
                    query.toString(),
                    (PreparedStatement ps) -> {
                        int idx = 1;
                        if (!isEmpty(criteria.getTargetEnvCockpitIds())) {
                            idx = getOrm().setArguments(ps, criteria.getTargetEnvCockpitIds(), idx);
                        }

                        if (!isEmpty(criteria.getStatuses())) {
                            List<String> statusesNames = criteria.getStatuses().stream().map(Enum::name).collect(Collectors.toList());
                            idx = getOrm().setArguments(ps, statusesNames, idx);
                        }

                        if (!isEmpty(criteria.getApiId())) {
                            ps.setString(idx++, criteria.getApiId());
                        }
                    },
                    getOrm().getRowMapper()
                );
            }

            return getResultAsPage(pageable, result);
        } catch (Exception e) {
            LOGGER.error("Failed to search {} items:", getOrm().getTableName(), e);
            throw new TechnicalException("Failed to search " + getOrm().getTableName() + " items", e);
        }
    }

    @Override
    public List<String> deleteByApiId(String apiId) throws TechnicalException {
        LOGGER.debug("JdbcPromotionRepository.deleteByApiId({})", apiId);
        try {
            final var rows = jdbcTemplate.queryForList("select id from " + this.tableName + " where api_id = ?", String.class, apiId);

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + this.tableName + " where api_id = ?", apiId);
            }

            LOGGER.debug("JdbcPromotionRepository.deleteByApiId({}) - Done", apiId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete Promotion by apiId: {}", apiId, ex);
            throw new TechnicalException("Failed to delete Promotion by apiId", ex);
        }
    }

    void applySortable(Sortable sortable, StringBuilder query) {
        if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
            query.append(" order by ");
            if ("created_at".equals(sortable.field())) {
                query.append(sortable.field());
            } else {
                query.append(" lower(");
                query.append(sortable.field());
                query.append(") ");
            }

            query.append(sortable.order().equals(Order.ASC) ? " asc " : " desc ");
        }
    }
}
