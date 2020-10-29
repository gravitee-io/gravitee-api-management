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

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@Repository
public class JdbcOrganizationRepository extends JdbcAbstractCrudRepository<Organization, String> implements OrganizationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcOrganizationRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Organization.class, "organizations", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Organization item) {
        return item.getId();
    }

    @Override
    public Optional<Organization> findById(String id) throws TechnicalException {
        Optional<Organization> findById = super.findById(id);
        if (findById.isPresent()) {
            final Organization organization = findById.get();
            addDomainRestrictions(organization);
            addHrids(organization);
        }
        return findById;
    }

    @Override
    public Organization create(Organization item) throws TechnicalException {
        super.create(item);
        storeDomainRestrictions(item, false);
        storeHrids(item, false);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public Organization update(Organization item) throws TechnicalException {
        super.update(item);
        storeDomainRestrictions(item, true);
        storeHrids(item, true);
        return findById(item.getId()).orElse(null);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from organization_domain_restrictions where organization_id = ?", id);
        jdbcTemplate.update("delete from organization_hrids where organization_id = ?", id);
        super.delete(id);
    }

    @Override
    public Long count() throws TechnicalException {
        try {
            return jdbcTemplate.queryForObject("select count(*) from organizations o", Long.class);
        } catch (Exception e) {
            LOGGER.error("An error occurred when counting organizations", e);
            throw new TechnicalException("An error occurred when counting organization");
        }
    }

    private void addDomainRestrictions(Organization parent) {
        List<String> domainRestrictions = jdbcTemplate.queryForList("select domain_restriction from organization_domain_restrictions where organization_id = ?", String.class, parent.getId());
        parent.setDomainRestrictions(domainRestrictions);
    }

    private void addHrids(Organization parent) {
        List<String> hrids = jdbcTemplate.queryForList("select hrid from organization_hrids where organization_id = ? order by pos", String.class, parent.getId());
        parent.setHrids(hrids);
    }

    private void storeDomainRestrictions(Organization organization, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from organization_domain_restrictions where organization_id = ?", organization.getId());
        }
        List<String> filteredDomainRestrictions = ORM.filterStrings(organization.getDomainRestrictions());
        if (!filteredDomainRestrictions.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into organization_domain_restrictions (organization_id, domain_restriction) values ( ?, ? )"
                    , ORM.getBatchStringSetter(organization.getId(), filteredDomainRestrictions));
        }
    }

    private void storeHrids(Organization organization, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from organization_hrids where organization_id = ?", organization.getId());
        }
        List<String> hrids = ORM.filterStrings(organization.getHrids());
        if (!hrids.isEmpty()) {
            final List<Object[]> params = new ArrayList<>(hrids.size());

            for (int i = 0; i < hrids.size(); i++) {
                params.add(new Object[]{organization.getId(), hrids.get(i), i});
            }

            jdbcTemplate.batchUpdate("insert into organization_hrids (organization_id, hrid, pos) values ( ?, ?, ? )", params);
        }
    }
}