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
package io.gravitee.gateway.jupiter.flow;

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.jupiter.api.context.HttpRequest;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.reactivex.subscribers.TestSubscriber;
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
    public RequestExecutionContext executionContext;

    @Mock
    public Request request;

    @Test
    public void shouldResolveBestMatchFlowApiResolver() {
        BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver);
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
