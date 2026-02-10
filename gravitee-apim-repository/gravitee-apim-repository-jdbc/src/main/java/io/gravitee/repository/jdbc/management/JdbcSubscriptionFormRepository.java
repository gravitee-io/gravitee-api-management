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
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the SubscriptionFormRepository.
 *
 * @author Gravitee.io Team
 */
@CustomLog
@Repository
public class JdbcSubscriptionFormRepository
    extends JdbcAbstractCrudRepository<SubscriptionForm, String>
    implements SubscriptionFormRepository {

    JdbcSubscriptionFormRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "subscription_forms");
    }

    @Override
    protected JdbcObjectMapper<SubscriptionForm> buildOrm() {
        return JdbcObjectMapper.builder(SubscriptionForm.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("gmd_content", Types.LONGNVARCHAR, String.class)
            .addColumn("enabled", Types.BIT, boolean.class)
            .build();
    }

    @Override
    protected String getId(SubscriptionForm item) {
        return item.getId();
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findByIdAndEnvironmentId({}, {})", id, environmentId);
        try {
            List<SubscriptionForm> list = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where id = ? and environment_id = ?",
                getOrm().getRowMapper(),
                id,
                environmentId
            );
            if (list.size() > 1) {
                throw new TechnicalException("Multiple subscription forms found for id and environment id: " + id + ", " + environmentId);
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
        } catch (final Exception ex) {
            log.error("Failed to find subscription form by id and environment id: {}, {}", id, environmentId, ex);
            throw new TechnicalException("Failed to find subscription form by id and environment id", ex);
        }
    }

    @Override
    public Optional<SubscriptionForm> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findByEnvironmentId({})", environmentId);
        try {
            List<SubscriptionForm> list = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where environment_id = ?",
                getOrm().getRowMapper(),
                environmentId
            );
            if (list.size() > 1) {
                throw new TechnicalException("Multiple subscription forms found for environment id: " + environmentId);
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
        } catch (final Exception ex) {
            log.error("Failed to find subscription form by environment id: {}", environmentId, ex);
            throw new TechnicalException("Failed to find subscription form by environment id", ex);
        }
    }

    @Override
    public SubscriptionForm update(SubscriptionForm item) throws TechnicalException {
        try {
            return super.update(item);
        } catch (IllegalStateException ex) {
            if (item == null) {
                throw new TechnicalException("Subscription form must not be null", ex);
            }
            throw new TechnicalException("Subscription form not found with id [" + item.getId() + "]", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            log.error("Failed to delete subscription forms by environment id: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete subscription forms by environment id", ex);
        }
    }

    @Override
    public Set<SubscriptionForm> findAll() throws TechnicalException {
        return new HashSet<>(super.findAll());
    }
}
