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

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Rule;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.impl.SwaggerServiceImpl;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.OAIPolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.SwaggerPolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_CreateAPITest {

    @Mock
    private PolicyOperationVisitorManager policyOperationVisitorManager;

    @InjectMocks
    private SwaggerServiceImpl swaggerService;

    @Before
    public void setup() {
        PolicyOperationVisitor swaggerPolicyOperationVisitor = mock(PolicyOperationVisitor.class);
        when(swaggerPolicyOperationVisitor.getId()).thenReturn("mock");

        PolicyOperationVisitor oaiPolicyOperationVisitor = mock(PolicyOperationVisitor.class);
        when(oaiPolicyOperationVisitor.getId()).thenReturn("mock");
        io.gravitee.policy.api.swagger.v3.OAIOperationVisitor oaiPolicyOperationVisitorImpl = mock(
            io.gravitee.policy.api.swagger.v3.OAIOperationVisitor.class
        );

        when(policyOperationVisitorManager.getPolicyVisitors())
            .thenReturn(asList(swaggerPolicyOperationVisitor, oaiPolicyOperationVisitor));

        OAIOperationVisitor op = mock(OAIPolicyOperationVisitor.class);
        when(op.visit(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Optional.of(new Policy()));
        when(policyOperationVisitorManager.getOAIOperationVisitor(anyString())).thenReturn(op);
    }

    // Swagger v1
    @Test
    public void shouldPrepareAPIFromSwaggerV1_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v1.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV1_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v1.json"));
    }

    // Swagger v2
    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v2.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v2.yaml"));
    }

    // OpenAPI
    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/openapi.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/openapi.yaml"));
    }

    private void validate(SwaggerApiEntity api) {
        assertEquals("1.2.3", api.getVersion());
        assertEquals("Gravitee.io Swagger API", api.getName());
        assertEquals("https://demo.gravitee.io/gateway/echo", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        assertTrue(api.getPaths().keySet().containsAll(asList("/pets", "/pets/:petId")));
    }

    private SwaggerApiEntity prepareInline(String file) throws IOException {
        return prepareInline(file, false);
    }

    private SwaggerApiEntity prepareInline(String file, boolean withPolicyPaths) throws IOException {
        URL url = Resources.getResource(file);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.INLINE);
        swaggerDescriptor.setPayload(descriptor);
        swaggerDescriptor.setWithPolicyPaths(withPolicyPaths);
        swaggerDescriptor.setWithPolicies(asList("mock"));
        return swaggerService.createAPI(swaggerDescriptor);
    }

    private SwaggerApiEntity prepareUrl(String file) {
        URL url = Resources.getResource(file);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.URL);
        try {
            swaggerDescriptor.setPayload(url.toURI().getPath());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return swaggerService.createAPI(swaggerDescriptor);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/api-with-examples.yaml", true);
        assertEquals("2.0.0", api.getVersion());
        assertEquals("Simple API overview", api.getName());
        assertEquals("simpleapioverview", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        assertTrue(api.getPaths().keySet().containsAll(asList("/", "/v2")));
        Path path = api.getPaths().get("/");
        assertEquals(2, path.getRules().size());
        Rule rule = path.getRules().get(0);
        assertTrue(rule.getMethods().containsAll(asList(HttpMethod.GET)));
        assertEquals("List API versions", rule.getDescription());
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithSimpleTypedExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/callback-example.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Callback Example", api.getName());
        assertEquals("callbackexample", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(1, api.getPaths().size());
        assertTrue(api.getPaths().keySet().containsAll(asList("/streams")));
        Path path = api.getPaths().get("/streams");
        assertEquals(2, path.getRules().size());
        Rule rule = path.getRules().get(0);
        assertTrue(rule.getMethods().containsAll(asList(HttpMethod.POST)));
        assertEquals("subscribes a client to receive out-of-band data", rule.getDescription());
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithLinks() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/link-example.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Link Example", api.getName());
        assertEquals("linkexample", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(6, api.getPaths().size());
        final Path usersUsername = api.getPaths().get("/2.0/users/:username");
        assertEquals("/2.0/users/:username", usersUsername.getPath());
        final Path repositoriesUsername = api.getPaths().get("/2.0/repositories/:username");
        assertEquals("/2.0/repositories/:username", repositoriesUsername.getPath());
        assertEquals("/2.0/repositories/:username/:slug", api.getPaths().get("/2.0/repositories/:username/:slug").getPath());
        assertEquals(
            "/2.0/repositories/:username/:slug/pullrequests",
            api.getPaths().get("/2.0/repositories/:username/:slug/pullrequests").getPath()
        );
        assertEquals(
            "/2.0/repositories/:username/:slug/pullrequests/:pid",
            api.getPaths().get("/2.0/repositories/:username/:slug/pullrequests/:pid").getPath()
        );
        assertEquals(
            "/2.0/repositories/:username/:slug/pullrequests/:pid/merge",
            api.getPaths().get("/2.0/repositories/:username/:slug/pullrequests/:pid/merge").getPath()
        );
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstore() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/petstore.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("/v1", api.getContextPath());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals("http://petstore.swagger.io/v1", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        final Path pets = api.getPaths().get("/pets");
        assertNotNull(pets);
        final Path petsId = api.getPaths().get("/pets/:petId");
        assertNotNull(petsId);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstoreExpanded() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/petstore-expanded.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals("/api", api.getContextPath());
        assertEquals("http://petstore.swagger.io/api", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        final Path pets = api.getPaths().get("/pets");
        assertEquals("/pets", pets.getPath());
        final Path petsId = api.getPaths().get("/pets/:id");
        assertEquals("/pets/:id", petsId.getPath());
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/uspto.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("USPTO Data Set API", api.getName());
        assertEquals("/ds-api", api.getContextPath());
        assertEquals(2, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("http://developer.uspto.gov/ds-api"));
        assertTrue(api.getEndpoint().contains("https://developer.uspto.gov/ds-api"));
        assertEquals(3, api.getPaths().size());
        final Path metadata = api.getPaths().get("/");
        assertNotNull(metadata);
        final Path fields = api.getPaths().get("/:dataset/:version/fields");
        assertNotNull(fields);
        final Path searchRecords = api.getPaths().get("/:dataset/:version/records");
        assertNotNull(searchRecords);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithEnumExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/enum-example.yml", true);
        assertEquals("v1", api.getVersion());
        assertEquals("Gravitee Import Mock Example", api.getName());
        assertEquals("graviteeimportmockexample", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("/"));
        assertEquals(1, api.getPaths().size());
        final Path swaggerPath = api.getPaths().get("/");
        assertNotNull(swaggerPath);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMonoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-monoserver.yaml", true);
        assertEquals("/v1", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://development.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMultiServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-multiserver.yaml", true);
        assertEquals("/v1", api.getContextPath());
        assertEquals(3, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://development.gigantic-server.com/v1"));
        assertTrue(api.getEndpoint().contains("https://staging.gigantic-server.com/v1"));
        assertTrue(api.getEndpoint().contains("https://api.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithNoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-noserver.yaml", true);
        assertEquals("noserver", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("/"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithVariablesInServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-variables-in-server.yaml", true);
        assertEquals("/v2", api.getContextPath());
        assertEquals(2, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://demo.gigantic-server.com:443/v2"));
        assertTrue(api.getEndpoint().contains("https://demo.gigantic-server.com:8443/v2"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithComplexReferences() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/json-api.yml", true);

        assertEquals(2, api.getPaths().size());
        final Path swaggerPath = api.getPaths().get("/drives");
        assertNotNull(swaggerPath);
    }
}
