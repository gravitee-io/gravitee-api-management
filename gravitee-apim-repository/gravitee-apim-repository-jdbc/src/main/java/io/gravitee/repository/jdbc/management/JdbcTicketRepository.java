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
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.model.Ticket;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 */
@Repository
public class JdbcTicketRepository extends JdbcAbstractCrudRepository<Ticket, String> implements TicketRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTicketRepository.class);

    JdbcTicketRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "tickets");
    }

    @Override
    protected JdbcObjectMapper<Ticket> buildOrm() {
        return JdbcObjectMapper.builder(Ticket.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("from_user", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("subject", Types.NVARCHAR, String.class)
            .addColumn("content", Types.CLOB, String.class)
            .build();
    }

    @Override
    protected String getId(Ticket ticket) {
        return ticket.getId();
    }

    @Override
    public Page<Ticket> search(TicketCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcTicketRepository.search() - {}", getOrm().getTableName());

        try {
            List result;
            final StringBuilder query = new StringBuilder(getOrm().getSelectAllSql());

            if (criteria == null) {
                applySortable(sortable, query);
                result = jdbcTemplate.query(query.toString(), getRowMapper());
            } else {
                query.append(" where 1=1 ");

                if (criteria.getFromUser() != null && criteria.getFromUser().length() > 0) {
                    query.append(" and from_user = ? ");
                }

                if (criteria.getApi() != null && criteria.getApi().length() > 0) {
                    query.append(" and api = ? ");
                }

                applySortable(sortable, query);

                result = jdbcTemplate.query(
                    query.toString(),
                    (PreparedStatement ps) -> {
                        int idx = 1;
                        if (criteria.getFromUser() != null && criteria.getFromUser().length() > 0) {
                            idx = getOrm().setArguments(ps, Arrays.asList(criteria.getFromUser()), idx);
                        }

                        if (criteria.getApi() != null && criteria.getApi().length() > 0) {
                            idx = getOrm().setArguments(ps, Arrays.asList(criteria.getApi()), idx);
                        }
                    },
                    getOrm().getRowMapper()
                );
            }
            return getResultAsPage(pageable, result);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all {} items:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find all " + getOrm().getTableName() + " items", ex);
        }
    }

    private void applySortable(Sortable sortable, StringBuilder query) {
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
        } else {
            query.append(" order by created_at desc ");
        }
    }

    @Override
    public List<String> deleteByApiId(String apiId) throws TechnicalException {
        LOGGER.debug("JdbcTicketRepository.deleteByApiId({})", apiId);
        try {
            final var rows = jdbcTemplate.queryForList("select id from " + this.tableName + " where api = ?", String.class, apiId);

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where api = ?", apiId);
            }

            LOGGER.debug("JdbcTicketRepository.deleteByApiId({}) - Done", apiId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete tickets by apiId: {}", apiId, ex);
            throw new TechnicalException("Failed to delete tickets by api", ex);
        }
    }

    @Override
    public List<String> deleteByApplicationId(String applicationId) throws TechnicalException {
        LOGGER.debug("JdbcTicketRepository.deleteByApplicationId({})", applicationId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where application = ?",
                String.class,
                applicationId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where application = ?", applicationId);
            }

            LOGGER.debug("JdbcTicketRepository.deleteByApplicationId({}) - Done", applicationId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete tickets by applicationId: {}", applicationId, ex);
            throw new TechnicalException("Failed to delete tickets by application", ex);
        }
    }
}
