/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.management;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcHelper.*;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * @author njt
 */
@Repository
public class JdbcSubscriptionRepository extends JdbcAbstractCrudRepository<Subscription, String> implements SubscriptionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSubscriptionRepository.class);

    JdbcSubscriptionRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "subscriptions");
    }

    @Override
    protected JdbcObjectMapper<Subscription> buildOrm() {
        return JdbcObjectMapper
            .builder(Subscription.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("plan", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("starting_at", Types.TIMESTAMP, Date.class)
            .addColumn("ending_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("processed_at", Types.TIMESTAMP, Date.class)
            .addColumn("processed_by", Types.NVARCHAR, String.class)
            .addColumn("subscribed_by", Types.NVARCHAR, String.class)
            .addColumn("client_id", Types.NVARCHAR, String.class)
            .addColumn("request", Types.NVARCHAR, String.class)
            .addColumn("reason", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, Subscription.Status.class)
            .addColumn("paused_at", Types.TIMESTAMP, Date.class)
            .addColumn("general_conditions_content_page_id", Types.NVARCHAR, String.class)
            .addColumn("general_conditions_content_revision", Types.INTEGER, Integer.class)
            .addColumn("general_conditions_accepted", Types.BOOLEAN, Boolean.class)
            .addColumn("days_to_expiration_on_last_notification", Types.INTEGER, Integer.class)
            .build();
    }

    @Override
    protected String getId(final Subscription item) {
        return item.getId();
    }

    @Override
    public Page<Subscription> search(final SubscriptionCriteria criteria, final Pageable pageable) throws TechnicalException {
        return searchPage(criteria, pageable);
    }

    @Override
    public List<Subscription> search(final SubscriptionCriteria criteria) throws TechnicalException {
        return searchPage(criteria, null).getContent();
    }

    @Override
    public Set<String> computeRanking(SubscriptionCriteria criteria) {
        final StringBuilder builder = new StringBuilder("select ");

        String group = "api";
        Collection<String> data = null;
        if (criteria.getApplications() != null && !criteria.getApplications().isEmpty()) {
            group = "application";
            data = criteria.getApplications();
        } else if (criteria.getApis() != null && !criteria.getApis().isEmpty()) {
            data = criteria.getApis();
        }

        builder
            .append(group)
            .append(", count(*) as numberOfSubscriptions, max(updated_at) as lastUpdatedAt from ")
            .append(this.tableName)
            .append(" where ")
            .append(group)
            .append(" is not null");

        if (data != null) {
            builder.append(" and ").append(group).append(" in (").append(getOrm().buildInClause(data)).append(")");
        }

        if (!isEmpty(criteria.getStatuses())) {
            builder.append(" and status ").append(" in (").append(getOrm().buildInClause(criteria.getStatuses())).append(")");
        }

        builder.append(" group by ").append(group).append(" order by numberOfSubscriptions desc, lastUpdatedAt desc");
        return jdbcTemplate.query(
            builder.toString(),
            fillPreparedStatement(data, criteria),
            resultSet -> {
                Set<String> ranking = new LinkedHashSet();
                while (resultSet.next()) {
                    String referenceId = resultSet.getString(1);
                    ranking.add(referenceId);
                }
                return ranking;
            }
        );
    }

    private PreparedStatementSetter fillPreparedStatement(Collection<String> data, SubscriptionCriteria criteria) {
        return ps -> {
            int index = 1;
            if (data != null) {
                getOrm().setArguments(ps, data, index++);
            }
            if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
                getOrm().setArguments(ps, criteria.getStatuses().stream().map(Enum::name).collect(toList()), index);
            }
        };
    }

    private Page<Subscription> searchPage(final SubscriptionCriteria criteria, final Pageable pageable) {
        final List<Object> argsList = new ArrayList<>();
        final StringBuilder builder = new StringBuilder(getOrm().getSelectAllSql() + " ");
        boolean started = false;
        if (criteria.getFrom() > 0) {
            builder.append(WHERE_CLAUSE);
            builder.append("updated_at >= ?");
            argsList.add(new Date(criteria.getFrom()));
            started = true;
        }
        if (criteria.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("updated_at <= ?");
            argsList.add(new Date(criteria.getTo()));
            started = true;
        }
        if (hasText(criteria.getClientId())) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("client_id = ?");
            argsList.add(criteria.getClientId());
            started = true;
        }
        if (criteria.getEndingAtAfter() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("ending_at >= ?");
            argsList.add(new Date(criteria.getEndingAtAfter()));
            started = true;
        }
        if (criteria.getEndingAtBefore() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("ending_at <= ?");
            argsList.add(new Date(criteria.getEndingAtBefore()));
            started = true;
        }

        started = addStringsWhereClause(criteria.getPlans(), escapeReservedWord("plan"), argsList, builder, started);
        started = addStringsWhereClause(criteria.getApplications(), "application", argsList, builder, started);
        started = addStringsWhereClause(criteria.getApis(), "api", argsList, builder, started);

        if (!isEmpty(criteria.getStatuses())) {
            final Collection<String> statuses = criteria.getStatuses().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(statuses, "status", argsList, builder, started);
        }

        builder.append(" order by created_at desc ");

        List<Subscription> subscriptions;
        try {
            subscriptions = jdbcTemplate.query(builder.toString(), getOrm().getRowMapper(), argsList.toArray());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find subscription records:", ex);
            throw new IllegalStateException("Failed to find subscription records", ex);
        }
        return getResultAsPage(pageable, subscriptions);
    }
}
