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

import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.management.model.TenantReferenceType;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcTagRepository extends JdbcAbstractCrudRepository<Tag, String> implements TagRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcTagRepository.class);

    private final String TAG_GROUPS;

    JdbcTagRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "tags");
        TAG_GROUPS = getTableNameFor("tag_groups");
    }

    @Override
    protected JdbcObjectMapper<Tag> buildOrm() {
        return JdbcObjectMapper
            .builder(Tag.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, TagReferenceType.class)
            .build();
    }

    @Override
    protected String getId(final Tag item) {
        return item.getId();
    }

    @Override
    public Optional<Tag> findById(final String id) throws TechnicalException {
        final Optional<Tag> tag = super.findById(id);
        tag.ifPresent(this::addGroups);
        return tag;
    }

    @Override
    public Optional<Tag> findByIdAndReference(final String id, String referenceId, TagReferenceType referenceType)
        throws TechnicalException {
        try {
            Optional<Tag> tag = jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " t where id = ? and reference_id = ? and reference_type = ? ",
                    getOrm().getRowMapper(),
                    id,
                    referenceId,
                    referenceType.name()
                )
                .stream()
                .findFirst();
            tag.ifPresent(this::addGroups);
            return tag;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} tags by id, referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " tags by id, referenceId and referenceType", ex);
        }
    }

    @Override
    public Set<Tag> findByIdsAndReference(Set<String> tagIds, String referenceId, TagReferenceType referenceType)
        throws TechnicalException {
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() +
                    " where reference_id = ? and reference_type = ? and id in ( " +
                    getOrm().buildInClause(tagIds) +
                    " )",
                    (PreparedStatement ps) -> {
                        ps.setString(1, referenceId);
                        ps.setString(2, referenceType.name());
                        getOrm().setArguments(ps, tagIds, 3);
                    },
                    getOrm().getRowMapper()
                )
                .stream()
                .peek(this::addGroups)
                .collect(toSet());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} tags by ids, referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " tags by ids, referenceId and referenceType", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, TagReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcTagRepository.deleteByReferenceIdAndReferenceType({},{})", referenceId, referenceType);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + TAG_GROUPS + " where tag_id IN (" + getOrm().buildInClause(rows) + ")",
                    rows.toArray()
                );

                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }
            LOGGER.debug("JdbcTagRepository.deleteByReferenceIdAndReferenceType({},{}) - Done", referenceId, referenceType);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete tags by reference {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete tags by reference", ex);
        }
    }

    @Override
    public Set<Tag> findByReference(String referenceId, TagReferenceType referenceType) throws TechnicalException {
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " t where reference_id = ? and reference_type = ? ",
                    getOrm().getRowMapper(),
                    referenceId,
                    referenceType.name()
                )
                .stream()
                .peek(this::addGroups)
                .collect(toSet());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} tags referenceId and referenceType:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " tags by referenceId and referenceType", ex);
        }
    }

    @Override
    public Tag create(final Tag tag) throws TechnicalException {
        storeGroups(tag, false);
        return super.create(tag);
    }

    @Override
    public Tag update(final Tag tag) throws TechnicalException {
        storeGroups(tag, true);
        return super.update(tag);
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + TAG_GROUPS + " where tag_id = ?", id);
        super.delete(id);
    }

    private void storeGroups(final Tag tag, final boolean deleteFirst) {
        if (tag == null) {
            return;
        }
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + TAG_GROUPS + " where tag_id = ?", tag.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(tag.getRestrictedGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + TAG_GROUPS + " (tag_id, group_id) values ( ?, ? )",
                getOrm().getBatchStringSetter(tag.getId(), filteredGroups)
            );
        }
    }

    private void addGroups(final Tag tag) {
        final List<String> groups = getGroups(tag.getId());
        tag.setRestrictedGroups(groups);
    }

    private List<String> getGroups(final String tagId) {
        return jdbcTemplate.queryForList("select group_id from " + TAG_GROUPS + " where tag_id = ?", String.class, tagId);
    }
}
