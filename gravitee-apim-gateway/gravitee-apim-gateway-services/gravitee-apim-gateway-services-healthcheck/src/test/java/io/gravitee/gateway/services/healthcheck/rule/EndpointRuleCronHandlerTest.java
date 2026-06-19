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
package io.gravitee.gateway.services.healthcheck.rule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EndpointRuleCronHandlerTest {

    @Test
    void should_preserve_initial_cron_delay_and_enforce_minimum_interval_on_reschedule() {
        var vertx = mock(Vertx.class);
        var endpoint = mock(Endpoint.class);
        var api = mock(Api.class);
        @SuppressWarnings("unchecked")
        EndpointRule<Endpoint> rule = mock(EndpointRule.class);
        @SuppressWarnings("unchecked")
        EndpointRuleHandler<Endpoint> handler = mock(EndpointRuleHandler.class);
        when(rule.endpoint()).thenReturn(endpoint);
        when(rule.api()).thenReturn(api);
        when(endpoint.getName()).thenReturn("endpoint");
        when(api.getId()).thenReturn("api");
        when(handler.getDelayMillis()).thenReturn(500L);
        var cut = new EndpointRuleCronHandler<>(vertx, rule, 0, 5_000L);

        cut.schedule(handler);

        verify(vertx).setTimer(eq(500L), same(cut));
        @SuppressWarnings("unchecked")
        var rescheduleCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(handler).setRescheduleHandler(rescheduleCaptor.capture());

        cut.handle(1L);
        rescheduleCaptor.getValue().handle(null);

        verify(vertx).setTimer(longThat(delay -> delay >= 4_900L && delay <= 5_000L), same(cut));
    }
}
