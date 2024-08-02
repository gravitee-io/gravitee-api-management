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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcMetadataRepository extends JdbcAbstractFindAllRepository<Metadata> implements MetadataRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMetadataRepository.class);

    JdbcMetadataRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "metadata");
    }

    @Override
    protected JdbcObjectMapper<Metadata> buildOrm() {
        return JdbcObjectMapper
            .builder(Metadata.class, this.tableName, "key")
            .updateSql(
                "update " +
                this.tableName +
                " set " +
                escapeReservedWord("key") +
                " = ?" +
                " , reference_type = ?" +
                " , reference_id = ?" +
                " , name = ?" +
                " , format = ?" +
                " , value = ?" +
                " , created_at = ? " +
                " , updated_at = ? " +
                " where " +
                escapeReservedWord("key") +
                " = ? " +
                "and reference_type = ? " +
                "and reference_id = ? "
            )
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, MetadataReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("format", Types.NVARCHAR, MetadataFormat.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Metadata create(final Metadata metadata) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.create({})", metadata);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(metadata));
            return findById(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create metadata", ex);
            throw new TechnicalException("Failed to create metadata", ex);
        }
    }

    @Override
    public Metadata update(final Metadata metadata) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.update({})", metadata);
        if (metadata == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(
                getOrm()
                    .buildUpdatePreparedStatementCreator(
                        metadata,
                        metadata.getKey(),
                        metadata.getReferenceType().name(),
                        metadata.getReferenceId()
                    )
            );
            return findById(metadata.getKey(), metadata.getReferenceId(), metadata.getReferenceType())
                .orElseThrow(() ->
                    new IllegalStateException(
                        format(
                            "No metadata found with id [%s, %s, %s]",
                            metadata.getKey(),
                            metadata.getReferenceId(),
                            metadata.getReferenceType()
                        )
                    )
                );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update metadata", ex);
            throw new TechnicalException("Failed to update metadata", ex);
        }
    }

    @Override
    public void delete(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.delete({}, {}, {})", key, referenceId, referenceType);
        try {
            jdbcTemplate.update(
                "delete from " +
                this.tableName +
                " where " +
                escapeReservedWord("key") +
                " = ? and reference_type = ? and reference_id = ? ",
                key,
                referenceType.name(),
                referenceId
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete metadata", ex);
            throw new TechnicalException("Failed to delete metadata", ex);
        }
    }

    @Override
    public Optional<Metadata> findById(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.findById({}, {}, {})", key, referenceId, referenceType);
        try {
            final List<Metadata> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where " + escapeReservedWord("key") + " = ? and reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                key,
                referenceType.name(),
                referenceId
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find metadata by id", ex);
            throw new TechnicalException("Failed to find metadata by id", ex);
        }
    }

    @Override
    public List<Metadata> findByKeyAndReferenceType(String key, MetadataReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.findByKeyAndReferenceType({}, {})", key, referenceType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where " + escapeReservedWord("key") + " = ? and reference_type = ?",
                getOrm().getRowMapper(),
                key,
                referenceType.name()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find metadata by key and reference type", ex);
            throw new TechnicalException("Failed to find metadata by key and reference type", ex);
        }
    }

    @Override
    public List<Metadata> findByReferenceType(final MetadataReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.findByReferenceType({})", referenceType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ?",
                getOrm().getRowMapper(),
                referenceType.name()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find metadata by reference type", ex);
            throw new TechnicalException("Failed to find metadata by reference type", ex);
        }
    }

    @Override
    public List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId)
        throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.findById({}, {})", referenceId, referenceType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                referenceType.name(),
                referenceId
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find metadata by reference type and reference id", ex);
            throw new TechnicalException("Failed to find metadata by reference type and reference id", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, MetadataReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcMetadataRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceId, referenceType);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select " + escapeReservedWord("key") + " from " + this.tableName + " where reference_type = ? and reference_id = ?",
                String.class,
                referenceType.name(),
                referenceId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + this.tableName + " where reference_type = ? and reference_id = ? ",
                    referenceType.name(),
                    referenceId
                );
            }

            LOGGER.debug("JdbcMetadataRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceId, referenceType);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete metadata for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete metadata by reference", ex);
        }
    }
}
