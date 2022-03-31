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

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.flow.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.flow.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.condition.ConditionalFlowResolver;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class BestMatchFlowResolverTest {

    private BestMatchFlowResolver cut;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FlowResolver flowResolver;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Request request;

    @Parameterized.Parameter(0)
    public List<String> flowPaths;

    @Parameterized.Parameter(1)
    public Operator operator;

    @Parameterized.Parameter(2)
    public String expectedBestMatchResult;

    @Parameterized.Parameter(3)
    public String requestPath;

    private final ConditionEvaluator evaluator = new CompositeConditionEvaluator(new PathBasedConditionEvaluator());

    @Before
    public void setUp() {
        cut = new BestMatchFlowResolver(new TestFlowResolver(evaluator, buildFlows()));
    }

    @Test
    public void shouldResolveBestMatchFlowApiResolver() {
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

    /**
     * Build list of parameters for test case.
     * @return Tests parameter objects with this structure:
     * { list of flow paths, expected path result, path used by request}
     */
    @Parameterized.Parameters(name = "{index}: Configured flows={0}, Request={3}, Operator={1}, Expected BestMatch={2}")
    public static Iterable<Object> data() {
        return Arrays.asList(
            new Object[][] {
                { List.of(), Operator.STARTS_WITH, null, "/path/55" },
                { List.of("/"), Operator.STARTS_WITH, "/", "" },
                { List.of("/"), Operator.STARTS_WITH, "/", "/" },
                { List.of("/"), Operator.STARTS_WITH, "/", "/path/55" },
                { List.of("/"), Operator.EQUALS, "/", "" },
                { List.of("/"), Operator.EQUALS, "/", "/" },
                { List.of("/"), Operator.EQUALS, null, "/path/55" },
                { List.of("/", "/path"), Operator.STARTS_WITH, "/", "/random" },
                { List.of("/", "/path"), Operator.STARTS_WITH, "/path", "/path/55" },
                { List.of("/path/:id"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id"), Operator.EQUALS, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.EQUALS, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.STARTS_WITH, "/path/staticId", "/path/staticId" },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/staticId",
                    "/path/staticId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/staticId",
                    "/path/staticId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/staticId",
                    "/path/staticId/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/secondId",
                    "/path/staticId/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/secondId",
                    "/path/5555/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/secondId",
                    "/path/5555/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id",
                    "/path/5555",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id",
                    "/path/5555",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/:id2",
                    "/path/5555/5959",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/:id2",
                    "/path/5555/5959",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/:id2",
                    "/path/5555/5559/5553",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/:id2",
                    "/path/5555/5559/5553",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2", "/path/:id/subResource/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/subResource/:id2",
                    "/path/5555/subResource/5553",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2", "/path/:id/subResource/:id2"),
                    Operator.EQUALS,
                    "/path/:id/subResource/:id2",
                    "/path/5555/subResource/5553",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/book/9999/chapter/145",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.EQUALS,
                    "/book/9999/chapter/145",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/",
                    "/food",
                },
                {
                    List.of(
                        "/book",
                        "/book/:bookId/chapter/:chapterId/page/:pageId",
                        "/book/:bookId",
                        "/book/:bookId/chapter/:chapterId",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/book/9999/chapter",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/book",
                        "/book/:bookId/chapter/:chapterId/page/:pageId",
                        "/book/:bookId",
                        "/book/:bookId/chapter/:chapterId",
                        "/book/9999/chapter"
                    ),
                    Operator.EQUALS,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
                {
                    List.of("/book", "/book/:bookId/chapter/:chapterId/page/:pageId", "/book/:bookId", "/book/:bookId/chapter/:chapterId"),
                    Operator.STARTS_WITH,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
                {
                    List.of("/book", "/book/:bookId/chapter/:chapterId/page/:pageId", "/book/:bookId", "/book/:bookId/chapter/:chapterId"),
                    Operator.EQUALS,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
            }
        );
    }

    private List<Flow> buildFlows() {
        return flowPaths
            .stream()
            .map(
                path -> {
                    Flow flow = new Flow();
                    PathOperator pathOperator = new PathOperator();
                    pathOperator.setPath(path);
                    // No need to test different operator in this test.
                    // Input of BestMatchPolicyResolver is already filtered by PathBasedConditionEvaluator
                    pathOperator.setOperator(operator);
                    flow.setPathOperator(pathOperator);
                    return flow;
                }
            )
            .collect(Collectors.toList());
    }

    private static class TestFlowResolver extends ConditionalFlowResolver {

        private List<Flow> flows;

        public TestFlowResolver(ConditionEvaluator evaluator, List<Flow> flows) {
            super(evaluator);
            this.flows = flows;
        }

        @Override
        protected List<Flow> resolve0(ExecutionContext context) {
            return flows;
        }
    }
}
