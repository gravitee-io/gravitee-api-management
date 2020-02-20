/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.service.impl.SwaggerServiceImpl;
import io.gravitee.management.service.swagger.SwaggerDescriptor;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_ParseTest {

    private SwaggerService swaggerService;

    @Before
    public void setUp() {
        swaggerService = new SwaggerServiceImpl();
    }

    private PageEntity getPage(String resource, String contentType) throws IOException {
        URL url = Resources.getResource(resource);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        PageEntity pageEntity = new PageEntity();
        pageEntity.setContent(descriptor);
        pageEntity.setContentType(contentType);
        return pageEntity;
    }

    @Test
    public void shouldParseSwaggerV1_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/management/service/swagger-v1.json", MediaType.APPLICATION_JSON);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        assertEquals(SwaggerDescriptor.Version.SWAGGER_V1, descriptor.getVersion());
        validateV2(Json.mapper().readTree(descriptor.toJson()));
    }

    @Test
    public void shouldParseSwaggerV2_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/management/service/swagger-v2.json", MediaType.APPLICATION_JSON);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        assertEquals(SwaggerDescriptor.Version.SWAGGER_V2, descriptor.getVersion());
        validateV2(Json.mapper().readTree(descriptor.toJson()));
    }

    @Test
    public void shouldParseSwaggerV2_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/management/service/swagger-v2.yaml", MediaType.TEXT_PLAIN);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        assertEquals(SwaggerDescriptor.Version.SWAGGER_V2, descriptor.getVersion());
        validateV2(Yaml.mapper().readTree(descriptor.toYaml()));
    }

    @Test
    public void shouldParseSwaggerV3_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/management/service/openapi.json", MediaType.APPLICATION_JSON);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        assertEquals(SwaggerDescriptor.Version.OAI_V3, descriptor.getVersion());
        validateV3(Json.mapper().readTree(descriptor.toJson()));
    }

    @Test
    public void shouldParseSwaggerV3_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/management/service/openapi.yaml", MediaType.TEXT_PLAIN);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        assertEquals(SwaggerDescriptor.Version.OAI_V3, descriptor.getVersion());
        validateV3(Yaml.mapper().readTree(descriptor.toYaml()));
    }

    private void validateV2(JsonNode node) {
        assertEquals("1.2.3", node.get("info").get("version").asText());
        assertEquals("Gravitee.io Swagger API", node.get("info").get("title").asText());
        assertEquals("https", node.get("schemes").get(0).asText());
        assertEquals("demo.gravitee.io", node.get("host").asText());
        assertEquals("/gateway/echo", node.get("basePath").asText());
    }

    private void validateV3(JsonNode node) {
        assertEquals("1.2.3", node.get("info").get("version").asText());
        assertEquals("Gravitee.io Swagger API", node.get("info").get("title").asText());
        assertEquals("https://demo.gravitee.io/gateway/echo", node.get("servers").get(0).get("url").asText());
        assertEquals("oauth2", node.get("components").get("securitySchemes").get("oauth2Scheme").get("type").asText());
    }
}
