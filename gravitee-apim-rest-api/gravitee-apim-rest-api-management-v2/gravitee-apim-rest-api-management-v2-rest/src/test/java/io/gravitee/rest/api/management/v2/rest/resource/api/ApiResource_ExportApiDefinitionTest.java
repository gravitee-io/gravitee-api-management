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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.services.Services;
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
import io.gravitee.rest.api.model.AccessControlEntity;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource_ExportApiDefinitionTest extends ApiResourceTest {

    private static final Set<String> EXCLUDE_ADDITIONAL_DATA = Set.of();

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
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_export_v2_apis() {
        doThrow(new ApiDefinitionVersionNotSupportedException("2.0.0"))
            .when(apiImportExportService)
            .exportApi(GraviteeContext.getExecutionContext(), API, USER_NAME, EXCLUDE_ADDITIONAL_DATA);
        Response response = rootTarget().request().get();
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_export() throws JsonProcessingException {
        when(apiImportExportService.exportApi(GraviteeContext.getExecutionContext(), API, USER_NAME, EXCLUDE_ADDITIONAL_DATA))
            .thenReturn(this.fakeExportApiEntity());

        Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        final ExportApiV4 export = response.readEntity(ExportApiV4.class);
        assertNotNull(export.getMembers());
        assertNotNull(export.getMetadata());
        assertNotNull(export.getPlans());
        assertNotNull(export.getPages());
        assertNotNull(export.getApi());

        final ApiV4 api = export.getApi();
        testReturnedApi(api);

        final Set<Member> members = export.getMembers();
        testReturnedMembers(members);

        final Set<Metadata> metadata = export.getMetadata();
        testReturnedMetadata(metadata);

        final Set<PlanV4> plans = export.getPlans();
        testReturnedPlans(plans);

        final Set<Page> pages = export.getPages();
        final List<Media> mediaList = export.getApiMedia();
        testReturnedPages(pages);
        testReturnedMedia(mediaList);
    }

    // Fakers
    private io.gravitee.rest.api.model.api.ApiEntity fakeApiEntityV2() {
        var apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        apiEntity.setId(API);
        apiEntity.setName(API);

        var proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("host.io", "/test")));
        apiEntity.setProxy(proxy);
        var properties = new Properties();
        properties.setProperties(List.of(new io.gravitee.definition.model.Property("key", "value")));
        apiEntity.setProperties(properties);
        apiEntity.setServices(new Services());
        apiEntity.setResources(List.of(new io.gravitee.definition.model.plugins.resources.Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());

        return apiEntity;
    }

    private ExportApiEntity fakeExportApiEntity() {
        var exportApiEntity = new ExportApiEntity();
        exportApiEntity.setApiEntity(fakeApiEntityV4());
        exportApiEntity.setApiMedia(fakeApiMedia());
        exportApiEntity.setMembers(fakeApiMembers());
        exportApiEntity.setMetadata(fakeApiMetadata());
        exportApiEntity.setPages(fakeApiPages());
        exportApiEntity.setPlans(fakeApiPlans());

        return exportApiEntity;
    }

    private ApiEntity fakeApiEntityV4() {
        var apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setId(API);
        apiEntity.setName(API);
        apiEntity.setApiVersion("v1.0");
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("my.fake.host", "/test")));
        httpListener.setPathMappings(Set.of("/test"));

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

        apiEntity.setListeners(List.of(httpListener, subscriptionListener, tcpListener));
        apiEntity.setProperties(List.of(new Property()));
        apiEntity.setServices(new ApiServices());
        apiEntity.setResources(List.of(new Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setAnalytics(new Analytics());

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setType("http-get");
        Endpoint endpoint = new Endpoint();
        endpoint.setType("http-get");
        endpoint.setConfiguration(
            "{\n" +
            "                        \"bootstrapServers\": \"kafka:9092\",\n" +
            "                        \"topics\": [\n" +
            "                            \"demo\"\n" +
            "                        ],\n" +
            "                        \"producer\": {\n" +
            "                            \"enabled\": false\n" +
            "                        },\n" +
            "                        \"consumer\": {\n" +
            "                            \"encodeMessageId\": true,\n" +
            "                            \"enabled\": true,\n" +
            "                            \"autoOffsetReset\": \"earliest\"\n" +
            "                        }\n" +
            "                    }"
        );
        endpointGroup.setEndpoints(List.of(endpoint));
        apiEntity.setEndpointGroups(List.of(endpointGroup));

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
        apiEntity.setFlows(List.of(flow));

        return apiEntity;
    }

    private Set<MemberEntity> fakeApiMembers() {
        var role = new RoleEntity();
        role.setName("OWNER");
        var userMember = new MemberEntity();
        userMember.setId("memberId");
        userMember.setDisplayName("John Doe");
        userMember.setRoles(List.of(role));
        userMember.setType(MembershipMemberType.USER);

        var poRole = new RoleEntity();
        poRole.setName("PRIMARY_OWNER");
        var poUserMember = new MemberEntity();
        poUserMember.setId("poMemberId");
        poUserMember.setDisplayName("Thomas Pesquet");
        poUserMember.setRoles(List.of(poRole));
        poUserMember.setType(MembershipMemberType.USER);

        return Set.of(userMember, poUserMember);
    }

    private Set<ApiMetadataEntity> fakeApiMetadata() {
        ApiMetadataEntity firstMetadata = new ApiMetadataEntity();
        firstMetadata.setApiId(API);
        firstMetadata.setKey("my-metadata-1");
        firstMetadata.setName("My first metadata");
        firstMetadata.setFormat(MetadataFormat.NUMERIC);
        firstMetadata.setValue("1");
        firstMetadata.setDefaultValue("5");

        ApiMetadataEntity secondMetadata = new ApiMetadataEntity();
        secondMetadata.setApiId(API);
        secondMetadata.setKey("my-metadata-2");
        secondMetadata.setName("My second metadata");
        secondMetadata.setFormat(MetadataFormat.STRING);
        secondMetadata.setValue("Very important data !!");
        secondMetadata.setDefaultValue("Important data");

        return Set.of(firstMetadata, secondMetadata);
    }

    private Set<PlanEntity> fakeApiPlans() {
        PlanEntity planEntity = new PlanEntity();
        planEntity.setApiId(API);
        planEntity.setCharacteristics(List.of("characteristic1", "characteristic2"));
        planEntity.setCommentMessage("commentMessage");
        planEntity.setCommentRequired(true);
        planEntity.setCrossId("crossId");
        planEntity.setCreatedAt(new Date(5025000));
        planEntity.setClosedAt(null);
        planEntity.setDescription("description");
        planEntity.setExcludedGroups(List.of("excludedGroup"));
        planEntity.setGeneralConditions("generalConditions");
        planEntity.setId("planId");
        planEntity.setName("planName");
        planEntity.setNeedRedeployAt(null);
        planEntity.setOrder(1);
        planEntity.setPublishedAt(new Date(5026000));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSelectionRule(null);
        planEntity.setTags(Set.of("tag1", "tag2"));
        planEntity.setType(PlanType.API);
        planEntity.setUpdatedAt(new Date(5027000));
        planEntity.setValidation(PlanValidationType.AUTO);

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

        planEntity.setFlows(List.of(planFlow));

        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("key-less");
        planSecurity.setConfiguration("{}");
        planEntity.setSecurity(planSecurity);

        return Set.of(planEntity);
    }

    private List<PageEntity> fakeApiPages() {
        PageEntity pageEntity = new PageEntity();
        AccessControlEntity accessControlEntity = new AccessControlEntity();
        accessControlEntity.setReferenceId("role-id");
        accessControlEntity.setReferenceType("ROLE");
        pageEntity.setAccessControls(Set.of(accessControlEntity));
        PageMediaEntity pageMediaEntity = new PageMediaEntity();
        pageMediaEntity.setMediaHash("media-hash");
        pageMediaEntity.setMediaName("media-name");
        pageMediaEntity.setAttachedAt(new Date(0));
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
        pageEntity.setLastModificationDate(new Date(0));
        pageEntity.setMessages(List.of("message1", "message2"));
        pageEntity.setMetadata(Map.of("page-metadata-key", "page-metadata-value"));
        pageEntity.setName("page-name");
        pageEntity.setOrder(1);
        pageEntity.setParentId("parent-id");
        pageEntity.setParentPath("parent-path");
        pageEntity.setPublished(true);
        pageEntity.setReferenceId("reference-id");
        pageEntity.setReferenceType("reference-type");
        PageSourceEntity pageSourceEntity = new PageSourceEntity();
        pageSourceEntity.setType("GITHUB");
        pageSourceEntity.setConfiguration(JsonNodeFactory.instance.objectNode());
        pageEntity.setSource(pageSourceEntity);
        pageEntity.setType("MARKDOWN");
        pageEntity.setTranslations(Collections.emptyList());
        pageEntity.setVisibility(Visibility.PUBLIC);

        return List.of(pageEntity);
    }

    private List<MediaEntity> fakeApiMedia() {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId("media-id");
        mediaEntity.setHash("media-hash");
        mediaEntity.setSize(1_000);
        mediaEntity.setFileName("media-file-name");
        mediaEntity.setType("media-type");
        mediaEntity.setSubType("media-sub-type");
        mediaEntity.setData("media-data".getBytes(StandardCharsets.UTF_8));
        mediaEntity.setUploadDate(new Date(0));

        return List.of(mediaEntity);
    }

    // Tests
    private void testReturnedApi(ApiV4 responseApi) throws JsonProcessingException {
        assertNotNull(responseApi);
        assertEquals(API, responseApi.getName());
        assertEquals(API, responseApi.getId());
        assertNull(responseApi.getLinks());
        assertNotNull(responseApi.getProperties());
        assertEquals(1, responseApi.getProperties().size());
        assertNotNull(responseApi.getServices());
        assertNotNull(responseApi.getResources());
        assertEquals(1, responseApi.getResources().size());
        assertNotNull(responseApi.getResponseTemplates());
        assertEquals(1, responseApi.getResponseTemplates().size());

        assertNotNull(responseApi.getListeners());
        assertEquals(3, responseApi.getListeners().size());

        io.gravitee.rest.api.management.v2.rest.model.HttpListener httpListener = responseApi.getListeners().get(0).getHttpListener();
        assertNotNull(httpListener);
        assertNotNull(httpListener.getPathMappings());
        assertNotNull(httpListener.getPaths());
        assertNotNull(httpListener.getPaths().get(0).getHost());

        io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener subscriptionListener = responseApi
            .getListeners()
            .get(1)
            .getSubscriptionListener();
        assertNotNull(subscriptionListener);
        assertNotNull(subscriptionListener.getEntrypoints());
        var foundEntrypoint = subscriptionListener.getEntrypoints().get(0);
        assertNotNull(foundEntrypoint);
        LinkedHashMap subscriptionConfig = (LinkedHashMap) foundEntrypoint.getConfiguration();
        assertEquals("configuration", subscriptionConfig.get("nice"));
        assertEquals("Entrypoint type", foundEntrypoint.getType());
        assertEquals("SUBSCRIPTION", subscriptionListener.getType().toString());

        io.gravitee.rest.api.management.v2.rest.model.TcpListener tcpListener = responseApi.getListeners().get(2).getTcpListener();
        assertNotNull(tcpListener);
        assertNotNull(tcpListener.getEntrypoints());
        var tcpFoundEntrypoint = tcpListener.getEntrypoints().get(0);
        assertNotNull(tcpFoundEntrypoint);
        LinkedHashMap tcpConfig = (LinkedHashMap) tcpFoundEntrypoint.getConfiguration();
        assertEquals("configuration", tcpConfig.get("nice"));
        assertEquals("Entrypoint type", tcpFoundEntrypoint.getType());
        assertEquals("TCP", tcpListener.getType().toString());

        assertNotNull(responseApi.getEndpointGroups());
        assertEquals(1, responseApi.getEndpointGroups().size());
        assertNotNull(responseApi.getEndpointGroups().get(0));
        assertNotNull(responseApi.getEndpointGroups().get(0).getEndpoints());
        assertEquals(1, responseApi.getEndpointGroups().get(0).getEndpoints().size());

        var endpoint = responseApi.getEndpointGroups().get(0).getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("http-get", endpoint.getType());

        LinkedHashMap endpointConfig = (LinkedHashMap) endpoint.getConfiguration();
        assertEquals("kafka:9092", endpointConfig.get("bootstrapServers"));
        assertEquals(List.of("demo"), endpointConfig.get("topics"));

        assertNotNull(responseApi.getFlows());
        assertEquals(1, responseApi.getFlows().size());

        var flow = responseApi.getFlows().get(0);
        assertNotNull(flow);
        assertEquals("flowName", flow.getName());
        assertEquals(Boolean.TRUE, flow.getEnabled());
        assertNotNull(flow.getTags());
        assertEquals(2, flow.getTags().size());
        assertEquals(Set.of("tag1", "tag2"), flow.getTags());
        assertNotNull(flow.getRequest());
        assertEquals(1, flow.getRequest().size());

        var step = flow.getRequest().get(0);
        assertNotNull(step);
        assertEquals(Boolean.TRUE, step.getEnabled());
        assertEquals("my-policy", step.getPolicy());
        assertEquals("my-condition", step.getCondition());

        assertNotNull(flow.getSelectors());
        assertEquals(3, flow.getSelectors().size());

        var httpSelector = flow.getSelectors().get(0).getHttpSelector();
        assertNotNull(httpSelector);
        assertEquals("/test", httpSelector.getPath());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH, httpSelector.getPathOperator());
        assertEquals(2, httpSelector.getMethods().size());
        assertEquals(
            Set.of(
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.GET,
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.POST
            ),
            httpSelector.getMethods()
        );

        var channelSelector = flow.getSelectors().get(1).getChannelSelector();
        assertEquals("my-channel", channelSelector.getChannel());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH, channelSelector.getChannelOperator());
        assertEquals(1, channelSelector.getOperations().size());
        assertEquals(
            Set.of(io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.SUBSCRIBE),
            channelSelector.getOperations()
        );
        assertEquals(1, channelSelector.getEntrypoints().size());
        assertEquals(Set.of("my-entrypoint"), channelSelector.getEntrypoints());

        var conditionSelector = flow.getSelectors().get(2).getConditionSelector();
        assertEquals("my-condition", conditionSelector.getCondition());
    }

    private void testReturnedMembers(Set<Member> members) {
        assertEquals(2, members.size());

        var userMember = new Member().displayName("John Doe").id("memberId").roles(List.of(new Role().name("OWNER")));
        var poUserMember = new Member().displayName("Thomas Pesquet").id("poMemberId").roles(List.of(new Role().name("PRIMARY_OWNER")));

        assertEquals(Set.of(userMember, poUserMember), members);
    }

    private void testReturnedMetadata(Set<Metadata> metadata) {
        assertEquals(2, metadata.size());

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

        assertEquals(Set.of(firstMetadata, secondMetadata), metadata);
    }

    private void testReturnedPlans(Set<PlanV4> plans) {
        assertEquals(1, plans.size());

        var plan = plans.iterator().next();
        assertEquals(API, plan.getApiId());
        assertEquals(List.of("characteristic1", "characteristic2"), plan.getCharacteristics());
        assertEquals("commentMessage", plan.getCommentMessage());
        assertEquals(true, plan.getCommentRequired());
        assertEquals("crossId", plan.getCrossId());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 1, 23, 45, 0, ZoneOffset.UTC), plan.getCreatedAt());
        assertNull(plan.getClosedAt());
        assertEquals("description", plan.getDescription());
        assertEquals(List.of("excludedGroup"), plan.getExcludedGroups());
        assertEquals("generalConditions", plan.getGeneralConditions());
        assertEquals("planId", plan.getId());
        assertEquals("planName", plan.getName());
        assertEquals(1, plan.getOrder().intValue());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 1, 23, 46, 0, ZoneOffset.UTC), plan.getPublishedAt());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanStatus.PUBLISHED, plan.getStatus());
        assertNull(plan.getSelectionRule());
        assertEquals(List.of("tag1", "tag2"), plan.getTags().stream().sorted().collect(Collectors.toList()));
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanType.API, plan.getType());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 1, 23, 47, 0, ZoneOffset.UTC), plan.getUpdatedAt());
        assertEquals(PlanValidation.AUTO, plan.getValidation());

        assertNotNull(plan.getFlows());
        assertEquals(1, plan.getFlows().size());

        var flowV4 = plan.getFlows().get(0);
        assertNotNull(flowV4);
        assertEquals(true, flowV4.getEnabled());
        assertEquals("planFlowName", flowV4.getName());
        assertNull(flowV4.getPublish());

        assertNotNull(flowV4.getRequest());
        assertEquals(1, flowV4.getRequest().size());

        var step = flowV4.getRequest().get(0);
        assertEquals(true, step.getEnabled());
        assertEquals(new LinkedHashMap<>(Map.of("nice", "configuration")), step.getConfiguration());
        assertEquals("stepDescription", step.getDescription());
        assertEquals("stepName", step.getName());
        assertEquals("stepCondition", step.getCondition());
        assertEquals("stepPolicy", step.getPolicy());
        assertEquals("stepMessageCondition", step.getMessageCondition());

        assertNull(flowV4.getResponse());
        assertNull(flowV4.getSubscribe());
        assertNull(flowV4.getTags());

        assertNotNull(flowV4.getSelectors());
        assertEquals(1, flowV4.getSelectors().size());

        var httpSelector = flowV4.getSelectors().get(0).getHttpSelector();
        assertNotNull(httpSelector);
        assertEquals("/test", httpSelector.getPath());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.Operator.STARTS_WITH, httpSelector.getPathOperator());
        assertEquals(2, httpSelector.getMethods().size());
        assertEquals(
            Set.of(
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.GET,
                io.gravitee.rest.api.management.v2.rest.model.HttpMethod.POST
            ),
            httpSelector.getMethods()
        );

        assertNotNull(plan.getSecurity());
        var planSecurity = plan.getSecurity();
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType.KEY_LESS, planSecurity.getType());
        assertEquals(new LinkedHashMap<>(), planSecurity.getConfiguration());
    }

    private void testReturnedPages(Set<Page> pages) {
        assertEquals(1, pages.size());

        var page = pages.iterator().next();
        assertNotNull(page.getAccessControls());
        assertEquals(1, page.getAccessControls().size());

        var accessControl = page.getAccessControls().get(0);
        assertNotNull(accessControl);
        assertEquals("role-id", accessControl.getReferenceId());
        assertEquals("ROLE", accessControl.getReferenceType());

        assertNotNull(page.getAttachedMedia());
        assertEquals(1, page.getAttachedMedia().size());

        var pageMedia = page.getAttachedMedia().get(0);
        assertNotNull(pageMedia);
        assertEquals("media-hash", pageMedia.getHash());
        assertEquals("media-name", pageMedia.getName());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), pageMedia.getAttachedAt());

        assertEquals(Map.of("page-config-key", "page-config-value"), page.getConfiguration());
        assertEquals("#content", page.getContent());

        assertNotNull(page.getContentRevision());
        var contentRevision = page.getContentRevision();
        assertEquals("page-revision-id", contentRevision.getId());
        assertEquals(1, contentRevision.getRevision().intValue());

        assertEquals("text/markdown", page.getContentType());
        assertEquals("crossId", page.getCrossId());

        assertEquals(false, page.getExcludedAccessControls());
        assertNotNull(page.getAccessControls());
        assertEquals(1, page.getAccessControls().size());
        assertEquals("role-id", page.getAccessControls().get(0).getReferenceId());
        assertEquals("ROLE", page.getAccessControls().get(0).getReferenceType());

        assertEquals(false, page.getHomepage());
        assertEquals("page-id", page.getId());
        assertEquals("last-contributor-id", page.getLastContributor());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), page.getUpdatedAt());
        assertEquals(Map.of("page-metadata-key", "page-metadata-value"), page.getMetadata());
        assertEquals("page-name", page.getName());
        assertEquals(1, page.getOrder().intValue());
        assertEquals("parent-id", page.getParentId());
        assertEquals("parent-path", page.getParentPath());
        assertEquals(true, page.getPublished());

        assertNotNull(page.getSource());
        var source = page.getSource();
        assertEquals("GITHUB", source.getType());
        assertEquals(new LinkedHashMap<>(), source.getConfiguration());

        assertEquals(PageType.MARKDOWN, page.getType());

        assertNotNull(page.getTranslations());
        assertEquals(0, page.getTranslations().size());

        assertEquals(io.gravitee.rest.api.management.v2.rest.model.Visibility.PUBLIC, page.getVisibility());
    }

    private void testReturnedMedia(List<Media> mediaList) {
        assertEquals(1, mediaList.size());

        var media = mediaList.get(0);
        assertNotNull(media);

        assertEquals("media-id", media.getId());
        assertEquals("media-hash", media.getHash());
        assertEquals(1_000, media.getSize().longValue());
        assertEquals("media-file-name", media.getFileName());
        assertEquals("media-type", media.getType());
        assertEquals("media-sub-type", media.getSubType());
        assertArrayEquals("media-data".getBytes(StandardCharsets.UTF_8), media.getData());
        assertEquals(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), media.getCreatedAt());
    }
}
