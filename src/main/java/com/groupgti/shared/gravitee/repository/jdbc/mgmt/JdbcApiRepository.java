/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
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
public class JdbcApiRepository implements ApiRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcApiRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Api.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .addColumn("Version", Types.NVARCHAR, String.class)
            .addColumn("Definition", Types.NVARCHAR, String.class)
            .addColumn("DeployedAt", Types.TIMESTAMP, Date.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Visibility", Types.NVARCHAR, Visibility.class)
            .addColumn("LifecycleState", Types.NVARCHAR, LifecycleState.class)
            .addColumn("Picture", Types.NVARCHAR, String.class)
            // .addColumn("Group", Types.NVARCHAR, String.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Api> CHILD_ADDER = (Api parent, ResultSet rs) -> {
        Set<String> views = parent.getViews();
        if (views == null) {
            views = new HashSet<>();
            parent.setViews(views);
        }
        if (rs.getString("View") != null) {
            views.add(rs.getString("View"));
        }
    };
    
    private void addLabels(Api parent) {        
        List<String> labels = getLabels(parent.getId());
        parent.setLabels(labels);
    }
    
    private void addGroups(Api parent) {        
        List<String> groups = getGroups(parent.getId());
        parent.setGroups(new HashSet<>(groups));
    }
    
    @Autowired
    public JdbcApiRepository(DataSource dataSource) {
        logger.debug("JdbcApiRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<Api> findById(String id) throws TechnicalException {

        logger.debug("JdbcApiRepository.findById({})", id);
        
        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Api a left join ApiView av on a.Id = av.ApiId where a.Id = ?"
                    , rowMapper
                    , id
            );
            Optional<Api> result = rowMapper.getRows().stream().findFirst();
            if (result.isPresent()) {
                addLabels(result.get());
                addGroups(result.get());
            }
            logger.debug("JdbcApiRepository.findById({}) = {}", id, result);
            return result;
        } catch (Throwable ex) {
            logger.error("Failed to find api by id:", ex);
            throw new TechnicalException("Failed to find api by id", ex);
        }
        
    }

    @Override
    public Api create(Api item) throws TechnicalException {
        logger.debug("JdbcApiRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeLabels(item, false);
            storeGroups(item, false);
            storeViews(item, false);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create api:", ex);
            throw new TechnicalException("Failed to create api", ex);
        }
    }

    @Override
    public Api update(Api item) throws TechnicalException {
        logger.debug("JdbcApiRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeLabels(item, true);
            storeGroups(item, true);
            storeViews(item, true);
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
        jdbcTemplate.update("delete from ApiLabel where ApiId = ?", id);
        jdbcTemplate.update("delete from ApiView where ApiId = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }
    
    private List<String> getLabels(String apiId) {
        return jdbcTemplate.queryForList("select Label from ApiLabel where ApiId = ?", String.class, apiId);
    }
    
    private void storeLabels(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from ApiLabel where ApiId = ?", api.getId());
        }
        List<String> filteredLabels = ORM.filterStrings(api.getLabels());
        if (! filteredLabels.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into ApiLabel ( ApiId, Label ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredLabels));
        }
    }
    
    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select GroupId from ApiGroup where ApiId = ?", String.class, apiId);
    }
    
    private void storeGroups(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from ApiGroup where ApiId = ?", api.getId());
        }
        List<String> filteredGroups = ORM.filterStrings(api.getGroups());
        if (! filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into ApiGroup ( ApiId, GroupId ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredGroups));
        }
    }
    
    private void storeViews(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from ApiView where ApiId = ?", api.getId());
        }
        List<String> filteredViews = ORM.filterStrings(api.getViews());
        if (! filteredViews.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into ApiView ( ApiId, View ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredViews));
        }
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {

        logger.debug("JdbcApiRepository.findAll()");

        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Api a left join ApiView av on a.Id = av.ApiId"
                    , rowMapper
            );
            List<Api> apis = rowMapper.getRows();
            for (Api api : apis) {
                addLabels(api);
                addGroups(api);
            }
            return new HashSet<>(apis);
        } catch (Throwable ex) {
            logger.error("Failed to find all users:", ex);
            throw new TechnicalException("Failed to find all users", ex);
        }
        
    }

    @Override
    public Set<Api> findByVisibility(Visibility visibility) throws TechnicalException {

        logger.debug("JdbcApiRepository.findByVisibility({})", visibility);
        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Api a left join ApiView av on a.Id = av.ApiId where a.Visibility = ?"
                    , rowMapper
                    , visibility
            );
            List<Api> apis = rowMapper.getRows();
            for (Api api : apis) {
                addLabels(api);
                addGroups(api);
            }
            return new HashSet<>(apis);
        } catch (Throwable ex) {
            logger.error("Failed to find apis by visibility:", ex);
            throw new TechnicalException("Failed to find apis by visibility", ex);
        }
    }

    @Override
    public Set<Api> findByIds(List<String> ids) throws TechnicalException {
        
        logger.debug("JdbcApiRepository.findByIds({})", ids);

        if ((ids == null) || ids.isEmpty()) {
            return new HashSet<>();
        }
        
        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Api a left join ApiView av on a.Id = av.ApiId where a.Id in ("
                    + ORM.buildInClause(ids) + " )"
                    , (PreparedStatement ps) -> { ORM.setArguments(ps, ids, 1); }
                    , rowMapper
            );
            List<Api> apis = rowMapper.getRows();
            for (Api api : apis) {
                addLabels(api);
                addGroups(api);
            }
            return new HashSet<>(apis);
        } catch (Throwable ex) {
            logger.error("Failed to find api by ids:", ex);
            throw new TechnicalException("Failed to find api by ids", ex);
        }
    }

    @Override
    public Set<Api> findByGroups(List<String> groupIds) throws TechnicalException {
        
        logger.debug("JdbcApiRepository.findByGroups({})", groupIds);

        if ((groupIds == null) || groupIds.isEmpty()) {
            return new HashSet<>();
        }

        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select distinct a.*, av.* from Api a "
                    + "join ApiGroup ag on a.Id = ag.ApiId "
                    + "left join ApiView av on a.Id = av.ApiId "
                    + "where ag.GroupId in ("
                    + ORM.buildInClause(groupIds) + " )"
                    , (PreparedStatement ps) -> { ORM.setArguments(ps, groupIds, 1); }
                    , rowMapper
            );
            List<Api> apis = rowMapper.getRows();
            for (Api api : apis) {
                addLabels(api);
                addGroups(api);
            }
            return new HashSet<>(apis);
        } catch (Throwable ex) {
            logger.error("Failed to find api by groups:", ex);
            throw new TechnicalException("Failed to find api by groups", ex);
        }
    }
    
}
