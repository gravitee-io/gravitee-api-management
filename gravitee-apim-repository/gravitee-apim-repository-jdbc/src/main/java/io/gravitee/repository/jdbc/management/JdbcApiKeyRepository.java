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
import static io.gravitee.repository.jdbc.utils.FieldUtils.toSnakeCase;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.JdbcHelper.CollatingRowMapper;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApiKeyRepository extends JdbcAbstractCrudRepository<ApiKey, String> implements ApiKeyRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiKeyRepository.class);

    private final String keySubscriptions;
    private final String subscription;

    private static final JdbcHelper.ChildAdder<ApiKey> CHILD_ADDER = (ApiKey parent, ResultSet rs) -> {
        String subscriptionId = rs.getString("subscription_id");
        if (!rs.wasNull()) {
            List<String> subscriptions = parent.getSubscriptions();
            if (subscriptions == null) {
                subscriptions = new ArrayList<>();
            }
            subscriptions.add(subscriptionId);
            parent.setSubscriptions(subscriptions);
        }
    };

    JdbcApiKeyRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "keys");
        keySubscriptions = getTableNameFor("key_subscriptions");
        subscription = getTableNameFor("subscriptions");
    }

    @Override
    protected JdbcObjectMapper<ApiKey> buildOrm() {
        return JdbcObjectMapper
            .builder(ApiKey.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("subscription", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("plan", Types.NVARCHAR, String.class)
            .addColumn("expire_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("revoked", Types.BOOLEAN, boolean.class)
            .addColumn("paused", Types.BOOLEAN, boolean.class)
            .addColumn("revoked_at", Types.TIMESTAMP, Date.class)
            .addColumn("federated", Types.BOOLEAN, boolean.class)
            .addColumn("days_to_expiration_on_last_notification", Types.INTEGER, Integer.class)
            .build();
    }

    @Override
    protected String getId(ApiKey item) {
        return item.getId();
    }

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiKey));
            if (!isEmpty(apiKey.getSubscriptions())) {
                storeSubscriptions(apiKey);
            }
            return findById(apiKey.getId()).orElse(null);
        } catch (Exception e) {
            LOGGER.error("Failed to create API Key", e);
            throw new TechnicalException("Failed to create API Key", e);
        }
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        if (apiKey == null) {
            throw new IllegalStateException("Trying to update a null API Key");
        }

        try {
            findById(apiKey.getId()).orElseThrow(() -> new IllegalStateException("Trying to update a non existing key")); //NOSONAR It's just an existence check so we will not use the result of this call

            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(apiKey, apiKey.getId()));
            if (!isEmpty(apiKey.getSubscriptions())) {
                storeSubscriptions(apiKey);
            }

            return findById(apiKey.getId()).orElse(null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to update API Key " + apiKey.getId(), e);
            throw new TechnicalException("Failed to update API Key " + apiKey.getId(), e);
        }
    }

    @Override
    public List<ApiKey> findByCriteria(final ApiKeyCriteria filter) throws TechnicalException {
        return findByCriteria(filter, null);
    }

    @Override
    public List<ApiKey> findByCriteria(final ApiKeyCriteria criteria, final Sortable sortable) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByCriteria({})", criteria);
        try {
            List<Object> args = new ArrayList<>();

            StringBuilder query = new StringBuilder(getOrm().getSelectAllSql())
                .append(" k left join ")
                .append(keySubscriptions)
                .append(" ks on ks.key_id = k.id");

            boolean first = true;

            if (!isEmpty(criteria.getSubscriptions())) {
                first = addClause(first, query);
                query
                    .append("k.id in ( select key_id from ")
                    .append(keySubscriptions)
                    .append(" where subscription_id in ( ")
                    .append(getOrm().buildInClause(criteria.getSubscriptions()))
                    .append(" ) )");
                args.addAll(criteria.getSubscriptions());
            }

            if (!isEmpty(criteria.getEnvironments())) {
                first = addClause(first, query);
                query.append(" ( k.environment_id in ( ").append(getOrm().buildInClause(criteria.getEnvironments())).append(" ) )");
                args.addAll(criteria.getEnvironments());
            }

            if (!criteria.isIncludeRevoked()) {
                first = addClause(first, query);
                query.append(" ( k.revoked = ? ) ");
                args.add(false);
            }

            if (!criteria.isIncludeFederated()) {
                first = addClause(first, query);
                query.append(" ( k.federated = ? ) ");
                args.add(false);
            }

            if (criteria.getFrom() > 0) {
                first = addClause(first, query);
                query.append(" ( k.updated_at >= ? ) ");
                args.add(new Date(criteria.getFrom()));
            }

            if (criteria.getTo() > 0) {
                first = addClause(first, query);
                query.append(" ( k.updated_at <= ? ) ");
                args.add(new Date(criteria.getTo()));
            }

            if (criteria.getExpireAfter() > 0) {
                first = addClause(first, query);
                if (criteria.isIncludeWithoutExpiration()) {
                    query.append(" ( k.expire_at is NULL or k.expire_at >= ? ) ");
                } else {
                    query.append("( k.expire_at >= ? )");
                }
                args.add(new Date(criteria.getExpireAfter()));
            }

            if (criteria.getExpireBefore() > 0) {
                addClause(first, query);
                if (criteria.isIncludeWithoutExpiration()) {
                    query.append(" ( k.expire_at is NULL or k.expire_at <= ? ) ");
                } else {
                    query.append("( k.expire_at <= ? )");
                }
                args.add(new Date(criteria.getExpireBefore()));
            }
            if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
                query.append(" order by k.");
                query.append(toSnakeCase(sortable.field()));
                query.append(sortable.order() == null || sortable.order().equals(Order.ASC) ? " asc " : " desc ");
            } else {
                query.append(" order by k.updated_at desc ");
            }

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString(), rowMapper, args.toArray());

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Keys by criteria:", ex);
            throw new TechnicalException("Failed to find API Keys by criteria", ex);
        }
    }

    @Override
    public Optional<ApiKey> addSubscription(String id, String subscriptionId) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.addSubscription({}, {})", id, subscriptionId);
        Optional<ApiKey> apiKey = findById(id);
        if (apiKey.isEmpty()) {
            return apiKey;
        }
        jdbcTemplate.update("insert into " + keySubscriptions + " ( key_id, subscription_id ) values ( ?, ? )", id, subscriptionId);
        return findById(id);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var keyIds = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!keyIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + keySubscriptions + " where key_id IN (" + getOrm().buildInClause(keyIds) + ")",
                    keyIds.toArray()
                );
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }
            LOGGER.debug("JdbcApiKeyRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return keyIds;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete api keys by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete api keys by environment", ex);
        }
    }

    private boolean addClause(boolean first, StringBuilder query) {
        if (first) {
            query.append(" where ");
        } else {
            query.append(" and ");
        }
        return false;
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findBySubscription({})", subscription);
        try {
            String query = String.format(
                "%s k join %s ks on ks.key_id = k.id where k.id in ( select key_id from %s where subscription_id = ?)",
                getOrm().getSelectAllSql(),
                keySubscriptions,
                keySubscriptions
            );

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, subscription);

            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Keys by subscription:", ex);
            throw new TechnicalException("Failed to find API Keys by subscription", ex);
        }
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByPlan({})", plan);
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " join " +
                keySubscriptions +
                " ks on ks.key_id = k.id" +
                " join " +
                subscription +
                " s on ks.subscription_id = s.id and s." +
                escapeReservedWord("plan") +
                " = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, plan);

            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Keys by plan:", ex);
            throw new TechnicalException("Failed to find API Keys by plan", ex);
        }
    }

    @Override
    public List<ApiKey> findByKey(String key) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByKey(****)");
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " left join " +
                keySubscriptions +
                " ks on ks.key_id = k.id " +
                "where k." +
                escapeReservedWord("key") +
                " = ?" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Key by key", ex);
            throw new TechnicalException("Failed to find API Key by key", ex);
        }
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByApplication({})", applicationId);
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " left join " +
                keySubscriptions +
                " ks on ks.key_id = k.id " +
                "where k.application = ?" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, applicationId);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Keys by application", ex);
            throw new TechnicalException("Failed to find API Keys by application", ex);
        }
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String key, String api) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByKeyAndApi(****, {})", api);
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " join " +
                keySubscriptions +
                " ks on ks.key_id = k.id" +
                " join " +
                subscription +
                " s on ks.subscription_id = s.id " +
                " where k." +
                escapeReservedWord("key") +
                " = ?" +
                " and s.api = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key, api);

            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find API Key by key and api", ex);
            throw new TechnicalException("Failed to find API Key by key and api", ex);
        }
    }

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findById({})", id);
        try {
            String query =
                getOrm().getSelectAllSql() + " k " + " left join " + keySubscriptions + " ks on ks.key_id = k.id" + " where k.id = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, id);

            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find key by id", ex);
            throw new TechnicalException("Failed to find key by id", ex);
        }
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findAll()");
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " left join " +
                keySubscriptions +
                " ks on ks.key_id = k.id" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper);

            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all keys", ex);
            throw new TechnicalException("Failed to find all keys", ex);
        }
    }

    private void storeSubscriptions(ApiKey key) {
        List<String> subscriptions = key.getSubscriptions();

        jdbcTemplate.update("delete from " + keySubscriptions + " where key_id = ?", key.getId());

        jdbcTemplate.batchUpdate(
            "insert into " + keySubscriptions + " ( key_id, subscription_id ) values ( ?, ? )",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, key.getId());
                    ps.setString(2, subscriptions.get(i));
                }

                @Override
                public int getBatchSize() {
                    return subscriptions.size();
                }
            }
        );
    }
}
