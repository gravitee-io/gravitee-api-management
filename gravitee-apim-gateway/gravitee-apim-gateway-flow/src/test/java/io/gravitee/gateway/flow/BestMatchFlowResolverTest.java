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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.jupiter.flow.BestMatchFlowBaseTest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class BestMatchFlowResolverTest extends BestMatchFlowBaseTest {

    @Mock
    public ExecutionContext executionContext;

    @Mock
    public Request request;

    @Test
    public void shouldResolveBestMatchFlowApiResolver() {
        BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver);

        when(executionContext.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(requestPath);

        final List<Flow> result = cut.resolve(executionContext);

        if (expectedBestMatchResult == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).hasSize(1);
            final Flow bestMatchFlow = result.get(0);
            assertThat(bestMatchFlow.getPath()).isEqualTo(expectedBestMatchResult);
        }
    }
}
