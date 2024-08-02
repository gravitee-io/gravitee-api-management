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

import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.management.model.TenantReferenceType;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcTenantRepository extends JdbcAbstractCrudRepository<Tenant, String> implements TenantRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcTenantRepository.class);

    JdbcTenantRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "tenants");
    }

    @Override
    protected JdbcObjectMapper<Tenant> buildOrm() {
        return JdbcObjectMapper
            .builder(Tenant.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, TenantReferenceType.class)
            .build();
    }

    @Override
    protected String getId(Tenant item) {
        return item.getId();
    }

    @Override
    public Optional<Tenant> findByIdAndReference(final String id, String referenceId, TenantReferenceType referenceType)
        throws TechnicalException {
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " t where id = ? and reference_id = ? and reference_type = ? ",
                    getOrm().getRowMapper(),
                    id,
                    referenceId,
                    referenceType.name()
                )
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} tenant by id, referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " tenant by id, referenceId and referenceType", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, TenantReferenceType referenceType)
        throws TechnicalException {
        try {
            LOGGER.debug("JdbcTenantRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceType, referenceId);
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ? ",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            LOGGER.debug("JdbcTenantRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceType, referenceId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete tenants for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete tenants by reference", ex);
        }
    }

    @Override
    public Set<Tenant> findByReference(String referenceId, TenantReferenceType referenceType) throws TechnicalException {
        try {
            return new HashSet<>(
                jdbcTemplate.query(
                    getOrm().getSelectAllSql() + " t where reference_id = ? and reference_type = ? ",
                    getOrm().getRowMapper(),
                    referenceId,
                    referenceType.name()
                )
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} tenants referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " tenants by referenceId and referenceType", ex);
        }
    }
}
