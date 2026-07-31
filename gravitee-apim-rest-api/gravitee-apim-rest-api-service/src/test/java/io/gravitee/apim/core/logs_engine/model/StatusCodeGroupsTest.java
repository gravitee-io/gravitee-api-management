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
package io.gravitee.apim.core.logs_engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatusCodeGroupsTest {

    @Test
    void should_mirror_the_repository_group_vocabulary() {
        // Core cannot depend on the repository layer, so the vocabulary is restated in StatusCodeGroups.
        // This is what stops the two copies drifting: a group added downstream fails here.
        assertThat(StatusCodeGroups.NAMES).isEqualTo(HttpStatusCodeGroups.GROUP_BOUNDS.keySet());
    }

    @ParameterizedTest
    @ValueSource(strings = { "5xx", " 5XX ", "5XX" })
    void should_canonicalise_a_known_group(String raw) {
        assertThat(StatusCodeGroups.canonicalise(raw)).contains("5XX");
    }

    @ParameterizedTest
    @ValueSource(strings = { "6XX", "5", "", "XX5" })
    void should_reject_an_unknown_group(String raw) {
        assertThat(StatusCodeGroups.canonicalise(raw)).isEmpty();
    }

    @Test
    void should_reject_a_null_group() {
        assertThat(StatusCodeGroups.canonicalise(null)).isEmpty();
    }
}
