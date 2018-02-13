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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

import static io.gravitee.repository.jdbc.management.JdbcHelper.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 *
 * @author njt
 */
@Repository
public class JdbcEventRepository extends JdbcAbstractPageableRepository<Event> implements EventRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEventRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Event.class, "events", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("type", Types.NVARCHAR, EventType.class)
            .addColumn("payload", Types.NVARCHAR, String.class)
            .addColumn("parent_id", Types.NVARCHAR, String.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();    

    private static final JdbcHelper.ChildAdder<Event> CHILD_ADDER = (Event parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("property_key") != null) {
            properties.put(rs.getString("property_key"), rs.getString("property_value"));
        }
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void storeProperties(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from event_properties where event_id = ?", event.getId());
        }
        if (event.getProperties() != null) {
            List<Entry<String, String>> list = new ArrayList<>(event.getProperties().entrySet());
            jdbcTemplate.batchUpdate("insert into event_properties ( event_id, property_key, property_value ) values ( ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
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
            });
        }
    }

    @Override
    public Optional<Event> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcEventRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Event> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query("select * from events e left join event_properties ep on e.id = ep.event_id where e.id = ?"
                    , rowMapper
                    , id
            );
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
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(event));
            storeProperties(event, false);
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
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(event, event.getId()));
            storeProperties(event, true);
            return findById(event.getId()).orElseThrow(() -> new IllegalStateException(format("No event found with id [%s]", event.getId())));
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
            jdbcTemplate.update("delete from event_properties where event_id = ?", id);
            jdbcTemplate.update(ORM.getDeleteSql(), id);
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
        final StringBuilder builder = new StringBuilder("select e.*, ep.* from events e left join event_properties ep on e.id = ep.event_id ");
        boolean started = addPropertiesWhereClause(filter, args, builder);
        if (filter.getFrom() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("updated_at >= ?");
            args.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("updated_at < ?");
            args.add(new Date(filter.getTo()));
            started = true;
        }

        if (!isEmpty(filter.getTypes())) {
            final Collection<String> types = filter.getTypes().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(types, "type", args, builder, started);
        }

        builder.append(" order by updated_at desc ");
        String sql = builder.toString();
        LOGGER.debug("SQL: {}", sql);
        LOGGER.debug("Args: {}", args);
        final JdbcHelper.CollatingRowMapper<Event> rowCallbackHandler
                = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
        jdbcTemplate.query((Connection cnctn) -> {
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
        }, rowCallbackHandler);
        final List<Event> events = rowCallbackHandler.getRows();
        LOGGER.debug("Events found: {}", events);
        return events;
    }

    private boolean addPropertiesWhereClause(EventCriteria filter, List<Object> args, StringBuilder builder) {
        if (!isEmpty(filter.getProperties())) {
            builder.append(" left join event_properties prop on prop.event_id = e.id ");
            builder.append(WHERE_CLAUSE);
            builder.append("(");
            boolean first = true;
            for (Entry<String, Object> property : filter.getProperties().entrySet()) {
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
    
    private String criteriaToString(EventCriteria filter) {
        return "{ " + "from: " + filter.getFrom() +
                ", " + "props: " + filter.getProperties() +
                ", " + "to: " + filter.getTo() +
                ", " + "types: " + filter.getTypes() +
                " }";
    }
}