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
import static java.util.Collections.emptySet;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
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
public class JdbcRoleRepository extends JdbcAbstractCrudRepository<Role, String> implements RoleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRoleRepository.class);

    private static final String SCOPE_FIELD = "scope";
    private final String ROLE_PERMISSIONS;

    JdbcRoleRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "roles");
        ROLE_PERMISSIONS = getTableNameFor("role_permissions");
    }

    @Override
    protected JdbcObjectMapper<Role> buildOrm() {
        return JdbcObjectMapper
            .builder(Role.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, RoleReferenceType.class)
            .addColumn(SCOPE_FIELD, Types.NVARCHAR, RoleScope.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("default_role", Types.BIT, boolean.class)
            .addColumn("system", Types.BIT, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    private static final JdbcHelper.ChildAdder<Role> CHILD_ADDER = (Role parent, ResultSet rs) -> {
        int permission = rs.getInt("permission");
        if (!rs.wasNull()) {
            int[] permissions = parent.getPermissions();
            if (permissions == null) {
                permissions = new int[1];
            } else {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
            }
            permissions[permissions.length - 1] = permission;
            parent.setPermissions(permissions);
        }
    };

    @Override
    protected String getId(Role item) {
        return item.getId();
    }

    @Override
    public Role create(Role item) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storePermissions(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create role", ex);
            throw new TechnicalException("Failed to create role", ex);
        }
    }

    @Override
    public Role update(final Role role) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.update({})", role);
        if (role == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(role, role.getId()));
            storePermissions(role, true);
            return findById(role.getId()).orElseThrow(() -> new IllegalStateException(format("No role found with id [%s]", role.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update role", ex);
            throw new TechnicalException("Failed to update role", ex);
        }
    }

    @Override
    public void delete(String roleId) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.delete({})", roleId);
        try {
            jdbcTemplate.update("delete from " + ROLE_PERMISSIONS + " where role_id = ?", roleId);
            jdbcTemplate.update(getOrm().getDeleteSql(), roleId);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete role:", ex);
            throw new TechnicalException("Failed to delete role", ex);
        }
    }

    int[] dedupePermissions(int[] original) {
        if (original == null) {
            return new int[0];
        }
        int[] permissions = Arrays.copyOf(original, original.length);
        Arrays.sort(permissions);
        int tgt = 0;
        for (int src = 1; src < permissions.length; ++src) {
            if (permissions[src] != permissions[tgt]) {
                permissions[++tgt] = permissions[src];
            }
        }
        if (tgt + 1 != original.length) {
            permissions = Arrays.copyOf(permissions, tgt + 1);
            LOGGER.warn("Permissions changed from {} to {}", original, permissions);
        }
        return permissions;
    }

    private void storePermissions(Role role, boolean deleteFirst) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.storePermissions({}, {})", role, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from " + ROLE_PERMISSIONS + " where role_id = ?", role.getId());
            }
            int[] permissions = dedupePermissions(role.getPermissions());
            if ((permissions != null) && permissions.length > 0) {
                jdbcTemplate.batchUpdate(
                    "insert into " + ROLE_PERMISSIONS + " ( role_id, permission ) values ( ?, ? )",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setString(1, role.getId());
                            ps.setInt(2, permissions[i]);
                        }

                        @Override
                        public int getBatchSize() {
                            return permissions.length;
                        }
                    }
                );
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to store role permissions:", ex);
            throw new TechnicalException("Failed to store role permissions", ex);
        }
    }

    @Override
    public Optional<Role> findById(String roleId) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findById({})", roleId);
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r" +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.id = ?" +
                " order by r.reference_type, r.reference_id, r.scope,r.name",
                rowMapper,
                roleId
            );
            Optional<Role> result = rowMapper.getRows().stream().findFirst();
            LOGGER.debug("JdbcRoleRepository.findById({}) = {}", roleId, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by id:", ex);
            throw new TechnicalException("Failed to find role by id", ex);
        }
    }

    @Override
    public Set<Role> findAllById(Set<String> ids) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findAllById({})", ids);
        if (isEmpty(ids)) {
            return emptySet();
        }

        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            String query =
                getOrm().getSelectAllSql() +
                " r" +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.id in ( " +
                getOrm().buildInClause(ids) +
                " ) order by r.reference_type, r.reference_id, r.scope,r.name";

            jdbcTemplate.query(query, (PreparedStatement ps) -> getOrm().setArguments(ps, ids, 1), rowMapper);
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find roles by ids:", ex);
            throw new TechnicalException("Failed to find roles by ids", ex);
        }
    }

    @Override
    public Optional<Role> findByScopeAndNameAndReferenceIdAndReferenceType(
        RoleScope scope,
        String name,
        String referenceId,
        RoleReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findByScopeAndName({}, {}, {}, {})", scope, name, referenceId, referenceType);
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r " +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.scope = ? and r.name = ? and r.reference_id = ? and r.reference_type = ?" +
                " order by r.reference_type, r.reference_id, r.scope, r.name",
                rowMapper,
                scope.name(),
                name,
                referenceId,
                referenceType.name()
            );
            List<Role> rows = rowMapper.getRows();
            if (rows.size() > 1) {
                throw new TechnicalException("More than 1 role exist with same name and same scope for this reference");
            }
            if (rows.size() == 0) {
                return Optional.empty();
            }
            return Optional.of(rows.get(0));
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by scope and name:", ex);
            throw new TechnicalException("Failed to find role by scope and name", ex);
        }
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findAll()");
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r " +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " order by r.reference_type, r.reference_id, r.scope, r.name",
                rowMapper
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all roles:", ex);
            throw new TechnicalException("Failed to find all roles", ex);
        }
    }

    @Override
    public Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findAllByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r " +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.reference_id = ? and r.reference_type = ?" +
                " order by r.reference_type, r.reference_id, r.scope, r.name",
                rowMapper,
                referenceId,
                referenceType.name()
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all roles by ref:", ex);
            throw new TechnicalException("Failed to find all roles by ref", ex);
        }
    }

    @Override
    public Optional<Role> findByIdAndReferenceIdAndReferenceType(String roleId, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findByIdAndReferenceIdAndReferenceType({}, {}, {})", roleId, referenceId, referenceType);
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r " +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.id = ? and r.reference_id = ? and r.reference_type = ?",
                rowMapper,
                roleId,
                referenceId,
                referenceType.name()
            );
            return rowMapper.getRows().stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by id and organization id:", ex);
            throw new TechnicalException("Failed to find role by id and organization id:", ex);
        }
    }

    @Override
    public Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findByScopeAndReferenceIdAndReferenceType({}, {}, {})", scope, referenceId, referenceType);
        try {
            JdbcHelper.CollatingRowMapper<Role> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
            jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " r " +
                " left join " +
                ROLE_PERMISSIONS +
                " rp on rp.role_id = r.id" +
                " where r.scope = ? and r.reference_id = ? and r.reference_type = ?" +
                " order by r.reference_type, r.reference_id, r.scope, r.name",
                rowMapper,
                scope.name(),
                referenceId,
                referenceType.name()
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by scope and ref:", ex);
            throw new TechnicalException("Failed to find role by scope and ref", ex);
        }
    }
}
