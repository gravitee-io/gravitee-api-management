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
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 *
 * @author njt
 */
@Repository
public class JdbcTagRepository extends JdbcAbstractCrudRepository<Tag, String> implements TagRepository {

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Tag.class, "tags", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
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
    public Set<Tag> findAll() throws TechnicalException {
        return super.findAll().stream().peek(this::addGroups).collect(toSet());
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
        jdbcTemplate.update("delete from tag_groups where tag_id = ?", id);
        super.delete(id);
    }

    private void storeGroups(final Tag tag, final boolean deleteFirst) {
        if (tag == null) {
            return;
        }
        if (deleteFirst) {
            jdbcTemplate.update("delete from tag_groups where tag_id = ?", tag.getId());
        }
        List<String> filteredGroups = ORM.filterStrings(tag.getRestrictedGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into tag_groups (tag_id, group_id) values ( ?, ? )"
                    , ORM.getBatchStringSetter(tag.getId(), filteredGroups));
        }
    }

    private void addGroups(final Tag tag) {
        final List<String> groups = getGroups(tag.getId());
        tag.setRestrictedGroups(groups);
    }

    private List<String> getGroups(final String tagId) {
        return jdbcTemplate.queryForList("select group_id from tag_groups where tag_id = ?", String.class, tagId);
    }
}