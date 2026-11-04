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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@CustomLog
@Repository
public class JdbcPortalNotificationRepository
    extends JdbcAbstractCrudRepository<PortalNotification, String>
    implements PortalNotificationRepository {

    JdbcPortalNotificationRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_notifications");
    }

    @Override
    protected JdbcObjectMapper<PortalNotification> buildOrm() {
        return JdbcObjectMapper.builder(PortalNotification.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("read_at", Types.TIMESTAMP, Date.class)
            .addColumn("hook", Types.NVARCHAR, String.class)
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("application_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    public Page<PortalNotification> search(PortalNotificationSearchCriteria criteria, Pageable pageable) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.search({}, {})", criteria, pageable);
        try {
            StringJoiner where = new StringJoiner(" and ");
            List<Object> params = new ArrayList<>();
            where.add(scopeClause());
            params.add(criteria.getUser());
            params.add(criteria.getEnvironmentId());
            if (criteria.getRead() != null) {
                where.add(criteria.getRead() ? "read_at is not null" : "read_at is null");
            }
            if (criteria.getApiId() != null) {
                where.add("api_id = ?");
                params.add(criteria.getApiId());
            }
            if (criteria.getApplicationId() != null) {
                where.add("application_id = ?");
                params.add(criteria.getApplicationId());
            }

            Long total = jdbcTemplate.queryForObject(
                "select count(*) from " + this.tableName + " where " + where,
                Long.class,
                params.toArray()
            );
            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }

            List<PortalNotification> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " where " +
                    where +
                    " order by created_at desc, id desc " +
                    createPagingClause(pageable.pageSize(), pageable.from()),
                getRowMapper(),
                params.toArray()
            );
            return new Page<>(items, pageable.pageNumber(), items.size(), total);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to search notifications of user " + criteria.getUser(), ex);
        }
    }

    @Override
    public PortalNotificationReferences findDistinctReferences(String user, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.findDistinctReferences({}, {})", user, environmentId);
        try {
            return new PortalNotificationReferences(
                distinct("api_id", user, environmentId),
                distinct("application_id", user, environmentId)
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find notification references of user " + user, ex);
        }
    }

    @Override
    public boolean markAsRead(String user, String environmentId, String id, Date readAt) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.markAsRead({}, {}, {})", user, environmentId, id);
        try {
            // coalesce keeps the first read date while the row still counts as found
            int rows = jdbcTemplate.update(
                "update " + this.tableName + " set read_at = coalesce(read_at, ?) where id = ? and " + scopeClause(),
                readAt,
                id,
                user,
                environmentId
            );
            return rows > 0;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to mark notification " + id + " as read", ex);
        }
    }

    @Override
    public void markAllAsRead(String user, String environmentId, Date readAt) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.markAllAsRead({}, {})", user, environmentId);
        try {
            jdbcTemplate.update(
                "update " + this.tableName + " set read_at = ? where read_at is null and " + scopeClause(),
                readAt,
                user,
                environmentId
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to mark notifications of user " + user + " as read", ex);
        }
    }

    @Override
    public boolean delete(String user, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.delete({}, {}, {})", user, environmentId, id);
        try {
            return jdbcTemplate.update("delete from " + this.tableName + " where id = ? and " + scopeClause(), id, user, environmentId) > 0;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete notification " + id, ex);
        }
    }

    @Override
    public void deleteAll(String user, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.deleteAll({}, {})", user, environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where " + scopeClause(), user, environmentId);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete notifications of user " + user, ex);
        }
    }

    private static String scopeClause() {
        return escapeReservedWord("user") + " = ? and (environment_id = ? or environment_id is null)";
    }

    private Set<String> distinct(String column, String user, String environmentId) {
        return new HashSet<>(
            jdbcTemplate.queryForList(
                "select distinct " + column + " from " + this.tableName + " where " + column + " is not null and " + scopeClause(),
                String.class,
                user,
                environmentId
            )
        );
    }

    @Override
    protected String getId(PortalNotification item) {
        return item.getId();
    }

    @Override
    public List<PortalNotification> findByUser(String user) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.findByUser({})", user);
        try {
            List<PortalNotification> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where " + escapeReservedWord("user") + " = ?",
                getRowMapper(),
                user
            );
            return items;
        } catch (final Exception ex) {
            final String message = "Failed to find notifications by user";
            log.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public void create(List<PortalNotification> notifications) throws TechnicalException {
        for (PortalNotification notification : notifications) {
            this.create(notification);
        }
    }

    @Override
    public void deleteAll(String user) throws TechnicalException {
        log.debug("JdbcPortalNotificationRepository.deleteAll({})", user);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where " + escapeReservedWord("user") + " = ?", user);
        } catch (final Exception ex) {
            final String message = "Failed to delete notifications by user";
            log.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }
}
