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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.api.use_case.ExportApiUseCase;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.PageMedia;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Media;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.Metadata;
import io.gravitee.rest.api.management.v2.rest.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.PageType;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.model.Role;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource_ExportApiDefinitionTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_export/definition";
    }

    @Test
    public void should_not_export_when_no_definition_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.READ
            )
        )
            .thenReturn(false);
        Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    public void should_not_export_v2_apis() {
        when(exportApiUseCase.execute(eq(API), any(), any())).thenThrow(new ApiDefinitionVersionNotSupportedException("2.0.0"));
        Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    public void should_export_ApiEntityV4() {
        when(exportApiUseCase.execute(eq(API), any(), any()))
            .thenReturn(new ExportApiUseCase.Output(fakeExportApiEntity(fakeApiEntityV4())));

        Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final ExportApiV4 export = response.readEntity(ExportApiV4.class);
        assertThat(export.getMembers()).isNotNull();
        assertThat(export.getMetadata()).isNotNull();
        assertThat(export.getPlans()).isNotNull();
        assertThat(export.getPages()).isNotNull();
        assertThat(export.getApi()).isNotNull();

        testReturnedApi(export.getApi());

        testReturnedMembers(export.getMembers());

        testReturnedMetadata(export.getMetadata());

        testReturnedPlans(export.getPlans());

        testReturnedPages(export.getPages());
        assertThat(export.getApiMedia()).hasSize(1).have(testReturnedMedia());
    }

    @Test
    public void should_export_NativeApiEntityV4() {
        when(exportApiUseCase.execute(eq(API), any(), any()))
            .thenReturn(new ExportApiUseCase.Output(fakeExportApiEntity(fakeNativeApiEntityV4())));

        Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(OK_200);

        var export = response.readEntity(ExportApiV4.class);
        assertThat(export.getMembers()).isNotNull();
        assertThat(export.getMetadata()).isNotNull();
        assertThat(export.getPlans()).isNotNull();
        assertThat(export.getPages()).isNotNull();
        assertThat(export.getApi()).isNotNull();

        testReturnedNativeApi(export.getApi());

        testReturnedMembers(export.getMembers());

        testReturnedMetadata(export.getMetadata());

        testReturnedPlans(export.getPlans());

        testReturnedPages(export.getPages());
        assertThat(export.getApiMedia()).hasSize(1).have(testReturnedMedia());
    }

    private GraviteeDefinition fakeExportApiEntity(ApiDescriptor apiEntity) {
        return switch (apiEntity) {
            case ApiDescriptor.ApiDescriptorV4 v4 -> new GraviteeDefinition.V4(
                v4,
                fakeApiMembers(),
                fakeApiMetadata(),
                fakeApiPages(),
                fakeApiPlans(),
                fakeApiMedia(),
                null,
                null
            );
            case ApiDescriptor.ApiDescriptorNative v4Native -> new GraviteeDefinition.Native(
                v4Native,
                fakeApiMembers(),
                fakeApiMetadata(),
                fakeApiPages(),
                fakeApiPlans(),
                fakeApiMedia(),
                null,
                null
            );
            case null, default -> throw new RuntimeException("Unsupported api descriptor");
        };
    }

    private ApiDescriptor.ApiDescriptorV4 fakeApiEntityV4() {
        var httpListener = HttpListener.builder().paths(List.of(new Path("my.fake.host", "/test"))).pathMappings(Set.of("/test")).build();

        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        entrypoint.setDlq(new Dlq("my-endpoint"));
        entrypoint.setConfiguration("{\n \"nice\" : \"configuration\"\n}");
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        subscriptionListener.setType(ListenerType.SUBSCRIPTION);

        TcpListener tcpListener = new TcpListener();
        tcpListener.setType(ListenerType.TCP);
        tcpListener.setEntrypoints(List.of(entrypoint));

        var endpoint = Endpoint
            .builder()
            .type("http-get")
            .configuration(
                """
                        {
                                                "bootstrapServers": "kafka:9092",
                                                "topics": [
                                                    "demo"
                                                ],
                                                "producer": {
                                                    "enabled": false
                                                },
                                                "consumer": {
                                                    "encodeMessageId": true,
                                                    "enabled": true,
                                                    "autoOffsetReset": "earliest"
                                                }
                                            }"""
            )
            .build();
        var endpointGroup = EndpointGroup.builder().type("http-get").endpoints(List.of(endpoint)).build();

        Flow flow = new Flow();
        flow.setName("flowName");
        flow.setEnabled(true);

        Step step = new Step();
        step.setEnabled(true);
        step.setPolicy("my-policy");
        step.setCondition("my-condition");
        flow.setRequest(List.of(step));
        flow.setTags(Set.of("tag1", "tag2"));

        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/test");
        httpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        httpSelector.setPathOperator(Operator.STARTS_WITH);

        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setChannel("my-channel");
        channelSelector.setChannelOperator(Operator.STARTS_WITH);
        channelSelector.setOperations(Set.of(ChannelSelector.Operation.SUBSCRIBE));
        channelSelector.setEntrypoints(Set.of("my-entrypoint"));

        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition("my-condition");

        flow.setSelectors(List.of(httpSelector, channelSelector, conditionSelector));

        return ApiDescriptor.ApiDescriptorV4
            .builder()
            .id(API)
            .name(API)
            .apiVersion("v1.0")
            .listeners(List.of(httpListener, subscriptionListener, tcpListener))
            .properties(List.of(new Property()))
            .services(new ApiServices())
            .resources(List.of(new Resource()))
            .responseTemplates(Map.of("key", new HashMap<>()))
            .updatedAt(Instant.now())
            .analytics(new Analytics())
            .endpointGroups(List.of(endpointGroup))
            .flows(List.of(flow))
            .build();
    }

    private ApiDescriptor.ApiDescriptorNative fakeNativeApiEntityV4() {
        var endpoint = NativeEndpoint.builder().type("kafka").configuration("{\"bootstrapServers\": \"kafka:9092\"}").build();
        var endpointGroup = NativeEndpointGroup.builder().type("kafka").endpoints(List.of(endpoint)).build();

        var kafkaListener = KafkaListener.builder().host("my.fake.host").build();
        var step = Step.builder().enabled(true).policy("my-policy").condition("my-condition").build();

        var flow = NativeFlow.builder().name("flowName").enabled(true).interact(List.of(step)).tags(Set.of("tag1", "tag2")).build();

        return ApiDescriptor.ApiDescriptorNative
            .builder()
            .id(API)
            .name(API)
            .apiVersion("v1.0")
            .listeners(List.of(kafkaListener))
            .properties(List.of(new Property()))
            .resources(List.of(new Resource()))
            .updatedAt(Instant.now())
            .endpointGroups(List.of(endpointGroup))
            .flows(List.of(flow))
            .build();
    }

    private Set<ApiMember> fakeApiMembers() {
        var userMember = new ApiMember();
        userMember.setId("memberId");
        userMember.setDisplayName("John Doe");
        userMember.setRoles(List.of(ApiMemberRole.builder().name("OWNER").build()));
        userMember.setType(MembershipMemberType.USER);

        var poUserMember = new ApiMember();
        poUserMember.setId("poMemberId");
        poUserMember.setDisplayName("Thomas Pesquet");
        poUserMember.setRoles(List.of(ApiMemberRole.builder().name("PRIMARY_OWNER").build()));
        poUserMember.setType(MembershipMemberType.USER);

        return Set.of(userMember, poUserMember);
    }

    private Set<NewApiMetadata> fakeApiMetadata() {
        var firstMetadata = new NewApiMetadata();
        firstMetadata.setApiId(API);
        firstMetadata.setKey("my-metadata-1");
        firstMetadata.setName("My first metadata");
        firstMetadata.setFormat(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.NUMERIC);
        firstMetadata.setValue("1");
        firstMetadata.setDefaultValue("5");

        var secondMetadata = new NewApiMetadata();
        secondMetadata.setApiId(API);
        secondMetadata.setKey("my-metadata-2");
        secondMetadata.setName("My second metadata");
        secondMetadata.setFormat(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.STRING);
        secondMetadata.setValue("Very important data !!");
        secondMetadata.setDefaultValue("Important data");

        return Set.of(firstMetadata, secondMetadata);
    }

    private Set<PlanDescriptor.PlanDescriptorV4> fakeApiPlans() {
        Step step = new Step();
        step.setName("stepName");
        step.setDescription("stepDescription");
        step.setConfiguration("{\n \"nice\" : \"configuration\"\n}");
        step.setCondition("stepCondition");
        step.setPolicy("stepPolicy");
        step.setEnabled(true);
        step.setMessageCondition("stepMessageCondition");

        Flow planFlow = new Flow();
        planFlow.setEnabled(true);
        planFlow.setName("planFlowName");
        planFlow.setPublish(null);
        planFlow.setRequest(List.of(step));
        planFlow.setResponse(null);
        planFlow.setSubscribe(null);

        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        httpSelector.setPath("/test");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        planFlow.setSelectors(List.of(httpSelector));
        planFlow.setTags(null);

        return Set.of(
            PlanDescriptor.PlanDescriptorV4
                .builder()
                .apiId(API)
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("commentMessage")
                .commentRequired(true)
                .crossId("crossId")
                .createdAt(Instant.ofEpochMilli(5025000))
                .closedAt(null)
                .description("description")
                .excludedGroups(List.of("excludedGroup"))
                .generalConditions("generalConditions")
                .id("planId")
                .name("planName")
                //.needRedeployAt(null)
                .order(1)
                .publishedAt(Instant.ofEpochMilli(5026000))
                .status(PlanStatus.PUBLISHED)
                .selectionRule(null)
                .tags(Set.of("tag1", "tag2"))
                .type(Plan.PlanType.API)
                .updatedAt(Instant.ofEpochMilli(5027000))
                .validation(Plan.PlanValidationType.AUTO)
                .security(PlanSecurity.builder().type("key-less").configuration("{}").build())
                .flows(List.of(planFlow))
                .build()
        );
    }

    private List<PageExport> fakeApiPages() {
        PageExport pageEntity = new PageExport();
        var accessControlEntity = new AccessControl("role-id", "ROLE");
        pageEntity.setAccessControls(Set.of(accessControlEntity));
        PageMedia pageMediaEntity = new PageMedia("media-hash", "media-name", new Date(0));
        pageEntity.setAttachedMedia(List.of(pageMediaEntity));
        pageEntity.setConfiguration(Map.of("page-config-key", "page-config-value"));
        pageEntity.setContent("#content");
        pageEntity.setContentRevisionId(new PageEntity.PageRevisionId("page-revision-id", 1));
        pageEntity.setContentType("text/markdown");
        pageEntity.setCrossId("crossId");
        pageEntity.setExcludedGroups(List.of("excludedGroup"));
        pageEntity.setExcludedAccessControls(false);
        pageEntity.setGeneralConditions(true);
        pageEntity.setHomepage(false);
        pageEntity.setId("page-id");
        pageEntity.setLastContributor("last-contributor-id");
        pageEntity.setUpdatedAt(Instant.EPOCH);
        pageEntity.setMessages(List.of("message1", "message2"));
        pageEntity.setMetadata(Map.of("page-metadata-key", "page-metadata-value"));
        pageEntity.setName("page-name");
        pageEntity.setOrder(1);
        pageEntity.setParentId("parent-id");
        pageEntity.setParentPath("parent-path");
        pageEntity.setPublished(true);
        pageEntity.setReferenceId("reference-id");
        pageEntity.setReferenceType(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API);
        PageSource pageSourceEntity = new PageSource("GITHUB", "{}", Map.of());
        pageEntity.setSource(pageSourceEntity);
        pageEntity.setType(io.gravitee.apim.core.documentation.model.Page.Type.MARKDOWN);
        pageEntity.setTranslations(List.of());
        pageEntity.setVisibility(io.gravitee.apim.core.documentation.model.Page.Visibility.PUBLIC);

        return List.of(pageEntity);
    }

    private List<io.gravitee.apim.core.media.model.Media> fakeApiMedia() {
        var mediaEntity = new io.gravitee.apim.core.media.model.Media();
        mediaEntity.setId("media-id");
        mediaEntity.setHash("media-hash");
        mediaEntity.setSize(1_000L);
        mediaEntity.setFileName("media-file-name");
        mediaEntity.setType("media-type");
        mediaEntity.setSubType("media-sub-type");
        mediaEntity.setData("media-data".getBytes(UTF_8));
        mediaEntity.setCreatedAt(new Date(0));

        return List.of(mediaEntity);
    }

    // Tests
    private void testReturnedApi(ApiV4 responseApi) {
        assertThat(responseApi).isNotNull();
        assertThat(responseApi.getName()).isEqualTo(API);
        assertThat(responseApi.getId()).isEqualTo(API);
        assertThat(responseApi.getLinks()).isNull();
        assertThat(responseApi.getProperties()).hasSize(1);
        assertThat(responseApi.getServices()).isNotNull();
        assertThat(responseApi.getResources()).hasSize(1);
        assertThat(responseApi.getResponseTemplates()).hasSize(1);

        assertThat(responseApi.getListeners()).hasSize(3);

        var httpListener = responseApi.getListeners().getFirst().getHttpListener();
        assertThat(httpListener).isNotNull();
        assertThat(httpListener.getPathMappings()).isNotNull();
        assertThat(httpListener.getPaths()).isNotNull();
        assertThat(httpListener.getPaths().getFirst().getHost()).isNotNull();

        var subscriptionListener = responseApi.getListeners().get(1).getSubscriptionListener();
        assertThat(subscriptionListener).isNotNull();
        assertThat(subscriptionListener.getEntrypoints()).isNotNull();
        var foundEntrypoint = subscriptionListener.getEntrypoints().getFirst();
        assertThat(foundEntrypoint).isNotNull();
        LinkedHashMap subscriptionConfig = (LinkedHashMap) foundEntrypoint.getConfiguration();
        assertThat(subscriptionConfig.get("nice")).isEqualTo("configuration");
        assertThat(foundEntrypoint.getType()).isEqualTo("Entrypoint type");
        assertThat(subscriptionListener.getType().toString()).isEqualTo("SUBSCRIPTION");

        var tcpListener = responseApi.getListeners().get(2).getTcpListener();
        assertThat(tcpListener).isNotNull();
        assertThat(tcpListener.getEntrypoints()).isNotNull();
        var tcpFoundEntrypoint = tcpListener.getEntrypoints().getFirst();
        assertThat(tcpFoundEntrypoint).isNotNull();
        LinkedHashMap tcpConfig = (LinkedHashMap) tcpFoundEntrypoint.getConfiguration();
        assertThat(tcpConfig.get("nice")).isEqualTo("configuration");
        assertThat(tcpFoundEntrypoint.getType()).isEqualTo("Entrypoint type");
        assertThat(tcpListener.getType().toString()).isEqualTo("TCP");

        assertThat(responseApi.getEndpointGroups()).hasSize(1);
        assertThat(responseApi.getEndpointGroups().getFirst()).isNotNull();
        assertThat(responseApi.getEndpointGroups().getFirst().getEndpoints()).hasSize(1);

        var endpoint = responseApi.getEndpointGroups().getFirst().getEndpoints().getFirst();
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.getType()).isEqualTo("http-get");

        LinkedHashMap endpointConfig = (LinkedHashMap) endpoint.getConfiguration();
        assertThat(endpointConfig.get("bootstrapServers")).isEqualTo("kafka:9092");
        assertThat(endpointConfig.get("topics")).isEqualTo(List.of("demo"));

        assertThat(responseApi.getFlows()).hasSize(1);

        var flow = responseApi.getFlows().getFirst();
        assertThat(flow).isNotNull();
        assertThat(flow.getName()).isEqualTo("flowName");
        assertThat(flow.getEnabled()).isTrue();
        assertThat(flow.getTags()).containsOnly("tag1", "tag2");
        assertThat(flow.getRequest()).hasSize(1);

        var step = flow.getRequest().getFirst();
        assertThat(step).isNotNull();
        assertThat(step.getEnabled()).isTrue();
        assertThat(step.getPolicy()).isEqualTo("my-policy");
        assertThat(step.getCondition()).isEqualTo("my-condition");

        assertThat(flow.getSelectors()).hasSize(3);

        var httpSelector = flow.getSelectors().getFirst().getHttpSelector();
        assertThat(httpSelector).isNotNull();
        assertThat(httpSelector.getPath()).isEqualTo("/test");
        assertThat(httpSelector.getPathOperator()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH);
        assertThat(httpSelector.getMethods())
            .containsOnly(
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.GET,
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.POST
            );

        var channelSelector = flow.getSelectors().get(1).getChannelSelector();
        assertThat(channelSelector.getChannel()).isEqualTo("my-channel");
        assertThat(channelSelector.getChannelOperator()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH);
        assertThat(channelSelector.getOperations())
            .containsOnly(io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.SUBSCRIBE);
        assertThat(channelSelector.getEntrypoints()).containsOnly("my-entrypoint");

        var conditionSelector = flow.getSelectors().get(2).getConditionSelector();
        assertThat(conditionSelector.getCondition()).isEqualTo("my-condition");
    }

    private void testReturnedNativeApi(ApiV4 responseApi) {
        assertThat(responseApi).isNotNull();
        assertThat(responseApi.getName()).isEqualTo(API);
        assertThat(responseApi.getId()).isEqualTo(API);
        assertThat(responseApi.getLinks()).isNull();
        assertThat(responseApi.getProperties()).hasSize(1);
        assertThat(responseApi.getResources()).hasSize(1);

        assertThat(responseApi.getListeners()).hasSize(1);

        var kafkaListener = responseApi.getListeners().getFirst().getKafkaListener();
        assertThat(kafkaListener).isNotNull();
        assertThat(kafkaListener.getHost()).isEqualTo("my.fake.host");

        assertThat(responseApi.getEndpointGroups()).hasSize(1);
        assertThat(responseApi.getEndpointGroups().getFirst()).isNotNull();
        assertThat(responseApi.getEndpointGroups().getFirst().getEndpoints()).hasSize(1);

        var endpoint = responseApi.getEndpointGroups().getFirst().getEndpoints().getFirst();
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.getType()).isEqualTo("kafka");

        var endpointConfig = (LinkedHashMap) endpoint.getConfiguration();
        assertThat(endpointConfig).containsOnly(Map.entry("bootstrapServers", "kafka:9092"));

        assertThat(responseApi.getFlows()).hasSize(1);

        var flow = responseApi.getFlows().getFirst();
        assertThat(flow).isNotNull();
        assertThat(flow.getName()).isEqualTo("flowName");
        assertThat(flow.getEnabled()).isTrue();
        assertThat(flow.getTags()).containsOnly("tag1", "tag2");
        assertThat(flow.getInteract()).hasSize(1);

        var step = flow.getInteract().getFirst();
        assertThat(step).isNotNull();
        assertThat(step.getEnabled()).isTrue();
        assertThat(step.getPolicy()).isEqualTo("my-policy");
        assertThat(step.getCondition()).isEqualTo("my-condition");

        assertThat(flow.getSelectors()).isNull();
    }

    private void testReturnedMembers(Set<Member> members) {
        var userMember = new Member().displayName("John Doe").id("memberId").roles(List.of(new Role().name("OWNER")));
        var poUserMember = new Member().displayName("Thomas Pesquet").id("poMemberId").roles(List.of(new Role().name("PRIMARY_OWNER")));

        assertThat(members).containsOnly(userMember, poUserMember);
    }

    private void testReturnedMetadata(Set<Metadata> metadata) {
        var firstMetadata = new Metadata()
            .key("my-metadata-1")
            .name("My first metadata")
            .format(io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.NUMERIC)
            .value("1")
            .defaultValue("5");

        var secondMetadata = new Metadata()
            .key("my-metadata-2")
            .name("My second metadata")
            .format(io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.STRING)
            .value("Very important data !!")
            .defaultValue("Important data");

        assertThat(metadata).containsOnly(firstMetadata, secondMetadata);
    }

    private void testReturnedPlans(Set<PlanV4> plans) {
        assertThat(plans).hasSize(1);

        var plan = plans.iterator().next();
        assertThat(plan.getApiId()).isEqualTo(API);
        assertThat(plan.getCharacteristics()).containsOnly("characteristic1", "characteristic2");
        assertThat(plan.getCommentMessage()).isEqualTo("commentMessage");
        assertThat(plan.getCommentRequired()).isTrue();
        assertThat(plan.getCrossId()).isEqualTo("crossId");
        assertThat(plan.getCreatedAt()).isEqualTo(OffsetDateTime.of(1970, 1, 1, 1, 23, 45, 0, ZoneOffset.UTC));
        assertThat(plan.getClosedAt()).isNull();
        assertThat(plan.getDescription()).isEqualTo("description");
        assertThat(plan.getExcludedGroups()).containsOnly("excludedGroup");
        assertThat(plan.getGeneralConditions()).isEqualTo("generalConditions");
        assertThat(plan.getId()).isEqualTo("planId");
        assertThat(plan.getName()).isEqualTo("planName");
        assertThat(plan.getOrder()).isEqualTo(1);
        assertThat(plan.getPublishedAt()).isEqualTo(OffsetDateTime.of(1970, 1, 1, 1, 23, 46, 0, ZoneOffset.UTC));
        assertThat(plan.getStatus()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED);
        assertThat(plan.getSelectionRule()).isNull();
        assertThat(plan.getTags()).containsOnly("tag1", "tag2");
        assertThat(plan.getType()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PlanType.API);
        assertThat(plan.getUpdatedAt()).isEqualTo(OffsetDateTime.of(1970, 1, 1, 1, 23, 47, 0, ZoneOffset.UTC));
        assertThat(plan.getValidation()).isEqualTo(PlanValidation.AUTO);

        assertThat(plan.getFlows()).hasSize(1);

        var flowV4 = plan.getFlows().getFirst();
        assertThat(flowV4).isNotNull();
        assertThat(flowV4.getEnabled()).isTrue();
        assertThat(flowV4.getName()).isEqualTo("planFlowName");
        assertThat(flowV4.getPublish()).isNull();

        assertThat(flowV4.getRequest()).hasSize(1);

        var step = flowV4.getRequest().getFirst();
        assertThat(step.getEnabled()).isTrue();
        assertThat(step.getConfiguration()).isEqualTo(Map.of("nice", "configuration"));
        assertThat(step.getDescription()).isEqualTo("stepDescription");
        assertThat(step.getName()).isEqualTo("stepName");
        assertThat(step.getCondition()).isEqualTo("stepCondition");
        assertThat(step.getPolicy()).isEqualTo("stepPolicy");
        assertThat(step.getMessageCondition()).isEqualTo("stepMessageCondition");

        assertThat(flowV4.getResponse()).isNull();
        assertThat(flowV4.getSubscribe()).isNull();
        assertThat(flowV4.getTags()).isNull();

        assertThat(flowV4.getSelectors()).hasSize(1);

        var httpSelector = flowV4.getSelectors().getFirst().getHttpSelector();
        assertThat(httpSelector).isNotNull();
        assertThat(httpSelector.getPath()).isEqualTo("/test");
        assertThat(httpSelector.getPathOperator()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH);
        assertThat(httpSelector.getMethods())
            .containsOnly(
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.GET,
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.POST
            );

        assertThat(plan.getSecurity()).isNotNull();
        var planSecurity = plan.getSecurity();
        assertThat(planSecurity.getType()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType.KEY_LESS);
        assertThat(planSecurity.getConfiguration()).isEqualTo(new LinkedHashMap<>());
    }

    private void testReturnedPages(Set<Page> pages) {
        assertThat(pages).hasSize(1);

        var page = pages.iterator().next();
        assertThat(page.getAccessControls()).hasSize(1);

        var accessControl = page.getAccessControls().getFirst();
        assertThat(accessControl).isNotNull();
        assertThat(accessControl.getReferenceId()).isEqualTo("role-id");
        assertThat(accessControl.getReferenceType()).isEqualTo("ROLE");

        assertThat(page.getAttachedMedia()).hasSize(1);

        var pageMedia = page.getAttachedMedia().getFirst();
        assertThat(pageMedia).isNotNull();
        assertThat(pageMedia.getHash()).isEqualTo("media-hash");
        assertThat(pageMedia.getName()).isEqualTo("media-name");
        assertThat(pageMedia.getAttachedAt()).isEqualTo(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        assertThat(page.getConfiguration()).hasSize(1).containsEntry("page-config-key", "page-config-value");
        assertThat(page.getContent()).isEqualTo("#content");

        assertThat(page.getContentRevision()).isNotNull();
        var contentRevision = page.getContentRevision();
        assertThat(contentRevision.getId()).isEqualTo("page-revision-id");
        assertThat(contentRevision.getRevision()).isEqualTo(1);

        assertThat(page.getContentType()).isEqualTo("text/markdown");
        assertThat(page.getCrossId()).isEqualTo("crossId");

        assertThat(page.getExcludedAccessControls()).isFalse();

        assertThat(page.getHomepage()).isFalse();
        assertThat(page.getId()).isEqualTo("page-id");
        assertThat(page.getLastContributor()).isEqualTo("last-contributor-id");
        assertThat(page.getUpdatedAt()).isEqualTo(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(page.getMetadata()).hasSize(1).containsEntry("page-metadata-key", "page-metadata-value");
        assertThat(page.getName()).isEqualTo("page-name");
        assertThat(page.getOrder()).isEqualTo(1);
        assertThat(page.getParentId()).isEqualTo("parent-id");
        assertThat(page.getParentPath()).isEqualTo("parent-path");
        assertThat(page.getPublished()).isTrue();

        assertThat(page.getSource()).isNotNull();
        var source = page.getSource();
        assertThat(source.getType()).isEqualTo("GITHUB");
        assertThat(source.getConfiguration()).isEqualTo(new LinkedHashMap<>());

        assertThat(page.getType()).isEqualTo(PageType.MARKDOWN);

        assertThat(page.getTranslations()).isEmpty();
        assertThat(page.getVisibility()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.Visibility.PUBLIC);
    }

    private Condition<Media> testReturnedMedia() {
        var created = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new Condition<>(
            media ->
                media != null &&
                "media-id".equals(media.getId()) &&
                "media-hash".equals(media.getHash()) &&
                Objects.equals(1_000L, media.getSize()) &&
                "media-file-name".equals(media.getFileName()) &&
                "media-type".equals(media.getType()) &&
                "media-sub-type".equals(media.getSubType()) &&
                Arrays.equals("media-data".getBytes(UTF_8), media.getData()) &&
                created.equals(media.getCreatedAt()),
            "media-id"
        );
    }
}
