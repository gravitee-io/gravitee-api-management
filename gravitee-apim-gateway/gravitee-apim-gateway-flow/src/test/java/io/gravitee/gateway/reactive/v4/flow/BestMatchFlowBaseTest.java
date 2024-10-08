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

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.FlowBaseTest;
import io.gravitee.gateway.reactive.v4.flow.selection.HttpSelectorConditionFilter;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class BestMatchFlowBaseTest extends FlowBaseTest {

    public final ConditionFilter conditionFilter = new CompositeConditionFilter(new HttpSelectorConditionFilter());

    public TestFlowResolver flowResolver;

    @Before
    public void setUp() {
        flowResolver = new TestFlowResolver(conditionFilter, buildFlows());
    }

    private List<Flow> buildFlows() {
        return flowPaths
            .stream()
            .map(path -> {
                Flow flow = new Flow();
                // Http selector
                HttpSelector httpSelector = new HttpSelector();
                httpSelector.setPath(path);
                // No need to test different operator in this test.
                // Input of BestMatchPolicyResolver is already filtered by PathBasedConditionEvaluator
                httpSelector.setPathOperator(operator);

                // Channel selector
                ChannelSelector channelSelector = new ChannelSelector();
                channelSelector.setChannel(path);
                // No need to test different operator in this test.
                // Input of BestMatchPolicyResolver is already filtered by PathBasedConditionEvaluator
                channelSelector.setChannelOperator(operator);
                flow.setSelectors(List.of(httpSelector, channelSelector));
                return flow;
            })
            .collect(Collectors.toList());
    }

    protected static class TestFlowResolver extends AbstractFlowResolver {

        private final List<Flow> flows;

        public TestFlowResolver(ConditionFilter<BaseExecutionContext, Flow> conditionFilter, List<Flow> flows) {
            super(conditionFilter);
            this.flows = flows;
        }

        @Override
        public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
            return Flowable.fromIterable(flows);
        }

        @Override
        public Flowable<Flow> resolve(BaseExecutionContext ctx) {
            return super.resolve(ctx);
        }
    }
}
