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
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static java.lang.String.format;

/**
 *
 * @author njt
 */
@Repository
public class JdbcMembershipRepository implements MembershipRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMembershipRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Membership.class, "memberships")
            .updateSql("update memberships set "
                    + " user_id = ?"
                    + " , reference_type = ?"
                    + " , reference_id = ?"
                    + " , created_at = ? "
                    + " , updated_at = ? "
                    + WHERE_CLAUSE
                    + " user_id = ? "
                    + " and reference_type = ? "
                    + " and reference_id = ? "
            )
            .addColumn("user_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, MembershipReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    public Membership create(final Membership membership) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.create({})", membership);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(membership));
            storeMembershipRoles(membership, false);
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create membership", ex);
            throw new TechnicalException("Failed to create membership", ex);
        }
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.update({})", membership);
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
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).orElseThrow(() ->
                    new IllegalStateException(format("No membership found with id [%s, %s, %s]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update membership", ex);
            throw new TechnicalException("Failed to update membership", ex);
        }
    }

    private void addRoles(Membership parent) {
        Map<Integer, String> roles = getRoles(parent.getUserId(), parent.getReferenceId(), parent.getReferenceType());
        parent.setRoles(roles);
    }

    private Map<Integer, String> getRoles(String userId, String referenceId, MembershipReferenceType referenceType) {
        Map<Integer, String> roles = new HashMap<>();
        jdbcTemplate.query("select role_scope, role_name from membership_roles where user_id = ? and reference_id = ? and reference_type = ?"
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
            jdbcTemplate.update("delete from membership_roles where user_id = ? and reference_id = ? and reference_type = ?"
                    , parent.getUserId(), parent.getReferenceId(), parent.getReferenceType().name()
            );
        }
        if (parent.getRoles() != null) {
            List<Integer> scopes = new ArrayList<>(parent.getRoles().keySet());
            jdbcTemplate.batchUpdate("insert into membership_roles ( user_id, reference_id, reference_type, role_scope, role_name ) values ( ?, ?, ?, ?, ? )"
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
        LOGGER.debug("JdbcMembershipRepository.delete({})", membership);
        try {
            jdbcTemplate.update("delete from memberships where user_id = ? and reference_type = ? and reference_id = ? "
                    , membership.getUserId()
                    , membership.getReferenceType().name()
                    , membership.getReferenceId()
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete membership", ex);
            throw new TechnicalException("Failed to delete membership", ex);
        }
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findById({}, {}, {})", userId, referenceType, referenceId);
        try {
            final List<Membership> memberships = jdbcTemplate.query("select"
                            + " user_id, reference_type, reference_id, created_at, updated_at "
                            + " from memberships where user_id = ? and reference_type = ? and reference_id = ?"
                    , ORM.getRowMapper()
                    , userId
                    , referenceType.name()
                    , referenceId
            );
            if (!memberships.isEmpty()) {
                addRoles(memberships.get(0));
            }
            return memberships.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by id", ex);
            throw new TechnicalException("Failed to find membership by id", ex);
        }
    }

    @Override
    public Set<Membership> findByIds(String userId, MembershipReferenceType referenceType, Set<String> referenceIds) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByIds({}, {}, {})", userId, referenceType, referenceIds);
        final StringBuilder query = new StringBuilder("select user_id, reference_type, reference_id, created_at, updated_at "
                + " from memberships where user_id = ? and reference_type = ?");
        ORM.buildInCondition(false, query, "reference_id", referenceIds);
        try {
            final List<Membership> memberships = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        ps.setString(1, userId);
                        ps.setString(2, referenceType.name());
                        ORM.setArguments(ps, referenceIds, 3);
                    }
                    , ORM.getRowMapper()
            );
            for (Membership membership : memberships) {
                addRoles(membership);
            }
            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by ids", ex);
            throw new TechnicalException("Failed to find membership by ids", ex);
        }
    }


    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByReferenceAndRole({}, {}, {}, {})", referenceType, referenceId, roleScope, roleName);
        final StringBuilder query = new StringBuilder("select m.user_id, m.reference_type, m.reference_id, m.created_at, m.updated_at "
                + " from memberships m"
                + " left join membership_roles mr on mr.user_id = m.user_id" +
                " and mr.reference_type = m.reference_type and mr.reference_id = m.reference_id");
        initializeWhereClause(referenceId, referenceType, roleScope, roleName, query);
        return queryAndAddRoles(referenceId, referenceType, roleScope, roleName, query.toString());
    }

    private void initializeWhereClause(final String referenceId, final MembershipReferenceType referenceType,
                                       final RoleScope roleScope, final String roleName, final StringBuilder query) {
        boolean first = true;
        if (referenceId != null) {
            query.append(WHERE_CLAUSE);
            first = false;
            query.append(" m.reference_id= ? ");
        }
        if (referenceType != null) {
            query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
            first = false;
            query.append(" m.reference_type= ? ");
        }
        if (roleScope != null) {
            query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
            first = false;
            query.append(" role_scope= ? ");
        }
        if (roleName != null) {
            query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
            query.append(" role_name= ? ");
        }
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByReferencesAndRole({}, {}, {}, {})", referenceType, referenceIds, roleScope, roleName);
        try {
            StringBuilder query = new StringBuilder("select m.user_id, m.reference_type, m.reference_id, m.created_at, m.updated_at "
                    + " from memberships m "
                    + " left join membership_roles mr on mr.user_id = m.user_id and mr.reference_type = m.reference_type and mr.reference_id = m.reference_id");
            boolean first = true;
            if (referenceType != null) {
                query.append(WHERE_CLAUSE);
                first = false;
                query.append(" m.reference_type= ? ");
            }
            ORM.buildInCondition(first, query, "m.reference_id", referenceIds);
            if (roleScope != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                first = false;
                query.append(" role_scope= ? ");
            }
            if (roleName != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" role_name= ? ");
            }
            final List<Membership> items = jdbcTemplate.query(query.toString()
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
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership role", ex);
            throw new TechnicalException("Failed to find membership by references and membership role", ex);
        }
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByUserAndReferenceTypeAndRole({}, {}, {}, {})", userId, referenceType, roleScope, roleName);
        try {
            final StringBuilder query = new StringBuilder("select "
                    + " m.user_id, m.reference_type, m.reference_id, m.created_at, m.updated_at "
                    + " from memberships m "
                    + " left join membership_roles mr on mr.user_id = m.user_id and mr.reference_type = m.reference_type and mr.reference_id = m.reference_id");
            boolean first = true;
            if (userId != null) {
                query.append(WHERE_CLAUSE);
                query.append(" m.user_id = ? ");
                first = false;
            }
            if (referenceType != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" m.reference_type = ? ");
                first = false;
            }
            if (roleScope != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" role_scope = ? ");
                first = false;
            }
            if (roleName != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" role_name = ? ");
            }
            return queryAndAddRoles(userId, referenceType, roleScope, roleName, query.toString());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership type", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
    }

    private Set<Membership> queryAndAddRoles(final String referenceId, final MembershipReferenceType referenceType,
                                             final RoleScope roleScope, final String roleName, final String stringQuery) {
        final List<Membership> memberships = jdbcTemplate.query(stringQuery
                , (PreparedStatement ps) -> {
                    int idx = 1;
                    if (referenceId != null) {
                        ps.setString(idx++, referenceId);
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
        for (final Membership membership : memberships) {
            addRoles(membership);
        }
        return new HashSet<>(memberships);
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(final String userId, final MembershipReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByUserAndReferenceType({}, {}, {})", userId, referenceType);
        try {
            final String query = "select "
                    + " user_id, reference_type, reference_id, created_at, updated_at "
                    + " from memberships where user_id = ? and reference_type = ? ";
            return queryAndAddRoles(userId, referenceType, null, null, query);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user and membership type", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
    }
}