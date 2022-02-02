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

import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.*;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * @author njt
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcApiRepository extends JdbcAbstractPageableRepository<Api> implements ApiRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiRepository.class);
    private final String API_CATEGORIES;
    private final String API_LABELS;
    private final String API_GROUPS;

    JdbcApiRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "apis");
        API_CATEGORIES = getTableNameFor("api_categories");
        API_LABELS = getTableNameFor("api_labels");
        API_GROUPS = getTableNameFor("api_groups");
    }

    @Override
    protected JdbcObjectMapper<Api> buildOrm() {
        return JdbcObjectMapper
            .builder(Api.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("cross_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("version", Types.NVARCHAR, String.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("deployed_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("visibility", Types.NVARCHAR, Visibility.class)
            .addColumn("lifecycle_state", Types.NVARCHAR, LifecycleState.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn("api_lifecycle_state", Types.NVARCHAR, ApiLifecycleState.class)
            .addColumn("disable_membership_notifications", Types.BIT, boolean.class)
            .addColumn("background", Types.NVARCHAR, String.class)
            .build();
    }

    private static final JdbcHelper.ChildAdder<Api> CHILD_ADDER = (Api parent, ResultSet rs) -> {
        Set<String> categories = parent.getCategories();
        if (categories == null) {
            categories = new HashSet<>();
            parent.setCategories(categories);
        }
        if (rs.getString("category") != null) {
            categories.add(rs.getString("category"));
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

    @Override
    public Optional<Api> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() + " a left join " + API_CATEGORIES + " ac on a.id = ac.api_id where a.id = ?",
                rowMapper,
                id
            );
            Optional<Api> result = rowMapper.getRows().stream().findFirst();
            if (result.isPresent()) {
                addLabels(result.get());
                addGroups(result.get());
            }
            LOGGER.debug("JdbcApiRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api by id:", ex);
            throw new TechnicalException("Failed to find api by id", ex);
        }
    }

    @Override
    public Api create(Api item) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeLabels(item, false);
            storeGroups(item, false);
            storeCategories(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create api:", ex);
            throw new TechnicalException("Failed to create api", ex);
        }
    }

    @Override
    public Api update(final Api api) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.update({})", api);
        if (api == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(api, api.getId()));
            storeLabels(api, true);
            storeGroups(api, true);
            storeCategories(api, true);
            return findById(api.getId()).orElseThrow(() -> new IllegalStateException(format("No api found with id [%s]", api.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update api", ex);
            throw new TechnicalException("Failed to update api", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + API_LABELS + " where api_id = ?", id);
        jdbcTemplate.update("delete from " + API_GROUPS + " where api_id = ?", id);
        jdbcTemplate.update("delete from " + API_CATEGORIES + " where api_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    private List<String> getLabels(String apiId) {
        return jdbcTemplate.queryForList("select label from " + API_LABELS + " where api_id = ?", String.class, apiId);
    }

    private void storeLabels(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + API_LABELS + " where api_id = ?", api.getId());
        }
        List<String> filteredLabels = getOrm().filterStrings(api.getLabels());
        if (!filteredLabels.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + API_LABELS + " (api_id, label) values ( ?, ? )",
                getOrm().getBatchStringSetter(api.getId(), filteredLabels)
            );
        }
    }

    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select group_id from " + API_GROUPS + " where api_id = ?", String.class, apiId);
    }

    private void storeGroups(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + API_GROUPS + " where api_id = ?", api.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(api.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + API_GROUPS + " ( api_id, group_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(api.getId(), filteredGroups)
            );
        }
    }

    private void storeCategories(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + API_CATEGORIES + " where api_id = ?", api.getId());
        }
        List<String> filteredCategories = getOrm().filterStrings(api.getCategories());
        if (!filteredCategories.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + API_CATEGORIES + " ( api_id, category ) values ( ?, ? )",
                getOrm().getBatchStringSetter(api.getId(), filteredCategories)
            );
        }
    }

    @Override
    public Page<Api> search(
        ApiCriteria apiCriteria,
        Sortable sortable,
        Pageable pageable,
        ApiFieldExclusionFilter apiFieldExclusionFilter
    ) {
        final List<Api> apis = findByCriteria(apiCriteria, sortable, null);
        return getResultAsPage(pageable, apis);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        return findByCriteria(apiCriteria, null, null);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        return findByCriteria(apiCriteria, null, apiFieldExclusionFilter);
    }

    @Override
    public Set<Api> search(ApiCriteria apiCriteria, ApiFieldInclusionFilter apiFieldInclusionFilter) {
        final JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            CHILD_ADDER,
            "id"
        );

        final StringBuilder sbQuery = new StringBuilder("select a.id");

        if (apiFieldInclusionFilter.hasCategories()) {
            sbQuery.append(", ac.*");
        }

        sbQuery.append(" from ").append(this.tableName).append(" a ");

        if (apiFieldInclusionFilter.hasCategories()) {
            sbQuery.append("left join " + API_CATEGORIES + " ac on a.id = ac.api_id ");
        }

        addCriteriaClauses(sbQuery, apiCriteria);

        List<Api> apis = executeQuery(sbQuery, apiCriteria, rowMapper);

        return new HashSet<>(apis);
    }

    @Override
    public List<String> searchIds(Sortable sortable, ApiCriteria... criteria) {
        LOGGER.debug("JdbcApiRepository.searchIds({})", criteria);

        final StringBuilder sbQuery = new StringBuilder("select a.id from ")
            .append(this.tableName)
            .append(" a left join ")
            .append(API_CATEGORIES)
            .append(" ac on a.id = ac.api_id ");

        Optional<ApiCriteria> hasGroups = Arrays.stream(criteria).filter(apiCriteria -> !isEmpty(apiCriteria.getGroups())).findFirst();
        Optional<ApiCriteria> hasLabels = Arrays.stream(criteria).filter(apiCriteria -> hasText(apiCriteria.getLabel())).findFirst();
        if (hasGroups.isPresent()) {
            sbQuery.append("join " + API_GROUPS + " ag on a.id = ag.api_id ");
        }
        if (hasLabels.isPresent()) {
            sbQuery.append("join " + API_LABELS + " al on a.id = al.api_id ");
        }

        List<String> clauses = Arrays.stream(criteria).map(this::convert).filter(Objects::nonNull).collect(Collectors.toList());

        if (!clauses.isEmpty()) {
            sbQuery.append("where (").append(String.join(") or (", clauses)).append(") ");
        }

        applySortable(sortable, sbQuery);

        return jdbcTemplate.query(
            sbQuery.toString(),
            (PreparedStatement ps) -> {
                int lastIndex = 1;
                for (ApiCriteria apiCriteria : criteria) {
                    lastIndex = fillPreparedStatement(apiCriteria, ps, lastIndex);
                }
            },
            resultSet -> {
                List<String> ids = new ArrayList<>();
                while (resultSet.next()) {
                    String id = resultSet.getString(1);
                    ids.add(id);
                }
                return ids;
            }
        );
    }

    private List<Api> findByCriteria(ApiCriteria apiCriteria, Sortable sortable, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        final JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            CHILD_ADDER,
            "id"
        );
        LOGGER.debug("JdbcApiRepository.search({})", apiCriteria);

        String projection =
            "ac.*, a.id, a.environment_id, a.name, a.description, a.version, a.deployed_at, a.created_at, a.updated_at, " +
            "a.visibility, a.lifecycle_state, a.api_lifecycle_state";

        if (apiFieldExclusionFilter == null || !apiFieldExclusionFilter.isDefinition()) {
            projection += ", a.definition";
        }
        if (apiFieldExclusionFilter == null || !apiFieldExclusionFilter.isPicture()) {
            projection += ", a.picture, a.background";
        }

        final StringBuilder sbQuery = new StringBuilder("select ").append(projection).append(" from ").append(this.tableName).append(" a ");
        sbQuery.append("left join " + API_CATEGORIES + " ac on a.id = ac.api_id ");
        addCriteriaClauses(sbQuery, apiCriteria);
        applySortable(sortable, sbQuery);

        List<Api> apis = executeQuery(sbQuery, apiCriteria, rowMapper);

        for (final Api api : apis) {
            addLabels(api);
            addGroups(api);
        }
        return apis;
    }

    private void addCriteriaClauses(StringBuilder sbQuery, ApiCriteria apiCriteria) {
        if (apiCriteria != null) {
            String clauses = convert(apiCriteria);
            if (clauses != null) {
                if (!isEmpty(apiCriteria.getGroups())) {
                    sbQuery.append("join " + API_GROUPS + " ag on a.id = ag.api_id ");
                }
                if (hasText(apiCriteria.getLabel())) {
                    sbQuery.append("join " + API_LABELS + " al on a.id = al.api_id ");
                }
                sbQuery.append("where ").append(clauses).append(" ");
            }
        }
    }

    private List<Api> executeQuery(StringBuilder sbQuery, ApiCriteria apiCriteria, JdbcHelper.CollatingRowMapper<Api> rowMapper) {
        jdbcTemplate.query(sbQuery.toString(), ps -> fillPreparedStatement(apiCriteria, ps, 1), rowMapper);
        List<Api> apis = rowMapper.getRows();
        return apis;
    }

    private void applySortable(Sortable sortable, StringBuilder query) {
        if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
            query.append("order by ");
            if ("created_at".equals(sortable.field())) {
                query.append("a.").append(sortable.field());
            } else {
                query.append(" lower(a.").append(sortable.field()).append(") ");
            }

            query.append(sortable.order().equals(Order.ASC) ? " asc " : " desc ");
        } else {
            query.append("order by a.name");
        }
    }

    private int fillPreparedStatement(ApiCriteria apiCriteria, PreparedStatement ps, int lastIndex) throws java.sql.SQLException {
        if (apiCriteria != null) {
            if (!isEmpty(apiCriteria.getGroups())) {
                lastIndex = getOrm().setArguments(ps, apiCriteria.getGroups(), lastIndex);
            }
            if (!isEmpty(apiCriteria.getIds())) {
                lastIndex = getOrm().setArguments(ps, apiCriteria.getIds(), lastIndex);
            }
            if (hasText(apiCriteria.getLabel())) {
                ps.setString(lastIndex++, apiCriteria.getLabel());
            }
            if (hasText(apiCriteria.getName())) {
                ps.setString(lastIndex++, apiCriteria.getName());
            }
            if (apiCriteria.getState() != null) {
                ps.setString(lastIndex++, apiCriteria.getState().name());
            }
            if (hasText(apiCriteria.getVersion())) {
                ps.setString(lastIndex++, apiCriteria.getVersion());
            }
            if (hasText(apiCriteria.getCategory())) {
                ps.setString(lastIndex++, apiCriteria.getCategory());
            }
            if (apiCriteria.getVisibility() != null) {
                ps.setString(lastIndex++, apiCriteria.getVisibility().name());
            }
            if (!isEmpty(apiCriteria.getLifecycleStates())) {
                lastIndex = getOrm().setArguments(ps, apiCriteria.getLifecycleStates(), lastIndex);
            }
            if (hasText(apiCriteria.getEnvironmentId())) {
                ps.setString(lastIndex++, apiCriteria.getEnvironmentId());
            }
            if (!isEmpty(apiCriteria.getEnvironments())) {
                lastIndex = getOrm().setArguments(ps, apiCriteria.getEnvironments(), lastIndex);
            }
        }
        return lastIndex;
    }

    private String convert(ApiCriteria apiCriteria) {
        List<String> clauses = new ArrayList<>();
        if (!isEmpty(apiCriteria.getGroups())) {
            clauses.add(
                new StringBuilder()
                    .append("ag.group_id in (")
                    .append(getOrm().buildInClause(apiCriteria.getGroups()))
                    .append(")")
                    .toString()
            );
        }
        if (!isEmpty(apiCriteria.getIds())) {
            clauses.add(
                new StringBuilder().append("a.id in (").append(getOrm().buildInClause(apiCriteria.getIds())).append(")").toString()
            );
        }
        if (hasText(apiCriteria.getLabel())) {
            clauses.add("al.label = ?");
        }
        if (hasText(apiCriteria.getName())) {
            clauses.add("a.name = ?");
        }
        if (apiCriteria.getState() != null) {
            clauses.add("a.lifecycle_state = ?");
        }
        if (hasText(apiCriteria.getVersion())) {
            clauses.add("a.version = ?");
        }
        if (hasText(apiCriteria.getCategory())) {
            clauses.add("ac.category = ?");
        }
        if (apiCriteria.getVisibility() != null) {
            clauses.add("a.visibility = ?");
        }
        if (!StringUtils.isEmpty(apiCriteria.getLifecycleStates())) {
            clauses.add(
                new StringBuilder()
                    .append("a.api_lifecycle_state in (")
                    .append(getOrm().buildInClause(apiCriteria.getLifecycleStates()))
                    .append(")")
                    .toString()
            );
        }
        if (hasText(apiCriteria.getEnvironmentId())) {
            clauses.add("a.environment_id = ?");
        }
        if (!isEmpty(apiCriteria.getEnvironments())) {
            clauses.add(
                new StringBuilder()
                    .append("a.environment_id in (")
                    .append(getOrm().buildInClause(apiCriteria.getEnvironments()))
                    .append(")")
                    .toString()
            );
        }
        if (!clauses.isEmpty()) {
            return String.join(" and ", clauses);
        }
        return null;
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.findAll()");
        try {
            return new HashSet<>(jdbcTemplate.query(getOrm().getSelectAllSql(), getOrm().getRowMapper()));
        } catch (final Exception ex) {
            final String error = "Failed to find all api";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Set<String> listCategories(ApiCriteria apiCriteria) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.listCategories({})", apiCriteria);
        try {
            boolean hasInClause = apiCriteria.getIds() != null && !apiCriteria.getIds().isEmpty();

            StringBuilder queryBuilder = new StringBuilder("SELECT category from ").append(API_CATEGORIES);

            if (hasInClause) {
                queryBuilder.append(" where api_id IN (").append(getOrm().buildInClause(apiCriteria.getIds())).append(") ");
            }
            queryBuilder.append(" group by category order by category asc");

            return jdbcTemplate.query(
                queryBuilder.toString(),
                ps -> {
                    if (hasInClause) {
                        getOrm().setArguments(ps, apiCriteria.getIds(), 1);
                    }
                },
                resultSet -> {
                    Set<String> distinctCategories = new LinkedHashSet<>();
                    while (resultSet.next()) {
                        String referenceId = resultSet.getString(1);
                        distinctCategories.add(referenceId);
                    }
                    return distinctCategories;
                }
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to list categories api:", ex);
            throw new TechnicalException("Failed to list categories", ex);
        }
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.findByEnvironmentIdAndCrossId({}, {})", environmentId, crossId);
        try {
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " a left join " +
                API_CATEGORIES +
                " ac on a.id = ac.api_id where a.environment_id = ? and a.cross_id = ?",
                rowMapper,
                environmentId,
                crossId
            );

            if (rowMapper.getRows().size() > 1) {
                throw new TechnicalException("More than one API was found for environmentId " + environmentId + " and crossId " + crossId);
            }

            Optional<Api> result = rowMapper.getRows().stream().findFirst();

            if (result.isPresent()) {
                addLabels(result.get());
                addGroups(result.get());
            }

            LOGGER.debug("JdbcApiRepository.findByEnvironmentIdAndCrossId({}, {}) = {}", environmentId, crossId, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api by environment and crossId", ex);
            throw new TechnicalException("Failed to find api by environment and crossId", ex);
        }
    }
}
