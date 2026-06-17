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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpStatusLabelsTest {

    @Test
    void should_resolve_registered_status_with_netty_reason_phrase() {
        assertThat(HttpStatusLabels.labelForCode("404")).isEqualTo("404 Not Found");
    }

    @Test
    void should_strip_redundant_code_suffix_from_synthetic_netty_reason_phrase() {
        assertThat(HttpStatusLabels.labelForCode("999")).isEqualTo("999 Unknown Status");
        assertThat(HttpStatusLabels.labelForCode("418")).isEqualTo("418 Client Error");
    }

    @Test
    void should_fallback_to_raw_key_for_non_numeric_key() {
        assertThat(HttpStatusLabels.labelForCode("not-a-code")).isEqualTo("not-a-code");
    }
}
