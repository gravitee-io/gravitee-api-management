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
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.PolicyScope;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
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
public class DebugResponseStepTest {

    @Mock
    private Response beforeResponse;

    @Mock
    private Response afterResponse;

    private DebugResponseStep cut;

    @Before
    public void setUp() {
        cut = new DebugResponseStep("policy", StreamType.ON_REQUEST, "uid", PolicyScope.ON_REQUEST);
        when(beforeResponse.headers()).thenReturn(HttpHeaders.create().add("Header", "header-value"));

        when(beforeResponse.status()).thenReturn(200);
        when(beforeResponse.reason()).thenReturn("OK");
    }

    @Test
    public void shouldHaveHeadersInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_HEADERS));

        cut.before(beforeResponse, new HashMap<>());
        cut.after(afterResponse, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_STATUS_CODE)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_REASON)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveStatusInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_STATUS_CODE));

        cut.before(beforeResponse, new HashMap<>());
        cut.after(afterResponse, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_STATUS_CODE)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_REASON)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveReasonInDiffMap() {
        applyDifferentValuesForFields(List.of(DebugStep.DIFF_KEY_REASON));

        cut.before(beforeResponse, new HashMap<>());
        cut.after(afterResponse, new HashMap<>(), null, null);

        final Map<String, Object> resultDiff = cut.getDebugDiffContent();

        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_HEADERS)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_STATUS_CODE)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_REASON)).isTrue();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_ATTRIBUTES)).isFalse();
        assertThat(resultDiff.containsKey(DebugStep.DIFF_KEY_BODY_BUFFER)).isFalse();
    }

    @Test
    public void shouldHaveAttributesInDiffMap() {
        applyDifferentValuesForFields(List.of());

        cut.before(beforeResponse, new HashMap<>());
        final HashMap<String, Object> afterAttributes = new HashMap<>();
        afterAttributes.put("A-Key", "value");
        cut.after(afterResponse, afterAttributes, null, null);

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

        cut.before(beforeResponse, new HashMap<>());
        cut.after(afterResponse, new HashMap<>(), null, Buffer.buffer("outPut"));

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
        when(afterResponse.headers())
            .thenReturn(
                fields.contains(DebugStep.DIFF_KEY_HEADERS)
                    ? HttpHeaders.create().add("Other-Header", "other-value")
                    : HttpHeaders.create().add("Header", "header-value")
            );

        when(afterResponse.status()).thenReturn(fields.contains(DebugStep.DIFF_KEY_STATUS_CODE) ? 500 : 200);

        when(afterResponse.reason()).thenReturn(fields.contains(DebugStep.DIFF_KEY_REASON) ? "Internal Server Error" : "OK");
    }
}
