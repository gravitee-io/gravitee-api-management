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
package io.gravitee.rest.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import io.gravitee.rest.api.service.impl.SwaggerServiceImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_ParseTest {

    @InjectMocks
    private SwaggerServiceImpl swaggerService;

    @Mock
    private ImportConfiguration importConfiguration;

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
        SwaggerDescriptor descriptor = swaggerService.parse("/io/gravitee/rest/api/management/service/swagger-v1.json");

        assertNotNull(descriptor);
        validateV3(Json.mapper().readTree(descriptor.toJson()), false);
    }

    @Test
    public void shouldNotParseSwaggerV1WithoutInfo_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/swagger-v1-no-info.json", MediaType.APPLICATION_JSON);
        try {
            swaggerService.parse(pageEntity.getContent());
            fail("Expected SwaggerDescriptorException");
        } catch (SwaggerDescriptorException e) {
            assertEquals(e.getMessage(), "[\"attribute info.title is missing\"]");
        }
    }

    @Test
    public void shouldParseSwaggerV2_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/swagger-v2.json", MediaType.APPLICATION_JSON);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        validateV3(Json.mapper().readTree(descriptor.toJson()), false);
    }

    @Test
    public void shouldThrowSwaggerDescriptorExceptionWhenParseSwaggerV2WithoutInfo_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/swagger-v2-no-info.json", MediaType.APPLICATION_JSON);

        try {
            swaggerService.parse(pageEntity.getContent());
            fail("Expected SwaggerDescriptorException");
        } catch (SwaggerDescriptorException e) {
            assertEquals(e.getMessage(), "[\"attribute info is missing\"]");
        }
    }

    @Test
    public void shouldParseSwaggerV2_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/swagger-v2.yaml", MediaType.TEXT_PLAIN);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        validateV3(Yaml.mapper().readTree(descriptor.toYaml()), false);
    }

    @Test
    public void shouldThrowSwaggerDescriptorExceptionWhenParseSwaggerV2WithoutInfo_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/swagger-v2-no-info.yaml", MediaType.TEXT_PLAIN);

        try {
            swaggerService.parse(pageEntity.getContent());
            fail("Expected SwaggerDescriptorException");
        } catch (SwaggerDescriptorException e) {
            assertEquals(e.getMessage(), "[\"attribute info is missing\"]");
        }
    }

    @Test
    public void shouldParseSwaggerV3_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        validateV3(Json.mapper().readTree(descriptor.toJson()));
    }

    @Test
    public void shouldThrowSwaggerDescriptorExceptionWhenParseSwaggerV3WithoutInfo_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi-no-info.json", MediaType.APPLICATION_JSON);

        try {
            swaggerService.parse(pageEntity.getContent());
            fail("Expected SwaggerDescriptorException");
        } catch (SwaggerDescriptorException e) {
            assertEquals(e.getMessage(), "[\"attribute info is missing\"]");
        }
    }

    @Test
    public void shouldParseSwaggerV3_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.yaml", MediaType.TEXT_PLAIN);

        SwaggerDescriptor descriptor = swaggerService.parse(pageEntity.getContent());

        assertNotNull(descriptor);
        validateV3(Yaml.mapper().readTree(descriptor.toYaml()));
    }

    @Test
    public void shouldThrowSwaggerDescriptorExceptionWhenParseSwaggerV3WithoutInfo_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi-no-info.yaml", MediaType.TEXT_PLAIN);

        try {
            swaggerService.parse(pageEntity.getContent());
            fail("Expected SwaggerDescriptorException");
        } catch (SwaggerDescriptorException e) {
            assertEquals(e.getMessage(), "[\"attribute info is missing\"]");
        }
    }

    @Test(expected = UrlForbiddenException.class)
    public void shouldThrowUrlForbiddenException() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setContent("http://localhost");

        swaggerService.parse(pageEntity.getContent());
    }

    private void validateV2(JsonNode node) {
        assertEquals("1.2.3", node.get("info").get("version").asText());
        assertEquals("Gravitee.io Swagger API", node.get("info").get("title").asText());
        assertEquals("https", node.get("schemes").get(0).asText());
        assertEquals("demo.gravitee.io", node.get("host").asText());
        assertEquals("/gateway/echo", node.get("basePath").asText());
    }


    private void validateV3(JsonNode node) {
        validateV3(node, true);
    }

    private void validateV3(JsonNode node, Boolean withOauth) {
        assertEquals("1.2.3", node.get("info").get("version").asText());
        assertEquals("Gravitee.io Swagger API", node.get("info").get("title").asText());
        assertEquals("https://demo.gravitee.io/gateway/echo", node.get("servers").get(0).get("url").asText());
        assertEquals(2, node.get("paths").size());
        if (withOauth) {
            assertEquals("oauth2", node.get("components").get("securitySchemes").get("oauth2Scheme").get("type").asText());
        }
    }
}
