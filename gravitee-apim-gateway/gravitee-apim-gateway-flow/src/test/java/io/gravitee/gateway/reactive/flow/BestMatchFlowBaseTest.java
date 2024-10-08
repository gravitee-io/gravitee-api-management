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

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.condition.ConditionalFlowResolver;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
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

    public final ConditionEvaluator evaluator = new CompositeConditionEvaluator(new PathBasedConditionEvaluator());

    public TestFlowResolver flowResolver;

    @Before
    public void setUp() {
        flowResolver = new TestFlowResolver(evaluator, buildFlows());
    }

    private List<Flow> buildFlows() {
        return flowPaths
            .stream()
            .map(path -> {
                Flow flow = new Flow();
                PathOperator pathOperator = new PathOperator();
                pathOperator.setPath(path);
                // No need to test different operator in this test.
                // Input of BestMatchPolicyResolver is already filtered by PathBasedConditionEvaluator
                pathOperator.setOperator(operator);
                flow.setPathOperator(pathOperator);
                return flow;
            })
            .collect(Collectors.toList());
    }

    protected static class TestFlowResolver extends ConditionalFlowResolver implements FlowResolver<BaseExecutionContext> {

        private final List<Flow> flows;

        public TestFlowResolver(ConditionEvaluator<Flow> evaluator, List<Flow> flows) {
            super(evaluator);
            this.flows = flows;
        }

        @Override
        protected List<Flow> resolve0(ExecutionContext context) {
            return flows;
        }

        @Override
        public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
            return Flowable.fromIterable(flows);
        }

        @Override
        public Flowable<Flow> resolve(BaseExecutionContext ctx) {
            return Flowable.fromIterable(super.resolve(ExecutionContextAdapter.create((HttpPlainExecutionContext) ctx)));
        }
    }
}
