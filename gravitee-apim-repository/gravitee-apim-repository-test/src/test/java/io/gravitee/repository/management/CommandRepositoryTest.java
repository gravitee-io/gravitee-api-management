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
import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;

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
        assertEquals("id", "msg-to-create", command.getId());
        assertEquals("organization id", "DEFAULT", command.getOrganizationId());
        assertEquals("environment id", "DEFAULT", command.getEnvironmentId());
        assertEquals("to", "someone", command.getTo());
        assertTrue("tags: DATA_TO_INDEX", command.getTags().contains("DATA_TO_INDEX"));
        assertTrue("tags: INSERT", command.getTags().contains("INSERT"));
        assertEquals("content", "Hello, is it me you're looking for?", command.getContent());
        assertTrue("acknowledgments: 1", command.getAcknowledgments().contains("1"));
        assertTrue("acknowledgments: a", command.getAcknowledgments().contains("a"));
        assertTrue("createdAt", compareDate(new Date(1546305346000L), command.getCreatedAt()));
        assertTrue("updatedAt", compareDate(new Date(1548983746000L), command.getUpdatedAt()));
        assertTrue("deleteAt", compareDate(new Date(1551402946000L), command.getExpiredAt()));
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

        assertEquals("id", command.getId(), updatedCommand.getId());
        assertEquals("environmment id.", command.getEnvironmentId(), updatedCommand.getEnvironmentId());
        assertEquals("organization id.", command.getOrganizationId(), updatedCommand.getOrganizationId());
        assertEquals("to", command.getTo(), updatedCommand.getTo());
        assertEquals("from", command.getFrom(), updatedCommand.getFrom());
        assertTrue("tags: DATA_TO_INDEX", command.getTags().containsAll(updatedCommand.getTags()));
        assertEquals("content", command.getContent(), updatedCommand.getContent());
        assertTrue(
            "acknowledgments: " + updatedCommand.getAcknowledgments().size() + "/" + command.getAcknowledgments().size(),
            command.getAcknowledgments().containsAll(updatedCommand.getAcknowledgments())
        );
        assertTrue("createdAt", compareDate(command.getCreatedAt(), updatedCommand.getCreatedAt()));
        assertTrue("updatedAt", compareDate(command.getUpdatedAt(), updatedCommand.getUpdatedAt()));
        assertTrue("deleteAt", compareDate(command.getExpiredAt(), updatedCommand.getExpiredAt()));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownMessage() throws Exception {
        Command unknownCommand = new Command();
        commandRepository.update(unknownCommand);
        fail("An unknown message should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        commandRepository.update(null);
        fail("A null message should not be updated");
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        String idToDelete = "msg-to-delete";
        Optional<Command> message = commandRepository.findById(idToDelete);
        assertTrue("msg should exists before being deleted", message.isPresent());

        commandRepository.delete(idToDelete);

        message = commandRepository.findById(idToDelete);
        assertFalse("msg should not exists after being deleted", message.isPresent());

        // Deletion should be idempotent and not throw exception if message does not exist
        commandRepository.delete(idToDelete);
    }

    @Test
    public void shouldSearchByNotFrom() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notFrom("node1").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 5, commands.size());
        List<String> ids = commands.stream().map(Command::getId).collect(Collectors.toList());
        assertFalse("not contain 'search1'", ids.contains("search1"));
        assertTrue("contain 'search2'", ids.contains("search2"));
        assertTrue("contain 'search3'", ids.contains("search3"));
    }

    @Test
    public void shouldSearchByTo() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).to("node1").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 1, commands.size());
        assertEquals("contain 'search3'", "search3", commands.get(0).getId());
    }

    @Test
    public void shouldSearchByTag() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).tags("INSERT").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 4, commands.size());
        List<String> ids = commands.stream().map(Command::getId).collect(Collectors.toList());
        assertTrue("contain 'msg-to-create'", ids.contains("msg-to-create"));
        assertTrue("contain 'msg-to-update'", ids.contains("msg-to-update"));
        assertTrue("contain 'search1'", ids.contains("search1"));
        assertTrue("contain 'search2'", ids.contains("search2"));
    }

    @Test
    public void shouldSearchByTags() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).tags("DATA_TO_INDEX", "DELETE").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 5, commands.size());
        assertTrue(
            "contain [msg-to-create, msg-to-update, search1, search2, search3]",
            commands
                .stream()
                .map(Command::getId)
                .allMatch(List.of("msg-to-create", "msg-to-update", "search1", "search2", "search3")::contains)
        );
    }

    @Test
    public void shouldSearchByNotAck() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notAckBy("node3").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 4, commands.size());
        List<String> ids = commands.stream().map(Command::getId).collect(Collectors.toList());
        assertFalse("not contain 'search1'", ids.contains("search1"));
        assertFalse("not contain 'search3'", ids.contains("search3"));
    }

    @Test
    public void shouldSearchByNotDeleted() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).notDeleted().build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 1, commands.size());
        assertEquals("contain 'search2'", "search2", commands.get(0).getId());
    }

    @Test
    public void shouldSearchByEnvironment() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).environmentId("DEFAULT").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 1, commands.size());
        assertEquals("contain 'msg-to-create'", "msg-to-create", commands.get(0).getId());
    }

    @Test
    public void shouldSearchByOrganization() {
        List<Command> commands = commandRepository.search((new CommandCriteria.Builder()).organizationId("DEFAULT").build());

        assertNotNull("not null", commands);
        assertFalse("not empty", commands.isEmpty());
        assertEquals("result size", 1, commands.size());
        assertEquals("contain 'msg-to-create'", "msg-to-create", commands.get(0).getId());
    }
}
