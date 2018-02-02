/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRoleRepository implements RoleRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcRoleRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Role.class, "Key")
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Scope", Types.NVARCHAR, RoleScope.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .addColumn("DefaultRole", Types.BIT, boolean.class)
            .addColumn("System", Types.BIT, boolean.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Role> CHILD_ADDER = (Role parent, ResultSet rs) -> {
        int permission = rs.getInt("Permission");
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
    
    @Autowired
    public JdbcRoleRepository(DataSource dataSource) {
        logger.debug("JdbcRoleRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Role create(Role item) throws TechnicalException {
        logger.debug("JdbcRoleRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storePermissions(item, false);
            return findById(item.getScope(), item.getName()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create role:", ex);
            throw new TechnicalException("Failed to create role", ex);
        }
    }

    @Override
    public Role update(Role item) throws TechnicalException {
        logger.debug("JdbcRoleRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update("update Role set "
                    + " `Scope` = ?"
                    + " , `Name` = ?"
                    + " , `Description` = ?"
                    + " , `DefaultRole` = ?"
                    + " , `System` = ?"
                    + " , `CreatedAt` = ? "
                    + " , `UpdatedAt` = ? "
                    + " where "
                    + " Scope = ? "
                    + " and Name = ? "
                    , item.getScope() == null ? null : item.getScope().name()
                    , item.getName()
                    , item.getDescription()
                    , item.isDefaultRole()
                    , item.isSystem()
                    , item.getCreatedAt()
                    , item.getUpdatedAt()
                    , item.getScope() == null ? null : item.getScope().name()
                    , item.getName()
            );
            storePermissions(item, true);
            return findById(item.getScope(), item.getName()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update role:", ex);
            throw new TechnicalException("Failed to update role", ex);
        }
    }
    

    @Override
    public void delete(RoleScope scope, String name) throws TechnicalException {
        logger.debug("JdbcRoleRepository.delete({}, {})", scope, name);
        try {
            jdbcTemplate.update("delete from RolePermission where RoleScope = ? and RoleName = ?", scope.name(), name);
            jdbcTemplate.update("delete from Role where Scope = ? and Name = ?", scope.name(), name);
        } catch (Throwable ex) {
            logger.error("Failed to delete role:", ex);
            throw new TechnicalException("Failed to delete role", ex);
        }
    }    
    
    int[] dedupePermissions(int[] original) {
        if (original == null) {
            return null;
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
            logger.warn("Permissions changed from {} to {}", original, permissions);
        }
        return permissions;
    }
    
    private void storePermissions(Role role, boolean deleteFirst) throws TechnicalException {
        logger.debug("JdbcRoleRepository.storePermissions({}, {})", role, deleteFirst);
        try {
            if (deleteFirst) {
                jdbcTemplate.update("delete from RolePermission where RoleScope = ? and RoleName = ?"
                        , role.getScope() == null ? null : role.getScope().name()
                        , role.getName()
                );
            }

            int[] permissions = dedupePermissions(role.getPermissions());
            if ((permissions != null) && permissions.length > 0) {
                jdbcTemplate.batchUpdate("insert into RolePermission ( RoleScope, RoleName, Permission ) values ( ?, ?, ? )"
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
        } catch (Throwable ex) {
            logger.error("Failed to store role permissions:", ex);
            throw new TechnicalException("Failed to store role permissions", ex);
        }
    }    
    
    
    @Override
    public Optional<Role> findById(RoleScope scope, String name) throws TechnicalException {

        logger.debug("JdbcRoleRepository.findById({}, {})", scope, name);
        try {
        
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, "Scope", "Name");
            jdbcTemplate.query("select * from Role r "
                    + " left join RolePermission rp on rp.RoleScope = r.Scope and rp.RoleName = r.Name "
                    + " where r.Scope = ? and r.Name = ? "
                    + " order by r.Scope, r.Name"
                    , rowMapper
                    , scope == null ? null : scope.name()
                    , name
            );
            Optional<Role> result = rowMapper.getRows().stream().findFirst();
            logger.debug("JdbcRoleRepository.findById({}, {}) = {}", scope, name, result);
            return result;

        } catch (Throwable ex) {
            logger.error("Failed to find role by id:", ex);
            throw new TechnicalException("Failed to find role by id", ex);
        }

    }

    @Override
    public Set<Role> findByScope(RoleScope scope) throws TechnicalException {

        logger.debug("JdbcRoleRepository.findByScope({})", scope);
        try {
        
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, "Scope", "Name");
            jdbcTemplate.query("select * from Role r "
                    + " left join RolePermission rp on rp.RoleScope = r.Scope and rp.RoleName = r.Name "
                    + " where r.Scope = ? "
                    + " order by r.Scope, r.Name"
                    , rowMapper
                    , scope.name()
            );
            
            return new HashSet<>(rowMapper.getRows());

        } catch (Throwable ex) {
            logger.error("Failed to find role by scope:", ex);
            throw new TechnicalException("Failed to find role by scope", ex);
        }

    }

    @Override
    public Set<Role> findAll() throws TechnicalException {

        logger.debug("JdbcRoleRepository.findAll()");
        try {
        
            JdbcHelper.CollatingRowMapperTwoColumn<Role> rowMapper = new JdbcHelper.CollatingRowMapperTwoColumn<>(ORM.getRowMapper(), CHILD_ADDER, "Scope", "Name");
            jdbcTemplate.query("select * from Role r "
                    + " left join RolePermission rp on rp.RoleScope = r.Scope and rp.RoleName = r.Name "
                    + " order by r.Scope, r.Name"
                    , rowMapper
            );
            
            return new HashSet<>(rowMapper.getRows());

        } catch (Throwable ex) {
            logger.error("Failed to find all roles:", ex);
            throw new TechnicalException("Failed to find all roles", ex);
        }

    }
    
    
    
}
