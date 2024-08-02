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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
public class JdbcGenericNotificationConfigRepository
    extends JdbcAbstractCrudRepository<GenericNotificationConfig, String>
    implements GenericNotificationConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcGenericNotificationConfigRepository.class);
    private final String GENERIC_NOTIFICATION_CONFIG_HOOKS;

    JdbcGenericNotificationConfigRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "generic_notification_configs");
        GENERIC_NOTIFICATION_CONFIG_HOOKS = getTableNameFor("generic_notification_config_hooks");
    }

    @Override
    protected JdbcObjectMapper<GenericNotificationConfig> buildOrm() {
        return JdbcObjectMapper
            .builder(GenericNotificationConfig.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("notifier", Types.NVARCHAR, String.class)
            .addColumn("config", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, NotificationReferenceType.class)
            .addColumn("use_system_proxy", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(GenericNotificationConfig genericNotificationConfig) {
        return genericNotificationConfig.getId();
    }

    @Override
    public GenericNotificationConfig create(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        storeHooks(genericNotificationConfig, false);
        super.create(genericNotificationConfig);
        return genericNotificationConfig;
    }

    @Override
    public GenericNotificationConfig update(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        storeHooks(genericNotificationConfig, true);
        return super.update(genericNotificationConfig);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        super.delete(id);
        GenericNotificationConfig genericNotificationConfig = new GenericNotificationConfig();
        genericNotificationConfig.setId(id);
        storeHooks(genericNotificationConfig, true);
    }

    @Override
    public void deleteByConfig(String config) throws TechnicalException {
        LOGGER.debug("JdbcGenericNotificationConfigRepository.deleteByConfig({})", config);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where config = ?", config);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete JdbcGenericNotificationConfigRepository", ex);
            throw new TechnicalException("Failed to delete JdbcGenericNotificationConfigRepository", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, NotificationReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcGenericNotificationConfigRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceType, referenceId);
        try {
            final var notificationConfigIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_type = ? and reference_id = ?",
                String.class,
                referenceType.name(),
                referenceId
            );

            if (!notificationConfigIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " +
                    GENERIC_NOTIFICATION_CONFIG_HOOKS +
                    " where id IN (" +
                    getOrm().buildInClause(notificationConfigIds) +
                    ")",
                    notificationConfigIds.toArray()
                );

                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_type = ? and reference_id = ?",
                    referenceType.name(),
                    referenceId
                );
            }

            LOGGER.debug(
                "JdbcGenericNotificationConfigRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done",
                referenceType,
                referenceId
            );
            return notificationConfigIds;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete generic notification config for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete generic notification config by reference", ex);
        }
    }

    @Override
    public List<GenericNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        return this.findByReference(referenceType, referenceId)
            .stream()
            .filter(genericNotificationConfig -> genericNotificationConfig.getHooks().contains(hook))
            .collect(Collectors.toList());
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        LOGGER.debug("JdbcGenericNotificationConfigRepository.findByUser({}, {})", referenceType, referenceId);
        try {
            List<GenericNotificationConfig> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ?" + " and reference_id = ?",
                getRowMapper(),
                referenceType.name(),
                referenceId
            );
            items.forEach(this::addHooks);
            return items;
        } catch (final Exception ex) {
            final String message = "Failed to find notifications by user";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public Optional<GenericNotificationConfig> findById(String id) throws TechnicalException {
        Optional<GenericNotificationConfig> optionalConfig = super.findById(id);
        optionalConfig.ifPresent(this::addHooks);
        return optionalConfig;
    }

    private void addHooks(GenericNotificationConfig parent) {
        List<String> hooks = new ArrayList<>();
        jdbcTemplate.query(
            "select hook" + " from " + GENERIC_NOTIFICATION_CONFIG_HOOKS + " where id = ?",
            rs -> {
                hooks.add(rs.getString(1));
            },
            parent.getId()
        );
        parent.setHooks(hooks);
    }

    private void storeHooks(GenericNotificationConfig parent, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + GENERIC_NOTIFICATION_CONFIG_HOOKS + " where id = ?", parent.getId());
        }
        if (parent.getHooks() != null && !parent.getHooks().isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + GENERIC_NOTIFICATION_CONFIG_HOOKS + " ( id, hook ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, parent.getId());
                        ps.setString(2, parent.getHooks().get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return parent.getHooks().size();
                    }
                }
            );
        }
    }
}
