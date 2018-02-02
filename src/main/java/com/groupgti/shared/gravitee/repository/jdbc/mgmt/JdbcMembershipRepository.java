/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import java.sql.Types;
import java.util.Date;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcMembershipRepository implements MembershipRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcMembershipRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Membership.class, "Key")
            .updateSql("update Membership set "
                    + " `UserId` = ?"
                    + " , `ReferenceType` = ?"
                    + " , `ReferenceId` = ?"
                    + " , `CreatedAt` = ? "
                    + " , `UpdatedAt` = ? "
                    + " where "
                    + " UserId = ? "
                    + " and ReferenceType = ? "
                    + " and ReferenceId = ? "
            )
            .addColumn("UserId", Types.NVARCHAR, String.class)
            .addColumn("ReferenceType", Types.NVARCHAR, MembershipReferenceType.class)
            .addColumn("ReferenceId", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build();    
    
    @Autowired
    public JdbcMembershipRepository(DataSource dataSource) {
        logger.debug("JdbcMembershipRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        
        logger.debug("JdbcMembershipRepository.create({})", membership);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(membership));
            storeMembershipRoles(membership, false);
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create membership:", ex);
            throw new TechnicalException("Failed to create membership", ex);
        }
        
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        
        logger.debug("JdbcMembershipRepository.update({})", membership);
        if (membership == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(membership
                    , membership.getUserId()
                    , membership.getReferenceType().name()
                    , membership.getReferenceId()
            ));
            storeMembershipRoles(membership, true);
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).get();
        } catch (NoSuchElementException ex) {
            logger.error("Failed to update api:", ex);
            throw new IllegalStateException("Failed to update api", ex);
        } catch (Throwable ex) {
            logger.error("Failed to update membership:", ex);
            throw new TechnicalException("Failed to update membership", ex);
        }
        
    }

    private void addRoles(Membership parent) {        
        Map<Integer, String> roles = getRoles(parent.getUserId(), parent.getReferenceId(), parent.getReferenceType());
        parent.setRoles(roles);
    }
    
    private Map<Integer, String> getRoles(String userId, String referenceId, MembershipReferenceType referenceType) {
        Map<Integer, String> roles = new HashMap<>();
        jdbcTemplate.query("select RoleScope, RoleName from MembershipRole where UserId = ? and ReferenceId = ? and ReferenceType = ?"
                , new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                roles.put(rs.getInt(1), rs.getString(2));
            }
        }, userId, referenceId, referenceType.name());
        return roles;
    }
    
    private void storeMembershipRoles(Membership parent, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from MembershipRole where UserId = ? and ReferenceId = ? and ReferenceType = ?"
                    , parent.getUserId(), parent.getReferenceId(), parent.getReferenceType().name()
            );
        }
        if (parent.getRoles() != null) {
            List<Integer> scopes = new ArrayList<>(parent.getRoles().keySet());
            jdbcTemplate.batchUpdate("insert into MembershipRole ( UserId, ReferenceId, ReferenceType, RoleScope, RoleName ) values ( ?, ?, ?, ?, ? )"
                    , new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, parent.getUserId());
                    ps.setString(2, parent.getReferenceId());
                    ps.setString(3, parent.getReferenceType().name());
                    ps.setInt(4, scopes.get(i));
                    ps.setString(5, parent.getRoles().get(scopes.get(i)));
                }

                @Override
                public int getBatchSize() {
                    return scopes.size();
                }
            });
        }
    }
            
    @Override
    public void delete(Membership membership) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.delete({})", membership);
        try {
            jdbcTemplate.update("delete from Membership where UserId = ? and ReferenceType = ? and ReferenceId = ? "
                    , membership.getUserId()
                    , membership.getReferenceType().name()
                    , membership.getReferenceId()
            );
        } catch (Throwable ex) {
            logger.error("Failed to create item:", ex);
            throw new TechnicalException("Failed to create item", ex);
        }
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {

        logger.debug("JdbcMembershipRepository.findById({}, {}, {})", userId, referenceType, referenceId);
        
        try {
            List<Membership> items = jdbcTemplate.query("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership where UserId = ? and ReferenceType = ? and ReferenceId = ?"
                    , ORM.getRowMapper()
                    , userId
                    , referenceType.name()
                    , referenceId
            );
            if (!items.isEmpty()) {
                addRoles(items.get(0));
            }
            return items.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find membership by id:", ex);
            throw new TechnicalException("Failed to find membership by id", ex);
        }
        
    }
    
    @Override
    public Set<Membership> findByIds(String userId, MembershipReferenceType referenceType, Set<String> referenceIds) throws TechnicalException {

        logger.debug("JdbcMembershipRepository.findByIds({}, {}, {})", userId, referenceType, referenceIds);

        StringBuilder query = new StringBuilder("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership where UserId = ? and ReferenceType = ?");
        ORM.buildInCondition(false, query, "ReferenceId", referenceIds);
        
        try {
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        ps.setString(1, userId);
                        ps.setString(2, referenceType.name());
                        ORM.setArguments(ps, referenceIds, 3); 
                    }
                    , ORM.getRowMapper()
            );
            for (Membership membership : items) {
                addRoles(membership);
            }
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by id:", ex);
            throw new TechnicalException("Failed to find membership by id", ex);
        }
        
    }
    
    

    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByReferenceAndRole({}, {}, {}, {})", referenceType, referenceId, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select distinct "
                    + " m.`UserId`, m.`ReferenceType`, m.`ReferenceId`, m.`CreatedAt`, m.`UpdatedAt` "
                    + " from Membership m "
                    + " left join MembershipRole mr on mr.UserID = m.UserID and mr.ReferenceType = m.ReferenceType and mr.ReferenceId = m.ReferenceId");
            boolean first = true;
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" m.ReferenceType = ? ");                
            }
            if (referenceId != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" m.ReferenceId = ? ");                
            }
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }            
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        if (referenceId != null) {
                            ps.setString(idx++, referenceId);
                        }
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            for (Membership membership : items) {
                addRoles(membership);
            }
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByReferencesAndRole({}, {}, {}, {})", referenceType, referenceIds, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select distinct "
                    + " m.`UserId`, m.`ReferenceType`, m.`ReferenceId`, m.`CreatedAt`, m.`UpdatedAt` "
                    + " from Membership m "
                    + " left join MembershipRole mr on mr.UserID = m.UserID and mr.ReferenceType = m.ReferenceType and mr.ReferenceId = m.ReferenceId");
            boolean first = true;
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" m.ReferenceType = ? ");                
            }
            ORM.buildInCondition(first, query, "m.ReferenceId", referenceIds);
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        idx = ORM.setArguments(ps, referenceIds, idx);
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            for (Membership membership : items) {
                addRoles(membership);
            }
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByUserAndReferenceTypeAndRole({}, {}, {}, {})", userId, referenceType, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select distinct "
                    + " m.`UserId`, m.`ReferenceType`, m.`ReferenceId`, m.`CreatedAt`, m.`UpdatedAt` "
                    + " from Membership m "
                    + " left join MembershipRole mr on mr.UserID = m.UserID and mr.ReferenceType = m.ReferenceType and mr.ReferenceId = m.ReferenceId");
            boolean first = true;
            if (userId != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" m.UserId = ? ");                
            }
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" m.ReferenceType = ? ");                
            }
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }            
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (userId != null) {
                            ps.setString(idx++, userId);
                        }
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            for (Membership membership : items) {
                addRoles(membership);
            }
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {

        logger.debug("JdbcMembershipRepository.findByUserAndReferenceType({}, {}, {})", userId, referenceType);
        
        try {
            List<Membership> items = jdbcTemplate.query("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership where UserId = ? and ReferenceType = ? "
                    , ORM.getRowMapper()
                    , userId, referenceType.name()
            );
            for (Membership membership : items) {
                addRoles(membership);
            }
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by user and membership type:", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
       
    }
    
    
}
