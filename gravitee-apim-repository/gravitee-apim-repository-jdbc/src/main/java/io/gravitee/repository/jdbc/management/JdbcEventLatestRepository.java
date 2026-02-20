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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.management.JdbcEventRepository.CHILD_ADDER;
import static io.gravitee.repository.jdbc.management.JdbcEventRepository.criteriaToString;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
@Slf4j
@Repository
public class JdbcEventLatestRepository extends JdbcAbstractRepository<Event> implements EventLatestRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String EVENT_PROPERTIES;
    private final String EVENT_ENVIRONMENTS;
    private final String EVENT_ORGANIZATIONS;

    JdbcEventLatestRepository(
        @Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix,
        @Autowired JdbcTemplate jdbcTemplate
    ) {
        super(tablePrefix, "events_latest");
        this.jdbcTemplate = jdbcTemplate;
        EVENT_PROPERTIES = getTableNameFor("events_latest_properties");
        EVENT_ENVIRONMENTS = getTableNameFor("events_latest_environments");
        EVENT_ORGANIZATIONS = getTableNameFor("events_latest_organizations");
    }

    @Override
    public List<Event> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
        log.debug("JdbcEventLatestRepository.search({})", criteriaToString(criteria));

        var pageNumber = page != null ? page : 0;
        var pageSize = size != null ? size : 10;

        final List<Object> args = new ArrayList<>();
        var select = """
            WITH PagedEvents AS (%s)
            SELECT evt.*, evp.*, ev.*, evo.*
            FROM %s evt
                INNER JOIN PagedEvents pe ON evt.id = pe.id
                INNER JOIN %s evp ON evt.id = evp.event_id
                LEFT JOIN %s ev ON evt.id = ev.event_id
                LEFT JOIN %s evo ON evt.id = evo.event_id
            ORDER BY evt.updated_at ASC, evt.id ASC
            """.formatted(
                buildSelectIn(criteria, group, pageNumber, pageSize, args),
                tableName,
                EVENT_PROPERTIES,
                EVENT_ENVIRONMENTS,
                EVENT_ORGANIZATIONS
            );
        return queryEvents(select, args);
    }

    /**
     * Builds a SQL query fragment to select event IDs from a database table, optionally filtered by a specific event property
     * and supporting pagination through the use of page and size parameters.
     * <p>
     * This allows us to handle pagination properly for the event because events with properties result with more than one row.
     * </p>
     *
     * @param criteria the event search criteria.
     * @param group    the specific event property to filter by. If null, no event property filter is applied.
     * @param page     the page number used for pagination. If null, pagination is not applied.
     * @param size     the size of each page used for pagination. If null or less than or equal to zero, pagination is not applied.
     * @return a SQL query string selecting event IDs with optional filtering and pagination.
     */
    private String buildSelectIn(EventCriteria criteria, Event.EventProperties group, Long page, Long size, List<Object> args) {
        if (group != null || !criteria.getProperties().isEmpty()) {
            final StringBuilder where = new StringBuilder();
            appendCriteria(where, criteria, args, "e1", "ee1", "eo1");

            if (group != null) {
                where.append(!where.isEmpty() ? AND_CLAUSE : WHERE_CLAUSE);
                where.append("prop.property_key = ? ");
                args.add(group.getValue());
            }
            if (!criteria.getProperties().isEmpty()) {
                where.append(!where.isEmpty() ? AND_CLAUSE : WHERE_CLAUSE);
                var first = true;
                for (Map.Entry<String, Object> property : criteria.getProperties().entrySet()) {
                    if (property.getValue() instanceof Collection<?> collection) {
                        for (Object value : collection) {
                            first = addCondition(first, where, property.getKey(), value, args);
                        }
                    } else {
                        first = addCondition(first, where, property.getKey(), property.getValue(), args);
                    }
                }
            }

            var selectIn = """
                select e1.id from %s e1
                  inner join %s prop on e1.id = prop.event_id
                  left join %s ee1 on e1.id = ee1.event_id
                  left join %s eo1 on e1.id = eo1.event_id
                  %s
                  order by e1.updated_at asc, e1.id asc
                """.formatted(tableName, EVENT_PROPERTIES, EVENT_ENVIRONMENTS, EVENT_ORGANIZATIONS, where);

            if (page != null && size != null && size > 0) {
                final int limit = size.intValue();
                selectIn += createPagingClause(limit, (page.intValue() * limit));
            }
            return selectIn;
        }

        var where = new StringBuilder();
        appendCriteria(where, criteria, args, "e1", "ee1", "eo1");

        var selectIn = """
            select e1.id from %s e1
              left join %s ee1 on e1.id = ee1.event_id
              left join %s eo1 on e1.id = eo1.event_id
            %s
            order by e1.updated_at asc, e1.id asc
            """.formatted(tableName, EVENT_ENVIRONMENTS, EVENT_ORGANIZATIONS, where);
        if (page != null && size != null && size > 0) {
            final int limit = size.intValue();
            selectIn += createPagingClause(limit, (page.intValue() * limit));
        }
        return selectIn;
    }

    private StringBuilder createSearchQueryBuilder() {
        return new StringBuilder(
            "select evt.*, evp.*, ev.*, evo.* from " +
                this.tableName +
                " evt inner join " +
                EVENT_PROPERTIES +
                " evp on evt.id = evp.event_id " +
                "left join " +
                EVENT_ENVIRONMENTS +
                " ev on evt.id = ev.event_id " +
                "left join " +
                EVENT_ORGANIZATIONS +
                " evo on evt.id = evo.event_id "
        );
    }

    @Override
    protected JdbcObjectMapper<Event> buildOrm() {
        return JdbcObjectMapper.builder(Event.class, this.tableName, "id")
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

    private void storeOrganizations(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + EVENT_ORGANIZATIONS + " where event_id = ?", event.getId());
        }
        if (!CollectionUtils.isEmpty(event.getOrganizations())) {
            List<String> list = new ArrayList<>(event.getOrganizations());
            jdbcTemplate.batchUpdate(
                "insert into " + EVENT_ORGANIZATIONS + " ( event_id, organization_id) values ( ?, ? )",
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

    private List<Event> queryEvents(String sql, List<Object> args) {
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

    @Override
    public void delete(final String id) throws TechnicalException {
        log.debug("JdbcLatestEventRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from " + EVENT_PROPERTIES + " where event_id = ?", id);
            jdbcTemplate.update("delete from " + EVENT_ENVIRONMENTS + " where event_id = ?", id);
            jdbcTemplate.update("delete from " + EVENT_ORGANIZATIONS + " where event_id = ?", id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            log.error("Failed to delete event by id [{}]", id);
            throw new TechnicalException("Failed to delete event", ex);
        }
    }

    @Override
    public List<Event> findByEnvironmentId(String environmentId) {
        log.debug("JdbcEventLatestRepository.findByEnvironmentId({})", environmentId);
        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = createSearchQueryBuilder();
        addStringsWhereClause(Set.of(environmentId), "ev.environment_id", args, builder, false);
        return queryEvents(builder.toString(), args);
    }

    @Override
    public List<Event> findByOrganizationId(String organizationId) {
        log.debug("JdbcEventLatestRepository.findByOrganizationId({})", organizationId);
        final List<Object> args = new ArrayList<>();
        final StringBuilder builder = createSearchQueryBuilder();
        addStringsWhereClause(Set.of(organizationId), "evo.organization_id", args, builder, false);
        return queryEvents(builder.toString(), args);
    }

    @Override
    public Event createOrUpdate(Event event) {
        if (event == null || event.getId() == null || event.getType() == null) {
            throw new IllegalStateException("Event to create or update must have an id and a type");
        }

        final int updatedEventCount = jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(event, event.getId()));
        // No event updated so new one will be created
        if (updatedEventCount <= 0) {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(event));
            storeProperties(event, false);
            storeEnvironments(event, false);
            storeOrganizations(event, false);
        } else {
            storeProperties(event, true);
            storeEnvironments(event, true);
            storeOrganizations(event, true);
        }
        return event;
    }

    protected static void appendCriteria(
        StringBuilder builder,
        EventCriteria filter,
        List<Object> args,
        String eventTableAlias,
        String eventEnvironmentTableAlias,
        String eventOrganizationTableAlias
    ) {
        var started = false;
        if (filter.getFrom() > 0) {
            builder.append(WHERE_CLAUSE);
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
            started = addStringsWhereClause(
                filter.getEnvironments(),
                eventEnvironmentTableAlias + ".environment_id",
                args,
                builder,
                started
            );
            builder.insert(builder.lastIndexOf(")"), " or " + eventEnvironmentTableAlias + ".environment_id IS NULL");
        }

        if (!isEmpty(filter.getOrganizations())) {
            started = addStringsWhereClause(
                filter.getOrganizations(),
                eventOrganizationTableAlias + ".organization_id",
                args,
                builder,
                started
            );
            builder.insert(builder.lastIndexOf(")"), " or " + eventOrganizationTableAlias + ".organization_id IS NULL");
        }

        if (!isEmpty(filter.getTypes())) {
            final Collection<String> types = filter.getTypes().stream().map(Enum::name).collect(toList());
            addStringsWhereClause(types, eventTableAlias + ".type", args, builder, started);
        }
    }
}
