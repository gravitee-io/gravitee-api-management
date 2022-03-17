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
import io.gravitee.definition.model.flow.Step;
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

    private Step step;

    @Before
    public void setUp() {
        final Flow flow = new Flow();
        step = new Step();
        step.setEnabled(true);
        flow.setPre(List.of(step));
        cut = new FlowPolicyResolver(flow);
    }

    @Test
    public void shouldCreateNewPolicyMetadataIfNotInCache() {
        assertThat(cut.cache.size()).isEqualTo(0);
        final List<PolicyMetadata> result = cut.resolve(StreamType.ON_REQUEST, executionContext);
        assertThat(cut.cache.size()).isEqualTo(1);
        assertThat(result).hasSize(1);
    }

    @Test
    public void shouldNotCreateNewPolicyMetadataIfInCache() {
        final PolicyMetadata cachedPolicyMetadata = new PolicyMetadata("policy-id", "{}");
        cut.cache.put(step, cachedPolicyMetadata);

        assertThat(cut.cache.size()).isEqualTo(1);
        final List<PolicyMetadata> result = cut.resolve(StreamType.ON_REQUEST, executionContext);
        assertThat(cut.cache.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(cachedPolicyMetadata);
    }
}
