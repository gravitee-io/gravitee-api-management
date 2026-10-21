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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.isMySqlOrMariaDb;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.isPostgreSql;
import static io.gravitee.repository.jdbc.utils.FieldUtils.toSnakeCase;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.JdbcHelper.CollatingRowMapper;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@CustomLog
@Repository
public class JdbcApiKeyRepository extends JdbcAbstractCrudRepository<ApiKey, String> implements ApiKeyRepository {

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

    /** Writes every column but {@code updated_at}, which {@link #update(ApiKey)} only moves forward. */
    private final JdbcObjectMapper<ApiKey> ormWithoutUpdatedAt;

    JdbcApiKeyRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "keys");
        keySubscriptions = getTableNameFor("key_subscriptions");
        subscription = getTableNameFor("subscriptions");
        ormWithoutUpdatedAt = buildOrm(false);
    }

    @Override
    protected JdbcObjectMapper<ApiKey> buildOrm() {
        return buildOrm(true);
    }

    private JdbcObjectMapper<ApiKey> buildOrm(boolean withUpdatedAt) {
        JdbcObjectMapper.Builder<ApiKey> builder = JdbcObjectMapper.builder(ApiKey.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("subscription", Types.NVARCHAR, String.class)
            .addColumn("application", Types.NVARCHAR, String.class)
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("plan", Types.NVARCHAR, String.class)
            .addColumn("expire_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class);
        if (withUpdatedAt) {
            builder.addColumn("updated_at", Types.TIMESTAMP, Date.class);
        }
        return builder
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
            if (!insertUnlessIdExists(apiKey)) {
                throw new io.gravitee.repository.exceptions.DuplicateKeyException(
                    "An API Key with id [" + apiKey.getId() + "] already exists"
                );
            }
            if (!isEmpty(apiKey.getSubscriptions())) {
                storeSubscriptions(apiKey);
            }
            return findById(apiKey.getId()).orElse(null);
        } catch (io.gravitee.repository.exceptions.DuplicateKeyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create API Key", e);
            throw new TechnicalException("Failed to create API Key", e);
        }
    }

    /**
     * Inserts the key row, unless a key with the same id exists. That conflict must neither write anything else nor abort a
     * surrounding transaction (subscribing runs in one transaction, and the request goes on after losing the race to create the
     * first shared key of an application):
     * <ul>
     *     <li>PostgreSQL aborts a transaction on any failed statement, so the statement itself skips the conflict.</li>
     *     <li>MySQL and MariaDB leave a shared lock on the existing row after a duplicate-key error (also with
     *     {@code insert ignore}). When several transactions lose the race at once, each holds that lock, then each waits for
     *     the others to update the key: a deadlock. {@code on duplicate key update} locks the existing row exclusively instead,
     *     so the transactions that lost take turns. Its update moves {@code updated_at} (the transaction that lost links its
     *     subscription to the key and moves it again right after) so that the conflict counts as an updated row (2) whatever
     *     the connection's {@code CLIENT_FOUND_ROWS} setting, an inserted row counting 1.</li>
     *     <li>SQL Server only rolls back the failing statement.</li>
     * </ul>
     *
     * @return {@code false} when a key with this id already exists
     */
    private boolean insertUnlessIdExists(ApiKey apiKey) {
        if (isPostgreSql()) {
            return (
                jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiKey, sql -> sql + " on conflict (id) do nothing")) > 0
            );
        }
        if (isMySqlOrMariaDb()) {
            return (
                jdbcTemplate.update(
                    getOrm().buildInsertPreparedStatementCreator(
                        apiKey,
                        sql -> sql + " on duplicate key update updated_at = coalesce(updated_at, current_timestamp) + interval 1 second"
                    )
                ) ==
                1
            );
        }
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiKey));
            return true;
        } catch (DuplicateKeyException e) {
            // The primary key on id is the only unique constraint of the table.
            return false;
        }
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        if (apiKey == null) {
            throw new IllegalStateException("Trying to update a null API Key");
        }

        try {
            findById(apiKey.getId()).orElseThrow(() -> new IllegalStateException("Trying to update a non existing key")); //NOSONAR It's just an existence check so we will not use the result of this call

            // The links in key_subscriptions are only changed by addSubscription(s): rewriting them from the given copy
            // of the key would drop the links added since that copy was read.
            jdbcTemplate.update(ormWithoutUpdatedAt.buildUpdatePreparedStatementCreator(apiKey, apiKey.getId()));
            moveUpdatedAtForward(apiKey);

            return findById(apiKey.getId()).orElse(null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update API Key " + apiKey.getId(), e);
            throw new TechnicalException("Failed to update API Key " + apiKey.getId(), e);
        }
    }

    /**
     * Gateways synchronise keys by {@code updated_at}, so it never moves backwards: a copy of the key read before a concurrent
     * {@link #addSubscription(String, String)} must not undo the move made by it. The stored value is kept when it is more recent,
     * or when the given key has none. The row is locked by the update that precedes this statement in the same transaction, so the
     * stored value cannot change between the comparison and the commit.
     */
    private void moveUpdatedAtForward(ApiKey apiKey) {
        if (apiKey.getUpdatedAt() == null) {
            return;
        }
        jdbcTemplate.update(
            "update " + escapeReservedWord(this.tableName) + " set updated_at = ? where id = ? and (updated_at is null or updated_at < ?)",
            apiKey.getUpdatedAt(),
            apiKey.getId(),
            apiKey.getUpdatedAt()
        );
    }

    @Override
    public List<ApiKey> findByCriteria(final ApiKeyCriteria filter) throws TechnicalException {
        return findByCriteria(filter, null);
    }

    @Override
    public List<ApiKey> findByCriteria(final ApiKeyCriteria criteria, final Sortable sortable) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByCriteria({})", criteria);
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder query = buildCriteriaQuery(criteria, args);

            if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
                query.append(" order by k.");
                query.append(toSnakeCase(sortable.field()));
                query.append(sortable.order() == null || sortable.order().equals(Order.ASC) ? " asc " : " desc ");
            } else {
                query.append(" order by k.updated_at desc ");
            }

            return runCriteriaQuery(query, args);
        } catch (final Exception ex) {
            log.error("Failed to find API Keys by criteria:", ex);
            throw new TechnicalException("Failed to find API Keys by criteria", ex);
        }
    }

    @Override
    public List<ApiKey> findByCriteriaUnordered(final ApiKeyCriteria criteria) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByCriteriaUnordered({})", criteria);
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder query = buildCriteriaQuery(criteria, args);
            return runCriteriaQuery(query, args);
        } catch (final Exception ex) {
            log.error("Failed to find API Keys by criteria (unordered):", ex);
            throw new TechnicalException("Failed to find API Keys by criteria", ex);
        }
    }

    @Override
    public List<ApiKey> searchAfter(final ApiKeyCriteria criteria, final Sortable sortable, final ApiKeyCursor after, final int pageSize)
        throws TechnicalException {
        boolean sortByUpdatedAt;
        if (sortable == null || sortable.field() == null || "updatedAt".equals(sortable.field())) {
            sortByUpdatedAt = true;
        } else if ("id".equals(sortable.field())) {
            sortByUpdatedAt = false;
        } else {
            throw new TechnicalException("searchAfter supports sort field 'updatedAt' or 'id' only, got: " + sortable.field());
        }

        final List<Object> argsList = new ArrayList<>();
        // Apply pageSize LIMIT against the keys table BEFORE the key_subscriptions LEFT JOIN —
        // otherwise a federated key with multiple key_subscriptions rows consumes more than one
        // row of the limit (partial subscriptions + false short-page exhaustion). Build the page
        // of key ids in a subquery, then join key_subscriptions on the outer query.
        // The api-key table is named "keys", a reserved word on MySQL/MariaDB/SQL Server — escape it.
        final StringBuilder inner = new StringBuilder("select * from ").append(escapeReservedWord(this.tableName));

        boolean started = false;
        if (!isEmpty(criteria.getSubscriptions())) {
            inner.append(" where ");
            inner
                .append("id in ( select key_id from ")
                .append(keySubscriptions)
                .append(" where subscription_id in ( ")
                .append(getOrm().buildInClause(criteria.getSubscriptions()))
                .append(" ) )");
            argsList.addAll(criteria.getSubscriptions());
            started = true;
        }
        if (!isEmpty(criteria.getEnvironments())) {
            inner.append(started ? " and " : " where ");
            inner.append("( environment_id in ( ").append(getOrm().buildInClause(criteria.getEnvironments())).append(" ) )");
            argsList.addAll(criteria.getEnvironments());
            started = true;
        }
        if (!criteria.isIncludeRevoked()) {
            inner.append(started ? " and " : " where ");
            inner.append("revoked = ?");
            argsList.add(false);
            started = true;
        }
        if (!criteria.isIncludeFederated()) {
            inner.append(started ? " and " : " where ");
            inner.append("federated = ?");
            argsList.add(false);
            started = true;
        }
        if (criteria.getFrom() > 0) {
            inner.append(started ? " and " : " where ");
            inner.append("updated_at >= ?");
            argsList.add(new Date(criteria.getFrom()));
            started = true;
        }
        if (criteria.getTo() > 0) {
            inner.append(started ? " and " : " where ");
            inner.append("updated_at <= ?");
            argsList.add(new Date(criteria.getTo()));
            started = true;
        }
        if (criteria.getExpireAfter() > 0) {
            inner.append(started ? " and " : " where ");
            if (criteria.isIncludeWithoutExpiration()) {
                inner.append("( expire_at is NULL or expire_at >= ? )");
            } else {
                inner.append("expire_at >= ?");
            }
            argsList.add(new Date(criteria.getExpireAfter()));
            started = true;
        }
        if (criteria.getExpireBefore() > 0) {
            inner.append(started ? " and " : " where ");
            if (criteria.isIncludeWithoutExpiration()) {
                inner.append("( expire_at is NULL or expire_at <= ? )");
            } else {
                inner.append("expire_at <= ?");
            }
            argsList.add(new Date(criteria.getExpireBefore()));
            started = true;
        }
        if (after != null) {
            inner.append(started ? " and " : " where ");
            if (sortByUpdatedAt) {
                inner.append("( updated_at > ? or ( updated_at = ? and id > ? ) )");
                Date marker = new Date(after.updatedAt());
                argsList.add(marker);
                argsList.add(marker);
                argsList.add(after.id());
            } else {
                inner.append("id > ?");
                argsList.add(after.id());
            }
        }

        if (sortByUpdatedAt) {
            inner.append(" order by updated_at asc, id asc ");
        } else {
            inner.append(" order by id asc ");
        }
        inner.append(createPagingClause(pageSize, 0));

        final String query =
            "select k.*, ks.subscription_id as subscription_id from (" +
            inner +
            ") k left join " +
            keySubscriptions +
            " ks on ks.key_id = k.id " +
            (sortByUpdatedAt ? "order by k.updated_at asc, k.id asc" : "order by k.id asc");

        try {
            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(query, rowMapper, argsList.toArray());
            return rowMapper.getRows();
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to search API Keys after cursor", ex);
        }
    }

    private StringBuilder buildCriteriaQuery(final ApiKeyCriteria criteria, final List<Object> args) {
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
        return query;
    }

    private List<ApiKey> runCriteriaQuery(final StringBuilder query, final List<Object> args) {
        CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
        jdbcTemplate.query(query.toString(), rowMapper, args.toArray());
        return rowMapper.getRows();
    }

    @Override
    public Optional<ApiKey> addSubscription(String id, String subscriptionId) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.addSubscription({}, {})", id, subscriptionId);
        if (!touch(id)) {
            return Optional.empty();
        }
        insertLinkIfAbsent(id, subscriptionId);
        return findById(id);
    }

    @Override
    public Optional<ApiKey> addSubscriptions(String id, Collection<String> subscriptionIds) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.addSubscriptions({}, {})", id, subscriptionIds);
        if (!touch(id)) {
            return Optional.empty();
        }
        subscriptionIds
            .stream()
            .distinct()
            .forEach(subscriptionId -> insertLinkIfAbsent(id, subscriptionId));
        return findById(id);
    }

    /**
     * Inserts the (key, subscription) link unless it already exists. An existing link must neither fail nor abort the
     * surrounding transaction (subscribing runs in one transaction): PostgreSQL aborts a transaction on any failed
     * statement, so the conflict is skipped by the statement itself. MySQL, MariaDB and SQL Server only roll back the
     * failing statement, so the duplicate can be ignored there.
     */
    private void insertLinkIfAbsent(String id, String subscriptionId) {
        String insert = "insert into " + keySubscriptions + " ( key_id, subscription_id ) values ( ?, ? )";
        if (isPostgreSql()) {
            jdbcTemplate.update(insert + " on conflict do nothing", id, subscriptionId);
            return;
        }
        try {
            jdbcTemplate.update(insert, id, subscriptionId);
        } catch (DuplicateKeyException e) {
            log.debug("Subscription {} is already linked to API Key {}", subscriptionId, id);
        }
    }

    /**
     * Moves {@code updated_at} of the key: gateways synchronise keys by {@code updated_at}, so every link change must move it.
     * It is done before the links are written: the update locks the key row until the end of the transaction, so the key cannot
     * be deleted before its links are written ({@code key_subscriptions} has no foreign key to {@code keys}), and, unlike a plain
     * read, it finds the latest committed row also in REPEATABLE READ (MySQL, MariaDB). An existing key counts one row even if
     * {@code updated_at} keeps its value: the MySQL and MariaDB drivers count the matched rows by default.
     *
     * @return {@code false} when no key has this id
     */
    private boolean touch(String id) {
        return jdbcTemplate.update("update " + escapeReservedWord(this.tableName) + " set updated_at=? where id=?", new Date(), id) > 0;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var keyIds = jdbcTemplate.queryForList(
                "select id from " + escapeReservedWord(this.tableName) + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!keyIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + keySubscriptions + " where key_id IN (" + getOrm().buildInClause(keyIds) + ")",
                    keyIds.toArray()
                );
                jdbcTemplate.update("delete from " + escapeReservedWord(tableName) + " where environment_id = ?", environmentId);
            }
            log.debug("JdbcApiKeyRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return keyIds;
        } catch (final Exception ex) {
            log.error("Failed to delete api keys by environmentId: {}", environmentId, ex);
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
        log.debug("JdbcApiKeyRepository.findBySubscription({})", subscription);
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
            log.error("Failed to find API Keys by subscription:", ex);
            throw new TechnicalException("Failed to find API Keys by subscription", ex);
        }
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByPlan({})", plan);
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
            log.error("Failed to find API Keys by plan:", ex);
            throw new TechnicalException("Failed to find API Keys by plan", ex);
        }
    }

    @Override
    public List<ApiKey> findByKey(String key) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByKey(****)");
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
            log.error("Failed to find API Key by key", ex);
            throw new TechnicalException("Failed to find API Key by key", ex);
        }
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByApplication({})", applicationId);
        return findByApplication(applicationId, "");
    }

    /**
     * PostgreSQL and SQL Server read committed data at each statement (READ COMMITTED, their default). MySQL and MariaDB keep
     * the snapshot of the first read of the transaction (REPEATABLE READ): only a locking read sees a key committed since.
     */
    @Override
    public List<ApiKey> findLatestByApplication(String applicationId) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findLatestByApplication({})", applicationId);
        return findByApplication(applicationId, isMySqlOrMariaDb() ? " for update" : "");
    }

    private List<ApiKey> findByApplication(String applicationId, String lockingClause) throws TechnicalException {
        try {
            String query =
                getOrm().getSelectAllSql() +
                " k" +
                " left join " +
                keySubscriptions +
                " ks on ks.key_id = k.id " +
                "where k.application = ?" +
                " order by k.updated_at desc" +
                lockingClause;

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, applicationId);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            log.error("Failed to find API Keys by application", ex);
            throw new TechnicalException("Failed to find API Keys by application", ex);
        }
    }

    @Override
    public List<ApiKey> findByKeyAndEnvironmentId(String key, String environmentId) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByKeyAndEnvironmentId(*****, {})", environmentId);
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
                " and k." +
                escapeReservedWord("environment_id") +
                " = ?" +
                " order by k.updated_at desc ";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key, environmentId);

            return rowMapper.getRows();
        } catch (final Exception ex) {
            log.error("Failed to find API Keys by key and environment:", ex);
            throw new TechnicalException("Failed to find API Keys by key and environment", ex);
        }
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String key, String api) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByKeyAndApi(****, {})", api);
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
            log.error("Failed to find API Key by key and api", ex);
            throw new TechnicalException("Failed to find API Key by key and api", ex);
        }
    }

    @Override
    public Optional<ApiKey> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType)
        throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findByKeyAndReferenceIdAndReferenceType(****, {}, {})", referenceId, referenceType);
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
                " and s.reference_id = ?" +
                " and s.reference_type = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, key, referenceId, referenceType);

            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find API Key by key, referenceId and referenceType", ex);
        }
    }

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findById({})", id);
        try {
            String query =
                getOrm().getSelectAllSql() + " k " + " left join " + keySubscriptions + " ks on ks.key_id = k.id" + " where k.id = ?";

            CollatingRowMapper<ApiKey> rowMapper = new CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");

            jdbcTemplate.query(query, rowMapper, id);

            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            log.error("Failed to find key by id", ex);
            throw new TechnicalException("Failed to find key by id", ex);
        }
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        log.debug("JdbcApiKeyRepository.findAll()");
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
            log.error("Failed to find all keys", ex);
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
