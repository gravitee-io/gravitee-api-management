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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation of the SubscriptionFormRepository.
 *
 * @author Gravitee.io Team
 */
@CustomLog
@Repository
// A form and its API mappings are written together: any failure, including the checked DuplicateKeyException
// reported for a collision, rolls the whole write back (the inherited transaction only rolls back on unchecked ones).
@Transactional(value = "graviteeTransactionManager", rollbackFor = Exception.class)
public class JdbcSubscriptionFormRepository
    extends JdbcAbstractCrudRepository<SubscriptionForm, String>
    implements SubscriptionFormRepository {

    private final String SUBSCRIPTION_FORM_APIS;

    JdbcSubscriptionFormRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "subscription_forms");
        SUBSCRIPTION_FORM_APIS = getTableNameFor("subscription_form_apis");
    }

    @Override
    protected JdbcObjectMapper<SubscriptionForm> buildOrm() {
        return JdbcObjectMapper.builder(SubscriptionForm.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("gmd_content", Types.NVARCHAR, String.class)
            .addColumn("portal_page_content_id", Types.NVARCHAR, String.class)
            .addColumn("enabled", Types.BIT, boolean.class)
            .addColumn("default_form", Types.BIT, boolean.class)
            // Unique in the table: at most one default form per environment, enforced by the database.
            .addMirroredColumn("default_marker", Types.NVARCHAR, form -> form.isDefaultForm() ? form.getEnvironmentId() : null)
            .addColumn("validation_constraints", Types.CLOB, String.class)
            .build();
    }

    @Override
    protected String getId(SubscriptionForm item) {
        return item.getId();
    }

    @Override
    public Optional<SubscriptionForm> findById(String id) throws TechnicalException {
        Optional<SubscriptionForm> form = super.findById(id);
        form.ifPresent(this::attachApiIds);
        return form;
    }

    @Override
    public Set<SubscriptionForm> findAll() throws TechnicalException {
        List<SubscriptionForm> forms = new ArrayList<>(super.findAll());
        enrichWithApiIds(forms);
        return new HashSet<>(forms);
    }

    @Override
    public SubscriptionForm create(SubscriptionForm item) throws TechnicalException {
        try {
            var created = super.create(item);
            storeApiIds(item, false);
            attachApiIds(created);
            return created;
        } catch (TechnicalException ex) {
            throw translateDuplicateKey(ex, item);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw duplicateKey(ex, item);
        }
    }

    @Override
    public SubscriptionForm update(SubscriptionForm item) throws TechnicalException {
        try {
            var updated = super.update(item);
            storeApiIds(item, true);
            attachApiIds(updated);
            return updated;
        } catch (IllegalStateException ex) {
            if (item == null) {
                throw new TechnicalException("Subscription form must not be null", ex);
            }
            throw new TechnicalException("Subscription form not found with id [" + item.getId() + "]", ex);
        } catch (TechnicalException ex) {
            throw translateDuplicateKey(ex, item);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw duplicateKey(ex, item);
        }
    }

    /**
     * A duplicate key is a collision with another form of the environment (a second default form, an API already
     * mapped to another form), which the caller handles as a conflict rather than as a technical failure.
     */
    private static TechnicalException translateDuplicateKey(TechnicalException ex, SubscriptionForm item) {
        if (ex.getCause() instanceof org.springframework.dao.DuplicateKeyException cause) {
            return duplicateKey(cause, item);
        }
        return ex;
    }

    private static DuplicateKeyException duplicateKey(org.springframework.dao.DuplicateKeyException cause, SubscriptionForm item) {
        return new DuplicateKeyException(
            "Subscription form [" + item.getId() + "] collides with another form of environment [" + item.getEnvironmentId() + "]",
            cause
        );
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from " + SUBSCRIPTION_FORM_APIS + " where form_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            log.error("Failed to delete subscription form: {}", id, ex);
            throw new TechnicalException("Failed to delete subscription form", ex);
        }
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findByIdAndEnvironmentId({}, {})", id, environmentId);
        try {
            List<SubscriptionForm> list = queryAndEnrichWithApiIds(
                getOrm().getSelectAllSql() + " where id = ? and environment_id = ?",
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
    public List<SubscriptionForm> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findAllByEnvironmentId({})", environmentId);
        try {
            return queryAndEnrichWithApiIds(getOrm().getSelectAllSql() + " where environment_id = ? order by name", environmentId);
        } catch (final Exception ex) {
            log.error("Failed to find subscription forms by environment id: {}", environmentId, ex);
            throw new TechnicalException("Failed to find subscription forms by environment id", ex);
        }
    }

    @Override
    public Optional<SubscriptionForm> findDefaultByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findDefaultByEnvironmentId({})", environmentId);
        try {
            List<SubscriptionForm> list = queryAndEnrichWithApiIds(
                getOrm().getSelectAllSql() + " where environment_id = ? and default_form = ? order by id",
                environmentId,
                true
            );
            if (list.size() > 1) {
                log.warn(
                    "Environment [{}] has {} default subscription forms {}, using [{}]",
                    environmentId,
                    list.size(),
                    list.stream().map(SubscriptionForm::getId).toList(),
                    list.getFirst().getId()
                );
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
        } catch (final Exception ex) {
            log.error("Failed to find default subscription form by environment id: {}", environmentId, ex);
            throw new TechnicalException("Failed to find default subscription form by environment id", ex);
        }
    }

    @Override
    public Optional<SubscriptionForm> findByEnvironmentIdAndApiId(String environmentId, String apiId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.findByEnvironmentIdAndApiId({}, {})", environmentId, apiId);
        try {
            List<SubscriptionForm> list = queryAndEnrichWithApiIds(
                getOrm().getSelectAllSql() +
                    " where environment_id = ? and id in (select form_id from " +
                    SUBSCRIPTION_FORM_APIS +
                    " where api_id = ?) order by id",
                environmentId,
                apiId
            );
            if (list.size() > 1) {
                log.warn(
                    "API [{}] is mapped to {} subscription forms {}, using [{}]",
                    apiId,
                    list.size(),
                    list.stream().map(SubscriptionForm::getId).toList(),
                    list.getFirst().getId()
                );
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
        } catch (final Exception ex) {
            log.error("Failed to find subscription form by environment id and api id: {}, {}", environmentId, apiId, ex);
            throw new TechnicalException("Failed to find subscription form by environment id and api id", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSubscriptionFormRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update(
                "delete from " +
                    SUBSCRIPTION_FORM_APIS +
                    " where form_id in (select id from " +
                    this.tableName +
                    " where environment_id = ?)",
                environmentId
            );
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            log.error("Failed to delete subscription forms by environment id: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete subscription forms by environment id", ex);
        }
    }

    private void storeApiIds(SubscriptionForm form, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + SUBSCRIPTION_FORM_APIS + " where form_id = ?", form.getId());
        }
        List<String> apiIds = getOrm().filterStrings(form.getApiIds());
        if (!apiIds.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + SUBSCRIPTION_FORM_APIS + " ( form_id, api_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(form.getId(), apiIds)
            );
        }
    }

    private List<SubscriptionForm> queryAndEnrichWithApiIds(String sql, Object... args) {
        List<SubscriptionForm> forms = jdbcTemplate.query(sql, getOrm().getRowMapper(), args);
        enrichWithApiIds(forms);
        return forms;
    }

    private void attachApiIds(SubscriptionForm form) {
        if (form == null) {
            return;
        }
        form.setApiIds(
            jdbcTemplate.query(
                "select api_id from " + SUBSCRIPTION_FORM_APIS + " where form_id = ? order by api_id",
                (ResultSet rs, int rowNum) -> rs.getString("api_id"),
                form.getId()
            )
        );
    }

    private void enrichWithApiIds(List<SubscriptionForm> forms) {
        if (forms == null || forms.isEmpty()) {
            return;
        }
        List<String> ids = forms.stream().map(SubscriptionForm::getId).toList();
        Map<String, List<String>> apiIdsByFormId = new HashMap<>();
        jdbcTemplate.query(
            "select form_id, api_id from " +
                SUBSCRIPTION_FORM_APIS +
                " where form_id in (" +
                getOrm().buildInClause(ids) +
                ") order by api_id",
            rs -> {
                apiIdsByFormId.computeIfAbsent(rs.getString("form_id"), k -> new ArrayList<>()).add(rs.getString("api_id"));
            },
            ids.toArray()
        );
        forms.forEach(form -> form.setApiIds(apiIdsByFormId.getOrDefault(form.getId(), List.of())));
    }
}
