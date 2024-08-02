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

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.Origin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApplicationRepository extends JdbcAbstractCrudRepository<Application, String> implements ApplicationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApplicationRepository.class);

    private static final String TYPE_FIELD = "type";
    private static final String STATUS_FIELD = "status";
    private final String APPLICATION_GROUPS;
    private final String APPLICATION_METADATA;

    private static class GroupRowMapper implements RowMapper<List<String>> {

        @Override
        public List<String> mapRow(ResultSet rs, int i) throws SQLException {
            return List.of(rs.getString("application_id"), rs.getString("group_id"));
        }
    }

    private static final GroupRowMapper GROUP_MAPPER = new GroupRowMapper();

    JdbcApplicationRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "applications");
        APPLICATION_GROUPS = getTableNameFor("application_groups");
        APPLICATION_METADATA = getTableNameFor("application_metadata");
    }

    @Override
    protected JdbcObjectMapper<Application> buildOrm() {
        return JdbcObjectMapper
            .builder(Application.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("domain", Types.NVARCHAR, String.class)
            .addColumn(TYPE_FIELD, Types.NVARCHAR, ApplicationType.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn(STATUS_FIELD, Types.NVARCHAR, ApplicationStatus.class)
            .addColumn("disable_membership_notifications", Types.BIT, boolean.class)
            .addColumn("background", Types.NVARCHAR, String.class)
            .addColumn("api_key_mode", Types.NVARCHAR, ApiKeyMode.class)
            .addColumn("origin", Types.NVARCHAR, Origin.class)
            .build();
    }

    private static final String PROJECTION_WITHOUT_PICTURES =
        "a.id, a.environment_id, a.name, a.description, a.type, a.created_at, a.updated_at, a.status, a.disable_membership_notifications, a.api_key_mode, a.origin";

    private static final JdbcHelper.ChildAdder<Application> CHILD_ADDER = (Application parent, ResultSet rs) -> {
        Map<String, String> metadata = parent.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            parent.setMetadata(metadata);
        }
        if (rs.getString("am_k") != null) {
            metadata.put(rs.getString("am_k"), rs.getString("am_v"));
        }
    };

    @Override
    protected String getId(Application item) {
        return item.getId();
    }

    private void addGroups(Application parent) {
        List<String> groups = getGroups(parent.getId());
        parent.setGroups(new HashSet<>(groups));
    }

    private void addGroups(Collection<Application> applications) {
        Map<String, Application> applicationById = applications.stream().collect(toMap(Application::getId, identity()));
        Set<String> applicationIds = applicationById.keySet();

        if (applicationIds.isEmpty()) {
            return;
        }

        List<List<String>> rows = jdbcTemplate.query(
            "select application_id, group_id from " +
            APPLICATION_GROUPS +
            " where application_id in (" +
            getOrm().buildInClause(applicationIds) +
            ")",
            (PreparedStatement ps) -> {
                getOrm().setArguments(ps, applicationIds, 1);
            },
            GROUP_MAPPER
        );

        for (List<String> row : rows) {
            Application application = applicationById.get(row.get(0));
            if (application.getGroups() == null) {
                application.setGroups(new HashSet<>());
            }
            application.getGroups().add(row.get(1));
        }
    }

    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select group_id from " + APPLICATION_GROUPS + " where application_id = ?", String.class, apiId);
    }

    private void storeGroups(Application application, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + APPLICATION_GROUPS + " where application_id = ?", application.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(application.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + APPLICATION_GROUPS + " ( application_id, group_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(application.getId(), filteredGroups)
            );
        }
    }

    private void storeMetadata(Application application, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + APPLICATION_METADATA + " where application_id = ?", application.getId());
        }
        if (application.getMetadata() != null && !application.getMetadata().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(application.getMetadata().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + APPLICATION_METADATA + " ( application_id, k, v ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, application.getId());
                        ps.setString(2, entries.get(i).getKey());
                        ps.setString(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }

    @Override
    public Application create(Application item) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeGroups(item, false);
            storeMetadata(item, false);
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
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(application, application.getId()));
            storeGroups(application, true);
            storeMetadata(application, true);
            return findById(application.getId())
                .orElseThrow(() -> new IllegalStateException(format("No application found with id [%s]", application.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update application", ex);
            throw new TechnicalException("Failed to update application", ex);
        }
    }

    private class Rm implements RowMapper<Application> {

        @Override
        public Application mapRow(ResultSet rs, int i) throws SQLException {
            Application application = new Application();
            getOrm().setFromResultSet(application, rs);
            addGroups(application);
            return application;
        }
    }

    private final Rm mapper = new Rm();

    @Override
    public Optional<Application> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(
                "select a.*, am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id where a.id = ?",
                rowMapper,
                id
            );
            Optional<Application> result = rowMapper.getRows().stream().findFirst();
            result.ifPresent(this::addGroups);
            LOGGER.debug("JdbcApplicationRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find application by id:", ex);
            throw new TechnicalException("Failed to find application by id", ex);
        }
    }

    @Override
    public Set<Application> findByIds(Collection<String> ids) throws TechnicalException {
        return this.findByIds(ids, null);
    }

    @Override
    public Set<Application> findByIds(Collection<String> ids, Sortable sortable) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByIds({}, {})", ids, sortable);
        try {
            if (isEmpty(ids)) {
                return emptySet();
            }

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");

            String query =
                "select " +
                PROJECTION_WITHOUT_PICTURES +
                ", am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id where a.id in ( " +
                getOrm().buildInClause(ids) +
                " )";

            if (sortable != null && hasText(sortable.field())) {
                query += " order by a." + sortable.field() + " " + sortable.order().name();
            }

            jdbcTemplate.query(query, (PreparedStatement ps) -> getOrm().setArguments(ps, ids, 1), rowMapper);
            List<Application> applications = rowMapper.getRows();

            addGroups(applications);

            return new LinkedHashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by ids:", ex);
            throw new TechnicalException("Failed to find  applications by ids", ex);
        }
    }

    @Override
    public Set<Application> findAll() throws TechnicalException {
        return this.findAll(ApplicationStatus.values());
    }

    @Override
    public Set<Application> findAll(ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findAll({})", (Object[]) ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);

            StringBuilder query = new StringBuilder(
                "select " +
                PROJECTION_WITHOUT_PICTURES +
                ", am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id"
            );
            boolean first = true;
            getOrm().buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(query.toString(), (PreparedStatement ps) -> getOrm().setArguments(ps, statuses, 1), rowMapper);

            List<Application> applications = rowMapper.getRows();

            addGroups(applications);

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
        if (isEmpty(groupIds)) {
            return emptySet();
        }
        try {
            final List<ApplicationStatus> statuses = Arrays.asList(ass);
            final StringBuilder query = new StringBuilder(
                "select " +
                PROJECTION_WITHOUT_PICTURES +
                ", am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id join " +
                APPLICATION_GROUPS +
                " ag on ag.application_id = a.id "
            );
            boolean first = true;
            first = getOrm().buildInCondition(first, query, "group_id", groupIds);
            getOrm().buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int idx = getOrm().setArguments(ps, groupIds, 1);
                    getOrm().setArguments(ps, statuses, idx);
                },
                rowMapper
            );

            List<Application> applications = rowMapper.getRows();

            addGroups(applications);

            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by groups", ex);
            throw new TechnicalException("Failed to find applications by groups", ex);
        }
    }

    @Override
    public Set<Application> findByNameAndStatuses(String partialName, ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findByName({}, {})", partialName, ass);
        try {
            final List<ApplicationStatus> statuses = Arrays.asList(ass);
            StringBuilder query = new StringBuilder(
                "select " +
                PROJECTION_WITHOUT_PICTURES +
                ", am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id where lower(name) like ?"
            );
            getOrm().buildInCondition(false, query, "status", statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int idx = getOrm().setArguments(ps, singleton("%" + partialName.toLowerCase() + "%"), 1);
                    getOrm().setArguments(ps, statuses, idx);
                },
                rowMapper
            );

            List<Application> applications = rowMapper.getRows();

            addGroups(applications);

            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by name", ex);
            throw new TechnicalException("Failed to find applications by name", ex);
        }
    }

    @Override
    public Page<Application> search(ApplicationCriteria applicationCriteria, Pageable pageable, Sortable sortable) {
        List<Application> apps = search(applicationCriteria, sortable);

        addGroups(apps);

        return getResultAsPage(pageable, apps);
    }

    private List<Application> search(ApplicationCriteria applicationCriteria, Sortable sortable) {
        LOGGER.debug("JdbcApplicationRepository.search({})", applicationCriteria);
        final JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            CHILD_ADDER,
            "id"
        );

        final StringBuilder sbQuery = new StringBuilder(
            "select " +
            PROJECTION_WITHOUT_PICTURES +
            ", am.k as am_k, am.v as am_v from " +
            this.tableName +
            " a left join " +
            APPLICATION_METADATA +
            " am on a.id = am.application_id "
        );

        if (applicationCriteria != null) {
            if (!isEmpty(applicationCriteria.getGroups())) {
                sbQuery.append("join " + APPLICATION_GROUPS + " ag on ag.application_id = a.id ");
            }

            sbQuery.append("where 1 = 1 ");
            if (!isEmpty(applicationCriteria.getIds())) {
                sbQuery.append("and a.id in (").append(getOrm().buildInClause(applicationCriteria.getIds())).append(") ");
            }
            if (hasText(applicationCriteria.getName())) {
                sbQuery.append("and lower(a.name) like ? ");
            }
            if (applicationCriteria.getStatus() != null) {
                sbQuery.append("and a.status = ? ");
            }
            if (!isEmpty(applicationCriteria.getEnvironmentIds())) {
                sbQuery
                    .append("and a.environment_id in (")
                    .append(getOrm().buildInClause(applicationCriteria.getEnvironmentIds()))
                    .append(") ");
            }
            if (!isEmpty(applicationCriteria.getGroups())) {
                sbQuery.append("and ag.group_id in (").append(getOrm().buildInClause(applicationCriteria.getGroups())).append(") ");
            }
        }

        String direction = toSortDirection(sortable);
        String field = "name";
        if (sortable != null) {
            field = sortable.field();
        }

        sbQuery.append(String.format("order by a.%s %s", field, direction));

        jdbcTemplate.query(
            sbQuery.toString(),
            (PreparedStatement ps) -> {
                int lastIndex = 1;
                if (applicationCriteria != null) {
                    if (!isEmpty(applicationCriteria.getIds())) {
                        lastIndex = getOrm().setArguments(ps, applicationCriteria.getIds(), lastIndex);
                    }
                    if (hasText(applicationCriteria.getName())) {
                        ps.setString(lastIndex++, "%" + applicationCriteria.getName().toLowerCase() + "%");
                    }
                    if (applicationCriteria.getStatus() != null) {
                        ps.setString(lastIndex++, applicationCriteria.getStatus().name());
                    }
                    if (!isEmpty(applicationCriteria.getEnvironmentIds())) {
                        lastIndex = getOrm().setArguments(ps, applicationCriteria.getEnvironmentIds(), lastIndex);
                    }
                    if (!isEmpty(applicationCriteria.getGroups())) {
                        getOrm().setArguments(ps, applicationCriteria.getGroups(), lastIndex);
                    }
                }
            },
            rowMapper
        );
        return rowMapper.getRows();
    }

    @Override
    public Set<String> searchIds(ApplicationCriteria applicationCriteria, Sortable sortable) throws TechnicalException {
        List<Application> search = search(applicationCriteria, sortable);
        return search.parallelStream().map(Application::getId).collect(toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<Application> findAllByEnvironment(String environmentId, ApplicationStatus... ass) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.findAllByEnvironment({}, {})", environmentId, (Object[]) ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);

            StringBuilder query = new StringBuilder(
                "select " +
                PROJECTION_WITHOUT_PICTURES +
                ", am.k as am_k, am.v as am_v from " +
                this.tableName +
                " a left join " +
                APPLICATION_METADATA +
                " am on a.id = am.application_id where a.environment_id = ?"
            );
            boolean first = false;
            getOrm().buildInCondition(first, query, STATUS_FIELD, statuses);

            JdbcHelper.CollatingRowMapper<Application> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    getOrm().setArguments(ps, Arrays.asList(environmentId), 1);
                    getOrm().setArguments(ps, statuses, 2);
                },
                rowMapper
            );
            List<Application> applications = rowMapper.getRows();

            addGroups(applications);

            LOGGER.debug("Found {} applications: {}", applications.size(), applications);
            return new HashSet<>(applications);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find applications by environment:", ex);
            throw new TechnicalException("Failed to find applications by environment", ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcApplicationRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var applicationIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!applicationIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + APPLICATION_METADATA + " where application_id IN ( " + getOrm().buildInClause(applicationIds) + " )",
                    applicationIds.toArray()
                );
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            LOGGER.debug("JdbcApplicationRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return applicationIds;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete application by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete application by environment", ex);
        }
    }

    private String toSortDirection(Sortable sortable) {
        if (sortable != null) {
            return Order.DESC.equals(sortable.order()) ? "desc" : "asc";
        }
        return "asc";
    }
}
