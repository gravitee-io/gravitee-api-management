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
package io.gravitee.gamma.rest.resources.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import jakarta.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnumQueryParamTest {

    @Test
    void should_read_tokens_case_insensitively_and_trimmed() {
        assertThat(EnumQueryParam.parse(List.of("http_proxy", " Llm "), ApiType.class, "apiType")).containsExactlyInAnyOrder(
            ApiType.HTTP_PROXY,
            ApiType.LLM
        );
    }

    @Test
    void should_skip_null_and_blank_tokens() {
        assertThat(EnumQueryParam.parse(Arrays.asList(null, " ", "NATIVE"), ApiType.class, "apiType")).containsExactly(ApiType.NATIVE);
    }

    @Test
    void should_read_an_absent_or_empty_param_as_an_empty_set() {
        assertThat(EnumQueryParam.parse(null, ApiType.class, "apiType")).isEmpty();
        assertThat(EnumQueryParam.parse(List.of(), ApiType.class, "apiType")).isEmpty();
    }

    @Test
    void should_refuse_an_unknown_token_as_a_bad_request_naming_the_param() {
        assertThatThrownBy(() -> EnumQueryParam.parse(List.of("HTTP_PROXY", "nope"), ApiType.class, "apiType"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Unknown apiType value 'nope'");
    }

    @Test
    void should_refuse_an_unknown_token_with_the_exception_the_caller_builds() {
        assertThatThrownBy(() -> EnumQueryParam.parse(List.of("nope"), ApiType.class, token -> new IllegalStateException("bad " + token)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("bad nope");
    }
}
