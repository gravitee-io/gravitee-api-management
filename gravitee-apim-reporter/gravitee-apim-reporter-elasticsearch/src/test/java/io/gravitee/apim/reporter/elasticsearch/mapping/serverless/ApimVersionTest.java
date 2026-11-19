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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApimVersionTest {

    @ParameterizedTest(name = "{0} newer than {1}: {2}")
    @CsvSource(
        {
            "4.15.0, 4.14.0, true",
            "4.14.1, 4.14.0, true",
            "5.0.0, 4.14.9, true",
            "4.10.0, 4.9.0, true",
            "4.14.0, 4.14.0, false",
            "4.14.0-SNAPSHOT, 4.14.0, false",
            "4.13.9, 4.14.0, false",
            "4.14.0, unknown, true",
            "unknown, 4.14.0, false",
        }
    )
    void should_compare_the_numeric_parts_only(String version, String other, boolean newer) {
        assertThat(ApimVersion.isNewer(version, other)).isEqualTo(newer);
    }
}
