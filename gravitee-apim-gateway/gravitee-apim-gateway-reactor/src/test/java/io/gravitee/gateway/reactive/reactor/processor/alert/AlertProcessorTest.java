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
package io.gravitee.gateway.reactive.reactor.processor.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.alert.api.event.Event;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class AlertProcessorTest extends AbstractProcessorTest {

    @Mock
    AlertEventProducer mockEventProducer;

    @Mock
    Node mockNode;

    @Captor
    ArgumentCaptor<Event> eventCaptor;

    private AlertProcessor alertProcessor;

    @BeforeEach
    public void beforeEach() {
        alertProcessor = new AlertProcessor(mockEventProducer, mockNode, "1234");

        ctx.metrics(Metrics.builder().build());
        ctx.setAttribute(ExecutionContext.ATTR_API, "api");
        ctx.setAttribute(ExecutionContext.ATTR_APPLICATION, "application");
        ctx.setAttribute(ExecutionContext.ATTR_PLAN, "plan");
        ctx.setAttribute(ExecutionContext.ATTR_QUOTA_COUNT, "1");
        ctx.setAttribute(ExecutionContext.ATTR_QUOTA_LIMIT, "2");
        ctx.setAttribute(ExecutionContext.ATTR_ORGANIZATION, "organization");
        ctx.setAttribute(ExecutionContext.ATTR_ENVIRONMENT, "environment");

        when(mockNode.id()).thenReturn("node.id");
        when(mockNode.hostname()).thenReturn("node.hostname");
        when(mockNode.application()).thenReturn("node.application");
        when(mockNode.metadata()).thenReturn(Map.ofEntries(Map.entry("tenant", "tenant-id"), Map.entry("installation", "installation-id")));
    }

    @Test
    public void shouldSendAlert() {
        alertProcessor.execute(ctx).test().assertResult();

        verify(mockEventProducer).send(eventCaptor.capture());
        SoftAssertions.assertSoftly(softly -> {
            var event = eventCaptor.getValue();

            softly
                .assertThat(event.properties())
                .containsEntry("tenant", "tenant-id")
                .containsEntry("installation", "installation-id")
                .containsEntry("organization", "organization")
                .containsEntry("environment", "environment");
        });
    }

    @Test
    public void shouldIgnoreErrorWhenSendingAlert() {
        ctx.removeAttribute(ExecutionContext.ATTR_API);
        alertProcessor.execute(ctx).test().assertResult();
        verify(mockEventProducer).send(any());
    }
}
