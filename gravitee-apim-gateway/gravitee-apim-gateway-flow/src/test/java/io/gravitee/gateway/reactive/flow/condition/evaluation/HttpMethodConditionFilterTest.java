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
package io.gravitee.gateway.reactive.flow.condition.evaluation;

import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.GenericRequest;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
class HttpMethodConditionFilterTest {

    private final HttpMethodConditionFilter cut = new HttpMethodConditionFilter();

    @Mock
    private GenericExecutionContext ctx;

    @Mock
    private GenericRequest request;

    @Mock
    private FlowV2Impl flow;

    @BeforeEach
    void init() {
        when(ctx.request()).thenReturn(request);
    }

    @Test
    void shouldNotFilterWithRequestContextWhenNoMethodIsDefined() {
        when(flow.getMethods()).thenReturn(Collections.emptySet());

        final TestObserver<FlowV2Impl> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWithRequestContextWhenInvalidMethod() {
        when(flow.getMethods()).thenReturn(new HashSet<>(Collections.singletonList(HttpMethod.POST)));
        when(request.method()).thenReturn(HttpMethod.GET);

        final TestObserver<FlowV2Impl> obs = cut.filter(ctx, flow).test();

        obs.assertResult();
    }

    @Test
    void shouldNotFilterWithRequestContextWhenValidMethod() {
        when(flow.getMethods()).thenReturn(new HashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)));
        when(request.method()).thenReturn(HttpMethod.GET);

        final TestObserver<FlowV2Impl> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }
}
