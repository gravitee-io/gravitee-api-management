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
package io.gravitee.gateway.jupiter.v4.flow.selection;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
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
class HttpSelectorConditionFilterTest {

    private final HttpSelectorConditionFilter cut = new HttpSelectorConditionFilter();

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
    void shouldNotFilterWithNoHttpSelector() {
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.empty());

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenPathStartWithDefaultValue() {
        when(request.pathInfo()).thenReturn("/my/path");
        HttpSelector httpSelector = new HttpSelector();
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenPathEqualsSelector() {
        when(request.pathInfo()).thenReturn("/my/path");
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/my/path");
        httpSelector.setPathOperator(Operator.EQUALS);
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenPathMatchPatternWithPipeCharacter() {
        when(request.pathInfo()).thenReturn("/my/a|b");
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/my/:path");
        httpSelector.setPathOperator(Operator.EQUALS);
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenPathDoesntEqualSelector() {
        when(request.pathInfo()).thenReturn("/my/path2");
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/my/path");
        httpSelector.setPathOperator(Operator.EQUALS);
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    void shouldNotFilterWhenMethodsEqualsSelector() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(request.method()).thenReturn(HttpMethod.GET);
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setMethods(Set.of(HttpMethod.GET));
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenEntrypointModeDoesntEqualSelector() {
        when(request.method()).thenReturn(HttpMethod.POST);
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setMethods(Set.of(HttpMethod.GET));
        when(flow.selectorByType(SelectorType.HTTP)).thenReturn(Optional.of(httpSelector));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }
}
