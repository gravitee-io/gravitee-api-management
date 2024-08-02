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
import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.addStringsWhereClause;
import static io.gravitee.repository.jdbc.utils.FieldUtils.toSnakeCase;
import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * @author njt
 */
@Repository
public class JdbcSubscriptionRepository extends JdbcAbstractCrudRepository<Subscription, String> implements SubscriptionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSubscriptionRepository.class);
    private static final JdbcHelper.ChildAdder<Subscription> METADATA_ADDER = (Subscription parent, ResultSet rs) -> {
        Map<String, String> metadata = parent.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            parent.setMetadata(metadata);
        }
        if (rs.getString("sm_k") != null) {
            metadata.put(rs.getString("sm_k"), rs.getString("sm_v"));
        }
    };
    private final String plansTableName;
    private final String metadataTableName;

    JdbcSubscriptionRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "subscriptions");
        plansTableName = getTableNameFor("plans");
        metadataTableName = getTableNameFor("subscriptions_metadata");
    }

    @Override
    protected JdbcObjectMapper<Subscription> buildOrm() {
        return JdbcObjectMapper
            .builder(Subscription.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("plan", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("starting_at", Types.TIMESTAMP, Date.class)
            .addColumn("ending_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("processed_at", Types.TIMESTAMP, Date.class)
            .addColumn("processed_by", Types.NVARCHAR, String.class)
            .addColumn("subscribed_by", Types.NVARCHAR, String.class)
            .addColumn("client_id", Types.NVARCHAR, String.class)
            .addColumn("client_certificate", Types.NVARCHAR, String.class)
            .addColumn("request", Types.NVARCHAR, String.class)
            .addColumn("reason", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, Subscription.Status.class)
            .addColumn("consumer_status", Types.NVARCHAR, Subscription.ConsumerStatus.class)
            .addColumn("consumer_paused_at", Types.TIMESTAMP, Date.class)
            .addColumn("failure_cause", Types.NVARCHAR, String.class)
            .addColumn("paused_at", Types.TIMESTAMP, Date.class)
            .addColumn("general_conditions_content_page_id", Types.NVARCHAR, String.class)
            .addColumn("general_conditions_content_revision", Types.INTEGER, Integer.class)
            .addColumn("general_conditions_accepted", Types.BOOLEAN, Boolean.class)
            .addColumn("days_to_expiration_on_last_notification", Types.INTEGER, Integer.class)
            .addColumn("configuration", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, Subscription.Type.class)
            .build();
    }

    @Override
    public Subscription create(Subscription subscription) throws TechnicalException {
        LOGGER.debug("JdbcSubscriptionRepository.create({})", subscription);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(subscription));
            storeMetadata(subscription, false);
            return findById(subscription.getId()).orElse(null);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to create application", ex);
        }
    }

    @Override
    public Subscription update(final Subscription subscription) throws TechnicalException {
        LOGGER.debug("JdbcSubscriptionRepository.update({})", subscription);
        if (subscription == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(subscription, subscription.getId()));
            storeMetadata(subscription, true);
            return findById(subscription.getId())
                .orElseThrow(() -> new IllegalStateException(format("No subscription found with id [%s]", subscription.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to update subscription", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + this.metadataTableName + " where subscription_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    @Override
    protected String getId(final Subscription item) {
        return item.getId();
    }

    @Override
    public List<Subscription> search(final SubscriptionCriteria criteria) throws TechnicalException {
        return search(criteria, null);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcSubscriptionRepository.deleteByEnvironment({})", environmentId);
        try {
            List<String> rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + metadataTableName + " where subscription_id IN (" + getOrm().buildInClause(rows) + ")",
                    rows.toArray()
                );
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            LOGGER.debug("JdbcSubscriptionRepository.deleteByEnvironment({})", environmentId);
            return rows;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete subscription by environment", ex);
        }
    }

    @Override
    public List<Subscription> search(final SubscriptionCriteria criteria, final Sortable sortable) throws TechnicalException {
        return searchPage(criteria, sortable, null).getContent();
    }

    @Override
    public Page<Subscription> search(final SubscriptionCriteria criteria, final Sortable sortable, final Pageable pageable)
        throws TechnicalException {
        return searchPage(criteria, sortable, pageable);
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) {
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
        String orderAsString = order == null ? "asc" : order.name();
        builder
            .append(" group by ")
            .append(group)
            .append(" order by numberOfSubscriptions " + orderAsString + ", lastUpdatedAt " + orderAsString);
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
                index = getOrm().setArguments(ps, data, index);
            }
            if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
                getOrm().setArguments(ps, criteria.getStatuses(), index);
            }
        };
    }

    private Page<Subscription> searchPage(final SubscriptionCriteria criteria, final Sortable sortable, final Pageable pageable) {
        final List<Object> argsList = new ArrayList<>();
        JdbcHelper.CollatingRowMapper<Subscription> rowMapper = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            METADATA_ADDER,
            "id"
        );

        final StringBuilder builder = new StringBuilder(
            "select s.*, sm.k as sm_k, sm.v as sm_v from " +
            this.tableName +
            " s left join " +
            this.metadataTableName +
            " sm on s.id = sm.subscription_id "
        );

        boolean started = false;

        if (!isEmpty(criteria.getPlanSecurityTypes())) {
            builder.append("INNER JOIN " + plansTableName + " p ON s." + escapeReservedWord("plan") + " = p.id ");
            started = addStringsWhereClause(criteria.getPlanSecurityTypes(), "p.security", argsList, builder, started);
        }

        if (!isEmpty(criteria.getEnvironments())) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append(" ( s.environment_id in ( ").append(getOrm().buildInClause(criteria.getEnvironments())).append(" ) )");
            argsList.addAll(criteria.getEnvironments());
            started = true;
        }
        if (criteria.getFrom() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("s.updated_at >= ?");
            argsList.add(new Date(criteria.getFrom()));
            started = true;
        }
        if (criteria.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("s.updated_at <= ?");
            argsList.add(new Date(criteria.getTo()));
            started = true;
        }
        if (hasText(criteria.getClientId())) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("s.client_id = ?");
            argsList.add(criteria.getClientId());
            started = true;
        }

        if (criteria.getEndingAtAfter() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            if (criteria.isIncludeWithoutEnd()) {
                builder.append("( s.ending_at is NULL or s.ending_at >= ? )");
            } else {
                builder.append("s.ending_at >= ?");
            }
            argsList.add(new Date(criteria.getEndingAtAfter()));
            started = true;
        }
        if (criteria.getEndingAtBefore() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            if (criteria.isIncludeWithoutEnd()) {
                builder.append("( s.ending_at is NULL or s.ending_at <= ? )");
            } else {
                builder.append("s.ending_at <= ?");
            }
            argsList.add(new Date(criteria.getEndingAtBefore()));
            started = true;
        }

        started = addStringsWhereClause(criteria.getPlans(), "s." + escapeReservedWord("plan"), argsList, builder, started);
        started = addStringsWhereClause(criteria.getApplications(), "s.application", argsList, builder, started);
        started = addStringsWhereClause(criteria.getApis(), "s.api", argsList, builder, started);
        started = addStringsWhereClause(criteria.getIds(), "s.id", argsList, builder, started);

        addStringsWhereClause(criteria.getStatuses(), "s.status", argsList, builder, started);

        if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
            builder.append(" order by s.");
            builder.append(toSnakeCase(sortable.field()));
            builder.append(sortable.order() == null || sortable.order().equals(Order.ASC) ? " asc " : " desc ");
        } else {
            builder.append(" order by s.created_at desc ");
        }

        try {
            jdbcTemplate.query(builder.toString(), rowMapper, argsList.toArray());
            return getResultAsPage(pageable, rowMapper.getRows());
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to find subscription records", ex);
        }
    }

    @Override
    public Optional<Subscription> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcSubscriptionRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Subscription> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                METADATA_ADDER,
                "id"
            );
            jdbcTemplate.query(
                "select s.*, sm.k as sm_k, sm.v as sm_v from " +
                this.tableName +
                " s left join " +
                this.metadataTableName +
                " sm on s.id = sm.subscription_id where s.id = ?",
                rowMapper,
                id
            );
            Optional<Subscription> result = rowMapper.getRows().stream().findFirst();
            LOGGER.debug("JdbcSubscriptionRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find subscription by id", ex);
        }
    }

    public Set<Subscription> findAll() throws TechnicalException {
        LOGGER.debug("JdbcSubscriptionRepository.findAll()", getOrm().getTableName());
        try {
            JdbcHelper.CollatingRowMapper<Subscription> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                METADATA_ADDER,
                "id"
            );

            jdbcTemplate.query(
                "select s.*, sm.k as sm_k, sm.v as sm_v from " +
                this.tableName +
                " s left join " +
                this.metadataTableName +
                " sm on s.id = sm.subscription_id",
                rowMapper
            );
            LOGGER.debug("Found {} subscriptions: {}", rowMapper.getRows().size(), rowMapper.getRows());
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find subscriptions", ex);
        }
    }

    @Override
    public List<Subscription> findByIdIn(Collection<String> ids) throws TechnicalException {
        if (isEmpty(ids)) {
            return List.of();
        }

        try {
            StringBuilder queryBuilder = new StringBuilder(
                "select s.*, sm.k as sm_k, sm.v as sm_v from " +
                this.tableName +
                " s left join " +
                this.metadataTableName +
                " sm on s.id = sm.subscription_id"
            );
            getOrm().buildInCondition(true, queryBuilder, "s.id", ids);

            JdbcHelper.CollatingRowMapper<Subscription> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                METADATA_ADDER,
                "id"
            );
            jdbcTemplate.query(queryBuilder.toString(), (PreparedStatement ps) -> getOrm().setArguments(ps, ids, 1), rowMapper);
            return new ArrayList<>(rowMapper.getRows());
        } catch (final Exception e) {
            throw new TechnicalException("Failed to find subscriptions by ids", e);
        }
    }

    private void storeMetadata(Subscription subscription, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + metadataTableName + " where subscription_id = ?", subscription.getId());
        }
        if (subscription.getMetadata() != null && !subscription.getMetadata().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(subscription.getMetadata().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + metadataTableName + " ( subscription_id, k, v ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, subscription.getId());
                        ps.setString(2, entries.get(i).getKey());
                        ps.setString(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }
}
