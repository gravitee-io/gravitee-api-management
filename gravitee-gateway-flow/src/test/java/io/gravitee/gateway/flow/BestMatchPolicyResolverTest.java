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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BestMatchPolicyResolverTest {

    @InjectMocks
    private BestMatchPolicyResolver policyResolver;

    @Mock
    private FlowResolver flowResolver;

    @Mock
    private ExecutionContext context;

    @Test
    public void resolve_should_return_empty_list_cause_no_matching_flow() {
        mockInputRequestPath("/turtle");

        List<Flow> flows = List.of(
            buildFlow("/book"),
            buildFlow("/book/:bookId"),
            buildFlow("/book/:bookId/chapter/:chapterId"),
            buildFlow("/book/:bookId/chapter")
        );
        when(flowResolver.resolve(context)).thenReturn(flows);

        List<Flow> filteredFlows = policyResolver.resolve(context);

        assertEquals(0, filteredFlows.size());
    }

    @Test
    public void resolve_should_return_the_flow_with_deeper_matching_path() {
        mockInputRequestPath("/book/9999/chapter/145");

        List<Flow> flows = mockFlows(
            buildFlow("/book"),
            buildFlow("/book/9999/chapter/145/page/200/line"),
            buildFlow("/book/9999/chapter/145/page"),
            buildFlow("/city/washington/street/first/library/amazon/book/9999/chapter/145"),
            buildFlow("/book/7777/chapter/145"),
            buildFlow("/book/9999/chapter/147"),
            buildFlow("/book/9999/chapter/145"),
            buildFlow("/book/9999/chapter/148"),
            buildFlow("/book/9999/chapter")
        );

        List<Flow> filteredFlows = policyResolver.resolve(context);

        assertEquals(1, filteredFlows.size());
        assertSame(flows.get(6), filteredFlows.get(0));
    }

    @Test
    public void resolve_should_return_the_flow_with_deeper_matching_path_with_variables() {
        mockInputRequestPath("/book/9999/chapter/145");

        List<Flow> flows = mockFlows(
            buildFlow("/book"),
            buildFlow("/book/:bookId/chapter/:chapterId/page/:pageId"),
            buildFlow("/book/:bookId"),
            buildFlow("/book/:bookId/chapter/:chapterId"),
            buildFlow("/book/:bookId/chapter")
        );

        List<Flow> filteredFlows = policyResolver.resolve(context);

        assertEquals(1, filteredFlows.size());
        assertSame(flows.get(3), filteredFlows.get(0));
    }

    @Test
    public void resolve_should_not_care_of_trailing_separator() {
        mockInputRequestPath("/book/9999/chapter/145/");

        List<Flow> flows = mockFlows(buildFlow("/book/9999/chapter/145"));

        List<Flow> filteredFlows = policyResolver.resolve(context);

        assertEquals(1, filteredFlows.size());
        assertSame(flows.get(0), filteredFlows.get(0));
    }

    @Test
    public void resolve_should_return_multiple_flows_matching_with_same_depth() {
        mockInputRequestPath("/book/9999/chapter/145");

        List<Flow> flows = mockFlows(
            buildFlow("/book"),
            buildFlow("/book/:bookId/chapter/:chapterId"),
            buildFlow("/book/:bookId"),
            buildFlow("/book/:bookId/chapter/145"),
            buildFlow("/book/:bookId/chapter"),
            buildFlow("/book/9999/chapter/145"),
            buildFlow("/book/:bookId/chapter/145")
        );

        List<Flow> filteredFlows = policyResolver.resolve(context);

        assertEquals(4, filteredFlows.size());
        assertSame(flows.get(1), filteredFlows.get(0));
        assertSame(flows.get(3), filteredFlows.get(1));
        assertSame(flows.get(5), filteredFlows.get(2));
        assertSame(flows.get(6), filteredFlows.get(3));
    }

    private void mockInputRequestPath(String path) {
        Request request = Mockito.mock(Request.class);
        when(context.request()).thenReturn(request);
        when(request.pathInfo()).thenReturn(path);
    }

    private List<Flow> mockFlows(Flow... flows) {
        List<Flow> mockedFlows = Arrays.stream(flows).collect(toList());
        when(flowResolver.resolve(context)).thenReturn(mockedFlows);
        return mockedFlows;
    }

    private Flow buildFlow(String path) {
        Flow flow = new Flow();
        PathOperator pathOperator = new PathOperator();
        pathOperator.setPath(path);
        flow.setPathOperator(pathOperator);
        return flow;
    }
}
