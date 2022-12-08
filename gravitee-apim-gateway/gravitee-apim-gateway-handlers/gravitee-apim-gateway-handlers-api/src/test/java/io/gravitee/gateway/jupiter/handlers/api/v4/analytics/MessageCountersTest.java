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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageCountersTest {

    private MessageCounters messageCounters;

    @BeforeEach
    public void beforeEach() {
        messageCounters = new MessageCounters();
    }

    @Test
    void should_not_increment_error_counter_when_message_is_not_in_error() {
        MessageCounters.Counters counters = messageCounters.increment(DefaultMessage.builder().build());
        assertThat(counters.messageCount()).isEqualTo(1);
        assertThat(counters.errorCount()).isEqualTo(-1);

        MessageCounters.Counters counters2 = messageCounters.increment(DefaultMessage.builder().build());
        assertThat(counters2.messageCount()).isEqualTo(2);
        assertThat(counters2.errorCount()).isEqualTo(-1);
    }

    @Test
    void should_increment_both_counters_when_message_is_in_error() {
        MessageCounters.Counters counters = messageCounters.increment(DefaultMessage.builder().build());
        assertThat(counters.messageCount()).isEqualTo(1);
        assertThat(counters.errorCount()).isEqualTo(-1);

        MessageCounters.Counters counters2 = messageCounters.increment(DefaultMessage.builder().error(true).build());
        assertThat(counters2.messageCount()).isEqualTo(2);
        assertThat(counters2.errorCount()).isEqualTo(1);
    }
}
