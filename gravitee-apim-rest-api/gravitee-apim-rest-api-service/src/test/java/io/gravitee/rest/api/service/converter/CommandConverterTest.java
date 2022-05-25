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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandConverterTest {

    @Mock
    Node node;

    @InjectMocks
    CommandConverter converter;

    private static final String NODE_ID = "gravitee-test";

    @Before
    public void setUp() throws Exception {
        when(node.id()).thenReturn(NODE_ID);
    }

    @Test
    public void commandEntityShouldBeProcessedInCurrentNode() {
        final Command command = command(Instant.now(), List.of(NODE_ID));

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertTrue(commandEntity.isProcessedInCurrentNode());
    }

    @Test
    public void commandEntityShouldNotBeProcessedInCurrentNodeWithNullAcknowledgements() {
        final Command command = command(Instant.now(), null);

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertFalse(commandEntity.isProcessedInCurrentNode());
    }

    @Test
    public void commandEntityShouldNotBeProcessedInCurrentNodeWithEmptyAcknowledgements() {
        final Command command = command(Instant.now(), List.of());

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertFalse(commandEntity.isProcessedInCurrentNode());
    }

    @Test
    public void commandEntityShouldNotBeExpired() {
        final Command command = command(Instant.now().plus(1, ChronoUnit.HOURS));

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertFalse(commandEntity.isExpired());
    }

    @Test
    public void commandEntityShouldBeExpired() {
        final Command command = command(Instant.now().minus(5, ChronoUnit.SECONDS));

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertTrue(commandEntity.isExpired());
    }

    @Test
    public void shouldConvertCommandEntityTags() {
        final Command command = command(Instant.now(), List.of(), List.of("DATA_TO_INDEX"));

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertEquals(List.of(CommandTags.DATA_TO_INDEX), commandEntity.getTags());
    }

    @Test
    public void shouldNotConvertCommandEntityTags() {
        final Command command = command(Instant.now(), List.of(), List.of());

        CommandEntity commandEntity = converter.toCommandEntity(command);

        assertNull(commandEntity.getTags());
    }

    @Test
    public void shouldConvertToCommandSettingOrganizationId() {
        final Environment environment = new Environment();
        environment.setId("TEST");
        environment.setOrganizationId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(environment);

        Command command = converter.toCommand(executionContext, newCommandEntity("{}"));

        assertEquals("GRAVITEE", command.getOrganizationId());
        assertEquals("TEST", command.getEnvironmentId());
    }

    @Test
    public void shouldConvertToCommandSettingFromAsNodeId() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        Command command = converter.toCommand(executionContext, newCommandEntity("{}"));

        assertEquals(NODE_ID, command.getFrom());
    }

    @Test
    public void shouldConvertToCommandSettingExpireAt() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        Command command = converter.toCommand(executionContext, newCommandEntity("{}"));

        assertNotNull(command.getExpiredAt());
    }

    @Test
    public void shouldConvertToCommandWithEmptyTags() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        Command command = converter.toCommand(executionContext, newCommandEntity("{}", null));

        assertNotNull(command.getTags());
        assertTrue(command.getTags().isEmpty());
    }

    @Test
    public void shouldConvertToCommandWithTags() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        Command command = converter.toCommand(executionContext, newCommandEntity("{}", List.of(CommandTags.DATA_TO_INDEX)));

        assertNotNull(command.getTags());
        assertEquals(List.of("DATA_TO_INDEX"), command.getTags());
    }

    @Test
    public void shouldConvertToCommandAcceptingNullContent() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        Command command = converter.toCommand(executionContext, newCommandEntity(null));

        assertNull(command.getContent());
    }

    private static Command command(Instant expireAt) {
        return command(expireAt, List.of(), null);
    }

    private static Command command(Instant expireAt, List<String> acknowledgments) {
        return command(expireAt, acknowledgments, null);
    }

    private static Command command(Instant expireAt, List<String> acknowledgments, List<String> tags) {
        final Command command = new Command();
        command.setId("command-id");
        command.setOrganizationId("GRAVITEE");
        command.setEnvironmentId("DEV");
        command.setTo("MANAGEMENT_APIS");
        command.setContent("{}");
        command.setTags(tags);
        command.setExpiredAt(Date.from(expireAt));
        command.setAcknowledgments(acknowledgments);
        return command;
    }

    private static NewCommandEntity newCommandEntity(String content) {
        return newCommandEntity(content, null);
    }

    private static NewCommandEntity newCommandEntity(String content, List<CommandTags> tags) {
        NewCommandEntity command = new NewCommandEntity();
        command.setTtlInSeconds(60);
        command.setTo("MANAGEMENT_APIS");
        command.setContent(content);
        command.setTags(tags);
        return command;
    }
}
