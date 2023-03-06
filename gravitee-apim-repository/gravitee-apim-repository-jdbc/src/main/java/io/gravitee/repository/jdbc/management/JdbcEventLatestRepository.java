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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createOffsetClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.addCondition;
import static io.gravitee.repository.jdbc.management.JdbcHelper.addStringsWhereClause;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
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
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
@Slf4j
public class JdbcEventLatestRepository extends JdbcAbstractRepository<Event> implements EventLatestRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String EVENT_PROPERTIES;
    private final String EVENT_ENVIRONMENTS;

    JdbcEventLatestRepository(@Value("${management.jdbc.prefix:}") String tablePrefix, @Autowired JdbcTemplate jdbcTemplate) {
        super(tablePrefix, "events_latest");
        this.jdbcTemplate = jdbcTemplate;
        EVENT_PROPERTIES = getTableNameFor("events_latest_properties");
        EVENT_ENVIRONMENTS = getTableNameFor("events_latest_environments");
    }

    private static final JdbcHelper.ChildAdder<Event> CHILD_ADDER = (Event parent, ResultSet rs) -> {
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

    @Override
    public List<Event> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
        if (log.isDebugEnabled()) {
            log.debug("JdbcEventLatestRepository.search({})", criteriaToString(criteria));
        }

        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = new StringBuilder(
            "select evt.*, evp.*, ev.* from " +
            this.tableName +
            " evt inner join " +
            EVENT_PROPERTIES +
            " evp on evt.id = evp.event_id " +
            "left join " +
            EVENT_ENVIRONMENTS +
            " ev on evt.id = ev.event_id"
        );

        appendCriteria(builder, criteria, args, "evt");
        builder.append(args.isEmpty() ? WHERE_CLAUSE : AND_CLAUSE).append("evp.property_key = ? and evp.property_value is not null");
        args.add(group.getValue());

        if (page != null && size != null && size > 0) {
            final int limit = size.intValue();
            builder.append(createPagingClause(limit, (page.intValue() * limit)));
        } else {
            // Hack to add offset O because some db engines do not support ordering sub query without specifying offset (-> sqlserver), others do not support offset without limit (--> mysql).
            builder.append(createOffsetClause(0L));
        }
        builder.append(" order by evt.updated_at desc, evt.id desc");

        return queryEvents(builder.toString(), args);
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

    private void storeProperties(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + EVENT_PROPERTIES + " where event_id = ?", event.getId());
        }
        if (event.getProperties() != null) {
            List<Map.Entry<String, String>> list = new ArrayList<>(event.getProperties().entrySet());
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

    @Override
    public void delete(final String id) throws TechnicalException {
        log.debug("JdbcEventRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from " + EVENT_PROPERTIES + " where event_id = ?", id);
            jdbcTemplate.update("delete from " + EVENT_ENVIRONMENTS + " where event_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            log.error("Failed to delete event", ex);
            throw new TechnicalException("Failed to delete event", ex);
        }
    }

    @Override
    public Event createOrPatch(Event event) throws TechnicalException {
        if (event == null || event.getId() == null || event.getType() == null) {
            throw new IllegalStateException("Event to create or update must have an id and a type");
        }

        final int updatedEventCount = patchEvent(event);
        if (updatedEventCount <= 0) {
            createEvent(event);
        } else {
            // update properties only if event was correctly updated
            patchEventProperties(event);
            storeEnvironments(event, true);
        }
        return event;
    }

    public void createEvent(Event event) {
        jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(event));
        storeProperties(event, false);
        storeEnvironments(event, false);
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

    private void patchEventProperties(Event event) {
        if (event.getProperties() != null) {
            event.getProperties().forEach((property, value) -> updateEventProperty(event.getId(), property, value));
        }
    }

    private void updateEventProperty(String eventId, String propertyKey, String value) {
        jdbcTemplate.update(
            "update " + EVENT_PROPERTIES + " set property_value = ? where event_id = ? and property_key = ?",
            value,
            eventId,
            propertyKey
        );
    }

    List<Event> queryEvents(String sql, List<Object> args) {
        log.debug("SQL: {}", sql);
        log.debug("Args: {}", args);
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
        log.debug("Events found: {}", events);
        return events;
    }

    void appendCriteria(StringBuilder builder, EventCriteria filter, List<Object> args, String tableAlias) {
        boolean started = addPropertiesWhereClause(filter, args, builder, tableAlias);
        if (filter.getFrom() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append(tableAlias + ".updated_at >= ?");
            args.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append(tableAlias + ".updated_at < ?");
            args.add(new Date(filter.getTo()));
            started = true;
        }
        if (!isEmpty(filter.getEnvironments())) {
            started = addStringsWhereClause(filter.getEnvironments(), "ev.environment_id", args, builder, started);
        }
        if (!isEmpty(filter.getTypes())) {
            final Collection<String> types = filter.getTypes().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(types, tableAlias + ".type", args, builder, started);
        }
    }

    private boolean addPropertiesWhereClause(EventCriteria filter, List<Object> args, StringBuilder builder, String tableAlias) {
        if (!isEmpty(filter.getProperties())) {
            builder.append(" left join " + EVENT_PROPERTIES + " prop on prop.event_id = " + tableAlias + ".id ");
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

    String criteriaToString(EventCriteria filter) {
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
}
