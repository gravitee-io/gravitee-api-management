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
package io.gravitee.repository.ratelimit.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RateLimitTest {

    @Test
    void copy_ctor_preserves_all_fields() {
        RateLimit source = new RateLimit("k1");
        source.setCounter(7);
        source.setLimit(100);
        source.setResetTime(123_456L);
        source.setSubscription("sub-42");

        RateLimit copy = new RateLimit(source);

        assertThat(copy.getKey()).isEqualTo("k1");
        assertThat(copy.getCounter()).isEqualTo(7);
        assertThat(copy.getLimit()).isEqualTo(100);
        assertThat(copy.getResetTime()).isEqualTo(123_456L);
        assertThat(copy.getSubscription()).isEqualTo("sub-42");
    }

    @Test
    void key_override_ctor_preserves_other_fields() {
        RateLimit source = new RateLimit("original");
        source.setCounter(3);
        source.setLimit(50);
        source.setResetTime(999L);
        source.setSubscription("sub-99");

        RateLimit renamed = new RateLimit("override", source);

        assertThat(renamed.getKey()).isEqualTo("override");
        assertThat(renamed.getCounter()).isEqualTo(3);
        assertThat(renamed.getLimit()).isEqualTo(50);
        assertThat(renamed.getResetTime()).isEqualTo(999L);
        assertThat(renamed.getSubscription()).isEqualTo("sub-99");
    }
}
