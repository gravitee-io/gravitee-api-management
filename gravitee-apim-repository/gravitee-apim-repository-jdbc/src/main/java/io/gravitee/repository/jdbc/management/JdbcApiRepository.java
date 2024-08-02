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
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.jdbc.utils.FieldUtils;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

/**
 * @author njt
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcApiRepository extends JdbcAbstractPageableRepository<Api> implements ApiRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiRepository.class);
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
            .addColumn("origin", Types.NVARCHAR, String.class)
            .addColumn("mode", Types.NVARCHAR, String.class)
            .addColumn("sync_from", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("integration_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("version", Types.NVARCHAR, String.class)
            .addColumn("definition_version", Types.NVARCHAR, DefinitionVersion.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, ApiType.class)
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

    @Override
    /**
     * DO NOT USE THIS METHOD
     * @deprecated use {@link #search(ApiCriteria, Sortable, ApiFieldFilter, int)} instead
     */
    public Set<Api> findAll() throws TechnicalException {
        throw new IllegalStateException(
            "Too many results, please use search(ApiCriteria, Sortable, ApiFieldFilter, int) returning a Stream instead."
        );
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

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        final List<Api> apis = findByCriteria(apiCriteria, sortable, null);
        return getResultAsPage(pageable, apis);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldFilter apiFieldFilter) {
        return findByCriteria(apiCriteria, null, apiFieldFilter);
    }

    @Override
    public Stream<Api> search(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter, int batchSize) {
        // As in JDBC, we do not paginate, we cannot use the batch size
        return findByCriteria(apiCriteria, sortable, apiFieldFilter).stream();
    }

    @Override
    public Page<String> searchIds(List<ApiCriteria> criteria, Pageable pageable, Sortable sortable) {
        LOGGER.debug("JdbcApiRepository.searchIds({})", criteria);

        final StringBuilder sbQuery = new StringBuilder("select distinct a.id");

        if (sortable != null && sortable.field() != null && sortable.field().length() > 0) {
            sbQuery.append(",").append(sortable.field());
        }
        sbQuery.append(" from ").append(this.tableName).append(" a ");
        Optional<ApiCriteria> hasCategory = criteria.stream().filter(apiCriteria -> hasText(apiCriteria.getCategory())).findFirst();
        if (hasCategory.isPresent()) {
            sbQuery.append("left join ").append(API_CATEGORIES).append(" ac on a.id = ac.api_id ");
        }
        Optional<ApiCriteria> hasGroups = criteria.stream().filter(apiCriteria -> !isEmpty(apiCriteria.getGroups())).findFirst();
        if (hasGroups.isPresent()) {
            sbQuery.append("left join ").append(API_GROUPS).append(" ag on a.id = ag.api_id ");
        }
        Optional<ApiCriteria> hasLabels = criteria.stream().filter(apiCriteria -> hasText(apiCriteria.getLabel())).findFirst();
        if (hasLabels.isPresent()) {
            sbQuery.append("left join ").append(API_LABELS).append(" al on a.id = al.api_id ");
        }

        List<String> clauses = criteria.stream().map(this::convert).filter(Objects::nonNull).collect(Collectors.toList());

        if (!clauses.isEmpty()) {
            sbQuery.append("where (").append(String.join(") or (", clauses)).append(") ");
        }

        applySortable(sortable, sbQuery);

        List<String> apisIds = jdbcTemplate.query(
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
        return getResultAsPage(pageable, apisIds);
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
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var apiIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!apiIds.isEmpty()) {
                String inClause = getOrm().buildInClause(apiIds);
                jdbcTemplate.update("delete from " + API_LABELS + " where api_id IN ( " + inClause + " )", apiIds.toArray());
                jdbcTemplate.update("delete from " + API_GROUPS + " where api_id IN ( " + inClause + " )", apiIds.toArray());
                jdbcTemplate.update("delete from " + API_CATEGORIES + " where api_id IN ( " + inClause + " )", apiIds.toArray());
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            LOGGER.debug("JdbcApiRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return apiIds;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete api by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete api by environment", ex);
        }
    }

    // Labels
    private void addLabels(Api parent) {
        List<String> labels = getLabels(parent.getId());
        parent.setLabels(labels);
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

    // Groups
    private void addGroups(Api parent) {
        List<String> groups = getGroups(parent.getId());
        parent.setGroups(new HashSet<>(groups));
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

    // Categories
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

    private List<Api> findByCriteria(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
        final JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(
            getOrm().getRowMapper(),
            CHILD_ADDER,
            "id"
        );
        LOGGER.debug("JdbcApiRepository.search({})", apiCriteria);

        String projection =
            "ac.*, a.id, a.environment_id, a.cross_id, a.name, a.description, a.version, a.type, a.deployed_at, a.created_at, a.updated_at, " +
            "a.visibility, a.lifecycle_state, a.api_lifecycle_state, a.definition_version, a.origin, a.sync_from";

        if (apiFieldFilter == null || !apiFieldFilter.isDefinitionExcluded()) {
            projection += ", a.definition";
        }
        if (apiFieldFilter == null || !apiFieldFilter.isPictureExcluded()) {
            projection += ", a.picture, a.background";
        }

        final StringBuilder sbQuery = new StringBuilder("select ").append(projection).append(" from ").append(this.tableName).append(" a ");
        sbQuery.append("left join " + API_CATEGORIES + " ac on a.id = ac.api_id ");
        addCriteriaClauses(sbQuery, apiCriteria);
        if (sortable == null) {
            sortable = new SortableBuilder().field("name").setAsc(true).build();
        }
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
            String field = FieldUtils.toSnakeCase(sortable.field());
            query.append("order by ");
            if ("created_at".equals(field) || "updated_at".equals(field)) {
                query.append("a.").append(field);
            } else {
                query.append(" lower(a.").append(field).append(") ");
            }

            query.append(sortable.order() == null || sortable.order().equals(Order.ASC) ? " asc " : " desc ");
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
            if (hasText(apiCriteria.getCrossId())) {
                ps.setString(lastIndex++, apiCriteria.getCrossId());
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
            if (!isEmpty(apiCriteria.getDefinitionVersion())) {
                List<DefinitionVersion> definitionVersionList = new ArrayList<>(apiCriteria.getDefinitionVersion());
                definitionVersionList.remove(null);
                if (!definitionVersionList.isEmpty()) {
                    lastIndex = getOrm().setArguments(ps, definitionVersionList, lastIndex);
                }
            }
            if (hasText(apiCriteria.getIntegrationId())) {
                ps.setString(lastIndex++, apiCriteria.getIntegrationId());
            }
        }
        return lastIndex;
    }

    private String convert(ApiCriteria apiCriteria) {
        List<String> clauses = new ArrayList<>();
        if (!isEmpty(apiCriteria.getGroups())) {
            clauses.add("ag.group_id in (" + getOrm().buildInClause(apiCriteria.getGroups()) + ")");
        }
        if (!isEmpty(apiCriteria.getIds())) {
            clauses.add("a.id in (" + getOrm().buildInClause(apiCriteria.getIds()) + ")");
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
        if (hasText(apiCriteria.getCrossId())) {
            clauses.add("a.cross_id = ?");
        }
        if (!isEmpty(apiCriteria.getLifecycleStates())) {
            clauses.add("a.api_lifecycle_state in (" + getOrm().buildInClause(apiCriteria.getLifecycleStates()) + ")");
        }
        if (hasText(apiCriteria.getEnvironmentId())) {
            clauses.add("a.environment_id = ?");
        }
        if (!isEmpty(apiCriteria.getEnvironments())) {
            clauses.add("a.environment_id in (" + getOrm().buildInClause(apiCriteria.getEnvironments()) + ")");
        }
        if (hasText(apiCriteria.getIntegrationId())) {
            clauses.add("a.integration_id = ?");
        }
        if (!isEmpty(apiCriteria.getDefinitionVersion())) {
            List<DefinitionVersion> definitionVersionList = new ArrayList<>(apiCriteria.getDefinitionVersion());

            var lookingForV2 = apiCriteria.getDefinitionVersion().stream().anyMatch(DefinitionVersion.V2::equals);
            boolean addNullClause = definitionVersionList.remove(null);

            StringBuilder clauseBuilder = new StringBuilder();
            if (addNullClause || lookingForV2) {
                if (definitionVersionList.isEmpty()) {
                    clauseBuilder.append("a.definition_version is null");
                } else {
                    clauseBuilder
                        .append("(a.definition_version is null or a.definition_version in (")
                        .append(getOrm().buildInClause(definitionVersionList))
                        .append("))");
                }
            } else {
                clauseBuilder.append("a.definition_version in (").append(getOrm().buildInClause(definitionVersionList)).append(")");
            }
            clauses.add(clauseBuilder.toString());
        }
        if (!clauses.isEmpty()) {
            return String.join(" and ", clauses);
        }
        return null;
    }

    @Override
    public Optional<String> findIdByEnvironmentIdAndCrossId(final String environmentId, final String crossId) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.findIdByEnvironmentIdAndCrossId({}, {})", environmentId, crossId);
        List<String> rows = jdbcTemplate.queryForList(
            "select a.id from " + this.tableName + " a where a.environment_id = ? and a.cross_id = ?",
            String.class,
            environmentId,
            crossId
        );

        if (rows.size() > 1) {
            throw new TechnicalException("More than one API was found for environmentId " + environmentId + " and crossId " + crossId);
        }

        Optional<String> result = rows.stream().findFirst();

        LOGGER.debug("JdbcApiRepository.findIdByEnvironmentIdAndCrossId({}, {}) = {}", environmentId, crossId, result);
        return result;
    }

    @Override
    public boolean existById(final String apiId) throws TechnicalException {
        LOGGER.debug("JdbcApiRepository.existById({})", apiId);
        try {
            String idFound = jdbcTemplate.queryForObject("select a.id from " + this.tableName + " a where a.id = ?", String.class, apiId);

            LOGGER.debug("JdbcApiRepository.existById({}) = {}", apiId, idFound);
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}
