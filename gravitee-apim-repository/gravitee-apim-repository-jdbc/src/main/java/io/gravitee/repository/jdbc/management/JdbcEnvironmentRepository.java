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
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcEnvironmentRepository extends JdbcAbstractCrudRepository<Environment, String> implements EnvironmentRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEnvironmentRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper
        .builder(Environment.class, "environments", "id")
        .addColumn("id", Types.NVARCHAR, String.class)
        .addColumn("name", Types.NVARCHAR, String.class)
        .addColumn("description", Types.NVARCHAR, String.class)
        .addColumn("organization_id", Types.NVARCHAR, String.class)
        .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Environment item) {
        return item.getId();
    }

    @Override
    public Optional<Environment> findById(String id) throws TechnicalException {
        Optional<Environment> findById = super.findById(id);
        if (findById.isPresent()) {
            addDomainRestrictions(findById.get());
        }
        return findById;
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        Set<Environment> findAll = super.findAll();
        findAll.forEach(this::addDomainRestrictions);
        return findAll;
    }

    @Override
    public Environment create(Environment item) throws TechnicalException {
        super.create(item);
        storeDomainRestrictions(item, false);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public Environment update(Environment item) throws TechnicalException {
        super.update(item);
        storeDomainRestrictions(item, true);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from environment_domain_restrictions where environment_id = ?", id);
        super.delete(id);
    }

    private void addDomainRestrictions(Environment parent) {
        List<String> domainRestrictions = jdbcTemplate.queryForList(
            "select domain_restriction from environment_domain_restrictions where environment_id = ?",
            String.class,
            parent.getId()
        );
        parent.setDomainRestrictions(domainRestrictions);
    }

    private void storeDomainRestrictions(Environment environment, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from environment_domain_restrictions where environment_id = ?", environment.getId());
        }
        List<String> filteredDomainRestrictions = ORM.filterStrings(environment.getDomainRestrictions());
        if (!filteredDomainRestrictions.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into environment_domain_restrictions (environment_id, domain_restriction) values ( ?, ? )",
                ORM.getBatchStringSetter(environment.getId(), filteredDomainRestrictions)
            );
        }
    }

    @Override
    public Set<Environment> findByOrganization(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcEnvironmentRepository.findByOrganization({})", organizationId);
        try {
            List<Environment> environments = jdbcTemplate.query(
                "select * from environments where organization_id = ?",
                ORM.getRowMapper(),
                organizationId
            );
            return new HashSet<>(environments);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find environments by organization:", ex);
            throw new TechnicalException("Failed to find environments by organization", ex);
        }
    }
}
