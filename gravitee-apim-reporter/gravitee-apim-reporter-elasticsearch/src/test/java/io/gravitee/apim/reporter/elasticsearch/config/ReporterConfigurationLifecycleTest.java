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
package io.gravitee.apim.reporter.elasticsearch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReporterConfigurationLifecycleTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(LifecycleGroup.class)
    void should_read_the_lifecycle_settings_of_each_group_next_to_each_other(LifecycleGroup group) {
        final ReporterConfiguration configuration = bind(
            Map.of(
                "reporters.elasticsearch.lifecycle.policies." + group.key(),
                "policy-" + group.key(),
                "reporters.elasticsearch.lifecycle.retention." + group.key(),
                "7d"
            )
        );

        assertThat(configuration.lifecyclePolicy(group)).contains("policy-" + group.key());
        assertThat(configuration.dataRetention(group)).contains("7d");
    }

    private static ReporterConfiguration bind(Map<String, Object> properties) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.registerBean(ReporterConfiguration.class);
            context.refresh();
            return context.getBean(ReporterConfiguration.class);
        }
    }
}
