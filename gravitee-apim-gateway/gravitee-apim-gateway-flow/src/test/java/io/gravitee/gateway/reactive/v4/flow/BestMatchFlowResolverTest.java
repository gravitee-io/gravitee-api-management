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
package io.gravitee.gateway.reactive.v4.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Optional;
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

    @Mock
    public ReactableApi reactableApi;

    public AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector = new BestMatchFlowSelector();

    @Test
    public void should_resolve_bestMatchFlow_with_api_sync() {
        BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver, bestMatchFlowSelector);
        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);
        Api api = new Api();
        api.setType(ApiType.PROXY);
        when(reactableApi.getDefinition()).thenReturn(api);
        when(executionContext.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API)).thenReturn(reactableApi);

        final TestSubscriber<Flow> obs = cut.resolve(executionContext).test();
        obs.assertComplete();

        if (expectedBestMatchResult == null) {
            obs.assertNoValues();
        } else {
            obs.assertValue(bestMatchFlow -> {
                Optional<Selector> selector = bestMatchFlow.selectorByType(SelectorType.HTTP);
                assertThat(selector).isPresent();
                HttpSelector httpSelector = (HttpSelector) selector.get();
                assertThat(httpSelector.getPath()).isEqualTo(expectedBestMatchResult);
                return true;
            });
        }
    }

    @Test
    public void should_resolve_bestMatchFlow_with_api_async() {
        BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver, bestMatchFlowSelector);
        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);
        Api api = new Api();
        api.setType(ApiType.MESSAGE);
        when(reactableApi.getDefinition()).thenReturn(api);
        when(executionContext.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API)).thenReturn(reactableApi);

        final TestSubscriber<Flow> obs = cut.resolve(executionContext).test();
        obs.assertComplete();

        if (expectedBestMatchResult == null) {
            obs.assertNoValues();
        } else {
            obs.assertValue(bestMatchFlow -> {
                Optional<Selector> selector = bestMatchFlow.selectorByType(SelectorType.CHANNEL);
                assertThat(selector).isPresent();
                ChannelSelector channelSelector = (ChannelSelector) selector.get();
                assertThat(channelSelector.getChannel()).isEqualTo(expectedBestMatchResult);
                return true;
            });
        }
    }
}
