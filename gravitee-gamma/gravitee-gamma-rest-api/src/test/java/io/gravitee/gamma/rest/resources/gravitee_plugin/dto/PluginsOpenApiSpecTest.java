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
package io.gravitee.gamma.rest.resources.gravitee_plugin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** The plugins OpenAPI file is hand-written: these checks keep it in step with the records the endpoints serialize. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PluginsOpenApiSpecTest {

    private static final JsonNode SPEC = readSpec();

    @Test
    void should_document_the_policy_plugin_as_serialized() {
        SoftAssertions.assertSoftly(soft -> {
            soft
                .assertThat(SPEC.at("/components/schemas/Flow/enum"))
                .as("documented flows")
                .extracting(JsonNode::asText)
                .containsExactlyElementsOf(Arrays.stream(ApiProtocolType.values()).map(PolicyFlow::nameOf).toList());
            soft
                .assertThat(propertiesOf("PolicyPlugin"))
                .as("documented PolicyPlugin properties")
                .containsExactlyElementsOf(
                    Arrays.stream(PolicyPluginDto.class.getRecordComponents()).map(RecordComponent::getName).toList()
                );
        });
    }

    private static List<String> propertiesOf(String schema) {
        Iterable<String> names = () -> SPEC.at("/components/schemas/" + schema + "/properties").fieldNames();
        return StreamSupport.stream(names.spliterator(), false).toList();
    }

    private static JsonNode readSpec() {
        try (var spec = PluginsOpenApiSpecTest.class.getResourceAsStream("/openapi/plugins/openapi-plugins.yaml")) {
            return new YAMLMapper().readTree(spec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
