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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class OpenAPIResourceTest extends AbstractResourceTest {

    private static final YAMLMapper YAML = new YAMLMapper();
    private static final String OPEN_API_VERSION = "openapi";

    @Override
    protected String contextPath() {
        return "/open-api.yaml";
    }

    @Test
    void should_get_spec() {
        assertThat(expectSpec().get(OPEN_API_VERSION).asText()).isNotEmpty();
    }

    private JsonNode expectSpec() {
        try (var response = rootTarget().request().get()) {
            return readYAML(response.readEntity(String.class));
        }
    }

    private JsonNode readYAML(String yaml) {
        try {
            return YAML.readTree(yaml);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
