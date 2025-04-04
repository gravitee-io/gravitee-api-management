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
package io.gravitee.gateway.flow;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.flow.StepV2;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowPolicyResolverTest {

    private FlowPolicyResolver cut;

    @Mock
    private ExecutionContext executionContext;

    private StepV2 step;

    @Before
    public void setUp() {
        step = StepV2.builder().enabled(true).build();
        final FlowV2Impl flow = FlowV2Impl.builder().pre(List.of(step)).build();
        cut = new FlowPolicyResolver(flow);
    }

    @Test
    public void shouldCreateNewPolicyMetadataIfNotInCache() {
        assertThat(cut.cache.size()).isZero();
        final List<PolicyMetadata> result = cut.resolve(StreamType.ON_REQUEST, executionContext);
        assertThat(cut.cache.size()).isOne();
        assertThat(result).hasSize(1);
    }

    @Test
    public void shouldNotCreateNewPolicyMetadataIfInCache() {
        final PolicyMetadata cachedPolicyMetadata = new PolicyMetadata("policy-id", "{}");
        cut.cache.put(step, cachedPolicyMetadata);

        assertThat(cut.cache.size()).isOne();
        final List<PolicyMetadata> result = cut.resolve(StreamType.ON_REQUEST, executionContext);
        assertThat(cut.cache.size()).isOne();
        assertThat(result.getFirst()).isEqualTo(cachedPolicyMetadata);
    }
}
