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
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcColumn;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcIdentityProviderRepository extends JdbcAbstractRepository<IdentityProvider> implements IdentityProviderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcIdentityProviderRepository.class);

    JdbcIdentityProviderRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "identity_providers");
    }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final Rm mapper = new Rm();

    @Override
    protected JdbcObjectMapper<IdentityProvider> buildOrm() {
        return JdbcObjectMapper
            .builder(IdentityProvider.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, IdentityProviderType.class)
            .addColumn("enabled", Types.BOOLEAN, boolean.class)
            .addColumn("email_required", Types.BOOLEAN, Boolean.class)
            .addColumn("sync_mappings", Types.BOOLEAN, Boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    private class Rm implements RowMapper<IdentityProvider> {

        @Override
        public IdentityProvider mapRow(ResultSet rs, int i) throws SQLException {
            IdentityProvider identityProvider = new IdentityProvider();
            getOrm().setFromResultSet(identityProvider, rs);

            identityProvider.setConfiguration(convert(rs.getString("configuration"), Object.class, false));
            identityProvider.setGroupMappings(convert(rs.getString("group_mappings"), String.class, true));
            identityProvider.setRoleMappings(convert(rs.getString("role_mappings"), String.class, true));
            identityProvider.setUserProfileMapping(convert(rs.getString("user_profile_mapping"), String.class, false));

            return identityProvider;
        }

        private <T, C> Map<String, T> convert(String sMap, Class<C> valueType, boolean array) {
            TypeReference<HashMap<String, T>> typeRef = new TypeReference<HashMap<String, T>>() {};
            if (sMap != null && !sMap.isEmpty()) {
                try {
                    HashMap<String, T> value = JSON_MAPPER.readValue(sMap, typeRef);
                    if (array) {
                        value.forEach(
                            new BiConsumer<String, T>() {
                                @Override
                                public void accept(String s, T t) {
                                    List<C> list = (List<C>) t;
                                    C[] arr = (C[]) Array.newInstance(valueType, list.size());
                                    arr = list.toArray(arr);
                                    value.put(s, (T) arr);
                                }
                            }
                        );
                    }

                    return (Map<String, T>) value;
                } catch (IOException e) {}
            }

            return null;
        }
    }

    private class Psc implements PreparedStatementCreator {

        private final String sql;
        private final IdentityProvider identityProvider;
        private JdbcObjectMapper<IdentityProvider> orm;
        private final Object[] ids;

        public Psc(String sql, IdentityProvider identityProvider, JdbcObjectMapper<IdentityProvider> orm, Object... ids) {
            this.sql = sql;
            this.identityProvider = identityProvider;
            this.orm = orm;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            LOGGER.debug("SQL: {}", sql);
            LOGGER.debug("identity_provider: {}", identityProvider);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = orm.setStatementValues(stmt, identityProvider, 1);
            stmt.setString(idx++, convert(identityProvider.getConfiguration()));
            stmt.setString(idx++, convert(identityProvider.getGroupMappings()));
            stmt.setString(idx++, convert(identityProvider.getRoleMappings()));
            stmt.setString(idx++, convert(identityProvider.getUserProfileMapping()));

            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }

            return stmt;
        }

        private String convert(Object object) {
            if (object != null) {
                try {
                    return JSON_MAPPER.writeValueAsString(object);
                } catch (JsonProcessingException e) {}
            }

            return null;
        }
    }

    private String buildInsertStatement() {
        final StringBuilder builder = new StringBuilder("insert into " + this.tableName + " (");
        boolean first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
        }
        builder.append(", configuration");
        builder.append(", group_mappings");
        builder.append(", role_mappings");
        builder.append(", user_profile_mapping");
        builder.append(" ) values ( ");
        first = true;
        for (int i = 0; i < getOrm().getColumns().size(); i++) {
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

    private final String INSERT_SQL = buildInsertStatement();

    private String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update " + this.tableName + " set ");
        boolean first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
            builder.append(" = ?");
        }
        builder.append(", configuration = ?");
        builder.append(", group_mappings = ?");
        builder.append(", role_mappings = ?");
        builder.append(", user_profile_mapping = ?");

        builder.append(" where id = ?");
        builder.append(" and organization_id = ?");
        return builder.toString();
    }

    private final String UPDATE_SQL = buildUpdateStatement();

    @Override
    protected RowMapper<IdentityProvider> getRowMapper() {
        return mapper;
    }

    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(IdentityProvider identityProvider) {
        return new Psc(UPDATE_SQL, identityProvider, getOrm(), identityProvider.getId(), identityProvider.getOrganizationId());
    }

    protected PreparedStatementCreator buildInsertPreparedStatementCreator(IdentityProvider identityProvider) {
        return new Psc(INSERT_SQL, identityProvider, getOrm());
    }

    @Override
    public Set<IdentityProvider> findAll() throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository<{}>.findAll()", getOrm().getTableName());
        try {
            List<IdentityProvider> items = jdbcTemplate.query(getOrm().getSelectAllSql(), getRowMapper());
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all {} items:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find all " + getOrm().getTableName() + " items", ex);
        }
    }

    @Override
    public IdentityProvider create(IdentityProvider identityProvider) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository<{}>.create({})", getOrm().getTableName(), identityProvider);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(identityProvider));
            return findById(identityProvider.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create {} item:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to create " + getOrm().getTableName() + " item.", ex);
        }
    }

    @Override
    public IdentityProvider update(IdentityProvider identityProvider) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository<{}>.update({})", getOrm().getTableName(), identityProvider);
        if (identityProvider == null) {
            throw new IllegalStateException("Unable to update null identityProvider");
        }
        try {
            int rows = jdbcTemplate.update(buildUpdatePreparedStatementCreator(identityProvider));
            if (rows == 0) {
                throw new IllegalStateException("Unable to update " + getOrm().getTableName());
            } else {
                return findById(identityProvider.getId()).orElse(null);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update {} identityProvider:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to update " + getOrm().getTableName() + " identityProvider", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository<{}>.delete({})", getOrm().getTableName(), id);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where id = ?", id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete {} identityProvider:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to delete " + getOrm().getTableName() + " identityProvider", ex);
        }
    }

    @Override
    public Optional<IdentityProvider> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository<{}>.findById({})", getOrm().getTableName(), id);
        try {
            List<IdentityProvider> identityProviders = jdbcTemplate.query(getOrm().getSelectAllSql() + " where id = ?", getRowMapper(), id);
            return identityProviders.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} identityProviders by id:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " identityProviders by id", ex);
        }
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository.deleteByOrganizationId({})", organizationId);
        try {
            final var identityProviderIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where organization_id = ?",
                String.class,
                organizationId
            );

            if (!identityProviderIds.isEmpty()) {
                jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
            }

            LOGGER.debug("JdbcIdentityProviderRepository.deleteByOrganizationId({}) - Done", organizationId);
            return identityProviderIds;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete identityProviders by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete identityProviders by organizationId", ex);
        }
    }

    @Override
    public Set<IdentityProvider> findAllByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcIdentityProviderRepository.findAllByOrganizationId({})", organizationId);
        try {
            List<IdentityProvider> identityProviders = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where organization_id = ?",
                getRowMapper(),
                organizationId
            );
            return new HashSet<>(identityProviders);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} identityProviders by org:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " identityProviders by org", ex);
        }
    }
}
