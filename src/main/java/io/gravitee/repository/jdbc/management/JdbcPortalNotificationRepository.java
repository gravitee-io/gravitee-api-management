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
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcPortalNotificationRepository extends JdbcAbstractCrudRepository<PortalNotification, String> implements PortalNotificationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalNotificationRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(PortalNotification.class, "portal_notifications", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("user", Types.NVARCHAR, String.class)           
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(PortalNotification item) {
        return item.getId();
    }

    @Override
    public List<PortalNotification> findByUser(String user) throws TechnicalException {
        LOGGER.debug("JdbcPortalNotificationRepository.findByUser({})", user);
        try {
            List<PortalNotification> items = jdbcTemplate.query("select * from portal_notifications where " + escapeReservedWord("user") + " = ?"
                    , getRowMapper()
                    ,user
            );
            return items;
        } catch (final Exception ex) {
            final String message = "Failed to find notifications by user";
            LOGGER.error(message, ex);
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
        LOGGER.debug("JdbcPortalNotificationRepository.deleteAll({})", user);
        try {
            jdbcTemplate.update("delete from portal_notifications where " + escapeReservedWord("user") + " = ?", user);
        } catch (final Exception ex) {
            final String message = "Failed to delete notifications by user";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }
}