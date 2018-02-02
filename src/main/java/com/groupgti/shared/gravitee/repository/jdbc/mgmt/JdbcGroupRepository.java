/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.GroupEventRule;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcGroupRepository implements GroupRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcGroupRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Group.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Group> CHILD_ADDER = (Group parent, ResultSet rs) -> {
        if (parent.getAdministrators() == null) {
            parent.setAdministrators(new ArrayList<>());
        }
        if (rs.getString("Administrator") != null) {
            parent.getAdministrators().add(rs.getString("Administrator"));
        }
    };
    
    @Autowired
    public JdbcGroupRepository(DataSource dataSource) {
        logger.debug("JdbcGroupRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<Group> findById(String id) throws TechnicalException {

        logger.debug("JdbcGroupRepository.findById({})", id);
        
        try {
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId where Id = ?"
                    , rowMapper
                    , id
            );
            Optional<Group> group = rowMapper.getRows().stream().findFirst();
            if (group.isPresent()) {
                addGroupEvents(group.get());
            }
            return group;
        } catch (Throwable ex) {
            logger.error("Failed to find group by id:", ex);
            throw new TechnicalException("Failed to find group by id", ex);
        }
        
    }

    @Override
    public Group create(Group item) throws TechnicalException {
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeAdministrators(item, false);
            storeGroupEvents(item, false);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create api:", ex);
            throw new TechnicalException("Failed to create api", ex);
        }
    }

    @Override
    public Group update(Group item) throws TechnicalException {
        if (item == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeAdministrators(item, true);
            storeGroupEvents(item, true);
            return findById(item.getId()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update api:", ex);
            throw new TechnicalException("Failed to update api", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from GroupAdministrator where GroupId = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }

    private void addGroupEvents(Group parent) {        
        List<GroupEventRule> groupEvents = getEvents(parent.getId());
        parent.setEventRules(groupEvents);
    }
    
    private List<GroupEventRule> getEvents(String groupId) {
        List<GroupEvent> groupEvents = jdbcTemplate.query("select GroupEvent from GroupEventRule where GroupId = ?", (ResultSet rs, int rowNum) -> {
            String value = rs.getString(1);
            try {
                return GroupEvent.valueOf(value);
            } catch(IllegalArgumentException ex) {
                logger.error("Failed to parse {} as GroupEvent:", value, ex);
                return null;
            }
        }, groupId);
        
        List<GroupEventRule> groupEventRules = new ArrayList<>(groupEvents.size());
        for (GroupEvent groupEvent : groupEvents) {
            if (groupEvent != null) {
                groupEventRules.add(new GroupEventRule(groupEvent));
            }
        }
        
        return groupEventRules;
    }
    
    private void storeGroupEvents(Group group, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from GroupEventRule where GroupId = ?", group.getId());
        }
        List<String> events = new ArrayList<>();
        if (group.getEventRules() != null) {
            for (GroupEventRule groupEventRule : group.getEventRules()) {
                events.add(groupEventRule.getEvent().name());
            }
        }
        if (! events.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into GroupEventRule ( GroupId, GroupEvent ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(group.getId(), events));
        }
    }
        
    private void storeAdministrators(Group group, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from GroupAdministrator where GroupId = ?", group.getId());
        }
        List<String> filteredAdministrators = ORM.filterStrings(group.getAdministrators());
        logger.debug("Storing administrators ({}) for {}", filteredAdministrators, group.getId());
        if (! filteredAdministrators.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into GroupAdministrator ( GroupId, Administrator ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(group.getId(), filteredAdministrators));
        }
        if (group.getAdministrators() == null) {
            group.setAdministrators(new ArrayList<>());
        }
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {

        logger.debug("JdbcGroupRepository.findAll()");

        try {
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId "
                    , rowMapper
            );
            Set<Group> groups = new HashSet<>();
            for (Group group : rowMapper.getRows()) {
                addGroupEvents(group);
                groups.add(group);
            }
            return groups;
        } catch (Throwable ex) {
            logger.error("Failed to find all groups:", ex);
            throw new TechnicalException("Failed to find all groups", ex);
        }
        
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) throws TechnicalException {

        logger.debug("JdbcGroupRepository.findByIds({})", ids);
        
        StringBuilder query = new StringBuilder("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId ");

        ORM.buildInCondition(true, query, "Id", ids);
        
        try {            
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        ORM.setArguments(ps, ids, 1); 
                    }
                    , rowMapper
            );
            Set<Group> groups = new HashSet<>();
            for (Group group : rowMapper.getRows()) {
                addGroupEvents(group);
                groups.add(group);
            }
            return groups;
        } catch (Throwable ex) {
            logger.error("Failed to find group by id:", ex);
            throw new TechnicalException("Failed to find group by id", ex);
        }
        
    }

    
}
