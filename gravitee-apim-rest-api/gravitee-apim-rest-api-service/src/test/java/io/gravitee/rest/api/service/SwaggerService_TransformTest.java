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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.service.impl.SwaggerServiceImpl;
import io.gravitee.rest.api.service.impl.swagger.SwaggerProperties;
import io.gravitee.rest.api.service.impl.swagger.transformer.entrypoints.EntrypointsOAITransformer;
import io.gravitee.rest.api.service.impl.swagger.transformer.page.PageConfigurationOAITransformer;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_TransformTest {

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
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT, "true");
        pageConfigurationEntity.put(PageConfigurationKeys.SWAGGER_SWAGGERUI_TRY_IT_URL, "https://my.domain.com/v1");
        pageEntity.setConfiguration(pageConfigurationEntity);
        return pageEntity;
    }

    @Test
    public void shouldTransformAPIFromSwaggerV3_json() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        swaggerService.transform(descriptor, Collections.singleton(new PageConfigurationOAITransformer(pageEntity)));

        assertNotNull(descriptor.toJson());
        validateV3(Json.mapper().readTree(descriptor.toJson()));
    }

    @Test
    public void shouldTransformAPIWithServerUrl() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINTS_AS_SERVERS, "true");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://apis.gravitee.io", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIWithServerUrlAndContextPath() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINTS_AS_SERVERS, "true");
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINT_AS_BASEPATH, "true");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);
        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://apis.gravitee.io/test", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIWithOriginalServer() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINTS_AS_SERVERS, "false");
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINT_AS_BASEPATH, "false");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://demo.gravitee.io/gateway/echo", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIWithOriginalServerAndContextPath() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINTS_AS_SERVERS, "false");
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINT_AS_BASEPATH, "true");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://demo.gravitee.io/test", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIWithCustomServerUrl() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINT_AS_BASEPATH, "false");
        pageConfigurationEntity.put(SwaggerProperties.TRY_IT, "https://custom.gravitee.io/tryit?q=test");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://custom.gravitee.io/tryit?q=test", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIWithCustomServerUrlAndContextPath() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.json", MediaType.APPLICATION_JSON);
        Map<String, String> pageConfigurationEntity = new HashMap<>();
        pageConfigurationEntity.put(SwaggerProperties.ENTRYPOINT_AS_BASEPATH, "true");
        pageConfigurationEntity.put(SwaggerProperties.TRY_IT, "https://custom.gravitee.io/tryit?q=test");
        pageEntity.setConfiguration(pageConfigurationEntity);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        final ApiEntity apiEntity = getApiEntity();

        swaggerService.transform(
            descriptor,
            Arrays.asList(new PageConfigurationOAITransformer(pageEntity), new EntrypointsOAITransformer(pageEntity, apiEntity))
        );

        assertNotNull(descriptor.toJson());
        final JsonNode node = Json.mapper().readTree(descriptor.toJson());
        assertEquals("https://custom.gravitee.io/test?q=test", node.get("servers").get(0).get("url").asText());
    }

    @Test
    public void shouldTransformAPIFromSwaggerV3_yaml() throws IOException {
        PageEntity pageEntity = getPage("io/gravitee/rest/api/management/service/openapi.yaml", MediaType.TEXT_PLAIN);

        OAIDescriptor descriptor = (OAIDescriptor) swaggerService.parse(pageEntity.getContent(), false);

        swaggerService.transform(descriptor, Collections.singleton(new PageConfigurationOAITransformer(pageEntity)));

        assertNotNull(descriptor.toYaml());
        validateV3(Yaml.mapper().readTree(descriptor.toYaml()));
    }

    private void validateV3(JsonNode node) {
        assertEquals("1.2.3", node.get("info").get("version").asText());
        assertEquals("Gravitee.io Swagger API", node.get("info").get("title").asText());
        assertEquals("https://my.domain.com/v1", node.get("servers").get(0).get("url").asText());
        assertEquals("oauth2", node.get("components").get("securitySchemes").get("oauth2Scheme").get("type").asText());
    }

    private ApiEntity getApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setContextPath("/test");
        final ArrayList<ApiEntrypointEntity> entrypoints = new ArrayList<>();
        entrypoints.add(new ApiEntrypointEntity("https://apis.gravitee.io/test"));
        apiEntity.setEntrypoints(entrypoints);
        return apiEntity;
    }
}
