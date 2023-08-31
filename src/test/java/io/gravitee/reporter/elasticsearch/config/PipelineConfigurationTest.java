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
package io.gravitee.reporter.elasticsearch.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.reporter.elasticsearch.UnitTestConfiguration;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author GraviteeSource Team
 * @author Guillaume Gillon
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { UnitTestConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineConfigurationTest {

    @Autowired
    FreeMarkerComponent freeMarkerComponent;

    @Autowired
    PipelineConfiguration pipelineConfiguration;

    @Test
    public void should_not_create_pipeline_if_no_plugin() {
        String pipeline = new PipelineConfiguration("", null, freeMarkerComponent).createPipeline();

        assertThat(pipeline).isNull();
    }

    @Test
    public void should_create_default_pipeline() {
        String pipeline = pipelineConfiguration.createPipeline();
        assertThat(new JsonObject(pipeline))
            .isEqualTo(
                new JsonObject(
                    Map.of(
                        "description",
                        "Gravitee pipeline",
                        "processors",
                        new JsonArray(
                            Stream
                                .concat(expectedGeoIpProcessors().stream(), expectedUserAgentProcessors(null).stream())
                                .collect(Collectors.toList())
                        )
                    )
                )
            );
    }

    @Test
    public void should_ignore_unknown_plugins() {
        String pipeline = new PipelineConfiguration(
            "geoip, testAnother, unknown, user_agent, this does not exists",
            null,
            freeMarkerComponent
        )
            .createPipeline();
        assertThat(new JsonObject(pipeline))
            .isEqualTo(
                new JsonObject(
                    Map.of(
                        "description",
                        "Gravitee pipeline",
                        "processors",
                        new JsonArray(
                            Stream
                                .concat(expectedGeoIpProcessors().stream(), expectedUserAgentProcessors(null).stream())
                                .collect(Collectors.toList())
                        )
                    )
                )
            );
    }

    @Nested
    class UserAgentPluginTest {

        @Test
        public void should_create_pipeline_with_regex_file() {
            String pipeline = new PipelineConfiguration("user_agent", "regexes_custom.yml", freeMarkerComponent).createPipeline();
            JsonObject expectedPipeline = new JsonObject(
                Map.of("description", "Gravitee pipeline", "processors", new JsonArray(expectedUserAgentProcessors("regexes_custom.yml")))
            );
            assertThat(new JsonObject(pipeline)).isEqualTo(expectedPipeline);
        }
    }

    @Test
    public void should_return_pipeline_name_when_valid() {
        pipelineConfiguration.valid();
        assertThat(pipelineConfiguration.getPipeline()).isEqualTo(pipelineConfiguration.getPipelineName());
    }

    @Test
    public void should_not_return_pipeline_name_when_not_valided() {
        assertThat(pipelineConfiguration.getPipeline()).isNull();
    }

    List<JsonObject> expectedGeoIpProcessors() {
        return List.of(
            new JsonObject(Map.of("geoip", new JsonObject(Map.of("field", "remote-address")))),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(Map.entry("field", "geoip.city_name"), Map.entry("value", "Unknown"), Map.entry("override", false))
                    )
                )
            ),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(
                            Map.entry("field", "geoip.continent_name"),
                            Map.entry("value", "Unknown"),
                            Map.entry("override", false)
                        )
                    )
                )
            ),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(
                            Map.entry("field", "geoip.country_iso_code"),
                            Map.entry("value", "Unknown"),
                            Map.entry("override", false)
                        )
                    )
                )
            ),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(Map.entry("field", "geoip.region_name"), Map.entry("value", "Unknown"), Map.entry("override", false))
                    )
                )
            )
        );
    }

    List<JsonObject> expectedUserAgentProcessors(String regexFile) {
        Map<String, Object> userAgentProps = new HashMap<>();
        userAgentProps.put("field", "user-agent");
        if (null != regexFile) {
            userAgentProps.put("regex_file", "regexes_custom.yml");
        }

        return List.of(
            new JsonObject(Map.of("user_agent", new JsonObject(userAgentProps))),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(Map.entry("field", "user_agent.name"), Map.entry("value", "Unknown"), Map.entry("override", false))
                    )
                )
            ),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(
                            Map.entry("field", "user_agent.os_name"),
                            Map.entry("value", "{{user_agent.os.name}}"),
                            Map.entry("override", false)
                        )
                    )
                )
            ),
            new JsonObject(
                Map.of(
                    "set",
                    new JsonObject(
                        Map.ofEntries(Map.entry("field", "user_agent.os_name"), Map.entry("value", "Unknown"), Map.entry("override", false))
                    )
                )
            )
        );
    }
}
