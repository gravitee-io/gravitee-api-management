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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.Resources;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.SwaggerServiceImpl;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.OAIPolicyOperationVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Mock
    private GroupService groupService;

    @Mock
    private TagService tagService;

    @InjectMocks
    protected SwaggerServiceImpl swaggerService;

    protected DefinitionVersion getDefinitionVersion() {
        return DefinitionVersion.V1;
    }

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
        when(op.visit(any(), any())).thenReturn(Optional.of(new Policy()));
        when(policyOperationVisitorManager.getOAIOperationVisitor(anyString())).thenReturn(op);

        GroupEntity grp1 = new GroupEntity();
        grp1.setId("group1");
        GroupEntity grp2 = new GroupEntity();
        grp2.setId("group2");
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "group1")).thenReturn(Arrays.asList(grp1));
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "group2")).thenReturn(Arrays.asList(grp2));

        TagEntity tag1 = new TagEntity();
        tag1.setId("tagId1");
        tag1.setName("tag1");
        TagEntity tag2 = new TagEntity();
        tag2.setId("tagId2");
        tag2.setName("tag2");
        when(tagService.findByReference(any(), any())).thenReturn(Arrays.asList(tag1, tag2));
    }

    // Swagger v1
    @Test
    public void shouldPrepareAPIFromSwaggerV1_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v1.json", true, true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV1_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v1.json", true));
    }

    // Swagger v2
    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v2.json", true, true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v2.json", true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_json_extensions() throws IOException {
        final SwaggerApiEntity swaggerApiEntity = prepareUrl(
            "io/gravitee/rest/api/management/service/swagger-withExtensions-v2.json",
            true,
            true
        );
        validate(swaggerApiEntity);
        validateExtensions(swaggerApiEntity);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/swagger-v2.yaml", true, true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/swagger-v2.yaml", true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_yaml_extensions() throws IOException {
        final SwaggerApiEntity swaggerApiEntity = prepareUrl(
            "io/gravitee/rest/api/management/service/swagger-withExtensions-v2.yaml",
            true,
            true
        );
        validate(swaggerApiEntity);
        validateExtensions(swaggerApiEntity);
    }

    // OpenAPI
    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/openapi.json", true, true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/openapi.json", true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_json_extensions() throws IOException {
        final SwaggerApiEntity swaggerApiEntity = prepareUrl(
            "io/gravitee/rest/api/management/service/openapi-withExtensions.json",
            true,
            true
        );
        validate(swaggerApiEntity);
        validateExtensions(swaggerApiEntity);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/rest/api/management/service/openapi.yaml", true, true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/rest/api/management/service/openapi.yaml", true));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_yaml_extensions() throws IOException {
        final SwaggerApiEntity swaggerApiEntity = prepareUrl(
            "io/gravitee/rest/api/management/service/openapi-withExtensions.yaml",
            true,
            true
        );
        validate(swaggerApiEntity);
        validateExtensions(swaggerApiEntity);
    }

    private void validateExtensions(UpdateApiEntity updateApiEntity) {
        final List<VirtualHost> virtualHosts = updateApiEntity.getProxy().getVirtualHosts();
        assertEquals(1, virtualHosts.size());
        VirtualHost vHost = virtualHosts.get(0);
        assertEquals("myHost", vHost.getHost());
        assertEquals("myPath", vHost.getPath());
        assertEquals(false, vHost.isOverrideEntrypoint());

        final Set<String> categories = updateApiEntity.getCategories();
        assertEquals(2, categories.size());
        assertTrue(categories.containsAll(asList("cat1", "cat2")));

        final Set<String> groups = updateApiEntity.getGroups();
        assertEquals(2, groups.size());
        assertTrue(groups.containsAll(asList("group1", "group2")));

        final List<String> labels = updateApiEntity.getLabels();
        assertEquals(2, labels.size());
        assertTrue(labels.containsAll(asList("label1", "label2")));

        final Set<String> tags = updateApiEntity.getTags();
        assertEquals(2, tags.size());
        assertTrue(tags.containsAll(asList("tagId1", "tagId2")));

        final Map<String, String> properties = updateApiEntity.getProperties().toDefinition().getValues();
        assertEquals(2, properties.size());
        assertTrue(properties.keySet().containsAll(asList("prop1", "prop2")));
        assertTrue(properties.values().containsAll(asList("propValue1", "propValue2")));

        final Map<String, String> metadata = updateApiEntity
            .getMetadata()
            .stream()
            .collect(Collectors.toMap(ApiMetadataEntity::getName, ApiMetadataEntity::getValue));
        assertEquals(2, metadata.size());
        assertTrue(metadata.keySet().containsAll(asList("meta1", "meta2")));
        assertTrue(metadata.values().containsAll(asList("1234", "metaValue2")));

        assertEquals(Visibility.PRIVATE, updateApiEntity.getVisibility());
        assertEquals("data:image/png;base64,XXXXXXX", updateApiEntity.getPicture());
    }

    protected void validate(SwaggerApiEntity api) {
        assertEquals("1.2.3", api.getVersion());
        assertEquals("Gravitee.io Swagger API", api.getName());
        assertEquals(
            "https://demo.gravitee.io/gateway/echo",
            api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget()
        );
        validatePolicies(api, 2, 0, asList("/pets", "/pets/:petId"));
    }

    protected void validatePolicies(SwaggerApiEntity api, int expectedPathSize, int expectedOperationSize, List<String> expectedPaths) {
        assertEquals(expectedPathSize, api.getPaths().size());
        assertTrue(api.getPaths().keySet().containsAll(expectedPaths));

        List<HttpMethod> operations = api
            .getPaths()
            .values()
            .stream()
            .map(
                new Function<List<Rule>, Set<HttpMethod>>() {
                    @Nullable
                    @Override
                    public Set<HttpMethod> apply(@Nullable List<Rule> rules) {
                        Set<HttpMethod> collect = rules
                            .stream()
                            .map(
                                new Function<Rule, List<HttpMethod>>() {
                                    @Nullable
                                    @Override
                                    public List<HttpMethod> apply(@Nullable Rule rule) {
                                        return new ArrayList(rule.getMethods());
                                    }
                                }
                            )
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());

                        return collect;
                    }
                }
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        assertEquals(expectedOperationSize, operations.size());
    }

    private SwaggerApiEntity prepareInline(String file) throws IOException {
        return prepareInline(file, false, false);
    }

    private SwaggerApiEntity prepareInline(String file, boolean withPolicyPaths) throws IOException {
        return prepareInline(file, withPolicyPaths, false);
    }

    protected SwaggerApiEntity prepareInline(String file, boolean withPolicyPaths, boolean withPolicies) throws IOException {
        URL url = Resources.getResource(file);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.INLINE);
        swaggerDescriptor.setPayload(descriptor);
        swaggerDescriptor.setWithPolicyPaths(withPolicyPaths);
        if (withPolicies) {
            swaggerDescriptor.setWithPolicies(asList("mock"));
        }
        return this.createAPI(swaggerDescriptor);
    }

    private SwaggerApiEntity prepareUrl(String file, boolean withPolicyPaths, boolean withPathMapping) {
        URL url = Resources.getResource(file);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.URL);
        swaggerDescriptor.setWithPolicyPaths(withPolicyPaths);
        swaggerDescriptor.setWithPathMapping(withPathMapping);
        //        swaggerDescriptor.setWithPolicies(asList("mock"));
        try {
            swaggerDescriptor.setPayload(url.toURI().getPath());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return this.createAPI(swaggerDescriptor);
    }

    protected SwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        return swaggerService.createAPI(swaggerDescriptor);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/api-with-examples.yaml", true, true);
        assertEquals("2.0.0", api.getVersion());
        assertEquals("Simple API overview", api.getName());
        assertEquals("simpleapioverview", api.getProxy().getVirtualHosts().get(0).getPath());
        assertEquals("/", api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget());

        validatePolicies(api, 2, 2, asList("/", "/v2"));
        validateRules(api, "/", 2, asList(HttpMethod.GET), "List API versions");
    }

    protected void validateRules(
        SwaggerApiEntity api,
        String path,
        int expectedRuleSize,
        List<HttpMethod> firstRuleMethods,
        String firstRuleDescription
    ) {
        List<Rule> rules = api.getPaths().get(path);
        assertEquals(expectedRuleSize, rules.size());
        Rule rule = rules.get(0);
        assertTrue(rule.getMethods().containsAll(firstRuleMethods));
        assertEquals(firstRuleDescription, rule.getDescription());
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithSimpleTypedExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/callback-example.yaml", true, true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Callback Example", api.getName());
        assertEquals("callbackexample", api.getProxy().getVirtualHosts().get(0).getPath());
        assertEquals("/", api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget());

        validatePolicies(api, 1, 1, asList("/streams"));
        validateRules(api, "/streams", 2, asList(HttpMethod.POST), "subscribes a client to receive out-of-band data");
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithLinks() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/link-example.yaml", true, true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Link Example", api.getName());
        assertEquals("linkexample", api.getProxy().getVirtualHosts().get(0).getPath());
        assertEquals("/", api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget());

        validatePolicies(
            api,
            6,
            6,
            asList(
                "/2.0/users/:username",
                "/2.0/repositories/:username/:slug",
                "/2.0/repositories/:username/:slug/pullrequests",
                "/2.0/repositories/:username/:slug/pullrequests/:pid",
                "/2.0/repositories/:username/:slug/pullrequests/:pid/merge"
            )
        );
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstore() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/petstore.yaml", true, true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("/v1", api.getProxy().getVirtualHosts().get(0).getPath());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals(
            "http://petstore.swagger.io/v1",
            api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget()
        );

        validatePolicies(api, 2, 3, asList("/pets", "/pets/:petId"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstoreExpanded() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/petstore-expanded.yaml", true, true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals("/api", api.getProxy().getVirtualHosts().get(0).getPath());
        assertEquals(
            "http://petstore.swagger.io/api",
            api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget()
        );
        validatePolicies(api, 2, 4, asList("/pets", "/pets/:id"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/uspto.yaml", true, true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("USPTO Data Set API", api.getName());
        assertEquals("/ds-api", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(2, 4, endpoints.size());
        assertTrue(endpoints.contains("http://developer.uspto.gov/ds-api"));
        assertTrue(endpoints.contains("https://developer.uspto.gov/ds-api"));

        validatePolicies(api, 3, 3, asList("/", "/:dataset/:version/fields", "/:dataset/:version/records"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithEnumExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/enum-example.yml", true, true);
        assertEquals("v1", api.getVersion());
        assertEquals("Gravitee Import Mock Example", api.getName());
        assertEquals("graviteeimportmockexample", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains("/"));

        validatePolicies(api, 1, 1, asList("/"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMonoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-monoserver.yaml", true);
        assertEquals("/v1", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(1, 2, endpoints.size());
        assertTrue(endpoints.contains("https://development.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMultiServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-multiserver.yaml", true);
        assertEquals("/v1", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(3, endpoints.size());
        assertTrue(endpoints.contains("https://development.gigantic-server.com/v1"));
        assertTrue(endpoints.contains("https://staging.gigantic-server.com/v1"));
        assertTrue(endpoints.contains("https://api.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithNoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-noserver.yaml", true);
        assertEquals("noserver", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains("/"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithVariablesInServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/openapi-variables-in-server.yaml", true);
        assertEquals("/v2", api.getProxy().getVirtualHosts().get(0).getPath());

        final List<String> endpoints = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .stream()
            .map(Endpoint::getTarget)
            .collect(Collectors.toList());
        assertEquals(2, endpoints.size());
        assertTrue(endpoints.contains("https://demo.gigantic-server.com:443/v2"));
        assertTrue(endpoints.contains("https://demo.gigantic-server.com:8443/v2"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithComplexReferences() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/rest/api/management/service/mock/json-api.yml", true, true);
        validatePolicies(api, 2, 5, asList("/drives"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithURLAndPathMappingOnly() throws IOException {
        final SwaggerApiEntity swaggerApiEntity = prepareUrl("io/gravitee/rest/api/management/service/openapi.yaml", false, true);

        assertTrue(swaggerApiEntity.getPathMappings().containsAll(asList("/pets", "/pets/:petId")));
        validatePathMappings(swaggerApiEntity, asList("/pets", "/pets/:petId"));

        validatePolicies(swaggerApiEntity, 1, 0, this.getDefinitionVersion().equals(DefinitionVersion.V1) ? asList("/") : asList());
    }

    protected void validatePathMappings(SwaggerApiEntity api, List<String> expectedPaths) {
        assertTrue(api.getPathMappings().containsAll(expectedPaths));
    }
}
