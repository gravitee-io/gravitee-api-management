/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcEventRepository implements EventRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcEventRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Event.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Type", Types.NVARCHAR, EventType.class)
            .addColumn("Payload", Types.NVARCHAR, String.class)
            .addColumn("ParentId", Types.NVARCHAR, String.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build();    

    private static final JdbcHelper.ChildAdder<Event> CHILD_ADDER = (Event parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("PropertyName") != null) {
            properties.put(rs.getString("PropertyName"), rs.getString("PropertyValue"));        
        }
    };
    
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcEventRepository(DataSource dataSource) {
        logger.debug("JdbcEventRespository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    private void storeProperties(Event event, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from EventProperty where EventId = ?", event.getId());
        }
        if (event.getProperties() != null) {
            List<Entry<String, String>> list = new ArrayList<>(event.getProperties().entrySet());
            jdbcTemplate.batchUpdate("insert into EventProperty ( EventId, PropertyName, PropertyValue ) values ( ?, ?, ? )"
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

        logger.debug("JdbcEventRepository.findById({})", id);
        
        try {
            JdbcHelper.CollatingRowMapper<Event> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Event e left join EventProperty ep on e.Id = ep.EventId where e.Id = ?"
                    , rowMapper
                    , id
            );            
            return rowMapper.getRows().stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find event by id:", ex);
            throw new TechnicalException("Failed to find event by id", ex);
        }
        
    }
    
    @Override
    public Event create(Event event) throws TechnicalException {
        
        logger.debug("JdbcEventRepository.create({})", event);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(event));
            storeProperties(event, false);
            return findById(event.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create event:", ex);
            throw new TechnicalException("Failed to create event", ex);
        }

    }

    @Override
    public Event update(Event event) throws TechnicalException {
        logger.debug("JdbcEventRepository.update({})", event);
        if (event == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(event, event.getId()));
            storeProperties(event, true);
            return findById(event.getId()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update event:", ex);
            throw new TechnicalException("Failed to update event", ex);
        }

    }
    
    @Override
    public void delete(String id) throws TechnicalException {
        
        logger.debug("JdbcEventRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from EventProperty where EventId = ?", id);
            jdbcTemplate.update(ORM.getDeleteSql(), id);
        } catch (Throwable ex) {
            logger.error("Failed to delete event:", ex);
            throw new TechnicalException("Failed to delete event", ex);
        }
        
    }
    
    
    
    @Override
    public Page<Event> search(EventCriteria filter, Pageable page) {
        logger.debug("JdbcEventRespository.search({}, {})", criteriaToString(filter), page);

        List<Event> events = search(filter);

        if (page != null) {
            int start = page.from();
            if ((start == 0) && (page.pageNumber() > 0)) {
                start = page.pageNumber() * page.pageSize();
            }
            int rows = page.pageSize();
            if ((rows == 0) && (page.to() > 0)) {
                rows = page.to() - start;
            }
            if (start + rows > events.size()) {
                rows = events.size() - start;
            }

            if (rows > 0) {
                return new Page(events.subList(start, start + rows), start / page.pageSize(), rows, events.size());
            } else {
                return new Page(Collections.EMPTY_LIST, 0, 0, events.size());
            }
        }
        return new Page(events, 0, events.size(), events.size());
    }

    @Override
    public List<Event> search(EventCriteria filter) {
        logger.debug("JdbcEventRespository.search({})", criteriaToString(filter));
                
        List<Object> args = new ArrayList<>();
        StringBuilder builder = new StringBuilder("select e.*, ep.* from Event e left join EventProperty ep on e.Id = ep.EventId ");
        boolean started = false;
        if ((filter.getProperties() != null) && !filter.getProperties().isEmpty()) {
            builder.append(" left join EventProperty ep2 on ep2.EventId = e.Id ");
            builder.append(started ? " and " : " where ");
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
            started = true;
        }
        if (filter.getFrom() > 0) {
            builder.append(started ? " and " : " where ");
            builder.append("UpdatedAt >= ?");
            args.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? " and " : " where ");
            builder.append("UpdatedAt < ?");
            args.add(new Date(filter.getTo()));
            started = true;
        }
        if ((filter.getTypes() != null) && !filter.getTypes().isEmpty()) {
            builder.append(started ? " and " : " where ");
            builder.append("Type in (");
            boolean first = true;
            for (EventType type : filter.getTypes()) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append("?");
                args.add(type.name());
            }
            builder.append(")");
            started = true;
        }
        
        builder.append(" order by UpdatedAt desc ");
        
        String sql = builder.toString();
        
        logger.debug("SQL: {}", sql);
        logger.debug("Args: {}", args);
        
        JdbcHelper.CollatingRowMapper<Event> rowCallbackHandler 
                = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
        jdbcTemplate.query((Connection cnctn) -> {
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = 1;
            for (Object arg : args) {
                stmt.setObject(idx++, arg);
            }
            return stmt;
        }, rowCallbackHandler);
        List<Event> events = rowCallbackHandler.getRows();
        
        logger.debug("Events found: {}", events);
        return events;
    }

    private boolean addCondition(boolean first, StringBuilder builder, String propName, Object propVal, List<Object> args) {
        if (!first) {
            builder.append(" or ");
        }
        first = false;
        if (propVal == null) {
            builder.append(" ( ep2.PropertyName = ? and ep2.PropertyValue is null ) ");
            args.add(propName);
        } else {
            builder.append(" ( ep2.PropertyName = ? and ep2.PropertyValue = ? ) ");
            args.add(propName);
            args.add(propVal);
        }
        return first;
    }
    
    private String criteriaToString(EventCriteria filter) {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ").append("from: ").append(filter.getFrom());
        builder.append(", ").append("props: ").append(filter.getProperties());
        builder.append(", ").append("to: ").append(filter.getTo());
        builder.append(", ").append("types: ").append(filter.getTypes());
        builder.append(" }");
        return builder.toString();
    }
    
}
