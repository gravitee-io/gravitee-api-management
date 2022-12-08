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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_MESSAGE_RECORDABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.MessageSamplingStrategy;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageAnalyticsHelperTest {

    @Test
    void should_compute_recordable_message_when_strategy_return_true() {
        // Given
        MessageSamplingStrategy messageSamplingStrategy = mock(MessageSamplingStrategy.class);
        DefaultMessage message = DefaultMessage.builder().build();
        when(messageSamplingStrategy.isRecordable(eq(message), anyInt(), anyLong())).thenReturn(true);

        // When
        boolean recordable = MessageAnalyticsHelper.computeRecordable(message, messageSamplingStrategy, 1, 1L);

        // Then
        assertThat(recordable).isTrue();
        assertThat(message.attributes()).containsEntry(ATTR_INTERNAL_MESSAGE_RECORDABLE, true);
    }

    @Test
    void should_not_compute_recordable_message_when_strategy_return_false() {
        // Given
        MessageSamplingStrategy messageSamplingStrategy = mock(MessageSamplingStrategy.class);
        DefaultMessage message = DefaultMessage.builder().build();
        when(messageSamplingStrategy.isRecordable(eq(message), anyInt(), anyLong())).thenReturn(false);

        // When
        boolean recordable = MessageAnalyticsHelper.computeRecordable(message, messageSamplingStrategy, 1, 1L);

        // Then
        assertThat(recordable).isFalse();
        assertThat(message.attributes()).containsEntry(ATTR_INTERNAL_MESSAGE_RECORDABLE, false);
    }

    @Test
    void should_compute_recordable_message_when_message_is_on_error_and_strategy_return_true() {
        // Given
        MessageSamplingStrategy messageSamplingStrategy = mock(MessageSamplingStrategy.class);
        DefaultMessage message = DefaultMessage.builder().error(true).build();
        when(messageSamplingStrategy.isRecordable(eq(message), anyInt(), anyLong())).thenReturn(true);

        // When
        boolean recordable = MessageAnalyticsHelper.computeRecordable(message, messageSamplingStrategy, 1, 1L);

        // Then
        assertThat(recordable).isTrue();
        assertThat(message.attributes()).containsEntry(ATTR_INTERNAL_MESSAGE_RECORDABLE, true);
    }

    @Test
    void should_compute_recordable_message_when_message_is_on_error_and_strategy_return_false() {
        // Given
        MessageSamplingStrategy messageSamplingStrategy = mock(MessageSamplingStrategy.class);
        DefaultMessage message = DefaultMessage.builder().error(true).build();
        when(messageSamplingStrategy.isRecordable(eq(message), anyInt(), anyLong())).thenReturn(false);

        // When
        boolean recordable = MessageAnalyticsHelper.computeRecordable(message, messageSamplingStrategy, 1, 1L);

        // Then
        assertThat(recordable).isTrue();
        assertThat(message.attributes()).containsEntry(ATTR_INTERNAL_MESSAGE_RECORDABLE, true);
    }
}
