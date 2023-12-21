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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createOffsetClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.management.JdbcHelper.*;
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
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;
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
            final StringBuilder builder = new StringBuilder(getOrm().getSelectAllSql() + " e ");
            builder.append("left join ").append(EVENT_PROPERTIES).append(" ep on e.id = ep.event_id ");
            builder.append("left join ").append(EVENT_ENVIRONMENTS).append(" ev on e.id = ev.event_id ");
            builder.append("where e.id = ?");
            jdbcTemplate.query(builder.toString(), rowMapper, id);
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
    public List<Event> searchLatest(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcEventRepository.search({})", criteriaToString(criteria));
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

        builder.append(" inner join (select e.id from " + this.tableName + " e ");
        if (criteria.isStrictMode()) {
            builder.append(" inner join " + EVENT_ENVIRONMENTS + " ev1 on e.id = ev1.event_id");
            appendCriteria(builder, criteria, args, "e", "ev1");
            builder
                .append(args.isEmpty() ? WHERE_CLAUSE : AND_CLAUSE)
                .append("e.id in(")
                .append(joinLatest(group, criteria, args))
                .append(")");
        } else {
            builder.append(" where e.id in(").append(joinLatest(group, criteria, args)).append(")");
        }
        builder.append("    order by e.updated_at desc, e.id desc ");

        if (page != null && size != null && size > 0) {
            final int limit = size.intValue();
            builder.append(createPagingClause(limit, (page.intValue() * limit)));
        } else {
            // Hack to add offset O because some db engines do not support ordering sub query without specifying offset (-> sqlserver), others do not support offset without limit (--> mysql).
            builder.append(createOffsetClause(0L));
        }
        builder.append("    ) as je on je.id = evt.id ");
        builder.append(" order by evt.updated_at desc, evt.id desc");

        return queryEvents(builder.toString(), args);
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
        appendCriteria(builder, filter, args, "e", "ev");

        builder.append(" order by updated_at desc, id desc ");
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
            // update properties only if event was correctly updated
            patchEventProperties(event);
            storeEnvironments(event, true);
            return event;
        }
    }

    @Override
    public long deleteApiEvents(String apiId) throws TechnicalException {
        try {
            List<String> eventToDelete = jdbcTemplate.queryForList(
                "select event_id from " + EVENT_PROPERTIES + " where property_key = ? and property_value = ?",
                String.class,
                Event.EventProperties.API_ID.getValue(),
                apiId
            );

            if (eventToDelete.isEmpty()) {
                return 0;
            }

            String propertiesDeleteQuery =
                "delete from " + EVENT_PROPERTIES + " where event_id in (" + getOrm().buildInClause(eventToDelete) + ")";
            jdbcTemplate.update(
                propertiesDeleteQuery,
                (PreparedStatement ps) -> {
                    getOrm().setArguments(ps, eventToDelete, 1);
                }
            );

            String environmentsDeleteQuery =
                "delete from " + EVENT_ENVIRONMENTS + " where event_id in (" + getOrm().buildInClause(eventToDelete) + ")";
            jdbcTemplate.update(
                environmentsDeleteQuery,
                (PreparedStatement ps) -> {
                    getOrm().setArguments(ps, eventToDelete, 1);
                }
            );

            String eventsDeleteQuery = "delete from " + this.tableName + " where id in (" + getOrm().buildInClause(eventToDelete) + ")";
            return jdbcTemplate.update(
                eventsDeleteQuery,
                (PreparedStatement ps) -> {
                    getOrm().setArguments(ps, eventToDelete, 1);
                }
            );
        } catch (final Exception ex) {
            String error = String.format("An error occurred when deleting all events of API %s", apiId);
            LOGGER.error(error, apiId, ex);
            throw new TechnicalException(error);
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

    private void appendCriteria(
        StringBuilder builder,
        EventCriteria filter,
        List<Object> args,
        String eventTableAlias,
        String eventEnvionmentTableAlias
    ) {
        boolean started = addPropertiesWhereClause(filter, args, builder, eventTableAlias);
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
                addStringsWhereClause(filter.getEnvironments(), eventEnvionmentTableAlias + ".environment_id", args, builder, started);
        }
        if (!isEmpty(filter.getTypes())) {
            final Collection<String> types = filter.getTypes().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(types, eventTableAlias + ".type", args, builder, started);
        }
    }

    private boolean addPropertiesWhereClause(EventCriteria filter, List<Object> args, StringBuilder builder, String eventTableAlias) {
        if (!isEmpty(filter.getProperties())) {
            builder.append(" left join " + EVENT_PROPERTIES + " prop on prop.event_id = " + eventTableAlias + ".id ");
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

    /**
     * Create a select query that can be used in a join in order to select the latest event of each api or dictionary (eg: group).
     */
    private StringBuilder joinLatest(Event.EventProperties group, EventCriteria criteria, List<Object> args) {
        // Add group argument twice as there are 2 inner selects to include.
        args.add(group.getValue());
        StringBuilder innerSelect1 = innerSelectLatest(criteria, args);
        args.add(group.getValue());
        StringBuilder maxEventDateInnerJoin = computeMaxEventDateInnerJoin(criteria, args);

        return new StringBuilder()
            .append("select t1.event_id ")
            .append("from ( ")
            .append(innerSelect1)
            .append(") as t1 ")
            .append("inner join ( ")
            .append(maxEventDateInnerJoin)
            .append(") as t3 on t1.api_id = t3.api_id ")
            .append("where t1.event_date = t3.event_date ");
    }

    private StringBuilder computeMaxEventDateInnerJoin(EventCriteria criteria, List<Object> args) {
        final StringBuilder query = new StringBuilder("select max(event_date) as event_date, t2.api_id ")
            .append("from ( ")
            .append("select ep1.property_value as api_id, max(e1.updated_at) as event_date ")
            .append("from " + this.tableName + " e1 ")
            .append("inner join " + EVENT_PROPERTIES + " ep1 on e1.id = ep1.event_id and ep1.property_key = ? ");

        if (!criteria.isStrictMode()) {
            query.append("inner join " + EVENT_ENVIRONMENTS + " ev1 on e1.id = ev1.event_id ");
            appendCriteria(query, criteria, args, "e1", "ev1");
        }

        return query.append("group by ep1.property_value, ep1.event_id ) as t2 ").append("group by api_id ");
    }

    private StringBuilder innerSelectLatest(EventCriteria criteria, List<Object> args) {
        final StringBuilder innerSelectLatestQuery = new StringBuilder()
            .append("select ep1.property_value as api_id, ep1.event_id as event_id, max(e1.updated_at) as event_date ")
            .append("from " + this.tableName + " e1 ")
            .append(
                "inner join " +
                EVENT_PROPERTIES +
                " ep1 on e1.id = ep1.event_id and ep1.property_key = ? and ep1.property_value is not null "
            );

        if (!criteria.isStrictMode()) {
            innerSelectLatestQuery.append("inner join " + EVENT_ENVIRONMENTS + " ev1 on e1.id = ev1.event_id");
            appendCriteria(innerSelectLatestQuery, criteria, args, "e1", "ev1");
        }
        return innerSelectLatestQuery.append(" group by ep1.property_value, ep1.event_id ");
    }

    private String criteriaToString(EventCriteria filter) {
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
