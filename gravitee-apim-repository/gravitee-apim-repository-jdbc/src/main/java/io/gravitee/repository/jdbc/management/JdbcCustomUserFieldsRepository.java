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
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.MetadataFormat;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcCustomUserFieldsRepository extends JdbcAbstractFindAllRepository<CustomUserField> implements CustomUserFieldsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCustomUserFieldsRepository.class);

    private final String CUSTOM_USER_FIELDS_VALUES;

    JdbcCustomUserFieldsRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "custom_user_fields");
        CUSTOM_USER_FIELDS_VALUES = getTableNameFor("custom_user_fields_values");
    }

    @Override
    protected JdbcObjectMapper<CustomUserField> buildOrm() {
        return JdbcObjectMapper
            .builder(CustomUserField.class, this.tableName, "key")
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, CustomUserFieldReferenceType.class)
            .addColumn("label", Types.NVARCHAR, String.class)
            .addColumn("format", Types.NVARCHAR, MetadataFormat.class) // use MetadataFormat because the value will be stored into the Metadata table
            .addColumn("required", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .updateSql(
                "UPDATE  " +
                this.tableName +
                " set " +
                escapeReservedWord("key") +
                " = ?, " +
                " reference_id = ?, " +
                " reference_type = ?, " +
                " label = ?, " +
                " format = ?, " +
                " required = ?, " +
                " created_at = ?, " +
                " updated_at = ? " +
                " WHERE " +
                escapeReservedWord("key") +
                " = ? AND reference_id = ?  AND reference_type = ? "
            )
            .build();
    }

    private static final JdbcHelper.ChildAdder<CustomUserField> CHILD_ADDER = (CustomUserField field, ResultSet rs) -> {
        List<String> values = field.getValues();
        if (values == null) {
            values = new ArrayList<>();
            field.setValues(values);
        }
        if (rs.getString("value") != null) {
            values.add(rs.getString("value"));
        }
    };

    private void deleteValues(String key, String refId, CustomUserFieldReferenceType refType) {
        jdbcTemplate.update(
            "delete from " +
            CUSTOM_USER_FIELDS_VALUES +
            " where " +
            escapeReservedWord("key") +
            " = ? and reference_id = ? and reference_type = ?",
            key,
            refId,
            refType.name()
        );
    }

    private void storeValues(CustomUserField field, boolean deleteFirst) {
        if (deleteFirst) {
            deleteValues(field.getKey(), field.getReferenceId(), field.getReferenceType());
        }

        if (field.getValues() != null && !field.getValues().isEmpty()) {
            List<String> entries = field.getValues();
            jdbcTemplate.batchUpdate(
                "insert into " +
                CUSTOM_USER_FIELDS_VALUES +
                " ( " +
                escapeReservedWord("key") +
                ", reference_id, reference_type, value ) values ( ?, ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, field.getKey());
                        ps.setString(2, field.getReferenceId());
                        ps.setString(3, field.getReferenceType().name());
                        ps.setString(4, entries.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }

    @Override
    public Optional<CustomUserField> findById(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.findById({}, {})", key, refId);
        try {
            JdbcHelper.CollatingRowMapper<CustomUserField> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "key"
            );
            jdbcTemplate.query(
                "select c.*, cv.value from " +
                this.tableName +
                " c " +
                " left join " +
                CUSTOM_USER_FIELDS_VALUES +
                " cv on " +
                " c." +
                escapeReservedWord("key") +
                " = cv." +
                escapeReservedWord("key") +
                " and c.reference_id = cv.reference_id and c.reference_type = cv.reference_type " +
                " where c." +
                escapeReservedWord("key") +
                " = ? AND c.reference_id = ? AND c.reference_type = ?",
                rowMapper,
                key,
                refId,
                refType.name()
            );
            LOGGER.debug("JdbcCustomUserFieldsRepository.findById({}, {}, {})", key, refId, refType, rowMapper.getRows());
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find custom user fields for key and refId: {}, {}/{})", key, refId, refType, ex);
            throw new TechnicalException("Failed to find custom user fields by key and refId", ex);
        }
    }

    @Override
    public List<CustomUserField> findByReferenceIdAndReferenceType(String refId, CustomUserFieldReferenceType refType)
        throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.findByReferenceIdAndReferenceType({}, {})", refId, refType);
        try {
            JdbcHelper.CollatingRowMapper<CustomUserField> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "key"
            );
            jdbcTemplate.query(
                "select c.*, cValues.value from " +
                this.tableName +
                " c " +
                " left join " +
                CUSTOM_USER_FIELDS_VALUES +
                " cValues on c." +
                escapeReservedWord("key") +
                " = cValues." +
                escapeReservedWord("key") +
                " and " +
                " c.reference_id = cValues.reference_id and c.reference_type = cValues.reference_type " +
                " where c.reference_id = ? and  c.reference_type = ? ",
                rowMapper,
                refId,
                refType.name()
            );
            LOGGER.debug(
                "JdbcCustomUserFieldsRepository.findByReferenceIdAndReferenceType({}, {}) = {}",
                refId,
                refType,
                rowMapper.getRows()
            );
            return rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find custom user fields for refId: {}/{}", refId, refType, ex);
            throw new TechnicalException("Failed to find custom user fields by reference", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, CustomUserFieldReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.deleteByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            final var keys = jdbcTemplate.queryForList(
                "select " + escapeReservedWord("key") + " from " + tableName + " where reference_id = ? and  reference_type = ? ",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!keys.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
                jdbcTemplate.update(
                    "delete from " + CUSTOM_USER_FIELDS_VALUES + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            LOGGER.debug("JdbcCustomUserFieldsRepository.deleteByReferenceIdAndReferenceType({}, {}) - Done", referenceId, referenceType);
            return keys;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete custom user fields for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete custom user fields by reference", ex);
        }
    }

    @Override
    public void delete(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.delete({}, {}, {})", key, refId, refType);
        try {
            deleteValues(key, refId, refType);
            jdbcTemplate.update(
                "delete from " +
                this.tableName +
                " where " +
                escapeReservedWord("key") +
                " = ? AND reference_id = ? AND reference_type = ?",
                key,
                refId,
                refType.name()
            );
            LOGGER.debug("JdbcCustomUserFieldsRepository.delete({}, {}, {})", key, refId, refType);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete custom user fields for key and refId: {}, {}, {})", key, refId, refType, ex);
            throw new TechnicalException("Failed to delete custom user fields by key and refId", ex);
        }
    }

    @Override
    public CustomUserField create(CustomUserField field) throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.create({})", field);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(field));
            storeValues(field, true);
            return findById(field.getKey(), field.getReferenceId(), field.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create custom user field", ex);
            throw new TechnicalException("Failed to create custom user field", ex);
        }
    }

    @Override
    public CustomUserField update(CustomUserField field) throws TechnicalException {
        LOGGER.debug("JdbcCustomUserFieldsRepository.update({})", field);
        try {
            jdbcTemplate.update(
                getOrm().buildUpdatePreparedStatementCreator(field, field.getKey(), field.getReferenceId(), field.getReferenceType().name())
            );
            storeValues(field, true);
            return findById(field.getKey(), field.getReferenceId(), field.getReferenceType())
                .orElseThrow(() ->
                    new IllegalStateException(
                        format(
                            "No CustomUserField found with id [%s, %s, %s]",
                            field.getKey(),
                            field.getReferenceId(),
                            field.getReferenceType()
                        )
                    )
                );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to create custom user field", ex);
            throw new TechnicalException("Failed to create custom user field", ex);
        }
    }
}
