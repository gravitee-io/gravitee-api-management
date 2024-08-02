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

import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static java.util.Collections.emptySet;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 *
 * @author njt
 */
@Repository
public class JdbcMembershipRepository extends JdbcAbstractCrudRepository<Membership, String> implements MembershipRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMembershipRepository.class);

    JdbcMembershipRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "memberships");
    }

    @Override
    protected JdbcObjectMapper<Membership> buildOrm() {
        return JdbcObjectMapper
            .builder(Membership.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("member_id", Types.NVARCHAR, String.class)
            .addColumn("member_type", Types.NVARCHAR, MembershipMemberType.class)
            .addColumn("reference_type", Types.NVARCHAR, MembershipReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("role_id", Types.NVARCHAR, String.class)
            .addColumn("source", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(Membership item) {
        return item.getId();
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, MembershipReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.deleteByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            List<String> rows = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where reference_type = ?" + " and reference_id = ?",
                String.class,
                referenceType.name(),
                referenceId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + this.tableName + " where reference_type = ?" + " and reference_id = ?",
                    referenceType.name(),
                    referenceId
                );
            }

            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete memberships for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete memberships by reference", ex);
        }
    }

    @Override
    public Set<Membership> findByIds(Set<String> membershipIds) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByIds({})", membershipIds);
        try {
            if (isEmpty(membershipIds)) {
                return emptySet();
            }
            List<Membership> memberships = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " m where m.id in ( " + getOrm().buildInClause(membershipIds) + " )",
                (PreparedStatement ps) -> getOrm().setArguments(ps, membershipIds, 1),
                getOrm().getRowMapper()
            );
            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by ids", ex);
            throw new TechnicalException("Failed to find membership by ids", ex);
        }
    }

    @Override
    public Set<Membership> findByReferenceAndRoleId(MembershipReferenceType referenceType, String referenceId, String roleId)
        throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByReferenceAndRoleId({}, {}, {}})", referenceType, referenceId, roleId);
        final StringBuilder query = new StringBuilder("select m.* from " + this.tableName + " m");
        initializeWhereClause(referenceId, referenceType, roleId, query);
        return query(null, null, referenceId, referenceType, roleId, query.toString());
    }

    @Override
    public Set<Membership> findByRoleId(String roleId) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByRoleId({})", roleId);
        final StringBuilder query = new StringBuilder("select m.* from " + this.tableName + " m");
        initializeWhereClause(null, null, roleId, query);
        return query(null, null, null, null, roleId, query.toString());
    }

    private void initializeWhereClause(
        final String referenceId,
        final MembershipReferenceType referenceType,
        final String roleId,
        final StringBuilder query
    ) {
        boolean first = true;
        if (referenceId != null) {
            query.append(WHERE_CLAUSE);
            query.append(" m.reference_id = ? ");
            first = false;
        }
        if (referenceType != null) {
            query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
            query.append(" m.reference_type = ? ");
            first = false;
        }
        if (roleId != null) {
            query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
            query.append(" role_id = ? ");
        }
    }

    @Override
    public Set<Membership> findByReferencesAndRoleId(MembershipReferenceType referenceType, List<String> referenceIds, String roleId)
        throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByReferencesAndRoleId({}, {}, {})", referenceType, referenceIds, roleId);
        try {
            if (CollectionUtils.isEmpty(referenceIds)) {
                return Set.of();
            }

            StringBuilder query = new StringBuilder("select m.* from " + this.tableName + " m ");
            boolean first = true;
            if (referenceType != null) {
                query.append(WHERE_CLAUSE);
                first = false;
                query.append(" m.reference_type = ? ");
            }
            getOrm().buildInCondition(first, query, "m.reference_id", referenceIds);
            if (roleId != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" role_id = ? ");
            }
            final List<Membership> items = jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int idx = 1;
                    if (referenceType != null) {
                        ps.setString(idx++, referenceType.name());
                    }
                    idx = getOrm().setArguments(ps, referenceIds, idx);
                    if (roleId != null) {
                        ps.setString(idx++, roleId);
                    }
                },
                getOrm().getRowMapper()
            );
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership role_id", ex);
            throw new TechnicalException("Failed to find membership by references and membership role_id", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
        String memberId,
        final MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String roleId
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId({}, {}, {}, {})",
            memberId,
            memberType,
            referenceType,
            roleId
        );
        try {
            final StringBuilder query = new StringBuilder("select m.* from " + this.tableName + " m");
            boolean first = true;
            if (memberId != null) {
                query.append(WHERE_CLAUSE);
                query.append(" m.member_id = ? ");
                first = false;
            }
            if (memberType != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" m.member_type = ? ");
                first = false;
            }
            if (referenceType != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" m.reference_type = ? ");
                first = false;
            }
            if (roleId != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" m.role_id = ? ");
            }
            return query(memberId, memberType, null, referenceType, roleId, query.toString());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership type", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn({}, {}, {}, [{}])",
            memberId,
            memberType,
            referenceType,
            roleIds
        );
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        try {
            final StringBuilder queryBuilder = new StringBuilder("select m.* from " + this.tableName + " m")
                .append(WHERE_CLAUSE)
                .append(" m.member_id = ? ")
                .append(AND_CLAUSE)
                .append(" m.member_type = ? ")
                .append(AND_CLAUSE)
                .append(" m.reference_type = ? ");

            getOrm().buildInCondition(false, queryBuilder, "m.role_id", roleIds);

            List<Membership> memberships = jdbcTemplate.query(
                queryBuilder.toString(),
                (PreparedStatement ps) -> {
                    ps.setString(1, memberId);
                    ps.setString(2, memberType.name());
                    ps.setString(3, referenceType.name());
                    getOrm().setArguments(ps, roleIds, 4);
                },
                getOrm().getRowMapper()
            );

            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership type", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
    }

    @Override
    public Set<String> findRefIdByMemberAndRefTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findReferenceIdByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn({}, {}, {}, [{}])",
            memberId,
            memberType,
            referenceType,
            roleIds
        );
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        try {
            final StringBuilder queryBuilder = new StringBuilder("select m.reference_id from " + this.tableName + " m")
                .append(WHERE_CLAUSE)
                .append(" m.member_id = ? ")
                .append(AND_CLAUSE)
                .append(" m.member_type = ? ")
                .append(AND_CLAUSE)
                .append(" m.reference_type = ? ");

            getOrm().buildInCondition(false, queryBuilder, "m.role_id", roleIds);

            return jdbcTemplate.query(
                queryBuilder.toString(),
                (PreparedStatement ps) -> {
                    ps.setString(1, memberId);
                    ps.setString(2, memberType.name());
                    ps.setString(3, referenceType.name());
                    getOrm().setArguments(ps, roleIds, 4);
                },
                resultSet -> {
                    Set<String> ids = new HashSet<>();
                    while (resultSet.next()) {
                        String id = resultSet.getString(1);
                        ids.add(id);
                    }
                    return ids;
                }
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find reference ids by references and membership type and member", ex);
            throw new TechnicalException("Failed to find reference ids by references and membership type and member", ex);
        }
    }

    private Set<Membership> query(
        final String memberId,
        final MembershipMemberType memberType,
        final String referenceId,
        final MembershipReferenceType referenceType,
        final String roleId,
        final String stringQuery
    ) {
        final List<Membership> memberships = jdbcTemplate.query(
            stringQuery,
            preparedStatement(memberId, memberType, referenceId, referenceType, roleId),
            getOrm().getRowMapper()
        );
        return new HashSet<>(memberships);
    }

    private Stream<Membership> queryForStream(
        final String memberId,
        final MembershipMemberType memberType,
        final String referenceId,
        final MembershipReferenceType referenceType,
        final String roleId,
        final String stringQuery
    ) {
        return jdbcTemplate.queryForStream(
            stringQuery,
            preparedStatement(memberId, memberType, referenceId, referenceType, roleId),
            getOrm().getRowMapper()
        );
    }

    private static PreparedStatementSetter preparedStatement(
        String memberId,
        MembershipMemberType memberType,
        String referenceId,
        MembershipReferenceType referenceType,
        String roleId
    ) {
        return (PreparedStatement ps) -> {
            int idx = 1;
            if (memberId != null) {
                ps.setString(idx++, memberId);
            }
            if (memberType != null) {
                ps.setString(idx++, memberType.name());
            }
            if (referenceId != null) {
                ps.setString(idx++, referenceId);
            }
            if (referenceType != null) {
                ps.setString(idx++, referenceType.name());
            }
            if (roleId != null) {
                ps.setString(idx++, roleId);
            }
        };
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceType(
        final String memberId,
        final MembershipMemberType memberType,
        final MembershipReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType({}, {}, {}, {})",
            memberId,
            memberType,
            referenceType
        );
        try {
            final String query = getOrm().getSelectAllSql() + " where member_id = ? and member_type = ? and reference_type = ? ";
            return query(memberId, memberType, null, referenceType, null, query);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user and membership type", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
    }

    @Override
    public Stream<String> findRefIdsByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException {
        try {
            final String query =
                "select reference_id from " + getOrm().getTableName() + " where member_id = ? and member_type = ? and reference_type = ? ";
            return queryForStream(memberId, memberType, null, referenceType, null, query).map(Membership::getReferenceId);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find reference ids by member and reference type", ex);
            throw new TechnicalException("Failed to find reference ids by member and reference type", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberType(String memberId, final MembershipMemberType memberType) throws TechnicalException {
        LOGGER.debug("JdbcMembershipRepository.findByMemberIdAndMemberType({}, {})", memberId, memberType);
        try {
            final String query = getOrm().getSelectAllSql() + " where member_id = ? and member_type = ? ";
            return query(memberId, memberType, null, null, null, query);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user ", ex);
            throw new TechnicalException("Failed to find by user ", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
        String memberId,
        final MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId,
        String roleId
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId({}, {}, {}, {}, {})",
            memberId,
            memberType,
            referenceType,
            referenceId,
            roleId
        );
        try {
            final String query =
                getOrm().getSelectAllSql() +
                " where member_id = ? and member_type = ? and reference_type = ? and reference_id = ? and role_id = ? ";
            final List<Membership> memberships = jdbcTemplate.query(
                query,
                getOrm().getRowMapper(),
                memberId,
                memberType.name(),
                referenceType.name(),
                referenceId,
                roleId
            );
            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user and membership type", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdsAndMemberTypeAndReferenceType(
        List<String> memberIds,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdsAndMemberTypeAndReferenceType({}, {}, {})",
            memberIds,
            memberType,
            referenceType
        );
        try {
            StringBuilder query = new StringBuilder("select m.* from " + this.tableName + " m ");
            boolean first = true;
            if (referenceType != null) {
                query.append(WHERE_CLAUSE);
                first = false;
                query.append(" m.reference_type= ? ");
            }
            first = getOrm().buildInCondition(first, query, "m.member_id", memberIds);
            if (memberType != null) {
                query.append(first ? WHERE_CLAUSE : AND_CLAUSE);
                query.append(" m.member_type = ? ");
            }
            final List<Membership> items = jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int idx = 1;
                    if (referenceType != null) {
                        ps.setString(idx++, referenceType.name());
                    }
                    idx = getOrm().setArguments(ps, memberIds, idx);
                    if (memberType != null) {
                        ps.setString(idx++, memberType.name());
                    }
                },
                getOrm().getRowMapper()
            );
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by references and membership role", ex);
            throw new TechnicalException("Failed to find membership by references and membership role", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String sourceId
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource({}, {}, {}, {})",
            memberId,
            memberType,
            referenceType,
            sourceId
        );
        try {
            final String query =
                "select * from " + this.tableName + " where member_id = ? and member_type = ? and reference_type = ? and source = ? ";
            final List<Membership> memberships = jdbcTemplate.query(
                query,
                getOrm().getRowMapper(),
                memberId,
                memberType.name(),
                referenceType.name(),
                sourceId
            );
            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user, source and membership type", ex);
            throw new TechnicalException("Failed to find membership by user, source and membership type", ex);
        }
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId({}, {}, {}, {})",
            memberId,
            memberType,
            referenceType,
            referenceId
        );
        try {
            final String query = getOrm().getSelectAllSql() + " where member_id = ? and member_type = ? and reference_type = ?";

            final List<Membership> memberships;

            if (referenceId != null) {
                memberships =
                    jdbcTemplate.query(
                        query + " and reference_id = ?",
                        getOrm().getRowMapper(),
                        memberId,
                        memberType.name(),
                        referenceType.name(),
                        referenceId
                    );
            } else {
                memberships =
                    jdbcTemplate.query(
                        query + " and reference_id is null",
                        getOrm().getRowMapper(),
                        memberId,
                        memberType.name(),
                        referenceType.name()
                    );
            }

            return new HashSet<>(memberships);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find membership by user and membership type", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
    }
}
