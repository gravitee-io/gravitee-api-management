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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static java.lang.String.format;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRoleRepository implements RoleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRoleRepository.class);

    private static final String SCOPE_FIELD = "scope";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Role.class, "roles", "key")
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn(SCOPE_FIELD, Types.NVARCHAR, RoleScope.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("default_role", Types.BIT, boolean.class)
            .addColumn("system", Types.BIT, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

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
    public Role create(Role item) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storePermissions(item, false);
            return findById(item.getScope(), item.getName()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create role", ex);
            throw new TechnicalException("Failed to create role", ex);
        }
    }

    @Override
    public Role update(final Role role) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.update({})", role);
        if (role == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update("update roles set "
                            + " scope = ?"
                            + " , name = ?"
                            + " , description = ?"
                            + " , default_role = ?"
                            + " , system = ?"
                            + " , created_at = ? "
                            + " , updated_at = ? "
                            + " where "
                            + " scope = ? "
                            + " and name = ? "
                    , role.getScope() == null ? null : role.getScope().name()
                    , role.getName()
                    , role.getDescription()
                    , role.isDefaultRole()
                    , role.isSystem()
                    , role.getCreatedAt()
                    , role.getUpdatedAt()
                    , role.getScope() == null ? null : role.getScope().name()
                    , role.getName()
            );
            storePermissions(role, true);
            return findById(role.getScope(), role.getName()).orElseThrow(() ->
                    new IllegalStateException(format("No role found with id [%s, %s]", role.getScope(), role.getName())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update role", ex);
            throw new TechnicalException("Failed to update role", ex);
        }
    }


    @Override
    public void delete(RoleScope scope, String name) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.delete({}, {})", scope, name);
        try {
            jdbcTemplate.update("delete from role_permissions where role_scope = ? and role_name = ?", scope.name(), name);
            jdbcTemplate.update("delete from roles where scope = ? and name = ?", scope.name(), name);
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
                jdbcTemplate.update("delete from role_permissions where role_scope = ? and role_name = ?"
                        , role.getScope() == null ? null : role.getScope().name()
                        , role.getName()
                );
            }
            int[] permissions = dedupePermissions(role.getPermissions());
            if ((permissions != null) && permissions.length > 0) {
                jdbcTemplate.batchUpdate("insert into role_permissions ( role_scope, role_name, permission ) values ( ?, ?, ? )"
                        , new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, role.getScope().name());
                        ps.setString(2, role.getName());
                        ps.setInt(3, permissions[i]);
                    }

                    @Override
                    public int getBatchSize() {
                        return permissions.length;
                    }
                });
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to store role permissions:", ex);
            throw new TechnicalException("Failed to store role permissions", ex);
        }
    }

    @Override
    public Optional<Role> findById(RoleScope scope, String name) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findById({}, {})", scope, name);
        try {
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, SCOPE_FIELD, "name");
            jdbcTemplate.query("select * from roles r"
                    + " left join role_permissions rp on rp.role_scope = r.scope and rp.role_name = r.name"
                    + " where r.scope = ? and r.name = ?"
                    + " order by r.scope,r.name"
                    , rowMapper
                    , scope == null ? null : scope.name()
                    , name
            );
            Optional<Role> result = rowMapper.getRows().stream().findFirst();
            LOGGER.debug("JdbcRoleRepository.findById({}, {}) = {}", scope, name, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by id:", ex);
            throw new TechnicalException("Failed to find role by id", ex);
        }
    }

    @Override
    public Set<Role> findByScope(RoleScope scope) throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findByScope({})", scope);
        try {
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, SCOPE_FIELD, "name");
            jdbcTemplate.query("select * from roles r "
                    + " left join role_permissions rp on rp.role_scope = r.scope and rp.role_name = r.name "
                    + " where r.scope = ? "
                    + " order by r.scope, r.name"
                    , rowMapper
                    , scope.name()
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find role by scope:", ex);
            throw new TechnicalException("Failed to find role by scope", ex);
        }
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        LOGGER.debug("JdbcRoleRepository.findAll()");
        try {
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, SCOPE_FIELD, "name");
            jdbcTemplate.query("select * from roles r "
                    + " left join role_permissions rp on rp.role_scope = r.scope and rp.role_name = r.name "
                    + " order by r.scope, r.name"
                    , rowMapper
            );
            return new HashSet<>(rowMapper.getRows());

        } catch (final Exception ex) {
            LOGGER.error("Failed to find all roles:", ex);
            throw new TechnicalException("Failed to find all roles", ex);
        }
    }
}