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
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApplicationRepository extends JdbcAbstractCrudRepository<Application, String> implements ApplicationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApplicationRepository.class);

    private static final String STATUS_FIELD = "status";

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Application.class, "applications", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn(STATUS_FIELD, Types.NVARCHAR, ApplicationStatus.class)
            .addColumn("client_id", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Application item) {
        return item.getId();
    }
    
    private void addGroups(Application parent) {        
        List<String> groups = getGroups(parent.getId());
        parent.setGroups(new HashSet<>(groups));
    }
    
    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select group_id from application_groups where application_id = ?", String.class, apiId);
    }
    
    private void storeGroups(Application application, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from application_groups where application_id = ?", application.getId());
        }
        List<String> filteredGroups = ORM.filterStrings(application.getGroups());
        if (! filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into application_groups ( application_id, group_id ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(application.getId(), filteredGroups));
        }
    }


    @Override
    public Application create(Application item) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeGroups(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create application", ex);
            throw new TechnicalException("Failed to create application", ex);
        }
    }

    @Override
    public Application update(final Application application) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.update({})", application);
        if (application == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(application, application.getId()));
            storeGroups(application, true);
            return findById(application.getId()).orElseThrow(() -> new IllegalStateException(format("No application found with id [%s]", application.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update application", ex);
            throw new TechnicalException("Failed to update application", ex);
        }
    }

    @Override
    public Optional<Application> findById(String id) throws TechnicalException {
        final Optional<Application> result = super.findById(id);
        result.ifPresent(this::addGroups);
        return result;
    }

    @Override
    public Set<Application> findByIds(List<String> ids) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByIds({})", ids);
        try {
            if (isEmpty(ids)) {
                return emptySet();
            }
            List<Application> applications = jdbcTemplate.query("select * from applications where id in ( "
                    + ORM.buildInClause(ids) + " )"
                    , (PreparedStatement ps) -> ORM.setArguments(ps, ids, 1)
                    , ORM.getRowMapper()
            );
            for (Application application : applications) {
                addGroups(application);
            }
            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by ids:", ex);
            throw new TechnicalException("Failed to find  applications by ids", ex);
        }
    }

    @Override
    public Set<Application> findAll(ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findAll({})", (Object[])ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);
            
            StringBuilder query = new StringBuilder("select * from applications ");
            boolean first = true;
            ORM.buildInCondition(first, query, STATUS_FIELD, statuses);
            
            List<Application> applications = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> ORM.setArguments(ps, statuses, 1)
                    , ORM.getRowMapper()
            );
            for (Application application : applications) {
                addGroups(application);
            }
            LOGGER.debug("Found {} applications: {}", applications.size(), applications);
            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications:", ex);
            throw new TechnicalException("Failed to find applications", ex);
        }
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByGroups({}, {})", groupIds, ass);
        try {
            final List<ApplicationStatus> statuses = Arrays.asList(ass);
            final StringBuilder query = new StringBuilder("select a.* from applications a join application_groups ag on ag.application_id = a.id ");
            boolean first = true;
            first = ORM.buildInCondition(first, query, "group_id", groupIds);
            ORM.buildInCondition(first, query, STATUS_FIELD, statuses);
            List<Application> applications = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = ORM.setArguments(ps, groupIds, 1);
                        ORM.setArguments(ps, statuses, idx);
                    }
                    , ORM.getRowMapper()
            );
            for (Application application : applications) {
                addGroups(application);
            }
            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by groups", ex);
            throw new TechnicalException("Failed to find applications by groups", ex);
        }
    }

    @Override
    public Set<Application> findByName(String partialName) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByName({})", partialName);
        try {
            List<Application> applications = jdbcTemplate.query("select * from applications where lower(name) like ?"
                    , ORM.getRowMapper(), "%" + partialName.toLowerCase() + "%"
            );
            for (Application application : applications) {
                addGroups(application);
            }
            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by name", ex);
            throw new TechnicalException("Failed to find applications by name", ex);
        }
    }

    @Override
    public Optional<Application> findByClientId(final String clientId) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByClientId({})", clientId);
        try {
            final List applications = jdbcTemplate.query("select * from applications where client_id = ?"
                    , ORM.getRowMapper()
                    , clientId
            );
            return applications.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find application by client id", ex);
            throw new TechnicalException("Failed to find application by client id", ex);
        }
    }
}