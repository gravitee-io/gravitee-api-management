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
package io.gravitee.gamma.module.platform.rest.resource.am;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.module.platform.core.am.exception.AmUpstreamException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmCallsTest {

    @Test
    void should_passthrough_value_when_no_exception() {
        String result = AmCalls.run(() -> "ok");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void should_translate_am_not_configured_to_503() {
        WebApplicationException ex = catchThrowableOfType(
            () ->
                AmCalls.run(() -> {
                    throw new AmNotConfiguredException("AM is not configured");
                }),
            WebApplicationException.class
        );

        Response response = ex.getResponse();
        assertThat(response.getStatus()).isEqualTo(503);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getEntity();
        assertThat(body).containsEntry("code", "am_not_configured");
    }

    @Test
    void should_translate_am_upstream_to_underlying_status() {
        WebApplicationException ex = catchThrowableOfType(
            () ->
                AmCalls.run(() -> {
                    throw new AmUpstreamException("AM rejected request", 422);
                }),
            WebApplicationException.class
        );

        Response response = ex.getResponse();
        assertThat(response.getStatus()).isEqualTo(422);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body).containsEntry("code", "am_upstream_error").containsEntry("upstreamStatus", 422);
        assertThat(body.get("message")).isEqualTo("AM rejected request");
    }

    @Test
    void should_translate_am_upstream_with_unknown_status_to_502() {
        WebApplicationException ex = catchThrowableOfType(
            () ->
                AmCalls.run(() -> {
                    throw new AmUpstreamException("AM unreachable", null);
                }),
            WebApplicationException.class
        );

        assertThat(ex.getResponse().getStatus()).isEqualTo(502);
    }

    @Test
    void should_propagate_unknown_exceptions_unchanged() {
        IllegalStateException raw = new IllegalStateException("nope");
        assertThatThrownBy(() ->
            AmCalls.run(() -> {
                throw raw;
            })
        ).isSameAs(raw);
    }
}
