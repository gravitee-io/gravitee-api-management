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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowPolicyResolverFactoryTest {

    private FlowPolicyResolverFactory cut;

    @Before
    public void setUp() {
        cut = new FlowPolicyResolverFactory();
    }

    @Test
    public void shouldCreateNewFlowResolverIfNotInCache() {
        assertThat(cut.cache.size()).isEqualTo(0);
        cut.create(new Flow(), new MockFlowResolver());
        assertThat(cut.cache.size()).isEqualTo(1);
    }

    @Test
    public void shouldNotCreateNewFlowResolverIfInCache() {
        final Flow cachedFlow = new Flow();
        final MockFlowResolver flowResolver = new MockFlowResolver();
        final FlowPolicyResolver cachedFlowPolicyResolver = new FlowPolicyResolver(cachedFlow, flowResolver);
        cut.cache.put(cachedFlow, cachedFlowPolicyResolver);

        assertThat(cut.cache.size()).isEqualTo(1);
        FlowPolicyResolver result = cut.create(cachedFlow, flowResolver);
        assertThat(cut.cache.size()).isEqualTo(1);
        assertThat(result).isEqualTo(cachedFlowPolicyResolver);
        result = cut.create(cachedFlow, flowResolver);
        assertThat(cut.cache.size()).isEqualTo(1);
        assertThat(result).isEqualTo(cachedFlowPolicyResolver);
    }

    private static class MockFlowResolver implements FlowResolver {

        @Override
        public List<Flow> resolve(ExecutionContext context) {
            return null;
        }
    }
}
