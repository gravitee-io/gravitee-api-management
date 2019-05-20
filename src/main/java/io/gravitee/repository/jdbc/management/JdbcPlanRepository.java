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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static java.lang.String.format;

/**
 *
 * @author njt
 */
@Repository
public class JdbcPlanRepository implements PlanRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPlanRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Plan.class, "plans", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, Plan.PlanType.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("validation", Types.NVARCHAR, Plan.PlanValidationType.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("security_definition", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("status", Types.NVARCHAR, Plan.Status.class)
            .addColumn("security", Types.NVARCHAR, Plan.PlanSecurityType.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("published_at", Types.TIMESTAMP, Date.class)
            .addColumn("closed_at", Types.TIMESTAMP, Date.class)
            .addColumn("need_redeploy_at", Types.TIMESTAMP, Date.class)
            .addColumn("comment_required", Types.BOOLEAN, boolean.class)
            .addColumn("comment_message", Types.NVARCHAR, String.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Plan> CHILD_ADDER = (Plan parent, ResultSet rs) -> {
        Set<String> apis = parent.getApis();
        if (apis == null) {
            apis = new HashSet<>();
            parent.setApis(apis);
        }
        if (rs.getString("api") != null) {
            apis.add(rs.getString("api"));
        }
    };
    
    private void addApis(Plan parent) {
        List<String> apis = getApis(parent.getId());
        parent.setApis(new HashSet<>(apis));
    }

    private void addTags(Plan parent) {
        List<String> tags = getTags(parent.getId());
        parent.setTags(new HashSet<>(tags));
    }
    
    private void addCharacteristics(Plan parent) {
        List<String> characteristics = getCharacteristics(parent.getId());
        parent.setCharacteristics(characteristics);
    }
    
    private void addExcludedGroups(Plan parent) {        
        List<String> excludedGroups = getExcludedGroups(parent.getId());
        parent.setExcludedGroups(excludedGroups);
    }
        
    @Override
    public Optional<Plan> findById(String id) throws TechnicalException {
        
        LOGGER.debug("JdbcPlanRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Plan> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query("select * from plans p left join plan_apis pa on p.id = pa.plan_id where p.id = ?"
                    , rowMapper
                    , id
            );
            Optional<Plan> result = rowMapper.getRows().stream().findFirst();
            if (result.isPresent()) {
                addCharacteristics(result.get());
                addExcludedGroups(result.get());
                addTags(result.get());
            }
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find plan by id:", ex);
            throw new TechnicalException("Failed to find plan by id", ex);
        }
        
    }

    @Override
    public Plan create(Plan item) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeApis(item, false);
            storeCharacteristics(item, false);
            storeExcludedGroups(item, false);
            storeTags(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create plan", ex);
            throw new TechnicalException("Failed to create plan", ex);
        }
    }

    @Override
    public Plan update(final Plan plan) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.update({})", plan);
        if (plan == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(plan, plan.getId()));
            storeApis(plan, true);
            storeCharacteristics(plan, true);
            storeExcludedGroups(plan, true);
            storeTags(plan, true);
            return findById(plan.getId()).orElseThrow(() ->
                    new IllegalStateException(format("No plan found with id [%s]", plan.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update plan", ex);
            throw new TechnicalException("Failed to update plan", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from plan_apis where plan_id = ?", id);
            jdbcTemplate.update("delete from plan_tags where plan_id = ?", id);
            jdbcTemplate.update("delete from plan_characteristics where plan_id = ?", id);
            jdbcTemplate.update("delete from plan_excluded_groups where plan_id = ?", id);
            jdbcTemplate.update(ORM.getDeleteSql(), id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete plan:", ex);
            throw new TechnicalException("Failed to delete plan", ex);
        }
    }
    
    private List<String> getApis(String planId) {
        LOGGER.debug("JdbcPlanRepository.getApis({})", planId);
        return jdbcTemplate.queryForList("select api from plan_apis where plan_id = ?", String.class, planId);
    }

    private List<String> getTags(String planId) {
        LOGGER.debug("JdbcPlanRepository.getTags({})", planId);
        return jdbcTemplate.queryForList("select tag from plan_tags where plan_id = ?", String.class, planId);
    }
    
    private List<String> getCharacteristics(String planId) {
        LOGGER.debug("JdbcPlanRepository.getCharacteristics({})", planId);
        return jdbcTemplate.queryForList("select characteristic from plan_characteristics where plan_id = ?", String.class, planId);
    }
        
    private List<String> getExcludedGroups(String pageId) {
        return jdbcTemplate.query("select excluded_group from plan_excluded_groups where plan_id = ?"
                , (ResultSet rs, int rowNum) -> rs.getString(1)
                , pageId);
    }
    
    private void storeApis(Plan plan, boolean deleteFirst) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.storeApis({}, {})", plan, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from plan_apis where plan_id = ?", plan.getId());
            }
            List<String> filteredApis = ORM.filterStrings(plan.getApis());
            if (! filteredApis.isEmpty()) {
                jdbcTemplate.batchUpdate("insert into plan_apis ( plan_id, api ) values ( ?, ? )"
                        , ORM.getBatchStringSetter(plan.getId(), filteredApis));
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to store apis:", ex);
            throw new TechnicalException("Failed to store apis", ex);
        }
    }
    
    private void storeCharacteristics(Plan plan, boolean deleteFirst) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.storeApis({}, {})", plan, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from plan_characteristics where plan_id = ?", plan.getId());
            }
            List<String> filteredCharacteristics = ORM.filterStrings(plan.getCharacteristics());
            if (! filteredCharacteristics.isEmpty()) {
                jdbcTemplate.batchUpdate("insert into plan_characteristics ( plan_id, characteristic ) values ( ?, ? )"
                        , ORM.getBatchStringSetter(plan.getId(), filteredCharacteristics));
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to store characteristics:", ex);
            throw new TechnicalException("Failed to store characteristics", ex);
        }
    }
    
    private void storeExcludedGroups(Plan plan, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from plan_excluded_groups where plan_id = ?", plan.getId());
        }
        if ((plan.getExcludedGroups() != null) && !plan.getExcludedGroups().isEmpty()) {
            List<String> excludedGroups = plan.getExcludedGroups();
            jdbcTemplate.batchUpdate("insert into plan_excluded_groups ( plan_id, excluded_group ) values ( ?, ? )"
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

    private void storeTags(Plan plan, boolean deleteFirst) throws TechnicalException {
        LOGGER.debug("JdbcPlanRepository.storeTags({}, {})", plan, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from plan_tags where plan_id = ?", plan.getId());
            }
            List<String> filteredTags = ORM.filterStrings(plan.getTags());
            if (! filteredTags.isEmpty()) {
                jdbcTemplate.batchUpdate("insert into plan_tags ( plan_id, tag ) values ( ?, ? )"
                        , ORM.getBatchStringSetter(plan.getId(), filteredTags));
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to store tags:", ex);
            throw new TechnicalException("Failed to store tags", ex);
        }
    }

    @Override
    public Set<Plan> findByApi(String apiId) throws TechnicalException {

        LOGGER.debug("JdbcPlanRepository.findByApi({})", apiId);
        try {
            JdbcHelper.CollatingRowMapper<Plan> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query("select p.*, pa.* from plans p "
                    + " left join plan_apis pa on p.id = pa.plan_id "
                    + " left join plan_apis pa2 on p.id = pa2.plan_id "
                    + " where pa2.api = ?"
                    , rowMapper
                    , apiId
            );
            List<Plan> plans = rowMapper.getRows();
            for (Plan plan : plans) {
                addCharacteristics(plan);
                addExcludedGroups(plan);
                addApis(plan);
                addTags(plan);
            }
            return new HashSet<>(plans);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find plans by api:", ex);
            throw new TechnicalException("Failed to find plans by api", ex);
        }
    }
}