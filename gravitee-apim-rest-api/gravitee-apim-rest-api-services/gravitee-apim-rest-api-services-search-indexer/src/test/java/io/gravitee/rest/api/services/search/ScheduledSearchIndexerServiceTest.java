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
package io.gravitee.rest.api.services.search;

import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.CommandConverter;
import io.gravitee.rest.api.service.impl.CommandServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ScheduledSearchIndexerServiceTest {

    @InjectMocks
    ScheduledSearchIndexerService service = new ScheduledSearchIndexerService();

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    CommandService commandService;

    @Mock
    SearchEngineService searchEngineService;

    @Test
    void shouldDoNothing() throws TechnicalException {
        final Organization organization = new Organization();
        organization.setId("DEFAULT");

        final ExecutionContext expectedExecutionContext = new ExecutionContext(organization);

        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        when(commandService.search(eq(expectedExecutionContext), any())).thenReturn(Collections.emptyList());

        service.run();

        verify(commandService, never()).ack(anyString());
        verify(searchEngineService, never()).process(eq(expectedExecutionContext), any());
    }

    @Test
    void shouldRunForEachOrganization() throws TechnicalException {
        final Organization org1 = new Organization();
        org1.setId("DEFAULT_1");

        final ExecutionContext expectedExecutionContextForOrg1 = new ExecutionContext(org1);

        final Organization org2 = new Organization();
        org2.setId("DEFAULT_2");

        final ExecutionContext expectedExecutionContextForOrg2 = new ExecutionContext(org2);

        when(organizationRepository.findAll()).thenReturn(Set.of(org1, org2));

        CommandEntity insert = new CommandEntity();
        insert.setId("insertid");
        insert.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        insert.setContent("{\"id\":\"1\"}");
        CommandEntity delete = new CommandEntity();
        delete.setId("deleteid");
        delete.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        delete.setContent("{\"id\":\"2\"}");

        when(commandService.search(any(ExecutionContext.class), any())).thenAnswer(call -> {
            ExecutionContext context = call.getArgument(0);
            return "DEFAULT_1".equals(context.getOrganizationId()) ? List.of(delete, insert) : List.of();
        });

        service.run();

        verify(commandService, times(1)).search(eq(expectedExecutionContextForOrg1), any());
        verify(commandService, times(2)).ack(anyString());
        verify(searchEngineService, times(2)).process(eq(expectedExecutionContextForOrg1), any());

        verify(commandService, times(1)).search(eq(expectedExecutionContextForOrg2), any());
    }

    @Test
    void shouldInsertAndDelete() throws TechnicalException {
        final Organization organization = new Organization();
        organization.setId("DEFAULT");

        final ExecutionContext expectedExecutionContext = new ExecutionContext(organization);

        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        CommandEntity insert = new CommandEntity();
        insert.setId("insertid");
        insert.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        insert.setContent("{\"id\":\"1\"}");
        CommandEntity delete = new CommandEntity();
        delete.setId("deleteid");
        delete.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        delete.setContent("{\"id\":\"2\"}");
        when(commandService.search(eq(expectedExecutionContext), any())).thenReturn(List.of(delete, insert));

        service.run();

        verify(commandService, times(2)).ack(anyString());
        verify(searchEngineService, times(2)).process(eq(expectedExecutionContext), any());
    }

    @Test
    void should_index_the_remaining_commands_when_one_is_deleted_before_its_acknowledgment() throws TechnicalException {
        final Organization organization = new Organization();
        organization.setId("DEFAULT");
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        CommandRepository commandRepository = mock(CommandRepository.class);
        Command deleted = indexCommand("deleted");
        Command remaining = indexCommand("remaining");
        when(commandRepository.search(any())).thenReturn(List.of(deleted, remaining));
        when(commandRepository.findById("deleted")).thenReturn(Optional.of(deleted));
        when(commandRepository.findById("remaining")).thenReturn(Optional.of(remaining));
        when(commandRepository.update(deleted)).thenThrow(new IllegalStateException("No command found with id [deleted]"));

        ScheduledSearchIndexerService indexer = indexerWith(commandServiceOver(commandRepository));

        indexer.run();

        verify(commandRepository).update(remaining);
        verify(searchEngineService, times(2)).process(eq(new ExecutionContext(organization)), any());
    }

    private ScheduledSearchIndexerService indexerWith(CommandService realCommandService) {
        ScheduledSearchIndexerService indexer = new ScheduledSearchIndexerService();
        ReflectionTestUtils.setField(indexer, "organizationRepository", organizationRepository);
        ReflectionTestUtils.setField(indexer, "commandService", realCommandService);
        ReflectionTestUtils.setField(indexer, "searchEngineService", searchEngineService);
        return indexer;
    }

    private static CommandService commandServiceOver(CommandRepository commandRepository) {
        Node node = mock(Node.class);
        when(node.id()).thenReturn("node-id");
        CommandServiceImpl realCommandService = new CommandServiceImpl();
        ReflectionTestUtils.setField(realCommandService, "commandRepository", commandRepository);
        ReflectionTestUtils.setField(realCommandService, "commandConverter", new CommandConverter(node));
        ReflectionTestUtils.setField(realCommandService, "node", node);
        return realCommandService;
    }

    private static Command indexCommand(String id) {
        Command command = new Command();
        command.setId(id);
        command.setOrganizationId("DEFAULT");
        command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
        command.setTags(List.of(CommandTags.DATA_TO_INDEX.name()));
        command.setContent("{\"id\":\"" + id + "\"}");
        return command;
    }
}
