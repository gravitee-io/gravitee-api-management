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
package io.gravitee.gateway.jupiter.handlers.api.adapter.condition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
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
class ConditionEvaluatorAdapterTest {

    @Mock
    private RequestExecutionContext ctx;

    @Mock
    private ConditionEvaluator<Flow> legacyEvaluator;

    private ConditionEvaluatorAdapter<Flow> cut;

    @BeforeEach
    public void init() {
        cut = new ConditionEvaluatorAdapter<>(legacyEvaluator);
    }

    @Test
    public void shouldInvokeLegacyEvaluator() {
        when(legacyEvaluator.evaluate(any(ExecutionContext.class), any(Flow.class))).thenReturn(true);

        final TestSubscriber<Flow> obs = cut.filter(ctx, Flowable.just(mock(Flow.class), mock(Flow.class))).test();

        obs.assertComplete();
        obs.assertValueCount(2);
        verify(legacyEvaluator, times(2)).evaluate(any(ExecutionContext.class), any(Flow.class));
    }
}
