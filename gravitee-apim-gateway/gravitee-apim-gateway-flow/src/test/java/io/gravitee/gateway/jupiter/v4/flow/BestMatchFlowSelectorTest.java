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
package io.gravitee.gateway.jupiter.v4.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class BestMatchFlowSelectorTest extends BestMatchFlowBaseTest {

    @Mock
    public ExecutionContext executionContext;

    @Mock
    public Request request;

    @Test
    public void should_select_bestMatchFlow_with_api_sync() {
        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);

        List<Flow> flows = flowResolver.resolve(executionContext).toList().blockingGet();
        final Flow bestMatchFlow = BestMatchFlowSelector.forPath(ApiType.SYNC, flows, requestPath);

        if (expectedBestMatchResult == null) {
            assertThat(bestMatchFlow).isNull();
        } else {
            assertThat(bestMatchFlow).isNotNull();
            Optional<Selector> selector = bestMatchFlow.selectorByType(SelectorType.HTTP);
            assertThat(selector).isPresent();
            HttpSelector httpSelector = (HttpSelector) selector.get();
            assertThat(httpSelector.getPath()).isEqualTo(expectedBestMatchResult);
        }
    }

    @Test
    public void should_select_bestMatchFlow_with_api_async() {
        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);

        List<Flow> flows = flowResolver.resolve(executionContext).toList().blockingGet();
        final Flow bestMatchFlow = BestMatchFlowSelector.forPath(ApiType.ASYNC, flows, requestPath);

        if (expectedBestMatchResult == null) {
            assertThat(bestMatchFlow).isNull();
        } else {
            assertThat(bestMatchFlow).isNotNull();
            Optional<Selector> selector = bestMatchFlow.selectorByType(SelectorType.CHANNEL);
            assertThat(selector).isPresent();
            ChannelSelector channelSelector = (ChannelSelector) selector.get();
            assertThat(channelSelector.getChannel()).isEqualTo(expectedBestMatchResult);
        }
    }
}
