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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.event.CommandEvent;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionFailureNoticationCommandListenerImplTest {

    public static final String VALID_COMMAND_CONTENT =
        "{\n" + "  \"subscriptionId\": \"subscription-id\",\n" + "  \"failureCause\": \"failure-cause\"\n" + "}";

    public static final String INVALID_COMMAND_CONTENT =
        "{\n" + "  \"subscriptionId\": \"subscription-id\",\n" + "  \"failureCause\": \"failu";

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EventManager eventManager;

    private ObjectMapper objectMapper = new GraviteeMapper();

    private SubscriptionFailureNotificationCommandListenerImpl cut;

    @Before
    public void setUp() {
        cut = new SubscriptionFailureNotificationCommandListenerImpl(eventManager, subscriptionService, objectMapper);
    }

    @Test
    public void shouldDoNothingWhenEventHasNoContent() {
        cut.onEvent(new SimpleEvent<>(CommandEvent.TO_PROCESS, null));

        verify(subscriptionService, never()).fail(any(), any());
    }

    @Test
    public void shouldDoNothingWhenCommandIsNotSubscriptionFailure() {
        cut.onEvent(new SimpleEvent<>(CommandEvent.TO_PROCESS, buildCommand(List.of(CommandTags.DATA_TO_INDEX), VALID_COMMAND_CONTENT)));

        verify(subscriptionService, never()).fail(any(), any());
    }

    @Test
    public void shouldDoNothingWhenCommandHasNoContent() {
        cut.onEvent(
            new SimpleEvent<>(CommandEvent.TO_PROCESS, buildCommand(List.of(CommandTags.SUBSCRIPTION_FAILURE_NOTIFICATION_RETRY), null))
        );

        verify(subscriptionService, never()).fail(any(), any());
    }

    @Test
    public void shouldDoNothingWhenCommandHasInvalidContent() {
        cut.onEvent(
            new SimpleEvent<>(
                CommandEvent.TO_PROCESS,
                buildCommand(List.of(CommandTags.SUBSCRIPTION_FAILURE_NOTIFICATION_RETRY), INVALID_COMMAND_CONTENT)
            )
        );

        verify(subscriptionService, never()).fail(any(), any());
    }

    @Test
    public void shouldCallSubscriptionServiceFailWhenEventIsGood() {
        cut.onEvent(
            new SimpleEvent<>(
                CommandEvent.TO_PROCESS,
                buildCommand(List.of(CommandTags.SUBSCRIPTION_FAILURE_NOTIFICATION_RETRY), VALID_COMMAND_CONTENT)
            )
        );

        verify(subscriptionService).notifyError("subscription-id", "failure-cause");
    }

    private static CommandEntity buildCommand(List<CommandTags> tags, String content) {
        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setId("command-id");
        commandEntity.setTags(tags);
        commandEntity.setContent(content);
        return commandEntity;
    }
}
