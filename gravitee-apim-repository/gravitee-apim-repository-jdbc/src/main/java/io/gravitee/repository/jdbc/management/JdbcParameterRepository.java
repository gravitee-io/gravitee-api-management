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
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

/**
 * @author njt
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
@Repository
public class JdbcParameterRepository extends JdbcAbstractFindAllRepository<Parameter> implements ParameterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcParameterRepository.class);

    JdbcParameterRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "parameters");
    }

    @Override
    protected JdbcObjectMapper<Parameter> buildOrm() {
        return JdbcObjectMapper
            .builder(Parameter.class, this.tableName)
            .updateSql(
                "update " +
                this.tableName +
                " set " +
                escapeReservedWord("key") +
                " = ?" +
                " , reference_type = ?" +
                " , reference_id = ?" +
                " , value = ?" +
                " where " +
                escapeReservedWord("key") +
                " = ? " +
                "and reference_type = ? " +
                "and reference_id = ? "
            )
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, ParameterReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .build();
    }

    //    @Override
    //    protected String defineTableName() {
    //        return "parameters";
    //    }

    @Override
    public Parameter create(Parameter parameter) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.create({})", parameter);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(parameter));
            return findById(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create parameter", ex);
            throw new TechnicalException("Failed to create parameter", ex);
        }
    }

    @Override
    public Parameter update(Parameter parameter) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.update({})", parameter);
        if (parameter == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            final PreparedStatementCreator psc = getOrm()
                .buildUpdatePreparedStatementCreator(
                    parameter,
                    parameter.getKey(),
                    parameter.getReferenceType().name(),
                    parameter.getReferenceId()
                );
            jdbcTemplate.update(psc);

            return findById(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType())
                .orElseThrow(() ->
                    new IllegalStateException(
                        format(
                            "No parameter found with id [%s, %s, %s]",
                            parameter.getKey(),
                            parameter.getReferenceId(),
                            parameter.getReferenceType()
                        )
                    )
                );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update parameter", ex);
            throw new TechnicalException("Failed to update parameter", ex);
        }
    }

    @Override
    public void delete(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.delete({}, {}, {})", key, referenceId, referenceType);
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
            LOGGER.error("Failed to delete parameter", ex);
            throw new TechnicalException("Failed to delete parameter", ex);
        }
    }

    @Override
    public Optional<Parameter> findById(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findById({}, {}, {})", key, referenceId, referenceType);
        try {
            final List<Parameter> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where " + escapeReservedWord("key") + " = ? and reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                key,
                referenceType.name(),
                referenceId
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find parameter by id", ex);
            throw new TechnicalException("Failed to find parameter by id", ex);
        }
    }

    @Override
    public List<Parameter> findByKeys(List<String> keys, String referenceId, ParameterReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findByKeys({}, {}, {})", keys, referenceId, referenceType);
        try {
            if (isEmpty(keys)) {
                return Collections.emptyList();
            }
            List<Parameter> parameters = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " where reference_id = ? and reference_type = ? and " +
                escapeReservedWord("key") +
                " in ( " +
                getOrm().buildInClause(keys) +
                " )",
                (PreparedStatement ps) -> {
                    ps.setString(1, referenceId);
                    ps.setString(2, referenceType.name());
                    getOrm().setArguments(ps, keys, 3);
                },
                getOrm().getRowMapper()
            );
            return new ArrayList<>(parameters);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find parameters by keys:", ex);
            throw new TechnicalException("Failed to find parameters by keys", ex);
        }
    }

    @Override
    public List<Parameter> findAll(String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findAll({}, {})", referenceId, referenceType);
        try {
            List<Parameter> parameters = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_id = ? and reference_type = ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name()
            );
            return new ArrayList<>(parameters);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all parameters :", ex);
            throw new TechnicalException("Failed to find all parameters", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, ParameterReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceId, referenceType);
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

            LOGGER.debug("JdbcParameterRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceId, referenceType);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete parameter for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete parameter by reference", ex);
        }
    }
}
