/*
 *
 *  * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.apim.rest.api.automation.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HRIDHelperTest {

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        hello world,           hello-world
        hello-world,           hello-world
        Hello World,           hello-world
        "     Hello world   ", hello-world
        (Hello world),         hello-world
        ( Hello world ),       hello-world
        -- --Hello World-- --, hello-world
        123 POLIZEI,           123-polizei
        """
    )
    void should_produce_clean_hrid(String given, String expected) {
        assertThat(HRIDHelper.nameToHRID(given)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { " ", "", "__" })
    void should_fail_bad_empty_input(String given) {
        assertThat(HRIDHelper.nameToHRID(given)).startsWith(HRIDHelper.HRID_PREFIX).hasSizeGreaterThan(HRIDHelper.HRID_PREFIX.length());
    }
}
