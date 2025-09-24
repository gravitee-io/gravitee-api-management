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

import static io.gravitee.repository.jdbc.utils.CollectionUtils.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.model.JdbcIntegration;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Integration;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class JdbcIntegrationRepository extends JdbcAbstractCrudRepository<JdbcIntegration, String> implements IntegrationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcIntegrationRepository.class);
    private final String INTEGRATION_GROUPS;

    JdbcIntegrationRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "integrations");
        INTEGRATION_GROUPS = getTableNameFor("integration_groups");
    }

    @Override
    public Page<Integration> findAllByEnvironment(String environmentId, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcIntegrationRepository.findAllByEnvironment({}, {})", environmentId, pageable);
        final List<Integration> integrations;
        try {
            integrations = jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " where environment_id = ? order by updated_at desc",
                    getOrm().getRowMapper(),
                    environmentId
                )
                .stream()
                .flatMap(this::toEntityIntegration)
                .toList();

            integrations.forEach(this::addGroups);
        } catch (final Exception ex) {
            final String message = "Failed to find integrations of environment: " + environmentId;
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
        return getResultAsPage(pageable, integrations);
    }

    @Override
    public Page<Integration> findAllByEnvironmentAndGroups(
        String environmentId,
        Collection<String> integrationIds,
        Collection<String> groups,
        Pageable pageable
    ) throws TechnicalException {
        LOGGER.debug("JdbcIntegrationRepository.findAllByEnvironment({}, {}, {})", environmentId, groups, pageable);
        List<Integration> integrations;
        try {
            String query = "%s where environment_id = ? order by updated_at desc".formatted(getOrm().getSelectAllSql());
            integrations = jdbcTemplate
                .query(query, getOrm().getRowMapper(), environmentId)
                .stream()
                .flatMap(this::toEntityIntegration)
                .toList();

            integrations = integrations
                .stream()
                .peek(this::addGroups)
                .filter(
                    integration ->
                        integrationIds.contains(integration.getId()) || stream(integration.getGroups()).anyMatch(groups::contains)
                )
                .toList();
        } catch (final Exception ex) {
            final String message =
                "Failed to find integrations of environment: " + environmentId + " and groups: " + groups + ": " + ex.getMessage();
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
        return getResultAsPage(pageable, integrations);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcIntegrationRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + INTEGRATION_GROUPS + " where integration_id IN ( " + getOrm().buildInClause(rows) + ")",
                    rows.toArray()
                );
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            return rows;
        } catch (final Exception ex) {
            final String message = "Failed to find integrations of environment: " + environmentId;
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    public Optional<Integration> findByIntegrationId(String id) throws TechnicalException {
        var integration = super
            .findById(id)
            .flatMap(jdbcIntegration -> {
                try {
                    return Optional.of(jdbcIntegration.toIntegration(getGroups(jdbcIntegration.getId())));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to convert Integration from db to Integration id = " + id, e);
                    return Optional.empty();
                }
            });
        integration.ifPresent(this::addGroups);
        return integration;
    }

    @Override
    public Optional<JdbcIntegration> findById(String id) throws TechnicalException {
        return super.findById(id);
    }

    private void addGroups(Integration parent) {
        List<String> groups = getGroups(parent.getId());
        if (!groups.isEmpty()) {
            parent.setGroups(new HashSet<>(groups));
        }
    }

    private List<String> getGroups(String integrationId) {
        return jdbcTemplate.queryForList(
            "select group_id from " + INTEGRATION_GROUPS + " where integration_id = ?",
            String.class,
            integrationId
        );
    }

    @Override
    public Integration create(final Integration integration) throws TechnicalException {
        storeGroups(integration, false);
        try {
            return super.create(JdbcIntegration.fromIntegration(integration)).toIntegration(integration.getGroups());
        } catch (JsonProcessingException e) {
            throw new TechnicalException("Failed to create Integration name = " + integration.getName(), e);
        }
    }

    @Override
    public Integration update(final Integration integration) throws TechnicalException {
        storeGroups(integration, true);
        try {
            return super.update(JdbcIntegration.fromIntegration(integration)).toIntegration(integration.getGroups());
        } catch (JsonProcessingException e) {
            throw new TechnicalException("Failed to update Integration fid = " + integration.getId(), e);
        }
    }

    private void storeGroups(Integration integration, boolean deleteExistingGroups) {
        if (integration == null) {
            return;
        }
        if (deleteExistingGroups) {
            jdbcTemplate.update("delete from " + INTEGRATION_GROUPS + " where integration_id = ?", integration.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(integration.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + INTEGRATION_GROUPS + " ( integration_id, group_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(integration.getId(), filteredGroups)
            );
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + INTEGRATION_GROUPS + " where integration_id = ?", id);
        super.delete(id);
    }

    @Override
    protected String getId(JdbcIntegration item) {
        return item.getId();
    }

    @Override
    protected JdbcObjectMapper<JdbcIntegration> buildOrm() {
        return JdbcObjectMapper.builder(JdbcIntegration.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("provider", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("wellKnownUrls", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    private Stream<Integration> toEntityIntegration(JdbcIntegration jdbcIntegration) {
        try {
            return Stream.of(jdbcIntegration.toIntegration(getGroups(jdbcIntegration.getId())));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Integration from db id = {}", jdbcIntegration.getName());
            return Stream.empty();
        }
    }
}
