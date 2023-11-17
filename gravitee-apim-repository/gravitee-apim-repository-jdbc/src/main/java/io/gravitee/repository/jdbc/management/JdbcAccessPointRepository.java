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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
@Slf4j
public class JdbcAccessPointRepository extends JdbcAbstractCrudRepository<AccessPoint, String> implements AccessPointRepository {

    JdbcAccessPointRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "access_points");
    }

    @Override
    protected JdbcObjectMapper<AccessPoint> buildOrm() {
        return JdbcObjectMapper
            .builder(AccessPoint.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, AccessPointReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("target", Types.NVARCHAR, AccessPointTarget.class)
            .addColumn("host", Types.NVARCHAR, String.class)
            .addColumn("secured", Types.BOOLEAN, boolean.class)
            .addColumn("overriding", Types.BOOLEAN, boolean.class)
            .build();
    }

    @Override
    protected String getId(final AccessPoint item) {
        return item.getId();
    }

    @Override
    public Optional<AccessPoint> findByHost(final String host) throws TechnicalException {
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " a where host = ?", getOrm().getRowMapper(), host).stream().findFirst();
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by host", ex);
        }
    }

    @Override
    public List<AccessPoint> findByTarget(final AccessPointTarget target) throws TechnicalException {
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " t where target = ?", getOrm().getRowMapper(), target.name());
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by target", ex);
        }
    }

    @Override
    public List<AccessPoint> findByReferenceAndTarget(
        final AccessPointReferenceType referenceType,
        final String referenceId,
        final AccessPointTarget target
    ) throws TechnicalException {
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " t where reference_id = ? and reference_type = ? and target = ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name(),
                target.name()
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by reference and target", ex);
        }
    }

    @Override
    public void deleteByReference(final AccessPointReferenceType referenceType, final String referenceId) throws TechnicalException {
        try {
            jdbcTemplate.update(
                "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                referenceId,
                referenceType.name()
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete access points by reference", ex);
        }
    }
}
