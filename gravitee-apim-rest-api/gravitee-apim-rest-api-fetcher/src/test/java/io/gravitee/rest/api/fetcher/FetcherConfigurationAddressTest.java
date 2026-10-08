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
package io.gravitee.rest.api.fetcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FetcherConfigurationAddressTest {

    @Test
    void should_be_the_same_address_when_nothing_changes() {
        var configuration = "{\"url\":\"https://a.example/doc.md\",\"token\":\"secret\"}";

        assertThat(FetcherConfigurationAddress.sameAddress(configuration, configuration)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "{\"url\":\"https://a.example/doc.md\"}|{\"url\":\"https://b.example/doc.md\"}",
            "{\"githubUrl\":\"https://api.github.com\"}|{\"githubUrl\":\"https://github.attacker.example\"}",
            "{\"gitlabUrl\":\"https://gitlab.com/api/v4\"}|{\"gitlabUrl\":\"https://gitlab.attacker.example/api/v4\"}",
            "{\"bitbucketUrl\":\"https://api.bitbucket.org/2.0\"}|{\"bitbucketUrl\":\"https://bitbucket.attacker.example\"}",
            "{\"repository\":\"https://git.example/repo.git\"}|{\"repository\":\"https://git.attacker.example/repo.git\"}",
            "{\"url\":\"https://a.example/doc.md\"}|{}",
            "{}|{\"url\":\"https://a.example/doc.md\"}",
        }
    )
    void should_not_be_the_same_address_when_an_address_field_changes(String oldConfiguration, String newConfiguration) {
        assertThat(FetcherConfigurationAddress.sameAddress(oldConfiguration, newConfiguration)).isFalse();
    }

    @Test
    void should_be_the_same_address_when_only_other_fields_change() {
        var oldConfiguration =
            "{\"githubUrl\":\"https://api.github.com\",\"repository\":\"repo\",\"branchOrTag\":\"main\",\"filepath\":\"/a.md\"," +
            "\"fetchCron\":null,\"autoFetch\":false,\"editLink\":\"https://a.example\",\"personalAccessToken\":\"secret\"}";
        var newConfiguration =
            "{\"githubUrl\":\"https://api.github.com\",\"repository\":\"repo\",\"branchOrTag\":\"dev\",\"filepath\":\"/b.md\"," +
            "\"fetchCron\":\"0 0 * * * *\",\"autoFetch\":true,\"editLink\":\"https://b.example\",\"personalAccessToken\":\"********\"}";

        assertThat(FetcherConfigurationAddress.sameAddress(oldConfiguration, newConfiguration)).isTrue();
    }

    @Test
    void should_treat_a_null_address_as_a_missing_one() {
        assertThat(FetcherConfigurationAddress.sameAddress("{\"url\":null}", "{}")).isTrue();
    }

    @Test
    void should_not_be_the_same_address_when_a_configuration_is_not_an_object() {
        assertThat(FetcherConfigurationAddress.sameAddress("[]", "[]")).isFalse();
    }

    @Test
    void should_not_be_the_same_address_when_a_configuration_cannot_be_read() {
        assertThat(FetcherConfigurationAddress.sameAddress("{\"url\":\"https://a.example\"}", "not json")).isFalse();
        assertThat(FetcherConfigurationAddress.sameAddress(null, "{\"url\":\"https://a.example\"}")).isFalse();
    }
}
