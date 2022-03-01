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

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcAlertRepository extends JdbcAbstractCrudRepository<AlertTrigger, String> implements AlertTriggerRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcAlertRepository.class);
    private final String ALERT_EVENT_RULES;

    JdbcAlertRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "alert_triggers");
        ALERT_EVENT_RULES = getTableNameFor("alert_event_rules");
    }

    @Override
    protected JdbcObjectMapper<AlertTrigger> buildOrm() {
        return JdbcObjectMapper
            .builder(AlertTrigger.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("enabled", Types.BIT, boolean.class)
            .addColumn("severity", Types.NVARCHAR, String.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("template", Types.BIT, boolean.class)
            .build();
    }

    @Override
    protected String getId(final AlertTrigger alert) {
        return alert.getId();
    }

    @Override
    public List<AlertTrigger> findByReferenceAndReferenceIds(final String referenceType, final List<String> referenceIds)
        throws TechnicalException {
        LOGGER.debug("JdbcAlertRepository.findByReferenceAndReferenceIds({}, {})", referenceType, referenceIds);
        if (isEmpty(referenceIds)) {
            return emptyList();
        }
        try {
            List<AlertTrigger> rows = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " where reference_type = ? and reference_id in ( " +
                getOrm().buildInClause(referenceIds) +
                " )",
                (PreparedStatement ps) -> {
                    ps.setString(1, referenceType);
                    getOrm().setArguments(ps, referenceIds, 2);
                },
                getOrm().getRowMapper()
            );

            return rows.stream().peek(this::addEvents).collect(Collectors.toList());
        } catch (final Exception ex) {
            final String message = "Failed to find alerts by reference and referenceIds";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public Optional<AlertTrigger> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcAlertRepository.findById({})", id);
        try {
            Optional<AlertTrigger> alert = jdbcTemplate
                .query(getOrm().getSelectAllSql() + " a where id = ?", getOrm().getRowMapper(), id)
                .stream()
                .findFirst();
            if (alert.isPresent()) {
                addEvents(alert.get());
            }
            return alert;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find alert by id", ex);
            throw new TechnicalException("Failed to find alert by id", ex);
        }
    }

    @Override
    public AlertTrigger create(final AlertTrigger alert) throws TechnicalException {
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(alert));
            storeEvents(alert, false);
            return findById(alert.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create alert", ex);
            throw new TechnicalException("Failed to create alert", ex);
        }
    }

    @Override
    public AlertTrigger update(final AlertTrigger alert) throws TechnicalException {
        if (alert == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(alert, alert.getId()));
            storeEvents(alert, true);
            return findById(alert.getId())
                .orElseThrow(() -> new IllegalStateException(format("No alert found with id [%s]", alert.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update alert:", ex);
            throw new TechnicalException("Failed to update alert", ex);
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + ALERT_EVENT_RULES + " where alert_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    private void addEvents(AlertTrigger parent) {
        List<AlertEventRule> events = getEvents(parent.getId());
        parent.setEventRules(events);
    }

    private List<AlertEventRule> getEvents(String alertId) {
        List<AlertEventType> events = jdbcTemplate.query(
            "select alert_event from " + ALERT_EVENT_RULES + " where alert_id = ?",
            (ResultSet rs, int rowNum) -> {
                String value = rs.getString(1);
                try {
                    return AlertEventType.valueOf(value);
                } catch (IllegalArgumentException ex) {
                    LOGGER.error("Failed to parse {} as alert_event:", value, ex);
                    return null;
                }
            },
            alertId
        );

        List<AlertEventRule> eventRules = new ArrayList<>(events.size());
        for (AlertEventType event : events) {
            if (event != null) {
                eventRules.add(new AlertEventRule(event));
            }
        }

        return eventRules;
    }

    private void storeEvents(AlertTrigger alert, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + ALERT_EVENT_RULES + " where alert_id = ?", alert.getId());
        }
        List<String> events = new ArrayList<>();
        if (alert.getEventRules() != null) {
            for (AlertEventRule alertEventRule : alert.getEventRules()) {
                events.add(alertEventRule.getEvent().name());
            }
        }
        if (!events.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + ALERT_EVENT_RULES + " ( alert_id, alert_event ) values ( ?, ? )",
                getOrm().getBatchStringSetter(alert.getId(), events)
            );
        }
    }

    @Override
    public Set<AlertTrigger> findAll() throws TechnicalException {
        LOGGER.debug("JdbcAlertTriggerRepository.findAll()");
        try {
            List<AlertTrigger> rows = jdbcTemplate.query(getOrm().getSelectAllSql(), getOrm().getRowMapper());
            Set<AlertTrigger> alertTriggers = new HashSet<>();
            for (AlertTrigger alertTrigger : rows) {
                addEvents(alertTrigger);
                alertTriggers.add(alertTrigger);
            }
            return alertTriggers;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all alertTriggers:", ex);
            throw new TechnicalException("Failed to find all alertTriggers", ex);
        }
    }
}
