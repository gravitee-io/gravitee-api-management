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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.event.CommandEvent;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledCommandsRefresherServiceImplTest {

    @Mock
    private CommandService commandService;

    @Mock
    private Node node;

    @Mock
    private EventManager eventManager;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private Member clusterMember;

    private ScheduledCommandsRefresherServiceImpl cut;

    @Before
    public void setUp() {
        cut = new ScheduledCommandsRefresherServiceImpl(commandService, node, taskScheduler, "0/5 * * * * *", eventManager, clusterManager);
        when(node.id()).thenReturn("node-id");
        when(clusterManager.self()).thenReturn(clusterMember);
        when(clusterMember.primary()).thenReturn(true);
    }

    @Test
    public void shouldNotFindCommand() {
        when(
            commandService.search(
                ArgumentMatchers.argThat(
                    query -> query.getTo().equals(MessageRecipient.MANAGEMENT_APIS.name()) && query.getNotAckBy().equals("node-id")
                )
            )
        ).thenReturn(List.of());
        cut.run();

        verify(commandService, never()).delete(any());
        verify(commandService, never()).ack(any());
        verify(eventManager, never()).publishEvent(any(), any());
    }

    @Test
    public void shouldSyncCommandAndPublishEventAccordingly() {
        final CommandEntity dataCommand1 = buildCommand("data-1", null, false);
        final CommandEntity dataCommand2 = buildCommand("data-2", List.of(CommandTags.DATA_TO_INDEX), false);
        // this case has no real business sense but is here to test the code
        final CommandEntity dataCommand3 = buildCommand(
            "data-subscription-3",
            List.of(CommandTags.DATA_TO_INDEX, CommandTags.SUBSCRIPTION_FAILURE),
            false
        );
        final CommandEntity subscriptionCommand1 = buildCommand("subscription-1", List.of(CommandTags.SUBSCRIPTION_FAILURE), false);
        final CommandEntity subscriptionCommand2 = buildCommand("subscription-2", List.of(CommandTags.SUBSCRIPTION_FAILURE), false);
        final CommandEntity subscriptionCommand3 = buildCommand("subscription-3", List.of(CommandTags.SUBSCRIPTION_FAILURE), true);

        when(
            commandService.search(
                ArgumentMatchers.argThat(
                    query ->
                        query.getTo().equals(MessageRecipient.MANAGEMENT_APIS.name()) &&
                        query.getNotAckBy().equals("node-id") &&
                        !query.getTags().contains(CommandTags.DATA_TO_INDEX)
                )
            )
        ).thenReturn(List.of(dataCommand1, dataCommand2, dataCommand3, subscriptionCommand1, subscriptionCommand2, subscriptionCommand3));
        cut.run();

        ArgumentCaptor<String> deleteCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ackCaptor = ArgumentCaptor.forClass(String.class);

        verify(commandService, times(2)).delete(deleteCaptor.capture());
        verify(commandService, times(3)).ack(ackCaptor.capture());

        assertThat(ackCaptor.getAllValues()).containsExactly("data-1", "data-2", "data-subscription-3");
        assertThat(deleteCaptor.getAllValues()).containsExactly("subscription-1", "subscription-2");

        ArgumentCaptor<CommandEntity> argumentCaptor = ArgumentCaptor.forClass(CommandEntity.class);
        verify(eventManager, times(5)).publishEvent(eq(CommandEvent.TO_PROCESS), argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues())
            .extracting(CommandEntity::getId)
            .containsOnly("data-1", "data-2", "data-subscription-3", "subscription-1", "subscription-2");
    }

    @Test
    public void shouldNotDeleteExpiredCommandsWhenNotPrimary() {
        when(clusterMember.primary()).thenReturn(false);
        when(commandService.search(any())).thenReturn(List.of());

        cut.run();

        verify(commandService, never()).deleteByExpiredAtBefore(any());
    }

    @Test
    public void shouldDeleteExpiredCommandsWhenRunning() {
        when(commandService.deleteByExpiredAtBefore(any(Instant.class))).thenReturn(2);
        when(commandService.search(any())).thenReturn(List.of());

        cut.run();

        verify(commandService).deleteByExpiredAtBefore(any(Instant.class));
        verify(eventManager, never()).publishEvent(any(), any());
    }

    private static CommandEntity buildCommand(String id, List<CommandTags> tags, boolean expired) {
        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setId(id);
        commandEntity.setTags(tags);
        commandEntity.setExpired(expired);
        return commandEntity;
    }
}
