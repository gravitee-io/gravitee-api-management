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
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DebugRequestStepTest {

    @Mock
    private Request beforeRequest;

    @Mock
    private Request afterRequest;

    @Mock
    private PolicyMetadata policyMetadata;

    private DebugRequestStep cut;

    @Before
    public void setUp() {
        cut = new DebugRequestStep("policy", StreamType.ON_REQUEST, "uid", PolicyScope.ON_REQUEST, policyMetadata);
        when(beforeRequest.headers()).thenReturn(HttpHeaders.create().add("Header", "header-value"));

        when(beforeRequest.contextPath()).thenReturn(DebugStep.DIFF_KEY_CONTEXT_PATH);
        when(beforeRequest.path()).thenReturn(DebugStep.DIFF_KEY_PATH);
        when(beforeRequest.method()).thenReturn(HttpMethod.GET);

        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("param1", "value1");
        parameters.add("param2", "value2");
        when(beforeRequest.parameters()).thenReturn(parameters);

        final LinkedMultiValueMap<String, String> pathParameters = new LinkedMultiValueMap<>();
        pathParameters.add("path-param1", "path-value1");
        pathParameters.add("path-param2", "path-value2");
        when(beforeRequest.pathParameters()).thenReturn(pathParameters);
    }

    @Test
    public void shouldHaveHeadersInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_HEADERS));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveParametersInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_PARAMETERS));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHavePathParametersInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_PATH_PARAMETERS));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveMethodInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_METHOD));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHavePathInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_PATH));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveContextPathInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_CONTEXT_PATH));

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveAttributesInDiffMap() {
        applyDifferentValuesForFields(List.of());

        cut.before(beforeRequest, new HashMap<>());
        final HashMap<String, Object> afterAttributes = new HashMap<>();
        afterAttributes.put("A-Key", "value");
        cut.after(afterRequest, afterAttributes, null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveBufferInDiffMap() {
        applyDifferentValuesForFields(List.of());

        cut.before(beforeRequest, new HashMap<>());
        cut.after(afterRequest, new HashMap<>(), null, Buffer.buffer("outPut"));

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH_PARAMETERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_METHOD)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_CONTEXT_PATH)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isTrue();
    }

    private void applyDifferentValuesForFields(List<String> fields) {
        when(afterRequest.headers()).thenReturn(
            fields.contains(DebugStep.DIFF_KEY_HEADERS)
                ? HttpHeaders.create().add("Other-Header", "other-value")
                : HttpHeaders.create().add("Header", "header-value")
        );

        when(afterRequest.contextPath()).thenReturn(
            fields.contains(DebugStep.DIFF_KEY_CONTEXT_PATH) ? "otherContextPath" : DebugStep.DIFF_KEY_CONTEXT_PATH
        );

        when(afterRequest.path()).thenReturn(fields.contains(DebugStep.DIFF_KEY_PATH) ? "otherPath" : DebugStep.DIFF_KEY_PATH);

        when(afterRequest.method()).thenReturn(fields.contains(DebugStep.DIFF_KEY_METHOD) ? HttpMethod.DELETE : HttpMethod.GET);

        final LinkedMultiValueMap<String, String> originParameters = new LinkedMultiValueMap<>();
        originParameters.add("param1", "value1");
        originParameters.add("param2", "value2");
        final LinkedMultiValueMap<String, String> modifiedParameters = new LinkedMultiValueMap<>();
        modifiedParameters.add("otherparam1", "value1");
        modifiedParameters.add("otherparam2", "value2");
        when(afterRequest.parameters()).thenReturn(fields.contains(DebugStep.DIFF_KEY_PARAMETERS) ? modifiedParameters : originParameters);

        final LinkedMultiValueMap<String, String> originPathParameters = new LinkedMultiValueMap<>();
        originPathParameters.add("path-param1", "path-value1");
        originPathParameters.add("path-param2", "path-value2");
        final LinkedMultiValueMap<String, String> modifiedPathParameters = new LinkedMultiValueMap<>();
        modifiedPathParameters.add("otherpath-param1", "path-value1");
        modifiedPathParameters.add("otherpath-param2", "path-value2");
        when(afterRequest.pathParameters()).thenReturn(
            fields.contains(DebugStep.DIFF_KEY_PATH_PARAMETERS) ? modifiedPathParameters : originPathParameters
        );
    }
}
