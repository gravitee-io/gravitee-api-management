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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.selection.ConditionSelectorConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.selection.HttpSelectorConditionFilter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.helpers.NOPLogger;

/**
 * Characterizes how a flow condition takes part in the BEST_MATCH selection.
 *
 * In BEST_MATCH mode, a condition is not a filter applied to the flow that won the selection: it makes the flow
 * eligible, or not, <b>before</b> the most specific one is picked. A flow whose condition evaluates to false is
 * therefore not a candidate at all, and a less specific flow can be selected in its place.
 *
 * These tests describe the behaviour as it stands, so that it cannot be changed unknowingly.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BestMatchFlowResolverConditionTest {

    private static final String REQUEST_PATH = "/books/145";
    private static final String GOLD_TIER_CONDITION = "{#request.headers['X-Tier'][0] == 'gold'}";

    /**
     * Same wiring as the one built by DefaultApiReactorFactory: http selector and condition selector are both
     * evaluated by the resolver, hence during the selection. The raw type mirrors the production code, the two
     * filters not being bound to the same context type.
     */
    @SuppressWarnings("rawtypes")
    private static final ConditionFilter API_FLOW_FILTER = new CompositeConditionFilter(
        new HttpSelectorConditionFilter(),
        new ConditionSelectorConditionFilter()
    );

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainRequest request;

    @Mock
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(request.pathInfo()).thenReturn(REQUEST_PATH);
        lenient().when(ctx.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    void should_select_the_most_specific_flow_when_its_condition_is_true() {
        givenConditionEvaluatesTo(true);
        final Flow mostSpecific = conditionalFlow("book detail", "/books/:bookId", GOLD_TIER_CONDITION);
        final Flow lessSpecific = flow("all books", "/books");

        final TestSubscriber<Flow> resolved = resolve(mostSpecific, lessSpecific);

        resolved.assertValue(mostSpecific).assertComplete();
    }

    @Test
    void should_select_the_less_specific_flow_when_the_most_specific_condition_is_false() {
        givenConditionEvaluatesTo(false);
        final Flow mostSpecific = conditionalFlow("book detail", "/books/:bookId", GOLD_TIER_CONDITION);
        final Flow lessSpecific = flow("all books", "/books");

        final TestSubscriber<Flow> resolved = resolve(mostSpecific, lessSpecific);

        // The condition removes the flow from the candidates, it does not discard the whole selection.
        resolved.assertValue(lessSpecific).assertComplete();
    }

    @Test
    void should_select_no_flow_when_every_candidate_condition_is_false() {
        givenConditionEvaluatesTo(false);
        final Flow mostSpecific = conditionalFlow("book detail", "/books/:bookId", GOLD_TIER_CONDITION);
        final Flow lessSpecific = conditionalFlow("all books", "/books", GOLD_TIER_CONDITION);

        final TestSubscriber<Flow> resolved = resolve(mostSpecific, lessSpecific);

        resolved.assertNoValues().assertComplete();
    }

    private void givenConditionEvaluatesTo(final boolean result) {
        when(templateEngine.eval(GOLD_TIER_CONDITION, Boolean.class)).thenReturn(Maybe.just(result));
    }

    private TestSubscriber<Flow> resolve(final Flow... flows) {
        final BestMatchFlowResolver cut = new BestMatchFlowResolver(flowResolver(List.of(flows)), new BestMatchFlowSelector());
        return cut.resolve(ctx).test();
    }

    private AbstractFlowResolver flowResolver(final List<Flow> flows) {
        return new AbstractFlowResolver(API_FLOW_FILTER) {
            @Override
            public Flowable<Flow> provideFlows(final BaseExecutionContext ctx) {
                return Flowable.fromIterable(flows);
            }
        };
    }

    private Flow flow(final String name, final String path) {
        final Flow flow = new Flow();
        flow.setName(name);
        flow.setSelectors(List.of(httpSelector(path)));
        return flow;
    }

    private Flow conditionalFlow(final String name, final String path, final String condition) {
        final Flow flow = new Flow();
        flow.setName(name);
        final ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition(condition);
        flow.setSelectors(List.of(httpSelector(path), conditionSelector));
        return flow;
    }

    private HttpSelector httpSelector(final String path) {
        final HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath(path);
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        return httpSelector;
    }
}
