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

import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcInstallationRepository extends JdbcAbstractCrudRepository<Installation, String> implements InstallationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcInstallationRepository.class);
    private final String INSTALLATION_INFORMATIONS;

    JdbcInstallationRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "installation");
        INSTALLATION_INFORMATIONS = getTableNameFor("installation_informations");
    }

    @Override
    protected JdbcObjectMapper<Installation> buildOrm() {
        return JdbcObjectMapper.builder(Installation.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(Installation item) {
        return item.getId();
    }

    private static final JdbcHelper.ChildAdder<Installation> CHILD_ADDER = (Installation parent, ResultSet rs) -> {
        Map<String, String> additionalInformation = parent.getAdditionalInformation();
        if (additionalInformation == null) {
            additionalInformation = new HashMap<>();
            parent.setAdditionalInformation(additionalInformation);
        }
        if (rs.getString("information_key") != null) {
            additionalInformation.put(rs.getString("information_key"), rs.getString("information_value"));
        }
    };

    @Override
    public Optional<Installation> find() throws TechnicalException {
        LOGGER.debug("JdbcInstallationRepository.find()");
        try {
            JdbcHelper.CollatingRowMapper<Installation> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            jdbcTemplate.query(
                getOrm().getSelectAllSql() + " i left join " + INSTALLATION_INFORMATIONS + " ii on i.id = ii.installation_id",
                rowMapper
            );
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find installation", ex);
            throw new TechnicalException("Failed to find installation", ex);
        }
    }

    @Override
    public Optional<Installation> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcInstallationRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Installation> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " i left join " +
                    INSTALLATION_INFORMATIONS +
                    " ii on i.id = ii.installation_id where i.id = ?",
                rowMapper,
                id
            );
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find installation by id", ex);
            throw new TechnicalException("Failed to find installation by id", ex);
        }
    }

    @Override
    public Installation create(final Installation installation) throws TechnicalException {
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(installation));
            storeInstallationInformations(installation, false);
            return findById(installation.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create installation", ex);
            throw new TechnicalException("Failed to create installation", ex);
        }
    }

    @Override
    public Installation update(final Installation installation) throws TechnicalException {
        if (installation == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(installation, installation.getId()));
            storeInstallationInformations(installation, true);
            return findById(installation.getId()).orElseThrow(() ->
                new IllegalStateException(format("No installation found with id [%s]", installation.getId()))
            );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update installation:", ex);
            throw new TechnicalException("Failed to update installation", ex);
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + INSTALLATION_INFORMATIONS + " where installation_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    @Override
    public Set<Installation> findAll() throws TechnicalException {
        LOGGER.debug("JdbcInstallationRepository.findAll()");
        try {
            JdbcHelper.CollatingRowMapper<Installation> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            jdbcTemplate.query(
                getOrm().getSelectAllSql() + " i left join " + INSTALLATION_INFORMATIONS + " ii on i.id = ii.installation_id",
                rowMapper
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all installations:", ex);
            throw new TechnicalException("Failed to find all installations", ex);
        }
    }

    private void storeInstallationInformations(Installation installation, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + INSTALLATION_INFORMATIONS + " where installation_id = ?", installation.getId());
        }
        if (installation.getAdditionalInformation() != null) {
            List<Map.Entry<String, String>> list = new ArrayList<>(installation.getAdditionalInformation().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + INSTALLATION_INFORMATIONS + " ( installation_id, information_key, information_value ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, installation.getId());
                        ps.setString(2, list.get(i).getKey());
                        ps.setString(3, list.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return list.size();
                    }
                }
            );
        }
    }
}
