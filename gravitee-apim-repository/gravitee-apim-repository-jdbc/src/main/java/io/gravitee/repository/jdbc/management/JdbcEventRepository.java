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

import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.addCondition;
import static io.gravitee.repository.jdbc.management.JdbcHelper.addStringsWhereClause;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 *
 * @author njt
 */
@Repository
public class JdbcEventRepository extends JdbcAbstractPageableRepository<Event> implements EventRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEventRepository.class);
    private final String EVENT_PROPERTIES;
    private final String EVENT_ENVIRONMENTS;

    JdbcEventRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "events");
        EVENT_PROPERTIES = getTableNameFor("event_properties");
        EVENT_ENVIRONMENTS = getTableNameFor("event_environments");
    }

    @Override
    protected JdbcObjectMapper<Event> buildOrm() {
        return JdbcObjectMapper
            .builder(Event.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("type", Types.NVARCHAR, EventType.class)
            .addColumn("payload", Types.NVARCHAR, String.class)
            .addColumn("parent_id", Types.NVARCHAR, String.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    static final JdbcHelper.ChildAdder<Event> CHILD_ADDER = (Event parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("property_key") != null) {
            properties.put(rs.getString("property_key"), rs.getString("property_value"));
        }

        Set<String> environments = parent.getEnvironments();
        if (environments == null) {
            environments = new HashSet<>();
            parent.setEnvironments(environments);
        }
        if (rs.getString("environment_id") != null) {
            environments.add(rs.getString("environment_id"));
        }
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void storeProperties(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + EVENT_PROPERTIES + " where event_id = ?", event.getId());
        }
        if (event.getProperties() != null) {
            List<Entry<String, String>> list = new ArrayList<>(event.getProperties().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + EVENT_PROPERTIES + " ( event_id, property_key, property_value ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, event.getId());
                        ps.setString(2, list.get(i).getKey());
                        ps.setString(3, list.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return list.size();
                    }
                }
            );
        }
    }

    private void storeEnvironments(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + EVENT_ENVIRONMENTS + " where event_id = ?", event.getId());
        }
        if (!CollectionUtils.isEmpty(event.getEnvironments())) {
            List<String> list = new ArrayList<>(event.getEnvironments());
            jdbcTemplate.batchUpdate(
                "insert into " + EVENT_ENVIRONMENTS + " ( event_id, environment_id) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, event.getId());
                        ps.setString(2, list.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return list.size();
                    }
                }
            );
        }
    }

    @Override
    public Optional<Event> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcEventRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Event> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            String builder =
                getOrm().getSelectAllSql() +
                " e " +
                "left join " +
                EVENT_PROPERTIES +
                " ep on e.id = ep.event_id " +
                "left join " +
                EVENT_ENVIRONMENTS +
                " ev on e.id = ev.event_id " +
                "where e.id = ?";
            jdbcTemplate.query(builder, rowMapper, id);
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find event by id", ex);
            throw new TechnicalException("Failed to find event by id", ex);
        }
    }

    @Override
    public Event create(Event event) throws TechnicalException {
        LOGGER.debug("JdbcEventRepository.create({})", event);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(event));
            storeProperties(event, false);
            storeEnvironments(event, false);
            return findById(event.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create event:", ex);
            throw new TechnicalException("Failed to create event", ex);
        }
    }

    @Override
    public Event update(final Event event) throws TechnicalException {
        LOGGER.debug("JdbcEventRepository.update({})", event);
        if (event == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(event, event.getId()));
            storeProperties(event, true);
            storeEnvironments(event, true);
            return findById(event.getId())
                .orElseThrow(() -> new IllegalStateException(format("No event found with id [%s]", event.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update event", ex);
            throw new TechnicalException("Failed to update event", ex);
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        LOGGER.debug("JdbcEventRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from " + EVENT_PROPERTIES + " where event_id = ?", id);
            jdbcTemplate.update("delete from " + EVENT_ENVIRONMENTS + " where event_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete event", ex);
            throw new TechnicalException("Failed to delete event", ex);
        }
    }

    @Override
    public Page<Event> search(EventCriteria filter, Pageable page) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcEventRepository.search({}, {})", criteriaToString(filter), page);
        }
        List<Event> events = search(filter);
        return getResultAsPage(page, events);
    }

    @Override
    public List<Event> search(EventCriteria filter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcEventRepository.search({})", criteriaToString(filter));
        }
        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = createSearchQueryBuilder();
        appendCriteria(builder, filter, args, "e", "ev", EVENT_PROPERTIES);

        builder.append(" order by e.updated_at desc, e.id desc ");
        return queryEvents(builder.toString(), args);
    }

    @Override
    public Event createOrPatch(Event event) throws TechnicalException {
        if (event == null || event.getId() == null || event.getType() == null) {
            throw new IllegalStateException("Event to create or update must have an id and a type");
        }

        final int updatedEventCount = patchEvent(event);
        if (updatedEventCount <= 0) {
            return create(event);
        } else {
            // We don't want to erase existing properties only add/update new one
            patchEventProperties(event);
            // Environments are updated only if new ones are provided
            if (event.getEnvironments() != null) {
                storeEnvironments(event, true);
            }
            return event;
        }
    }

    private void patchEventProperties(Event event) {
        if (event.getProperties() != null) {
            event.getProperties().forEach((property, value) -> updateEventProperty(event.getId(), property, value));
        }
    }

    private void updateEventProperty(String eventId, String propertyKey, String value) {
        int update = jdbcTemplate.update(
            "update " + EVENT_PROPERTIES + " set property_value = ? where event_id = ? and property_key = ?",
            value,
            eventId,
            propertyKey
        );
        if (update <= 0) {
            jdbcTemplate.update(
                "insert into " + EVENT_PROPERTIES + " ( event_id, property_key, property_value ) values ( ?, ?, ? )",
                eventId,
                propertyKey,
                value
            );
        }
    }

    private int patchEvent(Event event) {
        List<Object> args = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        boolean hasSet = false;
        queryBuilder.append("update ").append(this.tableName);
        if (event.getType() != null) {
            queryBuilder.append(" set type = ?");
            args.add(event.getType().name());
            hasSet = true;
        }
        if (event.getPayload() != null) {
            queryBuilder.append(hasSet ? "," : " set").append(" payload = ?");
            args.add(event.getPayload());
            hasSet = true;
        }
        if (event.getParentId() != null) {
            queryBuilder.append(hasSet ? "," : " set").append(" parent_id = ?");
            args.add(event.getParentId());
            hasSet = true;
        }
        if (event.getUpdatedAt() != null) {
            queryBuilder.append(hasSet ? "," : " set").append(" updated_at = ?");
            args.add(event.getUpdatedAt());
        }
        queryBuilder.append(" where id = ? ");
        args.add(event.getId());
        return jdbcTemplate.update(queryBuilder.toString(), args.toArray());
    }

    private List<Event> queryEvents(String sql, List<Object> args) {
        LOGGER.debug("SQL: {}", sql);
        LOGGER.debug("Args: {}", args);
        final JdbcHelper.CollatingRowMapper<Event> rowCallbackHandler = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            CHILD_ADDER,
            "id"
        );
        jdbcTemplate.query(
            (Connection cnctn) -> {
                PreparedStatement stmt = cnctn.prepareStatement(sql);
                int idx = 1;
                for (final Object arg : args) {
                    if (arg instanceof Date) {
                        final Date date = (Date) arg;
                        stmt.setTimestamp(idx++, new Timestamp(date.getTime()));
                    } else {
                        stmt.setObject(idx++, arg);
                    }
                }
                return stmt;
            },
            rowCallbackHandler
        );
        final List<Event> events = rowCallbackHandler.getRows();
        LOGGER.debug("Events found: {}", events);
        return events;
    }

    private StringBuilder createSearchQueryBuilder() {
        final StringBuilder builder = new StringBuilder("select e.*, ep.*, ev.* from " + this.tableName + " e ");
        builder.append(" left join ").append(EVENT_PROPERTIES).append(" ep on e.id = ep.event_id ");
        builder.append(" left join ").append(EVENT_ENVIRONMENTS).append(" ev on e.id = ev.event_id ");
        return builder;
    }

    static void appendCriteria(
        StringBuilder builder,
        EventCriteria filter,
        List<Object> args,
        String eventTableAlias,
        String eventEnvironmentTableAlias,
        String eventPropertiesTable
    ) {
        boolean started = addPropertiesWhereClause(filter, args, builder, eventPropertiesTable, eventTableAlias);
        if (filter.getFrom() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append(eventTableAlias + ".updated_at >= ?");
            args.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append(eventTableAlias + ".updated_at < ?");
            args.add(new Date(filter.getTo()));
            started = true;
        }
        if (!isEmpty(filter.getEnvironments())) {
            started =
                addStringsWhereClause(filter.getEnvironments(), eventEnvironmentTableAlias + ".environment_id", args, builder, started);
        }
        if (!isEmpty(filter.getTypes())) {
            final Collection<String> types = filter.getTypes().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(types, eventTableAlias + ".type", args, builder, started);
        }
    }

    private static boolean addPropertiesWhereClause(
        EventCriteria filter,
        List<Object> args,
        StringBuilder builder,
        final String propertiesTable,
        String tableAlias
    ) {
        if (!isEmpty(filter.getProperties())) {
            builder.append(" left join " + propertiesTable + " prop on prop.event_id = " + tableAlias + ".id ");
            builder.append(WHERE_CLAUSE);
            builder.append("(");
            boolean first = true;
            for (Map.Entry<String, Object> property : filter.getProperties().entrySet()) {
                if (property.getValue() instanceof Collection) {
                    for (Object value : (Collection) property.getValue()) {
                        first = addCondition(first, builder, property.getKey(), value, args);
                    }
                } else {
                    first = addCondition(first, builder, property.getKey(), property.getValue(), args);
                }
            }
            builder.append(")");
            return true;
        }
        return false;
    }

    static String criteriaToString(EventCriteria filter) {
        return (
            "{ " +
            "from: " +
            filter.getFrom() +
            ", " +
            "props: " +
            filter.getProperties() +
            ", " +
            "to: " +
            filter.getTo() +
            ", " +
            "environments: " +
            filter.getEnvironments() +
            ", " +
            "types: " +
            filter.getTypes() +
            " }"
        );
    }

    @Override
    public Set<Event> findAll() throws TechnicalException {
        throw new IllegalStateException("not implemented cause of high amount of data. Use pageable search instead");
    }
}
