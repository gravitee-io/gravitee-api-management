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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;
import static java.lang.String.format;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcColumn;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProvider;
import io.gravitee.repository.management.model.DictionaryTrigger;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcDictionaryRepository extends JdbcAbstractCrudRepository<Dictionary, String> implements DictionaryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDictionaryRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Dictionary.class, "dictionaries", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, DictionaryType.class)
            .addColumn("state", Types.NVARCHAR, LifecycleState.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("deployed_at", Types.TIMESTAMP, Date.class)
            .build();

    private static final JdbcHelper.ChildAdder<Dictionary> CHILD_ADDER = (Dictionary parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("k") != null) {
            properties.put(rs.getString("k"), rs.getString("v"));
        }
    };


    private class Rm implements RowMapper<Dictionary> {
        @Override
        public Dictionary mapRow(ResultSet rs, int i) throws SQLException {
            Dictionary dictionary = new Dictionary();
            ORM.setFromResultSet(dictionary, rs);

            String providerType = rs.getString("provider_type");
            String providerConfiguration = rs.getString("provider_configuration");
            if ((providerType != null) || (providerConfiguration != null)) {
                DictionaryProvider provider = new DictionaryProvider();
                provider.setType(providerType);
                provider.setConfiguration(providerConfiguration);
                dictionary.setProvider(provider);
            }

            int triggerRate = rs.getInt("trigger_rate");
            String triggerUnit = rs.getString("trigger_unit");
            if ((triggerUnit != null) || (triggerRate != 0)) {
                DictionaryTrigger trigger = new DictionaryTrigger();
                trigger.setRate(triggerRate);
                trigger.setUnit(TimeUnit.valueOf(triggerUnit));
                dictionary.setTrigger(trigger);
            }

            return dictionary;
        }
    }

    private final Rm mapper = new Rm();

    private static class Psc implements PreparedStatementCreator {

        private final String sql;
        private final Dictionary dictionary;
        private final Object[] ids;

        public Psc(String sql, Dictionary dictionary, Object... ids) {
            this.sql = sql;
            this.dictionary = dictionary;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            LOGGER.debug("SQL: {}", sql);
            LOGGER.debug("dictionary: {}", dictionary);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = ORM.setStatementValues(stmt, dictionary, 1);
            stmt.setString(idx++, dictionary.getProvider() == null ? null : dictionary.getProvider().getType());
            stmt.setString(idx++, dictionary.getProvider() == null ? null : dictionary.getProvider().getConfiguration());
            stmt.setInt(idx++, dictionary.getTrigger() == null ? 0 : (int) dictionary.getTrigger().getRate());
            stmt.setString(idx++, dictionary.getTrigger() == null ? null : dictionary.getTrigger().getUnit().name());

            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
    }

    private static String buildInsertStatement() {
        final StringBuilder builder = new StringBuilder("insert into dictionaries (");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
        }
        builder.append(", provider_type");
        builder.append(", provider_configuration");
        builder.append(", trigger_rate");
        builder.append(", trigger_unit");
        builder.append(" ) values ( ");
        first = true;
        for (int i = 0; i < ORM.getColumns().size(); i++) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(", ?");
        builder.append(", ?");
        builder.append(", ?");
        builder.append(", ?");
        builder.append(" )");
        return builder.toString();
    }

    private static final String INSERT_SQL = buildInsertStatement();

    private static String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update dictionaries set ");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
            builder.append(" = ?");
        }
        builder.append(", provider_type = ?");
        builder.append(", provider_configuration = ?");
        builder.append(", trigger_rate = ?");
        builder.append(", trigger_unit = ?");

        builder.append(" where id = ?");
        return builder.toString();
    }

    private static final String UPDATE_SQL = buildUpdateStatement();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Dictionary item) {
        return item.getId();
    }

    @Override
    protected RowMapper<Dictionary> getRowMapper() {
        return mapper;
    }

    @Override
    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(Dictionary dictionary) {
        return new Psc(UPDATE_SQL, dictionary, dictionary.getId());
    }

    @Override
    protected PreparedStatementCreator buildInsertPreparedStatementCreator(Dictionary dictionary) {
        return new Psc(INSERT_SQL, dictionary);
    }

    private void storeProperties(Dictionary dictionary, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from dictionary_property where dictionary_id = ?", dictionary.getId());
        }
        if (dictionary.getProperties() != null && !dictionary.getProperties().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(dictionary.getProperties().entrySet());
            jdbcTemplate.batchUpdate("insert into dictionary_property ( dictionary_id, k, v ) values ( ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setString(1, dictionary.getId());
                            ps.setString(2, entries.get(i).getKey());
                            ps.setString(3, entries.get(i).getValue());
                        }

                        @Override
                        public int getBatchSize() {
                            return entries.size();
                        }
                    });
        }
    }

    @Override
    public Optional<Dictionary> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcDictionaryRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Dictionary> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query("select * from dictionaries d left join dictionary_property dp on d.id = dp.dictionary_id where d.id = ?"
                    , rowMapper
                    , id
            );
            Optional<Dictionary> result = rowMapper.getRows().stream().findFirst();
            LOGGER.debug("JdbcDictionaryRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find dictionary by id:", ex);
            throw new TechnicalException("Failed to find dictionary by id", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from dictionary_property where dictionary_id = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }

    @Override
    public Dictionary create(Dictionary item) throws TechnicalException {
        LOGGER.debug("JdbcDictionaryRepository.create({})", item);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(item));
            storeProperties(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create dictionary", ex);
            throw new TechnicalException("Failed to create dictionary", ex);
        }
    }

    @Override
    public Dictionary update(final Dictionary dictionary) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.update({})", dictionary);
        if (dictionary == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(buildUpdatePreparedStatementCreator(dictionary));
            storeProperties(dictionary, true);
            return findById(dictionary.getId()).orElseThrow(() ->
                    new IllegalStateException(format("No dictionary found with id [%s]", dictionary.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update dictionary", ex);
            throw new TechnicalException("Failed to update dictionary", ex);
        }
    }

    @Override
    public Set<Dictionary> findAllByEnvironment(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcDictionaryRepository.findAllByEnvironment({})", environmentId);
        try {
            JdbcHelper.CollatingRowMapper<Dictionary> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query("select * from dictionaries d left join dictionary_property dp on d.id = dp.dictionary_id where d.environment_id = ?"
                    , rowMapper
                    , environmentId
            );
            Set<Dictionary> result = new HashSet<>(rowMapper.getRows());
            LOGGER.debug("JdbcDictionaryRepository.findAllByEnvironment({}) = {}", environmentId, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find dictionary by environment:", ex);
            throw new TechnicalException("Failed to find dictionary by environment", ex);
        }
    }
}