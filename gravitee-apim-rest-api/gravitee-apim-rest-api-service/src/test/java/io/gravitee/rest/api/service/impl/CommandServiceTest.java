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
package io.gravitee.rest.api.service.impl;

import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.command.CommandQuery;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.CommandConverter;
import io.gravitee.rest.api.service.exceptions.Message2RecipientNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandServiceTest {

    @Mock
    CommandRepository commandRepository;

    @Mock
    CommandConverter commandConverter;

    @Mock
    Node node;

    @InjectMocks
    private CommandServiceImpl commandService = new CommandServiceImpl();

    @Test(expected = Message2RecipientNotFoundException.class)
    public void sendShouldNotCreateCommandWithNullRecipient() {
        final NewCommandEntity newCommand = new NewCommandEntity();
        newCommand.setContent("{}");
        newCommand.setTags(List.of(CommandTags.DATA_TO_INDEX));
        newCommand.setTo(null);
        commandService.send(GraviteeContext.getExecutionContext(), newCommand);
    }

    @Test
    public void sendShouldConvertAndCreate() throws TechnicalException {
        final NewCommandEntity newCommand = new NewCommandEntity();
        newCommand.setContent("{}");
        newCommand.setTags(List.of(CommandTags.DATA_TO_INDEX));
        newCommand.setTo("MANAGEMENT_APIS");
        when(commandConverter.toCommand(any(ExecutionContext.class), any(NewCommandEntity.class))).thenReturn(new Command());
        commandService.send(GraviteeContext.getExecutionContext(), newCommand);
        verify(commandConverter, times(1)).toCommand(any(ExecutionContext.class), eq(newCommand));
        verify(commandRepository, times(1)).create(any(Command.class));
    }

    @Test
    public void shouldSearchByOrganizationAndEnvironmentWithEmptyQuery() {
        final Environment environment = new Environment();
        environment.setId("DEV");
        environment.setOrganizationId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(environment);

        commandService.search(executionContext, new CommandQuery());

        verify(commandRepository, times(1))
            .search(argThat(criteria -> "GRAVITEE".equals(criteria.getOrganizationId()) && "DEV".equals(criteria.getEnvironmentId())));
    }

    @Test
    public void shouldNotSearchByEnvironmentWithOrganizationContext() {
        final Organization organization = new Organization();
        organization.setId("GRAVITEE");

        final ExecutionContext executionContext = new ExecutionContext(organization);

        commandService.search(executionContext, new CommandQuery());

        verify(commandRepository, times(1))
            .search(argThat(criteria -> "GRAVITEE".equals(criteria.getOrganizationId()) && Objects.isNull(criteria.getEnvironmentId())));
    }

    @Test
    public void shouldNotTryToUpdateUnknownCommandOnACK() throws TechnicalException {
        final String commandId = "unknown-command";
        when(commandRepository.findById(eq(commandId))).thenReturn(Optional.empty());
        commandService.ack(commandId);
        verify(commandRepository, never()).update(any());
    }

    @Test
    public void shouldAddNodeIdToAcknowledgmentsOnACK() throws TechnicalException {
        final String commandId = "known-command";
        when(node.id()).thenReturn("test");
        when(commandRepository.findById(eq(commandId))).thenReturn(Optional.of(new Command()));
        commandService.ack(commandId);
        verify(commandRepository, times(1)).update(argThat(command -> command.getAcknowledgments().contains("test")));
    }

    @Test
    public void shouldNotTryToDeleteUnknownCommand() throws TechnicalException {
        final String commandId = "unknown-command";
        when(commandRepository.findById(eq(commandId))).thenReturn(Optional.empty());
        commandService.delete(commandId);
        verify(commandRepository, never()).delete(any());
    }

    @Test
    public void shouldDeleteCommand() throws TechnicalException {
        final String commandId = "known-command";
        when(commandRepository.findById(eq(commandId))).thenReturn(Optional.of(new Command()));
        commandService.delete(commandId);
        verify(commandRepository, times(1)).delete(commandId);
    }
}
