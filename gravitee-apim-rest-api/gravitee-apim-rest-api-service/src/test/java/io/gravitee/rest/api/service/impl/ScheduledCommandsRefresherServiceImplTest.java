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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
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

    @BeforeEach
    public void setUp() {
        cut = refresher(100, 10);
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
                ),
                isNull(),
                eq(100)
            )
        ).thenReturn(List.of());
        cut.run();

        verify(commandService, never()).deleteIfExists(any());
        verify(commandService, never()).ackIfExists(any());
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
                ),
                isNull(),
                eq(100)
            )
        ).thenReturn(List.of(dataCommand1, dataCommand2, dataCommand3, subscriptionCommand1, subscriptionCommand2, subscriptionCommand3));
        when(commandService.deleteIfExists(any())).thenReturn(true);
        when(commandService.ackIfExists(any())).thenReturn(true);
        cut.run();

        ArgumentCaptor<String> deleteCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ackCaptor = ArgumentCaptor.forClass(String.class);

        verify(commandService, times(2)).deleteIfExists(deleteCaptor.capture());
        verify(commandService, times(3)).ackIfExists(ackCaptor.capture());

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
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of());

        cut.run();

        verify(commandService, never()).deleteByExpiredAtBefore(any());
    }

    @Test
    public void shouldDeleteExpiredCommandsWhenRunning() {
        when(commandService.deleteByExpiredAtBefore(any(Instant.class))).thenReturn(2);
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of());

        cut.run();

        verify(commandService).deleteByExpiredAtBefore(any(Instant.class));
        verify(eventManager, never()).publishEvent(any(), any());
    }

    @Test
    void should_read_pages_until_one_comes_back_short() {
        cut = refresher(2, 10);
        CommandEntity b = multicast("b");
        CommandEntity d = multicast("d");
        when(commandService.search(any(), isNull(), eq(2))).thenReturn(List.of(multicast("a"), b));
        when(commandService.search(any(), eq(b), eq(2))).thenReturn(List.of(multicast("c"), d));
        when(commandService.search(any(), eq(d), eq(2))).thenReturn(List.of(multicast("e")));
        when(commandService.ackIfExists(any())).thenReturn(true);

        cut.run();

        verify(commandService, times(3)).search(any(), any(), eq(2));
        assertThat(publishedIds()).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void should_stop_after_the_maximum_number_of_pages() {
        cut = refresher(2, 3);
        CommandEntity b = multicast("b");
        CommandEntity d = multicast("d");
        CommandEntity f = multicast("f");
        when(commandService.search(any(), isNull(), eq(2))).thenReturn(List.of(multicast("a"), b));
        when(commandService.search(any(), eq(b), eq(2))).thenReturn(List.of(multicast("c"), d));
        when(commandService.search(any(), eq(d), eq(2))).thenReturn(List.of(multicast("e"), f));
        when(commandService.ackIfExists(any())).thenReturn(true);

        cut.run();

        verify(commandService, times(3)).search(any(), any(), eq(2));
        verify(commandService, never()).search(any(), eq(f), anyInt());
        assertThat(publishedIds()).containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void should_read_past_a_full_page_of_commands_it_cannot_clear() {
        cut = refresher(2, 10);
        CommandEntity failing = multicast("failing");
        when(commandService.search(any(), isNull(), eq(2))).thenReturn(
            List.of(buildCommand("expired", List.of(CommandTags.EMAIL_TEMPLATE_UPDATE), true), failing)
        );
        when(commandService.search(any(), eq(failing), eq(2))).thenReturn(List.of(multicast("newer")));
        when(commandService.ackIfExists("failing")).thenReturn(false);
        when(commandService.ackIfExists("newer")).thenReturn(true);

        cut.run();

        verify(commandService, never()).ackIfExists("expired");
        assertThat(publishedIds()).containsExactly("newer");
    }

    /**
     * The commands table allows no creation date, and the next page starts after the last command's: a command without
     * one ends the run, and the commands after it are read on the next runs, once it is claimed and no longer read.
     */
    @Test
    void should_stop_reading_at_a_full_page_ending_with_a_command_without_creation_date() {
        cut = refresher(2, 10);
        CommandEntity legacy = multicast("legacy");
        legacy.setCreatedAt(null);
        when(commandService.search(any(), isNull(), eq(2))).thenReturn(List.of(multicast("a"), legacy));
        when(commandService.ackIfExists(any())).thenReturn(true);

        cut.run();

        verify(commandService, times(1)).search(any(), any(), anyInt());
        assertThat(publishedIds()).containsExactly("a", "legacy");
    }

    @Test
    void should_not_process_a_unicast_command_another_node_claimed() {
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of(unicast("claimed-elsewhere"), unicast("claimed-here")));
        when(commandService.deleteIfExists("claimed-elsewhere")).thenReturn(false);
        when(commandService.deleteIfExists("claimed-here")).thenReturn(true);

        cut.run();

        assertThat(publishedIds()).containsExactly("claimed-here");
    }

    @Test
    void should_leave_a_unicast_command_pending_when_its_claim_fails() {
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of(unicast("failing"), unicast("working")));
        when(commandService.deleteIfExists("failing")).thenThrow(new TechnicalManagementException("database unavailable"));
        when(commandService.deleteIfExists("working")).thenReturn(true);

        cut.run();

        verify(commandService, never()).ackIfExists(any());
        assertThat(publishedIds()).containsExactly("working");
    }

    @Test
    void should_not_process_a_multicast_command_it_could_not_acknowledge() {
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of(multicast("not-acknowledged"), multicast("acknowledged")));
        when(commandService.ackIfExists("not-acknowledged")).thenReturn(false);
        when(commandService.ackIfExists("acknowledged")).thenReturn(true);

        cut.run();

        assertThat(publishedIds()).containsExactly("acknowledged");
    }

    @Test
    void should_claim_each_command_before_publishing_it() {
        CommandEntity unicast = unicast("unicast");
        CommandEntity multicast = multicast("multicast");
        when(commandService.search(any(), any(), anyInt())).thenReturn(List.of(unicast, multicast));
        when(commandService.deleteIfExists("unicast")).thenReturn(true);
        when(commandService.ackIfExists("multicast")).thenReturn(true);

        cut.run();

        InOrder inOrder = inOrder(commandService, eventManager);
        inOrder.verify(commandService).deleteIfExists("unicast");
        inOrder.verify(eventManager).publishEvent(CommandEvent.TO_PROCESS, unicast);
        inOrder.verify(commandService).ackIfExists("multicast");
        inOrder.verify(eventManager).publishEvent(CommandEvent.TO_PROCESS, multicast);
    }

    @Test
    void should_process_the_later_commands_of_the_run_when_a_listener_throws() {
        cut = refresher(2, 10);
        CommandEntity poison = unicast("poison");
        when(commandService.search(any(), isNull(), eq(2))).thenReturn(List.of(unicast("before"), poison));
        when(commandService.search(any(), eq(poison), eq(2))).thenReturn(List.of(unicast("after")));
        when(commandService.deleteIfExists(any())).thenReturn(true);
        doThrow(new IllegalStateException("listener failure")).when(eventManager).publishEvent(CommandEvent.TO_PROCESS, poison);

        cut.run();

        verify(commandService).deleteIfExists("after");
        assertThat(publishedIds()).containsExactly("before", "poison", "after");
    }

    @Test
    void should_reject_a_batch_size_or_batch_count_below_one() {
        assertThatThrownBy(() -> refresher(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> refresher(100, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_a_batch_size_or_batch_count_above_the_limit() {
        assertThatThrownBy(() -> refresher(1_001, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> refresher(100, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThat(refresher(1_000, 100)).isNotNull();
    }

    private ScheduledCommandsRefresherServiceImpl refresher(int bulkItems, int maxBulks) {
        return new ScheduledCommandsRefresherServiceImpl(
            commandService,
            node,
            taskScheduler,
            "0/5 * * * * *",
            bulkItems,
            maxBulks,
            eventManager,
            clusterManager
        );
    }

    private List<String> publishedIds() {
        ArgumentCaptor<CommandEntity> published = ArgumentCaptor.forClass(CommandEntity.class);
        verify(eventManager, atLeast(0)).publishEvent(eq(CommandEvent.TO_PROCESS), published.capture());
        return published.getAllValues().stream().map(CommandEntity::getId).toList();
    }

    private static CommandEntity multicast(String id) {
        return buildCommand(id, List.of(CommandTags.EMAIL_TEMPLATE_UPDATE), false);
    }

    private static CommandEntity unicast(String id) {
        return buildCommand(id, List.of(CommandTags.SUBSCRIPTION_FAILURE), false);
    }

    private static CommandEntity buildCommand(String id, List<CommandTags> tags, boolean expired) {
        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setId(id);
        commandEntity.setTags(tags);
        commandEntity.setExpired(expired);
        commandEntity.setCreatedAt(new Date());
        return commandEntity;
    }
}
