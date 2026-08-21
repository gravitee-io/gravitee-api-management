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
package io.gravitee.gateway.reactive.reactor.path;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestPathHandling;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The predicate exists so a caller can know, before dispatching, that the gateway will answer 400.
 * It restates a decision {@code DefaultHttpRequestDispatcher.dispatch} makes inline for speed, and
 * this class is what keeps the two readings from drifting apart — a drift that would fail open, by
 * letting a caller believe a request will be routed when it will not.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RequestPathRejectionTest {

    private static final String CANONICAL = "/alpha/api/echo";
    private static final String WITH_DOT_SEGMENTS = "/alpha/api/../../beta/api/echo";
    private static final String WITH_ENCODED_DOT_SEGMENTS = "/alpha/api/%2e%2e/%2e%2e/beta/api/echo";
    private static final String MALFORMED_PERCENT = "/alpha/api/%zz";

    private static RequestPathConfiguration configured(final RequestPathHandling handling) {
        return new RequestPathConfiguration(handling);
    }

    @Nested
    class Under_raw {

        @ParameterizedTest
        @ValueSource(strings = { CANONICAL, WITH_DOT_SEGMENTS, WITH_ENCODED_DOT_SEGMENTS, MALFORMED_PERCENT })
        void should_never_reject_since_the_gateway_decides_nothing_about_the_path(final String path) {
            assertThat(RequestPathRejection.applies(configured(RequestPathHandling.RAW), path)).isFalse();
        }
    }

    @Nested
    class Under_reject {

        @ParameterizedTest
        @ValueSource(strings = { WITH_DOT_SEGMENTS, WITH_ENCODED_DOT_SEGMENTS, MALFORMED_PERCENT })
        void should_reject_a_path_that_is_not_already_in_its_normalized_form(final String path) {
            assertThat(RequestPathRejection.applies(configured(RequestPathHandling.REJECT), path)).isTrue();
        }

        @Test
        void should_let_a_canonical_path_through() {
            assertThat(RequestPathRejection.applies(configured(RequestPathHandling.REJECT), CANONICAL)).isFalse();
        }
    }

    @Nested
    class Under_normalize {

        @ParameterizedTest
        @ValueSource(strings = { CANONICAL, WITH_DOT_SEGMENTS, WITH_ENCODED_DOT_SEGMENTS })
        void should_let_through_anything_that_has_a_normalized_form(final String path) {
            assertThat(RequestPathRejection.applies(configured(RequestPathHandling.NORMALIZE), path)).isFalse();
        }

        @Test
        void should_still_reject_a_path_that_has_no_normalized_form_at_all() {
            // The one case a caller reading only the mode would miss: there is nothing to resolve,
            // so NORMALIZE has no more to offer than REJECT and the request is refused either way.
            assertThat(RequestPathRejection.applies(configured(RequestPathHandling.NORMALIZE), MALFORMED_PERCENT)).isTrue();
        }
    }

    @Nested
    class Against_the_normalizer {

        @ParameterizedTest
        @ValueSource(strings = { CANONICAL, WITH_DOT_SEGMENTS, WITH_ENCODED_DOT_SEGMENTS, MALFORMED_PERCENT })
        void should_agree_with_what_normalization_can_produce(final String path) {
            // Restating the rule from the other side: a rejection under NORMALIZE happens if and
            // only if the normalizer cannot produce a path. If someone changes one and not the
            // other, this is what fails.
            boolean rejected = RequestPathRejection.applies(configured(RequestPathHandling.NORMALIZE), path);
            boolean hasNoNormalizedForm = RequestPathNormalizer.needsNormalization(path) && RequestPathNormalizer.normalize(path) == null;

            assertThat(rejected).isEqualTo(hasNoNormalizedForm);
        }
    }
}
