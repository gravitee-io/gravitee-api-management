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
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author njt
 */
@Repository
public class JdbcGenericNotificationConfigRepository extends JdbcAbstractCrudRepository<GenericNotificationConfig, String> implements GenericNotificationConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcGenericNotificationConfigRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(GenericNotificationConfig.class, "generic_notification_configs", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("notifier", Types.NVARCHAR, String.class)
            .addColumn("config", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, NotificationReferenceType.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
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
    public List<GenericNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        return this.findByReference(referenceType, referenceId)
                .stream()
                .filter(genericNotificationConfig ->
                        genericNotificationConfig.getHooks().contains(hook))
                .collect(Collectors.toList());
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcGenericNotificationConfigRepository.findByUser({}, {})", referenceType, referenceId);
        try {
            List<GenericNotificationConfig> items = jdbcTemplate.query(
                    "select *" +
                            " from generic_notification_configs" +
                            " where reference_type = ?" +
                            " and reference_id = ?"
                    , getRowMapper()
                    , referenceType.name()
                    , referenceId
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
                "select hook" +
                        " from generic_notification_config_hooks" +
                        " where id = ?"
                , rs -> { hooks.add(rs.getString(1)); },
                parent.getId());
        parent.setHooks(hooks);
    }

    private void storeHooks(GenericNotificationConfig parent, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update(
                    "delete from generic_notification_config_hooks where id = ?"
                    , parent.getId()
            );
        }
        if (parent.getHooks() != null && !parent.getHooks().isEmpty()) {
            jdbcTemplate.batchUpdate("insert into generic_notification_config_hooks ( id, hook ) values ( ?, ? )"
                    , new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setString(1, parent.getId());
                            ps.setString(2, parent.getHooks().get(i));
                        }

                        @Override
                        public int getBatchSize() {
                            return parent.getHooks().size();
                        }
                    });
        }
    }
}