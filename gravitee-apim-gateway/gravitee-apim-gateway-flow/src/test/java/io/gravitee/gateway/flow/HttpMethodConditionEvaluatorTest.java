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
package io.gravitee.gateway.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpMethodConditionEvaluatorTest {

    private final ConditionEvaluator<Flow> evaluator = new HttpMethodConditionEvaluator();

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Flow flow;

    @Before
    public void setUp() {
        when(context.request()).thenReturn(request);
    }

    @Test
    public void shouldEvaluate_noMethod() {
        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_invalidMethod() {
        when(flow.getMethods()).thenReturn(new HashSet<>(Collections.singletonList(HttpMethod.POST)));
        when(request.method()).thenReturn(HttpMethod.GET);

        assertFalse(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_validMethod() {
        when(flow.getMethods()).thenReturn(new HashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)));
        when(request.method()).thenReturn(HttpMethod.GET);

        assertTrue(evaluator.evaluate(flow, context));
    }
}
