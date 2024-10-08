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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
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
class FlowResolverFactoryTest {

    @Mock
    private ConditionFilter<BaseExecutionContext, Flow> conditionFilter;

    @Mock
    private AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector;

    private FlowResolverFactory cut;
    private Api api;

    @BeforeEach
    void init() {
        cut = new FlowResolverFactory(conditionFilter, bestMatchFlowSelector);
        final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setType(ApiType.PROXY);
        api = new Api(definition);
        definition.setFlowExecution(new FlowExecution());
    }

    @Test
    void shouldCreateApiFlowResolver() {
        final FlowResolver flowResolver = cut.forApi(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiFlowResolver);
    }

    @Test
    void shouldCreateApiPlanFlowResolver() {
        final FlowResolver flowResolver = cut.forApiPlan(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiPlanFlowResolver);
    }
}
