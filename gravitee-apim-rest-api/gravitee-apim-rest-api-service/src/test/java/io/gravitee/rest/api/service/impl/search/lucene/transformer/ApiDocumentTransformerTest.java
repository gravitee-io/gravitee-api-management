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
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ALLOW_IN_API_PRODUCTS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_API_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PROVIDER_ORGANIZATION_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_STATUS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_STATUS_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_VISIBILITY;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.federation.FederatedApiAgentEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentTransformerTest {

    private static final BytesRef SORT_KEY_OF_STARTED = pinnedSortKey("0066006700530065006700580056000000010001000100010001000100010000");

    private static final BytesRef SORT_KEY_OF_ALPHA_BETA_GAMMA_WITHOUT_SPECIAL_CHARS = pinnedSortKey(
        "0053005f0063005b00530054005800670053005a005300600060005300000001000100010001000100010001000100010077000100010001000100010000"
    );

    @InjectMocks
    ApiDocumentTransformer cut = new ApiDocumentTransformer(new ApiServiceImpl());

    @Test
    void shouldTransform() {
        ApiEntity toTransform = getApiEntity();
        Document transformed = cut.transform(toTransform);
        assertDocumentMatchesInputApiEntity(toTransform, transformed);
    }

    @Test
    void shouldTransformWithoutError_OnMissingReferenceId() {
        ApiEntity api = new ApiEntity();
        api.setId("api-uuid");
        api.setLifecycleState(ApiLifecycleState.CREATED);
        api.setVisibility(Visibility.PUBLIC);
        Document doc = cut.transform(api);
        assertThat(doc.get("id")).isEqualTo(api.getId());
    }

    @Test
    void shouldTransformWithoutError_WithIdOnly() {
        // When we delete a document only the id is filled in
        ApiEntity api = new ApiEntity();
        api.setId("api-uuid");
        Document doc = cut.transform(api);
        assertThat(doc.get("id")).isEqualTo(api.getId());
        assertThat(doc.get("type")).isEqualTo("api");
    }

    @Test
    void should_transform_id_and_type_only_when_definition_version_and_name_are_null() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setLifecycleState(ApiLifecycleState.CREATED);
        api.setVisibility(Visibility.PUBLIC);
        api.setDefinitionVersion(null);
        api.setName(null);

        Document doc = cut.transform(api);

        assertThat(doc.getFields()).extracting(IndexableField::name).containsExactlyInAnyOrder(FIELD_ID, FIELD_TYPE);
        assertThat(doc.get(FIELD_ID)).isEqualTo(api.getId());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apiTypes")
    void should_index_the_api_type_term(String caseName, GenericApiEntity api, String expectedApiType) {
        Document doc = cut.transform(api);

        assertThat(doc.get(FIELD_ID)).isEqualTo(api.getId());
        assertThat(doc.get(FIELD_VISIBILITY)).isEqualTo("PUBLIC");
        assertThat(doc.get(FIELD_API_TYPE)).isEqualTo(expectedApiType);
    }

    private static Stream<Arguments> apiTypes() {
        return Stream.of(
            Arguments.of("a v4 message api", v4Api(ApiType.MESSAGE), "V4_MESSAGE"),
            Arguments.of("a v4 llm proxy api", v4Api(ApiType.LLM_PROXY), "V4_LLM_PROXY"),
            Arguments.of("a v4 mcp proxy api", v4Api(ApiType.MCP_PROXY), "V4_MCP_PROXY"),
            Arguments.of("a v4 native api", v4Api(ApiType.NATIVE), "V4_KAFKA"),
            Arguments.of("a native api entity", nativeApi(), "V4_KAFKA"),
            Arguments.of("a v4 proxy api listening on tcp", v4ProxyApi(TcpListener.builder().build()), "V4_TCP_PROXY"),
            Arguments.of("a v4 proxy api listening on http", v4ProxyApi(HttpListener.builder().paths(List.of()).build()), "V4_HTTP_PROXY"),
            Arguments.of("a federated api", federatedApi(), "FEDERATED"),
            Arguments.of("a federated agent", federatedAgent(), "FEDERATED_AGENT"),
            Arguments.of("a v2 api", v2Api(), "V2")
        );
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity v4Api(ApiType type) {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(type);
        api.setVisibility(Visibility.PUBLIC);
        return api;
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity v4ProxyApi(Listener listener) {
        var api = v4Api(ApiType.PROXY);
        api.setListeners(List.of(listener));
        return api;
    }

    private static NativeApiEntity nativeApi() {
        return NativeApiEntity.builder().id("api-uuid").definitionVersion(DefinitionVersion.V4).visibility(Visibility.PUBLIC).build();
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity federatedApi() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.FEDERATED);
        api.setType(ApiType.PROXY);
        api.setVisibility(Visibility.PUBLIC);
        api.setListeners(List.of(HttpListener.builder().paths(List.of()).build()));
        return api;
    }

    private static ApiEntity v2Api() {
        return ApiEntity.builder().id("api-uuid").name("API 1").graviteeDefinitionVersion("2.0.0").visibility(Visibility.PUBLIC).build();
    }

    @Test
    void transform_api_entity_v4_http_proxy_should_index_allow_in_api_products_flag() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        api.setVisibility(Visibility.PUBLIC);
        api.setAllowedInApiProducts(true);

        Document doc = cut.transform(api);
        assertThat(doc.get(FIELD_ALLOW_IN_API_PRODUCTS)).isEqualTo("true");
    }

    @Test
    void transform_api_entity_v4_http_proxy_should_index_allow_in_api_products_as_false_when_null() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        api.setVisibility(Visibility.PUBLIC);
        api.setAllowedInApiProducts(null);

        Document doc = cut.transform(api);
        assertThat(doc.get(FIELD_ALLOW_IN_API_PRODUCTS)).isEqualTo("false");
    }

    @Test
    void transform_api_entity_v4_http_proxy_should_index_allow_in_api_products_as_false_when_false() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        api.setVisibility(Visibility.PUBLIC);
        api.setAllowedInApiProducts(false);

        Document doc = cut.transform(api);
        assertThat(doc.get(FIELD_ALLOW_IN_API_PRODUCTS)).isEqualTo("false");
    }

    @Test
    void transform_api_entity_v4_message_should_still_index_allow_in_api_products_as_false_when_null() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.MESSAGE);
        api.setVisibility(Visibility.PUBLIC);
        api.setAllowedInApiProducts(null);

        Document doc = cut.transform(api);
        assertThat(doc.get(FIELD_ALLOW_IN_API_PRODUCTS)).isEqualTo("false");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statuses")
    void should_index_the_status_and_its_sort_key(String caseName, GenericApiEntity api, String expectedStatus, BytesRef expectedSortKey) {
        Document doc = cut.transform(api);

        assertThat(doc.get(FIELD_STATUS)).isEqualTo(expectedStatus);
        assertThat(doc.getFields(FIELD_STATUS_SORTED))
            .extracting(IndexableField::binaryValue)
            .containsExactlyElementsOf(expectedSortKey == null ? List.of() : List.of(expectedSortKey));
    }

    @Test
    void should_strip_special_characters_before_building_the_name_sort_key() {
        var api = v4Api(ApiType.PROXY);
        api.setName("Alpha-Beta (Gamma)");

        Document doc = cut.transform(api);

        assertThat(doc.getField(FIELD_NAME_SORTED).binaryValue()).isEqualTo(SORT_KEY_OF_ALPHA_BETA_GAMMA_WITHOUT_SPECIAL_CHARS);
    }

    private static BytesRef pinnedSortKey(String hex) {
        return new BytesRef(HexFormat.of().parseHex(hex));
    }

    private static Stream<Arguments> statuses() {
        return Stream.of(
            Arguments.of("a non federated api indexes its status", startedV4Api(), "STARTED", SORT_KEY_OF_STARTED),
            Arguments.of("a federated api carries no status term", federatedApi(), null, null),
            Arguments.of("a federated agent carries no status term", federatedAgent(), null, null)
        );
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity startedV4Api() {
        var api = v4Api(ApiType.PROXY);
        api.setState(Lifecycle.State.STARTED);
        return api;
    }

    private static FederatedApiAgentEntity federatedAgent() {
        return FederatedApiAgentEntity.builder().id("api-agent").name("Alpha Agent").visibility(Visibility.PUBLIC).build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("agentProviders")
    void should_index_the_agent_card_provider_organization_lower_cased(
        String caseName,
        FederatedApiAgentEntity.Provider provider,
        String expectedTerm
    ) {
        var api = FederatedApiAgentEntity.builder()
            .id("api-agent")
            .name("Beta Runner")
            .visibility(Visibility.PUBLIC)
            .provider(provider)
            .build();

        Document doc = cut.transform(api);

        assertThat(doc.get(FIELD_ID)).isEqualTo("api-agent");
        assertThat(doc.get(FIELD_PROVIDER_ORGANIZATION_LOWERCASE)).isEqualTo(expectedTerm);
    }

    private static Stream<Arguments> agentProviders() {
        return Stream.of(
            Arguments.of(
                "an organization is indexed as one whole lower cased term",
                new FederatedApiAgentEntity.Provider("Acme Robotics", "https://example.net"),
                "acme robotics"
            ),
            Arguments.of("an agent card with no provider at all carries no organization term", null, null),
            Arguments.of(
                "a provider whose organization is null carries no organization term",
                new FederatedApiAgentEntity.Provider(null, "https://example.net"),
                null
            ),
            Arguments.of(
                "a provider whose organization is blank carries no organization term",
                new FederatedApiAgentEntity.Provider("   ", "https://example.net"),
                null
            )
        );
    }

    @Test
    void transform_v4_api_should_index_paths_and_hosts_lowercase() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        api.setVisibility(Visibility.PUBLIC);

        io.gravitee.definition.model.v4.listener.http.Path path = new io.gravitee.definition.model.v4.listener.http.Path();
        path.setPath("/TestPath");
        path.setHost("api.TestHost.com");

        api.setListeners(List.of(HttpListener.builder().paths(List.of(path)).build()));
        Document doc = cut.transform(api);
        assertThat(doc.getFields("paths_lowercase")[0].stringValue()).isEqualTo("/testpath");
        assertThat(doc.getFields("hosts_lowercase")[0].stringValue()).isEqualTo("api.testhost.com");
    }

    @Nested
    class HasHealthCheck {

        @Test
        void v4_as_endpoint_group() {
            var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
            api.setId("api-uuid");
            api.setLifecycleState(ApiLifecycleState.CREATED);
            api.setVisibility(Visibility.PUBLIC);
            api.setDefinitionVersion(DefinitionVersion.V4);
            api.setType(ApiType.PROXY);
            api.setName("name");
            api.setEndpointGroups(List.of(new EndpointGroup(new EndpointGroupServices(null, new Service(true, true, "type", "conf")))));

            Document doc = cut.transform(api);
            assertThat(doc.get("has_health_check")).isEqualTo("true");
        }

        @Test
        void v4_as_endpoint_() {
            var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
            api.setId("api-uuid");
            api.setLifecycleState(ApiLifecycleState.CREATED);
            api.setVisibility(Visibility.PUBLIC);
            api.setDefinitionVersion(DefinitionVersion.V4);
            api.setType(ApiType.PROXY);
            api.setName("name");
            var endpointGroup = new EndpointGroup(new EndpointGroupServices(null, null));
            endpointGroup.setEndpoints(List.of(new Endpoint(new EndpointServices(new Service(true, true, "type", "conf")))));
            api.setEndpointGroups(List.of(endpointGroup));

            Document doc = cut.transform(api);
            assertThat(doc.get("has_health_check")).isEqualTo("true");
        }
    }

    @NotNull
    private ApiEntity getApiEntity() {
        ApiEntity toTransform = new ApiEntity();
        toTransform.setId("apiId");
        toTransform.setName("name");
        toTransform.setLifecycleState(ApiLifecycleState.CREATED);
        toTransform.setVisibility(Visibility.PUBLIC);
        toTransform.setDescription("description");
        toTransform.setReferenceId("xxxxxx");
        toTransform.setReferenceType("env1");
        UserEntity userEntity = new UserEntity();
        userEntity.setId("userId");
        userEntity.setEmail("userMail");
        userEntity.setFirstname("userFirstname");
        userEntity.setLastname("userLastname");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        toTransform.setPrimaryOwner(primaryOwnerEntity);
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Arrays.asList(new VirtualHost("host", "path"), new VirtualHost("host2", "path2")));
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        Services services = new Services();
        services.setHealthCheckService(healthCheckService);
        toTransform.setServices(services);
        toTransform.setProxy(proxy);
        toTransform.setLabels(Arrays.asList("label1", "label2", "label2"));
        toTransform.setCategories(new HashSet<>(Arrays.asList("cat1", "cat2")));
        toTransform.setTags(new HashSet<>(Arrays.asList("tag1", "tag2")));
        Date date = new Date();
        toTransform.setCreatedAt(date);
        toTransform.setUpdatedAt(date);
        HashMap<String, Object> metadatas = new HashMap<>();
        metadatas.put("metadata1", "value1");
        metadatas.put("metadata2", "value2");
        metadatas.put("metadata3", "value3");
        toTransform.setMetadata(metadatas);
        DefinitionContext context = new DefinitionContext();
        toTransform.setDefinitionContext(context);
        return toTransform;
    }

    private void assertDocumentMatchesInputApiEntity(ApiEntity toTransform, Document transformed) {
        assertThat(toTransform.getId()).isEqualTo(transformed.get("id"));
        assertThat(toTransform.getName()).isEqualTo(transformed.get("name"));
        assertThat(toTransform.getDescription()).isEqualTo(transformed.get("description"));
        assertThat(toTransform.getPrimaryOwner().getDisplayName()).isEqualTo(transformed.get("ownerName"));
        assertThat(toTransform.getPrimaryOwner().getEmail()).isEqualTo(transformed.get("ownerMail"));
        IndexableField[] paths = transformed.getFields("paths");
        IndexableField[] hosts = transformed.getFields("hosts");
        assertThat(toTransform.getProxy().getVirtualHosts()).hasSize(paths.length);
        assertThat(toTransform.getProxy().getVirtualHosts()).hasSameSizeAs(hosts);
        assertThat(toTransform.getLabels()).hasSameSizeAs(transformed.getFields("labels"));
        assertThat(toTransform.getCategories()).hasSameSizeAs(transformed.getFields("categories"));
        assertThat(toTransform.getTags()).hasSameSizeAs(transformed.getFields("tags"));
        assertThat(toTransform.getCreatedAt().getTime()).isEqualTo(((LongPoint) transformed.getField("createdAt")).numericValue());
        assertThat(toTransform.getUpdatedAt().getTime()).isEqualTo(((LongPoint) transformed.getField("updatedAt")).numericValue());
        assertThat(toTransform.getMetadata().values()).hasSameSizeAs(transformed.getFields("metadata"));
        assertThat(toTransform.getDefinitionContext().getOrigin()).isEqualTo(transformed.get("origin"));
        assertThat("true").isEqualTo(transformed.get("has_health_check"));
    }

    @Test
    public void shouldSortListCorrectlyWithCollatorAndBytesRef() throws Exception {
        List<String> names = List.of("nano", "Zorro", "äther", "vem", "foo/bar", "Épée", "épona", "öko", "bns-One");
        List<String> expectedSorted = List.of("äther", "bns-One", "Épée", "épona", "foo/bar", "nano", "öko", "vem", "Zorro");
        Method toSortedValueMethod = ApiDocumentTransformer.class.getDeclaredMethod("toSortedValue", String.class);
        toSortedValueMethod.setAccessible(true);

        Field collatorField = ApiDocumentTransformer.class.getDeclaredField("collator");
        collatorField.setAccessible(true);
        Collator collator = (Collator) collatorField.get(cut);
        List<String> sortedByCollator = new ArrayList<>(names);
        sortedByCollator.sort(collator);
        Map<String, BytesRef> bytesRefMap = new HashMap<>();
        for (String name : names) {
            bytesRefMap.put(name, (BytesRef) toSortedValueMethod.invoke(cut, name));
        }
        List<String> sortedByBytesRef = new ArrayList<>(names);
        sortedByBytesRef.sort(Comparator.comparing(bytesRefMap::get));
        assertThat(sortedByCollator).isEqualTo(expectedSorted);
        assertThat(sortedByBytesRef).isEqualTo(expectedSorted);
    }
}
