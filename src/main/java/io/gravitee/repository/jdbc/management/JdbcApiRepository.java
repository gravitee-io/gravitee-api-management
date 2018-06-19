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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;

import static java.lang.String.format;

/**
 * @author njt
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcApiRepository extends JdbcAbstractPageableRepository<Api> implements ApiRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Api.class, "apis", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
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
            .build();

    private static final JdbcHelper.ChildAdder<Api> CHILD_ADDER = (Api parent, ResultSet rs) -> {
        Set<String> views = parent.getViews();
        if (views == null) {
            views = new HashSet<>();
            parent.setViews(views);
        }
        if (rs.getString("view") != null) {
            views.add(rs.getString("view"));
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
            JdbcHelper.CollatingRowMapper<Api> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query("select * from apis a left join api_views av on a.id = av.api_id where a.id = ?"
                    , rowMapper
                    , id
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
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeLabels(item, false);
            storeGroups(item, false);
            storeViews(item, false);
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
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(api, api.getId()));
            storeLabels(api, true);
            storeGroups(api, true);
            storeViews(api, true);
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
        jdbcTemplate.update("delete from api_labels where api_id = ?", id);
        jdbcTemplate.update("delete from api_views where api_id = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }

    private List<String> getLabels(String apiId) {
        return jdbcTemplate.queryForList("select label from api_labels where api_id = ?", String.class, apiId);
    }

    private void storeLabels(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from api_labels where api_id = ?", api.getId());
        }
        List<String> filteredLabels = ORM.filterStrings(api.getLabels());
        if (!filteredLabels.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into api_labels (api_id, label) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredLabels));
        }
    }

    private List<String> getGroups(String apiId) {
        return jdbcTemplate.queryForList("select group_id from api_groups where api_id = ?", String.class, apiId);
    }

    private void storeGroups(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from api_groups where api_id = ?", api.getId());
        }
        List<String> filteredGroups = ORM.filterStrings(api.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into api_groups ( api_id, group_id ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredGroups));
        }
    }

    private void storeViews(Api api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from api_views where api_id = ?", api.getId());
        }
        List<String> filteredViews = ORM.filterStrings(api.getViews());
        if (!filteredViews.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into api_views ( api_id, View ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(api.getId(), filteredViews));
        }
    }

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Pageable page) {
        final List<Api> apis = findByCriteria(apiCriteria, null);
        return getResultAsPage(page, apis);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        return findByCriteria(apiCriteria, null);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        return findByCriteria(apiCriteria, apiFieldExclusionFilter);
    }

    private List<Api> findByCriteria(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        LOGGER.debug("JdbcApiRepository.search({})", apiCriteria);
        final JdbcHelper.CollatingRowMapper<Api> rowMapper =
                new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "id");

        String projection ="av.*, a.id, a.name, a.description, a.version, a.deployed_at, a.created_at, a.updated_at, " +
                "a.visibility, a.lifecycle_state, a.picture";

        if (apiFieldExclusionFilter == null || !apiFieldExclusionFilter.isDefinition()) {
            projection += ", a.definition";
        }
        if (apiFieldExclusionFilter == null || !apiFieldExclusionFilter.isPicture()) {
            projection += ", a.picture";
        }

        final StringBuilder sbQuery = new StringBuilder("select ").append(projection).append(" from apis a ");
        sbQuery.append("left join api_views av on a.id = av.api_id ");

        if (apiCriteria != null) {
            if (!CollectionUtils.isEmpty(apiCriteria.getGroups())) {
                sbQuery.append("join api_groups ag on a.id = ag.api_id ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getLabel())) {
                sbQuery.append("join api_labels al on a.id = al.api_id ");
            }

            sbQuery.append("where 1 = 1 ");
            if (!CollectionUtils.isEmpty(apiCriteria.getGroups())) {
                sbQuery.append("and ag.group_id in (").append(ORM.buildInClause(apiCriteria.getGroups())).append(") ");
            }
            if (!CollectionUtils.isEmpty(apiCriteria.getIds())) {
                sbQuery.append("and a.id in (").append(ORM.buildInClause(apiCriteria.getIds())).append(") ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getLabel())) {
                sbQuery.append("and al.label = ? ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getName())) {
                sbQuery.append("and a.name = ? ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getState())) {
                sbQuery.append("and a.lifecycle_state = ? ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getVersion())) {
                sbQuery.append("and a.version = ? ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getView())) {
                sbQuery.append("and av.view = ? ");
            }
            if (!StringUtils.isEmpty(apiCriteria.getVisibility())) {
                sbQuery.append("and a.visibility = ? ");
            }
        }
        sbQuery.append("order by a.name");

        jdbcTemplate.query(sbQuery.toString(), (PreparedStatement ps) -> {
                    int lastIndex = 1;
                    if (apiCriteria != null) {
                        if (!CollectionUtils.isEmpty(apiCriteria.getGroups())) {
                            lastIndex = ORM.setArguments(ps, apiCriteria.getGroups(), lastIndex);
                        }
                        if (!CollectionUtils.isEmpty(apiCriteria.getIds())) {
                            lastIndex = ORM.setArguments(ps, apiCriteria.getIds(), lastIndex);
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getLabel())) {
                            ps.setString(lastIndex++, apiCriteria.getLabel());
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getName())) {
                            ps.setString(lastIndex++, apiCriteria.getName());
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getState())) {
                            ps.setString(lastIndex++, apiCriteria.getState().name());
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getVersion())) {
                            ps.setString(lastIndex++, apiCriteria.getVersion());
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getView())) {
                            ps.setString(lastIndex++, apiCriteria.getView());
                        }
                        if (!StringUtils.isEmpty(apiCriteria.getVisibility())) {
                            ps.setString(lastIndex, apiCriteria.getVisibility().name());
                        }
                    }
                }
                , rowMapper
        );
        final List<Api> apis = rowMapper.getRows();
        for (final Api api : apis) {
            addLabels(api);
            addGroups(api);
        }
        return apis;
    }
}