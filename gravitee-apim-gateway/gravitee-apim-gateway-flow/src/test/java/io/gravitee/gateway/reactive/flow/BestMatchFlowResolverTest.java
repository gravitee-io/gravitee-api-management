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
package io.gravitee.gateway.reactive.flow;

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.flow.BestMatchFlowSelector;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class BestMatchFlowResolverTest extends BestMatchFlowBaseTest {

    @Mock
    public HttpPlainExecutionContext executionContext;

    @Mock
    public HttpPlainRequest request;

    private AbstractBestMatchFlowSelector bestMatchFlowSelector = new BestMatchFlowSelector();

    @Test
    public void shouldResolveBestMatchFlowApiResolver() {
        BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver, bestMatchFlowSelector);
        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);

        final TestSubscriber<Flow> obs = cut.resolve(executionContext).test();
        obs.assertComplete();

        if (expectedBestMatchResult == null) {
            obs.assertNoValues();
        } else {
            obs.assertValue(bestMatchFlow -> bestMatchFlow.getPath().equals(expectedBestMatchResult));
        }
    }
}
