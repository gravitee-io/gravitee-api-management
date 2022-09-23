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
package io.gravitee.gateway.jupiter.v4.flow.selection;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.api.context.GenericRequest;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ChannelSelectorConditionFilterTest {

    private final ChannelSelectorConditionFilter cut = new ChannelSelectorConditionFilter();

    @Mock
    private GenericExecutionContext ctx;

    @Mock
    private GenericRequest request;

    @Mock
    private EntrypointConnector entrypointConnector;

    @Mock
    private Flow flow;

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(request.pathInfo()).thenReturn("/my/path");
    }

    @Test
    void shouldNotFilterWithNoChannelSelector() {
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.empty());

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenPathStartWithDefaultValue() {
        when(request.pathInfo()).thenReturn("/my/path");
        ChannelSelector channelSelector = new ChannelSelector();
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenPathEqualsSelectorChannel() {
        when(request.pathInfo()).thenReturn("/my/path");
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setChannel("/my/path");
        channelSelector.setChannelOperator(Operator.EQUALS);
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenPathDoesntEqualSelectorChannel() {
        when(request.pathInfo()).thenReturn("/my/path2");
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setChannel("/my/path");
        channelSelector.setChannelOperator(Operator.EQUALS);
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    void shouldNotFilterWhenEntrypointEqualsSelectorChannel() {
        when(entrypointConnector.id()).thenReturn("entrypoint");

        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setEntrypoints(Set.of("entrypoint"));
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenEntrypointDoesntEqualSelectorChannel() {
        when(entrypointConnector.id()).thenReturn("entrypoint");

        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setEntrypoints(Set.of("entrypoint-filter"));
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    void shouldNotFilterWhenEntrypointModeEqualsSelectorChannel() {
        when(entrypointConnector.id()).thenReturn("entrypoint");
        when(entrypointConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE));

        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setEntrypoints(Set.of("entrypoint"));
        channelSelector.setOperations(Set.of(ChannelSelector.Operation.SUBSCRIBE));
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenEntrypointModeDoesntEqualSelectorChannel() {
        when(entrypointConnector.id()).thenReturn("entrypoint");
        when(entrypointConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE));

        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setEntrypoints(Set.of("entrypoint"));
        channelSelector.setOperations(Set.of(ChannelSelector.Operation.PUBLISH));
        when(flow.selectorByType(SelectorType.CHANNEL)).thenReturn(Optional.of(channelSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }
}
