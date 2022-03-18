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
import static org.springframework.util.CollectionUtils.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.JdbcHelper.CollatingRowMapper;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
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

    private final String KEY_SUBSCRIPTIONS;
    private final String SUBSCRIPTION;

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
        KEY_SUBSCRIPTIONS = getTableNameFor("key_subscriptions");
        SUBSCRIPTION = getTableNameFor("subscriptions");
    }

    @Override
    protected JdbcObjectMapper<ApiKey> buildOrm() {
        return JdbcObjectMapper
            .builder(ApiKey.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
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
            LOGGER.error("Failed to create api key " + apiKey.getId(), e);
            throw new TechnicalException("Failed to create api key " + apiKey.getId(), e);
        }
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        try {
            if (apiKey == null) {
                throw new IllegalStateException("Trying to update a null API key");
            }

            findById(apiKey.getId()).orElseThrow(() -> new IllegalStateException("Trying to update a non existing key"));

            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(apiKey, apiKey.getId()));
            if (!isEmpty(apiKey.getSubscriptions())) {
                storeSubscriptions(apiKey);
            }

            return findById(apiKey.getId()).orElse(null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to update api key " + apiKey.getId(), e);
            throw new TechnicalException("Failed to update api key " + apiKey.getId(), e);
        }
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria criteria) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findByCriteria({})", criteria);
        try {
            List<Object> args = new ArrayList<>();

            StringBuilder query = new StringBuilder(getOrm().getSelectAllSql())
                .append(" k left join ")
                .append(KEY_SUBSCRIPTIONS)
                .append(" ks on ks.key_id = k.id");

            boolean first = true;

            if (!isEmpty(criteria.getPlans())) {
                first = addClause(first, query);
                query
                    .append(" k.id in (")
                    .append("   select key_id from " + KEY_SUBSCRIPTIONS + " ks")
                    .append("   join " + SUBSCRIPTION + " s on s.id = ks.subscription_id")
                    .append("   where s." + escapeReservedWord("plan") + " in (" + getOrm().buildInClause(criteria.getPlans()) + ")")
                    .append(")");
                args.addAll(criteria.getPlans());
            }

            if (!criteria.isIncludeRevoked()) {
                first = addClause(first, query);
                query.append(" ( k.revoked = ? ) ");
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
                query.append(" ( k.expire_at >= ? ) ");
                args.add(new Date(criteria.getExpireAfter()));
            }

            if (criteria.getExpireBefore() > 0) {
                addClause(first, query);
                query.append(" ( k.expire_at <= ? ) ");
                args.add(new Date(criteria.getExpireBefore()));
            }

            query.append(" order by k.updated_at desc ");

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString(), rowMapper, args.toArray());

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by criteria:", ex);
            throw new TechnicalException("Failed to find api keys by criteria", ex);
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
                KEY_SUBSCRIPTIONS,
                KEY_SUBSCRIPTIONS
            );

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, subscription);

            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by subscription:", ex);
            throw new TechnicalException("Failed to find api keys by subscription", ex);
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
                KEY_SUBSCRIPTIONS +
                " ks on ks.key_id = k.id" +
                " join " +
                SUBSCRIPTION +
                " s on ks.subscription_id = s.id and s." +
                escapeReservedWord("plan") +
                " = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, plan);

            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by plan:", ex);
            throw new TechnicalException("Failed to find api keys by plan", ex);
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
                KEY_SUBSCRIPTIONS +
                " ks on ks.key_id = k.id " +
                "where k." +
                escapeReservedWord("key") +
                " = ?" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api key by key", ex);
            throw new TechnicalException("Failed to find api key by key", ex);
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
                KEY_SUBSCRIPTIONS +
                " ks on ks.key_id = k.id " +
                "where k.application = ?" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, applicationId);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api keys by application", ex);
            throw new TechnicalException("Failed to find api keys by application", ex);
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
                KEY_SUBSCRIPTIONS +
                " ks on ks.key_id = k.id" +
                " join " +
                SUBSCRIPTION +
                " s on ks.subscription_id = s.id " +
                " where k." +
                escapeReservedWord("key") +
                " = ?" +
                " and s.api = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key, api);

            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api key by key and api", ex);
            throw new TechnicalException("Failed to find api key by key and api", ex);
        }
    }

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcApiKeyRepository.findById({})", id);
        try {
            String query =
                getOrm().getSelectAllSql() + " k " + " left join " + KEY_SUBSCRIPTIONS + " ks on ks.key_id = k.id" + " where k.id = ?";

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
                KEY_SUBSCRIPTIONS +
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

        jdbcTemplate.update("delete from " + KEY_SUBSCRIPTIONS + " where key_id = ?", key.getId());

        jdbcTemplate.batchUpdate(
            "insert into " + KEY_SUBSCRIPTIONS + " ( key_id, subscription_id ) values ( ?, ? )",
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
