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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static java.lang.String.format;

/**
 *
 * @author njt
 */
@Repository
public class JdbcPortalNotificationConfigRepository implements PortalNotificationConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalNotificationConfigRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(PortalNotificationConfig.class, "portal_notification_configs")
            .updateSql("update portal_notification_configs set "
                    + escapeReservedWord("user") + " = ?"
                    + " , reference_type = ?"
                    + " , reference_id = ?"
                    + " , created_at = ? "
                    + " , updated_at = ? "
                    + " where " + escapeReservedWord("user") + " = ? "
                    + " and reference_type = ? "
                    + " and reference_id = ? "
            )
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, NotificationReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    public PortalNotificationConfig create(final PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationConfigRepository.create({})", portalNotificationConfig);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(portalNotificationConfig));
            storeHooks(portalNotificationConfig, false);
            return findById(portalNotificationConfig.getUser(), portalNotificationConfig.getReferenceType(), portalNotificationConfig.getReferenceId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create PortalNotificationConfig", ex);
            throw new TechnicalException("Failed to create PortalNotificationConfig", ex);
        }
    }

    @Override
    public List<PortalNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationConfigRepository.findByReferenceAndHook({}, {}, {})", hook, referenceType, referenceId);
        try {
            List<String> users = new ArrayList<>();
            jdbcTemplate.query(
                    "select " + escapeReservedWord("user") +
                            " from portal_notification_config_hooks" +
                            " where hook = ?" +
                            " and reference_id = ?" +
                            " and reference_type = ?"
                    , rs -> { users.add(rs.getString(1)); }
                    , hook
                    , referenceId
                    , referenceType.name());

            StringBuilder q = new StringBuilder("select " + escapeReservedWord("user") + ", reference_type, reference_id, created_at, updated_at " +
                    " from portal_notification_configs" +
                    " where reference_type = ?" +
                    " and reference_id = ?");
            ORM.buildInCondition(false, q, escapeReservedWord("user"), users);
            final List<PortalNotificationConfig> items = jdbcTemplate.query(
                    q.toString()
                    , (PreparedStatement ps) -> {
                            ps.setString(1, referenceType.name());
                            ps.setString(2, referenceId);
                            ORM.setArguments(ps, users, 3);
                    }
                    , ORM.getRowMapper()
            );

            return items;

        } catch (final Exception ex) {
            final String message = "Failed to find notifications by reference and hook";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public PortalNotificationConfig update(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationConfigRepository.update({})", portalNotificationConfig);
        if (portalNotificationConfig == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(portalNotificationConfig
                    , portalNotificationConfig.getUser()
                    , portalNotificationConfig.getReferenceType().name()
                    , portalNotificationConfig.getReferenceId()
            ));
            storeHooks(portalNotificationConfig, true);
            return findById(portalNotificationConfig.getUser(), portalNotificationConfig.getReferenceType(), portalNotificationConfig.getReferenceId())
                    .orElseThrow(() ->
                            new IllegalStateException(format("No portalNotificationConfig found with id [%s, %s, %s]",
                                    portalNotificationConfig.getUser(), portalNotificationConfig.getReferenceType(), portalNotificationConfig.getReferenceId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update portalNotificationConfig", ex);
            throw new TechnicalException("Failed to update portalNotificationConfig", ex);
        }
    }

    @Override
    public Optional<PortalNotificationConfig> findById(String user, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationConfigRepository.findById({}, {}, {})", user, referenceType, referenceId);
        try {
            final List<PortalNotificationConfig> items = jdbcTemplate.query(
                    "select " + escapeReservedWord("user") + ", reference_type, reference_id, created_at, updated_at " +
                            " from portal_notification_configs" +
                            " where " + escapeReservedWord("user") + " = ?" +
                            " and reference_type = ?" +
                            " and reference_id = ?"
                    , ORM.getRowMapper()
                    , user
                    , referenceType.name()
                    , referenceId
            );
            if (!items.isEmpty()) {
                addHooks(items.get(0));
            }
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find PortalNotificationConfig by id", ex);
            throw new TechnicalException("Failed to find PortalNotificationConfig by id", ex);
        }
    }

    @Override
    public void delete(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationConfigRepository.delete({})", portalNotificationConfig);
        try {
            jdbcTemplate.update(
                    "delete from portal_notification_configs" +
                            " where " + escapeReservedWord("user") + " = ?" +
                            " and reference_type = ?" +
                            " and reference_id = ? "
                    , portalNotificationConfig.getUser()
                    , portalNotificationConfig.getReferenceType().name()
                    , portalNotificationConfig.getReferenceId()
            );
            portalNotificationConfig.setHooks(Collections.emptyList());
            storeHooks(portalNotificationConfig, true);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete PortalNotificationConfig", ex);
            throw new TechnicalException("Failed to delete PortalNotificationConfig", ex);
        }    }

    private void addHooks(PortalNotificationConfig parent) {
        List<String> hooks = new ArrayList<>();
        jdbcTemplate.query(
                "select hook" +
                        " from portal_notification_config_hooks" +
                        " where " + escapeReservedWord("user") + " = ?" +
                        " and reference_id = ?" +
                        " and reference_type = ?"
                , rs -> { hooks.add(rs.getString(1)); },
                parent.getUser(),
                parent.getReferenceId(),
                parent.getReferenceType().name());
        parent.setHooks(hooks);
    }

    private void storeHooks(PortalNotificationConfig parent, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update(
                    "delete from portal_notification_config_hooks" +
                            " where " + escapeReservedWord("user") + " = ?" +
                            " and reference_id = ?" +
                            " and reference_type = ?"
                    , parent.getUser(), parent.getReferenceId(), parent.getReferenceType().name()
            );
        }
        if (parent.getHooks() != null && !parent.getHooks().isEmpty()) {
            jdbcTemplate.batchUpdate("insert into portal_notification_config_hooks ( " + escapeReservedWord("user") + ", reference_id, reference_type, hook ) values ( ?, ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setString(1, parent.getUser());
                            ps.setString(2, parent.getReferenceId());
                            ps.setString(3, parent.getReferenceType().name());
                            ps.setString(4, parent.getHooks().get(i));
                        }

                        @Override
                        public int getBatchSize() {
                            return parent.getHooks().size();
                        }
                    });
        }
    }}