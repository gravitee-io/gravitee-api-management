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
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcCommandRepository extends JdbcAbstractCrudRepository<Command, String> implements CommandRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCommandRepository.class);

    private final String COMMAND_ACKNOWLEDGMENTS;
    private final String COMMAND_TAGS;

    JdbcCommandRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "commands");
        COMMAND_ACKNOWLEDGMENTS = getTableNameFor("command_acknowledgments");
        COMMAND_TAGS = getTableNameFor("command_tags");
    }

    @Override
    protected JdbcObjectMapper<Command> buildOrm() {
        return JdbcObjectMapper
            .builder(Command.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("from", Types.NVARCHAR, String.class)
            .addColumn("to", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .addColumn("expired_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    private static final JdbcHelper.ChildAdder<Command> CHILD_ADDER = (Command parent, ResultSet rs) -> {
        String acknowledgment = rs.getString("acknowledgment");
        fillCommandAcknowledgments(parent, acknowledgment);

        String tag = rs.getString("tag");
        fillCommandTags(parent, tag);
    };

    private static void fillCommandTags(Command command, String tag) {
        List<String> tags = command.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
            command.setTags(tags);
        }
        if (tag != null && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    private static void fillCommandAcknowledgments(Command command, String acknowledgment) {
        List<String> acknowledgments = command.getAcknowledgments();
        if (acknowledgments == null) {
            acknowledgments = new ArrayList<>();
            command.setAcknowledgments(acknowledgments);
        }
        if (acknowledgment != null && !acknowledgments.contains(acknowledgment)) {
            acknowledgments.add(acknowledgment);
        }
    }

    @Override
    protected String getId(Command item) {
        return item.getId();
    }

    @Override
    public Optional<Command> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.findById({})", id);
        try {
            // Find the command itself
            Optional<Command> command = jdbcTemplate.query(getOrm().getSelectByIdSql(), getRowMapper(), id).stream().findFirst();
            if (command.isEmpty()) {
                return command;
            }

            // Find the command's acknowledgments and update the command
            jdbcTemplate.query(
                "select acknowledgment from " + COMMAND_ACKNOWLEDGMENTS + " where command_id = ?",
                (PreparedStatement ps) -> ps.setString(1, id),
                (ResultSet rs) -> fillCommandAcknowledgments(command.get(), rs.getString("acknowledgment"))
            );

            // Find the command's tags and update the command
            jdbcTemplate.query(
                "select tag from " + COMMAND_TAGS + " where command_id = ?",
                (PreparedStatement ps) -> ps.setString(1, id),
                (ResultSet rs) -> fillCommandTags(command.get(), rs.getString("tag"))
            );

            return command;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find command by id:", ex);
            throw new TechnicalException("Failed to find command by id", ex);
        }
    }

    @Override
    public Command create(Command item) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeAcknowledgments(item, false);
            storeTags(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create command", ex);
            throw new TechnicalException("Failed to create command", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.delete({})", id);
        try {
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
            jdbcTemplate.update("delete from " + COMMAND_ACKNOWLEDGMENTS + " where command_id = ?", id);
            jdbcTemplate.update("delete from " + COMMAND_TAGS + " where command_id = ?", id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete command:", ex);
            throw new TechnicalException("Failed to delete command", ex);
        }
    }

    @Override
    public Command update(Command item) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException();
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(item, item.getId()));
            storeAcknowledgments(item, true);
            storeTags(item, true);
            return findById(item.getId())
                .orElseThrow(() -> new IllegalStateException(format("No command found with id [%s]", item.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update command", ex);
            throw new TechnicalException("Failed to update command", ex);
        }
    }

    @Override
    public List<Command> search(CommandCriteria criteria) {
        LOGGER.debug("JdbcCommandRepository.search({})", criteria);
        JdbcHelper.CollatingRowMapper<Command> rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
        final StringBuilder query = new StringBuilder(
            getOrm().getSelectAllSql() +
            " c " +
            "left join " +
            COMMAND_ACKNOWLEDGMENTS +
            " ca on c.id = ca.command_id " +
            "left join " +
            COMMAND_TAGS +
            " ct on c.id = ct.command_id " +
            "where 1=1 "
        );

        if (criteria.getNotAckBy() != null) {
            query
                .append(" and not exists (")
                .append("select 1 from " + COMMAND_ACKNOWLEDGMENTS + " cak ")
                .append("where cak.command_id = c.id ")
                .append("and cak.acknowledgment = ? ")
                .append(")");
        }
        if (criteria.getNotFrom() != null) {
            query.append(" and c.").append(escapeReservedWord("from")).append(" != ? ");
        }
        if (criteria.getTo() != null) {
            query.append(" and c.").append(escapeReservedWord("to")).append(" = ? ");
        }
        if (criteria.getOrganizationId() != null) {
            query.append(" and c.organization_id = ? ");
        }
        if (criteria.getEnvironmentId() != null) {
            query.append(" and c.environment_id = ? ");
        }
        if (criteria.isNotExpired()) {
            query.append(" and c.expired_at >= ? ");
        }

        List<Command> commands;
        try {
            jdbcTemplate.query(
                query.toString(),
                (PreparedStatement ps) -> {
                    int lastIndex = 1;
                    if (criteria.getNotAckBy() != null) {
                        ps.setString(lastIndex++, criteria.getNotAckBy());
                    }
                    if (criteria.getNotFrom() != null) {
                        ps.setString(lastIndex++, criteria.getNotFrom());
                    }
                    if (criteria.getTo() != null) {
                        ps.setString(lastIndex++, criteria.getTo());
                    }
                    if (criteria.getOrganizationId() != null) {
                        ps.setString(lastIndex++, criteria.getOrganizationId());
                    }
                    if (criteria.getEnvironmentId() != null) {
                        ps.setString(lastIndex++, criteria.getEnvironmentId());
                    }
                    if (criteria.isNotExpired()) {
                        ps.setDate(lastIndex++, new java.sql.Date(System.currentTimeMillis()));
                    }
                },
                rowMapper
            );
            commands = rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find command records:", ex);
            throw new IllegalStateException("Failed to find command records", ex);
        }

        if (criteria.getTags() != null && criteria.getTags().length > 0) {
            commands =
                commands
                    .stream()
                    .filter(command ->
                        command.getTags() != null && command.getTags().stream().anyMatch(Arrays.asList(criteria.getTags())::contains)
                    )
                    .collect(Collectors.toList());
        }

        LOGGER.debug("command records found ({}): {}", commands.size(), commands);
        return commands;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            LOGGER.debug("JdbcCommandRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete commands by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete commands by environment", ex);
        }
    }

    @Override
    public List<String> deleteByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcCommandRepository.deleteByOrganizationId({})", organizationId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where organization_id = ?",
                String.class,
                organizationId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where organization_id = ?", organizationId);
            }

            LOGGER.debug("JdbcCommandRepository.deleteByOrganizationId({}) - Done", organizationId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete commands by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to delete commands by organization", ex);
        }
    }

    private void storeAcknowledgments(Command command, boolean deleteFirst) {
        LOGGER.debug("JdbcCommandRepository.storeAcknowledgments({}, {})", command, deleteFirst);
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + COMMAND_ACKNOWLEDGMENTS + " where command_id = ?", command.getId());
        }
        List<String> acknowledgments = getOrm().filterStrings(command.getAcknowledgments());
        if (!acknowledgments.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + COMMAND_ACKNOWLEDGMENTS + " ( command_id, acknowledgment ) values ( ?, ? )",
                getOrm().getBatchStringSetter(command.getId(), acknowledgments)
            );
        }
    }

    private void storeTags(Command command, boolean deleteFirst) {
        LOGGER.debug("JdbcCommandRepository.storeTags({}, {})", command, deleteFirst);
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + COMMAND_TAGS + " where command_id = ?", command.getId());
        }

        List<String> tags = getOrm().filterStrings(command.getTags());
        if (!tags.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + COMMAND_TAGS + " ( command_id, tag ) values ( ?, ? )",
                getOrm().getBatchStringSetter(command.getId(), tags)
            );
        }
    }
}
