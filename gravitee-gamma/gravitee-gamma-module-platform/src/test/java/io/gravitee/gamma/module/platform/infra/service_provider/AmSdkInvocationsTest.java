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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.am.sdk.management.invoker.ApiException;
import io.gravitee.gamma.module.platform.core.am.exception.AmUpstreamException;
import io.vertx.core.Future;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSdkInvocationsTest {

    @Test
    void extracts_message_from_am_json_body() {
        ApiException ae = new ApiException(400, null, "{\"message\":\"Domain not found\",\"http_status\":400}");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("Domain not found")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("{").doesNotContain("http_status"));
    }

    @Test
    void falls_back_to_error_description_when_no_message() {
        ApiException ae = new ApiException(400, null, "{\"error\":\"invalid_request\",\"error_description\":\"bad redirect uri\"}");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("bad redirect uri")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("{"));
    }

    @Test
    void falls_back_to_error_when_no_message_or_description() {
        ApiException ae = new ApiException(400, null, "{\"error\":\"invalid_request\"}");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("invalid_request")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("{"));
    }

    @Test
    void uses_plain_text_body_when_not_json() {
        ApiException ae = new ApiException(502, null, "Bad Gateway");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("Bad Gateway");
    }

    @Test
    void never_echoes_raw_json_for_unrecognised_shape() {
        ApiException ae = new ApiException(400, "boom", null, "{\"foo\":\"bar\"}");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("boom")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("{").doesNotContain("foo"));
    }

    @Test
    void falls_back_when_body_is_html() {
        ApiException ae = new ApiException(502, "boom", null, "<html><body>502 Bad Gateway</body></html>");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("boom")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("<"));
    }

    @Test
    void falls_back_when_plain_text_body_is_oversized() {
        ApiException ae = new ApiException(500, "boom", null, "x".repeat(2000));

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("boom")
            .extracting(Throwable::getMessage)
            .satisfies(m -> assertThat(m).doesNotContain("xxxx"));
    }

    @Test
    void falls_back_to_exception_message_when_body_empty() {
        ApiException ae = new ApiException(500, "boom", null, "");

        assertThatThrownBy(() -> AmSdkInvocations.await(Future.failedFuture(ae)))
            .isInstanceOf(AmUpstreamException.class)
            .hasMessageContaining("boom");
    }
}
