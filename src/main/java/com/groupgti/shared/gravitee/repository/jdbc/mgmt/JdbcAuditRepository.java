/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcAuditRepository implements AuditRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcAuditRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Audit.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("ReferenceId", Types.NVARCHAR, String.class)
            .addColumn("ReferenceType", Types.NVARCHAR, Audit.AuditReferenceType.class)
            .addColumn("Username", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Event", Types.NVARCHAR, String.class)
            .addColumn("Patch", Types.NVARCHAR, String.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Audit> CHILD_ADDER = (Audit parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("Key") != null) {
            properties.put(rs.getString("Key"), rs.getString("Value"));
        }
    };
    
    private void addProperties(Audit parent) {        
        Map<String, String> properties = getProperties(parent.getId());
        parent.setProperties(properties);
    }
    
    
    @Autowired
    public JdbcAuditRepository(DataSource dataSource) {
        logger.debug("JdbcAuditRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {

        logger.debug("JdbcAuditRepository.findById({})", id);
        
        try {
            JdbcHelper.CollatingRowMapper<Audit> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Audit a left join AuditProperty ap on a.Id = ap.AuditId where a.Id = ?"
                    , rowMapper
                    , id
            );
            Optional<Audit> result = rowMapper.getRows().stream().findFirst();
            logger.debug("JdbcAuditRepository.findById({}) = {}", id, result);
            return result;
        } catch (Throwable ex) {
            logger.error("Failed to find audit by id:", ex);
            throw new TechnicalException("Failed to find audit by id", ex);
        }
        
    }

    @Override
    public Audit create(Audit item) throws TechnicalException {
        logger.debug("JdbcAuditRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeProperties(item, false);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create audit:", ex);
            throw new TechnicalException("Failed to create audit", ex);
        }
    }

    @Override
    public Audit update(Audit item) throws TechnicalException {
        logger.debug("JdbcAuditRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeProperties(item, true);
            return findById(item.getId()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update audit:", ex);
            throw new TechnicalException("Failed to update audit", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from AuditProperty where AuditId = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }
    
    private Map<String, String> getProperties(String auditId) {
        Map<String, String> properties = new HashMap<>();
        jdbcTemplate.query("select `Key`, `Value` from AuditProperty where AuditId = ?"
                , (ResultSet rs) -> { properties.put(rs.getString(1), rs.getString(2)); }
                , auditId
        );
        return properties;
    }
    
    private void storeProperties(Audit audit, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from AuditProperty where AuditId = ?", audit.getId());
        }
        if (! audit.getProperties().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(audit.getProperties().entrySet());
            jdbcTemplate.batchUpdate("insert into AuditProperty ( AuditId, `Key`, `Value` ) values ( ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, audit.getId());
                    ps.setString(2, entries.get(i).getKey());
                    ps.setString(3, entries.get(i).getValue());
                }

                @Override
                public int getBatchSize() {
                    return entries.size();
                }
            });
        }
    }
    
    private String criteriaToString(AuditCriteria filter) {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ").append("from: ").append(filter.getFrom());
        builder.append(", ").append("to: ").append(filter.getTo());
        builder.append(", ").append("references: ").append(filter.getReferences());
        builder.append(", ").append("props: ").append(filter.getProperties());
        builder.append(", ").append("events: ").append(filter.getEvents());
        builder.append(" }");
        return builder.toString();
    }

    private boolean addPropertyCondition(boolean first, StringBuilder builder, String propName, Object propVal, List<Object> args) {
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
    
    @Override
    public Page<Audit> search(AuditCriteria filter, Pageable page) {
        logger.debug("JdbcEventRespository.search({}, {})", criteriaToString(filter), page   );

        List<Object> argsList = new ArrayList<>();
        
        List<Map<String,Object>> all = jdbcTemplate.queryForList("select * from Audit a left join AuditProperty ap on a.Id = ap.AuditId");
        logger.debug("There are {} audit entries in the DB:", all.size());
        for (Map<String,Object> row : all) {
            logger.debug("\t{}", row);
        }
        
        StringBuilder builder = new StringBuilder("select * from Audit a left join AuditProperty ap on a.Id = ap.AuditId ");
        boolean started = false;
        if ((filter.getProperties() != null) && !filter.getProperties().isEmpty()) {
            builder.append(" left join AuditProperty ap2 on ap2.AuditId = a.Id ");
            builder.append(started ? " and " : " where ");
            builder.append("(");
            boolean first = true;
            for (Entry<String, String> property : filter.getProperties().entrySet()) {
                first = addPropertyCondition(first, builder, property.getKey(), property.getValue(), argsList);
            }
            builder.append(")");
            started = true;
        }
        if (filter.getFrom() > 0) {
            builder.append(started ? " and " : " where ");
            builder.append("CreatedAt >= ?");
            argsList.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? " and " : " where ");
            builder.append("CreatedAt <= ?");
            argsList.add(new Date(filter.getTo()));
            started = true;
        }
        if ((filter.getReferences() != null) && !filter.getReferences().isEmpty()) {
            logger.debug("filter.getReferences() = {}", filter.getReferences());
            logger.debug("argsList = {}", argsList);
            builder.append(started ? " and " : " where ");
            builder.append("(");
            for (Entry<Audit.AuditReferenceType, List<String>> ref : filter.getReferences().entrySet()) {
                builder.append("( ReferenceType = ? and ReferenceId in (");
                argsList.add(ref.getKey().toString());
                logger.debug("argsList after ref type = {}", argsList);
                boolean first = true;
                for (String id : ref.getValue()) {
                    if (!first) {
                        builder.append(", ");
                    }
                    first = false;
                    builder.append("?");
                    argsList.add(id);
                    logger.debug("argsList after ref id = {}", argsList);
                }
                builder.append(") )");
                started = true;
            }
            builder.append(") ");
            logger.debug("argsList = {}", argsList);
        }
        if ((filter.getEvents() != null) && !filter.getEvents().isEmpty()) {
            builder.append(started ? " and " : " where ");
            builder.append("( Event in (");
            boolean first = true;
            for (String event : filter.getEvents()) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append("?");
                argsList.add(event);
            }
            builder.append(") )");
            started = true;
        }
        
        builder.append(" order by CreatedAt desc ");
        
        String sql = builder.toString();
        
        logger.debug("argsList = {}", argsList);
        Object[] args = argsList.toArray();
        logger.debug("SQL: {}", sql);
        logger.debug("Args ({}): {}", args.length, args);
        for (int i = 0; i < args.length; ++i) {
            logger.debug("args[{}] = {} {}", i, args[i], args[i].getClass());
        }
        
        List<Audit> audits;
        try {
            JdbcHelper.CollatingRowMapper<Audit> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query(sql
                    , rowMapper
                    , args
            );
            audits = rowMapper.getRows();
        } catch (Throwable ex) {
            logger.error("Failed to find audit records:", ex);
            throw new IllegalStateException("Failed to find audit records", ex);
        }
        
        logger.debug("Audit records found ({}): {}", audits.size(), audits);

        if (page != null) {
            int start = page.from();
            if ((start == 0) && (page.pageNumber() > 0)) {
                start = page.pageNumber() * page.pageSize();
            }
            int rows = page.pageSize();
            if ((rows == 0) && (page.to() > 0)) {
                rows = page.to() - start;
            }
            if (start + rows > audits.size()) {
                rows = audits.size() - start;
            }

            if (rows > 0) {
                return new Page(audits.subList(start, start + rows), start / page.pageSize(), rows, audits.size());
            } else {
                return new Page(Collections.EMPTY_LIST, 0, 0, audits.size());
            }
        }
        return new Page(audits, 0, audits.size(), audits.size());
    }
    
    
}
