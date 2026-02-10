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
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.management.model.EntrypointReferenceType;
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
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcEntryPointRepository extends JdbcAbstractCrudRepository<Entrypoint, String> implements EntrypointRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEntryPointRepository.class);

    JdbcEntryPointRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "entrypoints");
    }

    @Override
    protected JdbcObjectMapper<Entrypoint> buildOrm() {
        return JdbcObjectMapper.builder(Entrypoint.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("target", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .addColumn("tags", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, EntrypointReferenceType.class)
            .build();
    }

    @Override
    protected String getId(Entrypoint item) {
        return item.getId();
    }

    @Override
    public Optional<Entrypoint> findByIdAndReference(final String id, String referenceId, EntrypointReferenceType referenceType)
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
            LOGGER.error("Failed to find {} entrypoint by id, referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException(
                "Failed to find " + getOrm().getTableName() + " entrypoint by id, referenceId and referenceType",
                ex
            );
        }
    }

    @Override
    public Set<Entrypoint> findByReference(String referenceId, EntrypointReferenceType referenceType) throws TechnicalException {
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
            LOGGER.error("Failed to find {} entrypoints referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " entrypoints by referenceId and referenceType", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(final String referenceId, final EntrypointReferenceType referenceType)
        throws TechnicalException {
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
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
            return rows;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete entrypoints by reference", ex);
        }
    }
}
