/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcPlanRepository implements PlanRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcPlanRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Plan.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Type", Types.NVARCHAR, Plan.PlanType.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .addColumn("Validation", Types.NVARCHAR, Plan.PlanValidationType.class)
            .addColumn("Definition", Types.NVARCHAR, String.class)
            .addColumn("Order", Types.INTEGER, int.class)
            .addColumn("Status", Types.NVARCHAR, Plan.Status.class)
            .addColumn("Security", Types.NVARCHAR, Plan.PlanSecurityType.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("PublishedAt", Types.TIMESTAMP, Date.class)
            .addColumn("ClosedAt", Types.TIMESTAMP, Date.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Plan> CHILD_ADDER = (Plan parent, ResultSet rs) -> {
        Set<String> apis = parent.getApis();
        if (apis == null) {
            apis = new HashSet<>();
            parent.setApis(apis);
        }
        if (rs.getString("Api") != null) {
            apis.add(rs.getString("Api"));
        }
    };
    
    
    private void addApis(Plan parent) {
        List<String> apis = getApis(parent.getId());
        parent.setApis(new HashSet<>(apis));
    }
    
    private void addCharacteristics(Plan parent) {
        List<String> characteristics = getCharacteristics(parent.getId());
        parent.setCharacteristics(characteristics);
    }

    
    private void addExcludedGroups(Plan parent) {        
        List<String> excludedGroups = getExcludedGroups(parent.getId());
        parent.setExcludedGroups(excludedGroups);
    }
        
    
    @Autowired
    public JdbcPlanRepository(DataSource dataSource) {
        logger.debug("JdbcPlanRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<Plan> findById(String id) throws TechnicalException {
        
        logger.debug("JdbcPlanRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Plan> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from Plan p left join PlanApi pa on p.Id = pa.PlanId where p.Id = ?"
                    , rowMapper
                    , id
            );
            Optional<Plan> result = rowMapper.getRows().stream().findFirst();
            if (result.isPresent()) {
                addCharacteristics(result.get());
                addExcludedGroups(result.get());
            }
            return result;
        } catch (Throwable ex) {
            logger.error("Failed to find plan by id:", ex);
            throw new TechnicalException("Failed to find plan by id", ex);
        }
        
    }

    @Override
    public Plan create(Plan item) throws TechnicalException {
        logger.debug("JdbcPlanRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeApis(item, false);
            storeCharacteristics(item, false);
            storeExcludedGroups(item, false);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create plan:", ex);
            throw new TechnicalException("Failed to create plan", ex);
        }
    }

    @Override
    public Plan update(Plan item) throws TechnicalException {
        logger.debug("JdbcPlanRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeApis(item, true);
            storeCharacteristics(item, true);
            storeExcludedGroups(item, true);
            return findById(item.getId()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update plan:", ex);
            throw new TechnicalException("Failed to update plan", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("JdbcPlanRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from PlanApi where PlanId = ?", id);
            jdbcTemplate.update("delete from PlanCharacteristic where PlanId = ?", id);
            jdbcTemplate.update("delete from PlanExcludedGroup where PlanId = ?", id);
            jdbcTemplate.update(ORM.getDeleteSql(), id);
        } catch (Throwable ex) {
            logger.error("Failed to delete plan:", ex);
            throw new TechnicalException("Failed to delete plan", ex);
        }
    }
    
    private List<String> getApis(String planId) {
        logger.debug("JdbcPlanRepository.getApis({})", planId);
        return jdbcTemplate.queryForList("select Api from PlanApi where PlanId = ?", String.class, planId);
    }
    
    private List<String> getCharacteristics(String planId) {
        logger.debug("JdbcPlanRepository.getCharacteristics({})", planId);
        return jdbcTemplate.queryForList("select Characteristic from PlanCharacteristic where PlanId = ?", String.class, planId);
    }
        
    private List<String> getExcludedGroups(String pageId) {
        List<String> excludedGroups = jdbcTemplate.query("select ExcludedGroup from PlanExcludedGroup where PlanId = ?"
                , (ResultSet rs, int rowNum) -> rs.getString(1)
                , pageId);
        return excludedGroups;
    }
    
    private void storeApis(Plan plan, boolean deleteFirst) throws TechnicalException {
        logger.debug("JdbcPlanRepository.storeApis({}, {})", plan, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from PlanApi where PlanId = ?", plan.getId());
            }
            List<String> filteredApis = ORM.filterStrings(plan.getApis());
            if (! filteredApis.isEmpty()) {
                jdbcTemplate.batchUpdate("insert into PlanApi ( PlanId, Api ) values ( ?, ? )"
                        , ORM.getBatchStringSetter(plan.getId(), filteredApis));
            }
        } catch (Throwable ex) {
            logger.error("Failed to store apis:", ex);
            throw new TechnicalException("Failed to store apis", ex);
        }
    }
    
    private void storeCharacteristics(Plan plan, boolean deleteFirst) throws TechnicalException {
        logger.debug("JdbcPlanRepository.storeApis({}, {})", plan, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from PlanCharacteristic where PlanId = ?", plan.getId());
            }
            List<String> filteredCharacteristics = ORM.filterStrings(plan.getCharacteristics());
            if (! filteredCharacteristics.isEmpty()) {
                jdbcTemplate.batchUpdate("insert into PlanCharacteristic ( PlanId, Characteristic ) values ( ?, ? )"
                        , ORM.getBatchStringSetter(plan.getId(), filteredCharacteristics));
            }
        } catch (Throwable ex) {
            logger.error("Failed to store characteristics:", ex);
            throw new TechnicalException("Failed to store characteristics", ex);
        }
    }
    
    private void storeExcludedGroups(Plan plan, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from PlanExcludedGroup where PlanId = ?", plan.getId());
        }
        if ((plan.getExcludedGroups() != null) && !plan.getExcludedGroups().isEmpty()) {
            List<String> excludedGroups = plan.getExcludedGroups();
            jdbcTemplate.batchUpdate("insert into PlanExcludedGroup ( PlanId, ExcludedGroup ) values ( ?, ? )"
                    , new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, plan.getId());
                    ps.setString(2, excludedGroups.get(i));
                }

                @Override
                public int getBatchSize() {
                    return excludedGroups.size();
                }
            });
        }
    }    

    @Override
    public Set<Plan> findByApi(String apiId) throws TechnicalException {

        logger.debug("JdbcPlanRepository.findByApi({})", apiId);
        try {
            JdbcHelper.CollatingRowMapper<Plan> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select distinct p.*, pa.* from Plan p "
                    + " left join PlanApi pa on p.Id = pa.PlanId "
                    + " left join PlanApi pa2 on p.Id = pa2.PlanId "
                    + " where pa2.Api = ?"
                    , rowMapper
                    , apiId
            );
            List<Plan> plans = rowMapper.getRows();
            for (Plan plan : plans) {
                addCharacteristics(plan);
                addExcludedGroups(plan);
                addApis(plan);
            }
            return new HashSet<>(plans);
        } catch (Throwable ex) {
            logger.error("Failed to find plans by api:", ex);
            throw new TechnicalException("Failed to find plans by api", ex);
        }
    }
}
