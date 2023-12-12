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

import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.License;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLicenseRepository extends JdbcAbstractFindAllRepository<License> implements LicenseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcLicenseRepository.class);

    JdbcLicenseRepository(@Value("${management.jdbc.prefix:}") String tablePrefix, @Autowired JdbcTemplate jdbcTemplate) {
        super(tablePrefix, "licenses");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected JdbcObjectMapper<License> buildOrm() {
        return JdbcObjectMapper
            .builder(License.class, this.tableName, "reference_id")
            .updateSql(
                "update " +
                this.tableName +
                " set " +
                " reference_id = ?" +
                " , reference_type = ?" +
                " , license = ?" +
                " , created_at = ? " +
                " , updated_at = ? " +
                WHERE_CLAUSE +
                " reference_id = ? " +
                " and reference_type = ? "
            )
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, License.ReferenceType.class)
            .addColumn("license", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Optional<License> findById(String referenceId, License.ReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcLicenseRepository.findById({}, {})", referenceId, referenceType);
        try {
            List<License> result = jdbcTemplate.query(
                "select ol.* from " + this.tableName + " ol where ol.reference_id = ? and ol.reference_type = ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name()
            );
            LOGGER.debug("JdbcLicenseRepository.findById({}, {}) = {}", referenceId, referenceType, result);
            return result.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find licenses with reference id and type: {} {}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to find licenses by id", ex);
        }
    }

    @Override
    public License create(License license) throws TechnicalException {
        LOGGER.debug("JdbcLicenseRepository.create({})", license);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(license));
            return findById(license.getReferenceId(), license.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create license", ex);
            throw new TechnicalException("Failed to create license", ex);
        }
    }

    @Override
    public License update(License license) throws TechnicalException {
        LOGGER.debug("JdbcLicenseRepository.update({})", license);
        try {
            int rows = jdbcTemplate.update(
                getOrm().buildUpdatePreparedStatementCreator(license, license.getReferenceId(), license.getReferenceType().name())
            );
            if (rows == 0) {
                throw new IllegalStateException("Unable to update " + getOrm().getTableName());
            }
            return findById(license.getReferenceId(), license.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to update license: {} {}", license.getReferenceId(), license.getReferenceType(), ex);
            throw new TechnicalException("Failed to update license", ex);
        }
    }

    @Override
    public void delete(String referenceId, License.ReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcLicenseRepository.delete({} {})", referenceId, referenceType);
        try {
            jdbcTemplate.update(
                "delete from " + this.tableName + " where reference_id = ? and reference_type = ?",
                referenceId,
                referenceType.name()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete {} license:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to delete license", ex);
        }
    }
}
