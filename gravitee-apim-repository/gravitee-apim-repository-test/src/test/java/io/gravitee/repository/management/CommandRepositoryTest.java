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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class CommandRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/command-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        //created by the json file

        Optional<Command> optMessage = commandRepository.findById("msg-to-create");

        assertTrue(optMessage.isPresent());
        Command command = optMessage.get();
        assertEquals("msg-to-create", command.getId(), "id");
        assertEquals("DEFAULT", command.getOrganizationId(), "organization id");
        assertEquals("DEFAULT", command.getEnvironmentId(), "environment id");
        assertEquals("someone", command.getTo(), "to");
        assertTrue(command.getTags().contains("DATA_TO_INDEX"), "tags: DATA_TO_INDEX");
        assertTrue(command.getTags().contains("INSERT"), "tags: INSERT");
        assertEquals("Hello, is it me you're looking for?", command.getContent(), "content");
        assertTrue(command.getAcknowledgments().contains("1"), "acknowledgments: 1");
        assertTrue(command.getAcknowledgments().contains("a"), "acknowledgments: a");
        assertTrue(compareDate(new Date(1546305346000L), command.getCreatedAt()), "createdAt");
        assertTrue(compareDate(new Date(1548983746000L), command.getUpdatedAt()), "updatedAt");
        assertTrue(compareDate(new Date(1551402946000L), command.getExpiredAt()), "deleteAt");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Command command = new Command();
        command.setId("msg-to-update");
        command.setOrganizationId("NEW_DEFAULT");
        command.setEnvironmentId("new_DEFAULT");
        command.setFrom("from updated");
        command.setTo("message updated");
        command.setTags(Collections.singletonList("DELETE"));
        command.setContent("updated content");
        command.setAcknowledgments(Arrays.asList("up", "date"));
        command.setCreatedAt(new Date(1546405346000L));
        command.setUpdatedAt(new Date(1546505346000L));
        command.setExpiredAt(new Date(1546605346000L));

        Command updatedCommand = commandRepository.update(command);

        assertEquals(command.getId(), updatedCommand.getId(), "id");
        assertEquals(command.getEnvironmentId(), updatedCommand.getEnvironmentId(), "environment id.");
        assertEquals(command.getOrganizationId(), updatedCommand.getOrganizationId(), "organization id.");
        assertEquals(command.getTo(), updatedCommand.getTo(), "to");
        assertEquals(command.getFrom(), updatedCommand.getFrom(), "from");
        assertTrue(command.getTags().containsAll(updatedCommand.getTags()), "tags: DATA_TO_INDEX");
        assertEquals(command.getContent(), updatedCommand.getContent(), "content");
        assertTrue(
            command.getAcknowledgments().containsAll(updatedCommand.getAcknowledgments()),
            "acknowledgments: " + updatedCommand.getAcknowledgments().size() + "/" + command.getAcknowledgments().size()
        );
        assertTrue(compareDate(command.getCreatedAt(), updatedCommand.getCreatedAt()), "createdAt");
        assertTrue(compareDate(command.getUpdatedAt(), updatedCommand.getUpdatedAt()), "updatedAt");
        assertTrue(compareDate(command.getExpiredAt(), updatedCommand.getExpiredAt()), "deleteAt");
    }

    @Test
    public void shouldNotUpdateUnknownMessage() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Command unknownCommand = new Command();
            commandRepository.update(unknownCommand);
            fail("An unknown message should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            commandRepository.update(null);
            fail("A null message should not be updated");
        });
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        String idToDelete = "msg-to-delete";
        Optional<Command> message = commandRepository.findById(idToDelete);
        assertTrue(message.isPresent(), "msg should exists before being deleted");

        commandRepository.delete(idToDelete);

        message = commandRepository.findById(idToDelete);
        assertFalse(message.isPresent(), "msg should not exists after being deleted");

        // Deletion should be idempotent and not throw exception if message does not exist
        commandRepository.delete(idToDelete);
    }

    @Test
    public void shouldSearchByNotFrom() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notFrom("node1").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(5, commands.size(), "result size");
        List<String> ids = commands.stream().map(Command::getId).toList();
        assertFalse(ids.contains("search1"), "not contain 'search1'");
        assertTrue(ids.contains("search2"), "contain 'search2'");
        assertTrue(ids.contains("search3"), "contain 'search3'");
    }

    @Test
    public void shouldSearchByTo() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).to("node1").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(1, commands.size(), "result size");
        assertEquals("search3", commands.get(0).getId(), "contain 'search3'");
    }

    @Test
    public void shouldSearchByTag() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).tags("INSERT").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(4, commands.size(), "result size");
        List<String> ids = commands.stream().map(Command::getId).collect(Collectors.toList());
        assertTrue(ids.contains("msg-to-create"), "contain 'msg-to-create'");
        assertTrue(ids.contains("msg-to-update"), "contain 'msg-to-update'");
        assertTrue(ids.contains("search1"), "contain 'search1'");
        assertTrue(ids.contains("search2"), "contain 'search2'");
    }

    @Test
    public void shouldSearchByTags() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).tags("DATA_TO_INDEX", "DELETE").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(5, commands.size(), "result size");
        assertTrue(
            commands
                .stream()
                .map(Command::getId)
                .allMatch(List.of("msg-to-create", "msg-to-update", "search1", "search2", "search3")::contains),
            "contain [msg-to-create, msg-to-update, search1, search2, search3]"
        );
    }

    @Test
    public void shouldSearchByNotAck() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notAckBy("node3").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(4, commands.size(), "result size");
        List<String> ids = commands.stream().map(Command::getId).collect(Collectors.toList());
        assertFalse(ids.contains("search1"), "not contain 'search1'");
        assertFalse(ids.contains("search3"), "not contain 'search3'");
    }

    @Test
    public void shouldSearchByNotDeleted() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notDeleted().build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(1, commands.size(), "result size");
        assertEquals("search2", commands.get(0).getId(), "contain 'search2'");
    }

    @Test
    public void shouldSearchByEnvironment() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).environmentId("DEFAULT").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(1, commands.size(), "result size");
        assertEquals("msg-to-create", commands.get(0).getId(), "contain 'msg-to-create'");
    }

    @Test
    public void shouldSearchByOrganization() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).organizationId("DEFAULT").build());

        assertNotNull(commands, "not null");
        assertFalse(commands.isEmpty(), "not empty");
        assertEquals(1, commands.size(), "result size");
        assertEquals("msg-to-create", commands.get(0).getId(), "contain 'msg-to-create'");
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        int nbBeforeDeletion = commandRepository.search((new CommandCriteria.Builder()).environmentId("ToBeDeleted").build()).size();
        List<String> deleted = commandRepository.deleteByEnvironmentId("ToBeDeleted");
        int nbAfterDeletion = commandRepository.search((new CommandCriteria.Builder()).environmentId("ToBeDeleted").build()).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted.size());
        assertEquals(0, nbAfterDeletion);
    }

    @Test
    public void should_delete_by_organization_id() throws TechnicalException {
        int nbBeforeDeletion = commandRepository.search((new CommandCriteria.Builder()).organizationId("ToBeDeleted").build()).size();
        List<String> deleted = commandRepository.deleteByOrganizationId("ToBeDeleted");
        int nbAfterDeletion = commandRepository.search((new CommandCriteria.Builder()).organizationId("ToBeDeleted").build()).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted.size());
        assertEquals(0, nbAfterDeletion);
    }

    @Test
    public void should_delete_by_expired_at_before() throws TechnicalException {
        // All fixture commands have expiredAt in the past except search2 (expiredAt = 2127427200000, year 2037)
        // Use a cutoff between the past dates and search2's date
        Instant cutoff = Instant.ofEpochMilli(2000000000000L); // ~May 2033

        int deletedCount = commandRepository.deleteByExpiredAtBefore(cutoff);

        // 9 commands should be deleted (all except search2)
        assertEquals(9, deletedCount);

        // Verify search2 still exists
        assertTrue(commandRepository.findById("search2").isPresent(), "search2 should still exist");
        // Verify a deleted one is gone
        assertFalse(commandRepository.findById("search1").isPresent(), "search1 should not exist anymore");
    }
}
