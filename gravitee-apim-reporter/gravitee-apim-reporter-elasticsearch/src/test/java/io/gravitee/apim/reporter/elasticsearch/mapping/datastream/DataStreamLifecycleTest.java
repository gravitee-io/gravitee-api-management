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
package io.gravitee.apim.reporter.elasticsearch.mapping.datastream;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.elasticsearch.version.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DataStreamLifecycleTest {

    @ParameterizedTest(name = "elasticsearch {0} -> data_retention {1}")
    @CsvSource({ "7.17.28, false", "8.13.4, false", "8.14.0, true", "9.2.1, true" })
    void should_attach_the_configured_ilm_property_on_elasticsearch(String number, boolean dataRetentionSupported) {
        final ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyPropertyName("index.custom.policy");

        assertThat(DataStreamLifecycle.of(version(null, number), configuration)).isEqualTo(
            new DataStreamLifecycle("index.custom.policy", dataRetentionSupported)
        );
    }

    @Test
    void should_default_to_the_ilm_property_on_elasticsearch() {
        assertThat(DataStreamLifecycle.of(version(null, "8.17.2"), new ReporterConfiguration()).policyPropertyName()).isEqualTo(
            "index.lifecycle.name"
        );
    }

    @Test
    void should_attach_no_policy_and_no_retention_on_opensearch() {
        final ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyPropertyName("index.plugins.index_state_management.policy_id");

        assertThat(DataStreamLifecycle.of(version("opensearch", "2.19.1"), configuration)).isEqualTo(new DataStreamLifecycle(null, false));
    }

    private static Version version(String distribution, String number) {
        final Version version = new Version();
        version.setDistribution(distribution);
        version.setNumber(number);
        return version;
    }
}
