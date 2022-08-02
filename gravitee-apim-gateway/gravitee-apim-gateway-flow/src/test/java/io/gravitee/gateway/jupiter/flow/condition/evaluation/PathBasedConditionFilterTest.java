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
package io.gravitee.gateway.jupiter.flow.condition.evaluation;

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpRequest;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class PathBasedConditionFilterTest {

    private final PathBasedConditionFilter cut = new PathBasedConditionFilter();

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Flow flow;

    @BeforeEach
    void init() {
        when(ctx.request()).thenReturn(request);
    }

    @Test
    public void shouldFilterWhenPathNotEquals() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path2");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    public void shouldFilterWhenPathNotEquals2() {
        when(request.pathInfo()).thenReturn("/my/path2");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    public void shouldNotFilterWhenPathEquals() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenPathEqualsWithTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path/");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenPathEqualsWithFlowTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.EQUALS);
        when(flow.getPath()).thenReturn("/my/path/");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldFilterWhenPathNotStartsWith() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path2");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
    }

    @Test
    public void shouldNotFilterWhenPathStartsWith() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenPathStartsWith2() {
        when(request.pathInfo()).thenReturn("/my/path2");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenPathStartsWithAndTrailingSlash() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/path/subpath/");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenPathStartsWithSingleSlash() {
        when(request.pathInfo()).thenReturn("/my/path");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }

    @Test
    public void shouldNotFilterWhenParam() {
        when(request.pathInfo()).thenReturn("/my/path/subpath");
        when(flow.getOperator()).thenReturn(Operator.STARTS_WITH);
        when(flow.getPath()).thenReturn("/my/:param");

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult(flow);
    }
}
