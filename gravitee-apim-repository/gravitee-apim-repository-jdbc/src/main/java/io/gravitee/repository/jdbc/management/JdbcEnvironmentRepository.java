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
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 *
 * @author njt
 */
@Repository
public class JdbcEnvironmentRepository extends JdbcAbstractCrudRepository<Environment, String> implements EnvironmentRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEnvironmentRepository.class);
    private final String ENVIRONMENT_DOMAIN_RESTRICTIONS;
    private final String ENVIRONMENT_HRIDS;

    JdbcEnvironmentRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "environments");
        ENVIRONMENT_DOMAIN_RESTRICTIONS = getTableNameFor("environment_domain_restrictions");
        ENVIRONMENT_HRIDS = getTableNameFor("environment_hrids");
    }

    @Override
    protected JdbcObjectMapper<Environment> buildOrm() {
        return JdbcObjectMapper
            .builder(Environment.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("cockpit_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(Environment item) {
        return item.getId();
    }

    @Override
    public Optional<Environment> findById(String id) throws TechnicalException {
        Optional<Environment> findById = super.findById(id);
        if (findById.isPresent()) {
            final Environment environment = findById.get();
            addDomainRestrictions(environment);
            addHrids(environment);
        }
        return findById;
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        Set<Environment> findAll = super.findAll();
        for (Environment env : findAll) {
            this.addDomainRestrictions(env);
            this.addHrids(env);
        }
        return findAll;
    }

    @Override
    public Environment create(Environment item) throws TechnicalException {
        super.create(item);
        storeDomainRestrictions(item, false);
        storeHrids(item, false);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public Environment update(Environment item) throws TechnicalException {
        super.update(item);
        storeDomainRestrictions(item, true);
        storeHrids(item, true);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + ENVIRONMENT_DOMAIN_RESTRICTIONS + " where environment_id = ?", id);
        jdbcTemplate.update("delete from " + ENVIRONMENT_HRIDS + " where environment_id = ?", id);
        super.delete(id);
    }

    private void addDomainRestrictions(Environment parent) {
        List<String> domainRestrictions = jdbcTemplate.queryForList(
            "select domain_restriction from " + ENVIRONMENT_DOMAIN_RESTRICTIONS + " where environment_id = ?",
            String.class,
            parent.getId()
        );
        parent.setDomainRestrictions(domainRestrictions);
    }

    private void addHrids(Environment environment) {
        List<String> hrids = jdbcTemplate.queryForList(
            "select hrid from " + ENVIRONMENT_HRIDS + " where environment_id = ? order by pos",
            String.class,
            environment.getId()
        );
        environment.setHrids(hrids);
    }

    private void storeDomainRestrictions(Environment environment, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + ENVIRONMENT_DOMAIN_RESTRICTIONS + " where environment_id = ?", environment.getId());
        }
        List<String> filteredDomainRestrictions = getOrm().filterStrings(environment.getDomainRestrictions());
        if (!filteredDomainRestrictions.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + ENVIRONMENT_DOMAIN_RESTRICTIONS + " (environment_id, domain_restriction) values ( ?, ? )",
                getOrm().getBatchStringSetter(environment.getId(), filteredDomainRestrictions)
            );
        }
    }

    private void storeHrids(Environment environment, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + ENVIRONMENT_HRIDS + " where environment_id = ?", environment.getId());
        }
        List<String> hrids = getOrm().filterStrings(environment.getHrids());
        if (!hrids.isEmpty()) {
            final List<Object[]> params = new ArrayList<>(hrids.size());

            for (int i = 0; i < hrids.size(); i++) {
                params.add(new Object[] { environment.getId(), hrids.get(i), i });
            }

            jdbcTemplate.batchUpdate("insert into " + ENVIRONMENT_HRIDS + " (environment_id, hrid, pos) values ( ?, ?, ? )", params);
        }
    }

    @Override
    public Set<Environment> findByOrganization(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcEnvironmentRepository.findByOrganization({})", organizationId);
        try {
            List<Environment> environments = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where organization_id = ?",
                getOrm().getRowMapper(),
                organizationId
            );
            for (Environment env : environments) {
                this.addDomainRestrictions(env);
                this.addHrids(env);
            }
            return new HashSet<>(environments);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find environments by organization:", ex);
            throw new TechnicalException("Failed to find environments by organization", ex);
        }
    }

    @Override
    public Set<Environment> findByOrganizationsAndHrids(Set<String> organizations, Set<String> hrids) throws TechnicalException {
        LOGGER.debug("findByOrganizationsAndHrids.findByHrids({}, {})", organizations, hrids);

        if (CollectionUtils.isEmpty(organizations) && CollectionUtils.isEmpty(hrids)) {
            return new HashSet<>();
        }

        final StringBuilder query = new StringBuilder(getOrm().getSelectAllSql())
            .append(" env")
            .append(" join ")
            .append(ENVIRONMENT_HRIDS)
            .append(" eh on env.id = eh.environment_id");

        final boolean first = getOrm().buildInCondition(true, query, "env.organization_id", organizations);
        getOrm().buildInCondition(first, query, "eh.hrid", hrids);

        try {
            List<Environment> environments = jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int idx = getOrm().setArguments(ps, organizations, 1);
                    getOrm().setArguments(ps, hrids, idx);
                },
                getOrm().getRowMapper()
            );
            for (Environment env : environments) {
                this.addDomainRestrictions(env);
                this.addHrids(env);
            }
            return new HashSet<>(environments);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find environments by organizations and hrids:", ex);
            throw new TechnicalException("Failed to find environments by organizations and hrids", ex);
        }
    }

    @Override
    public Optional<Environment> findByCockpit(String cockpitId) throws TechnicalException {
        LOGGER.debug("JdbcEnvironmentRepository.findByCockpit({})", cockpitId);
        try {
            List<Environment> environments = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where cockpit_id = ?",
                getOrm().getRowMapper(),
                cockpitId
            );

            final Optional<Environment> environment = environments.stream().findFirst();

            environment.ifPresent(
                env -> {
                    this.addDomainRestrictions(env);
                    this.addHrids(env);
                }
            );

            return environment;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find environments by cockpit:", ex);
            throw new TechnicalException("Failed to find environments by cockpit", ex);
        }
    }
}
