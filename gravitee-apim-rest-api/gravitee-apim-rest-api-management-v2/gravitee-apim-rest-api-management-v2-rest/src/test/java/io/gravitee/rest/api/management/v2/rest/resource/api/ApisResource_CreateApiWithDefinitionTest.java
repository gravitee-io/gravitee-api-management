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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static javax.ws.rs.client.Entity.json;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.AccessControl;
import io.gravitee.rest.api.management.v2.rest.model.Analytics;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ChannelSelector;
import io.gravitee.rest.api.management.v2.rest.model.ConditionSelector;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.Dlq;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpListener;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.HttpSelector;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.ListenerType;
import io.gravitee.rest.api.management.v2.rest.model.Media;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.Metadata;
import io.gravitee.rest.api.management.v2.rest.model.MetadataFormat;
import io.gravitee.rest.api.management.v2.rest.model.Operator;
import io.gravitee.rest.api.management.v2.rest.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.PageMedia;
import io.gravitee.rest.api.management.v2.rest.model.PageSource;
import io.gravitee.rest.api.management.v2.rest.model.PageType;
import io.gravitee.rest.api.management.v2.rest.model.PathV4;
import io.gravitee.rest.api.management.v2.rest.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.PlanType;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.model.Property;
import io.gravitee.rest.api.management.v2.rest.model.QoS;
import io.gravitee.rest.api.management.v2.rest.model.Resource;
import io.gravitee.rest.api.management.v2.rest.model.Revision;
import io.gravitee.rest.api.management.v2.rest.model.Role;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener;
import io.gravitee.rest.api.management.v2.rest.model.TcpListener;
import io.gravitee.rest.api.management.v2.rest.model.Visibility;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ApisResource_CreateApiWithDefinitionTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";
    private static final String ENVIRONMENT_ID = "my-env";

    private final ObjectMapper mapper = new GraviteeMapper(false);

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/apis/_import/definition";
    }

    @Before
    public void init() throws TechnicalException {
        reset(apiServiceV4);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT_ID);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID);
    }

    @Test
    public void should_not_import_when_no_definition_permission() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            );
        Response response = rootTarget().request().post(null);
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_import_v2_apis() {
        Response response = rootTarget().request().post(json(fakeExportApiV2()));
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_import() throws JsonProcessingException {
        doReturn(fakeApiEntityV4()).when(apiImportExportService).createFromExportedApi(any(), any(), anyString());

        Response response = rootTarget().request().post(json(fakeExportApiV4()));
        assertEquals(CREATED_201, response.getStatus());

        ArgumentCaptor<ExportApiEntity> captor = ArgumentCaptor.forClass(ExportApiEntity.class);
        verify(apiImportExportService).createFromExportedApi(eq(GraviteeContext.getExecutionContext()), captor.capture(), eq(USER_NAME));
        testConvertedExportApi(captor.getValue());

        final Api createdApi = response.readEntity(Api.class);
        assertEquals(ApiV4.class, createdApi.getActualInstance().getClass());
        ApiV4 apiV4 = createdApi.getApiV4();
        assertNotNull(apiV4);
        testReturnedApi(apiV4);
    }

    // Fakers
    private ExportApiV4 fakeExportApiV2() {
        var exportApi = new ExportApiV4();
        var apiV2 = new ApiV2();
        apiV2.setDefinitionVersion(DefinitionVersion.V2);
        apiV2.setId(API_ID);
        apiV2.setName(API_ID);

        exportApi.setApi(new Api(apiV2));
        return exportApi;
    }

    private ExportApiV4 fakeExportApiV4() {
        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(fakeApi());
        exportApiV4.setApiMedia(fakeApiMedia());
        exportApiV4.setMembers(fakeApiMembers());
        exportApiV4.setMetadata(fakeApiMetadata());
        exportApiV4.setPages(fakeApiPages());
        exportApiV4.setPlans(fakeApiPlans());

        return exportApiV4;
    }

    private Api fakeApi() {
        var apiV4 = new ApiV4();
        apiV4.setDefinitionVersion(DefinitionVersion.V4);
        apiV4.setId(API_ID);
        apiV4.setName(API_ID);
        apiV4.setApiVersion("v1.0");
        var httpListener = new HttpListener();
        httpListener.setPaths(List.of(new PathV4().host("my.fake.host").path("/test")));
        httpListener.setPathMappings(List.of("/test"));

        var subscriptionListener = new SubscriptionListener();
        var entrypoint = new Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(QoS.AT_LEAST_ONCE);
        entrypoint.setDlq(new Dlq().endpoint("my-endpoint"));
        entrypoint.setConfiguration(new LinkedHashMap<>(Map.of("nice", "configuration")));
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        subscriptionListener.setType(ListenerType.SUBSCRIPTION);

        var tcpListener = new TcpListener();
        tcpListener.setType(ListenerType.TCP);
        tcpListener.setEntrypoints(List.of(entrypoint));

        apiV4.setListeners(List.of(new Listener(httpListener), new Listener(subscriptionListener), new Listener(tcpListener)));
        apiV4.setProperties(List.of(new Property()));
        apiV4.setServices(new ApiServices());
        apiV4.setResources(List.of(new Resource()));
        apiV4.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiV4.setUpdatedAt(OffsetDateTime.parse("1970-01-01T00:00:00Z"));
        apiV4.setAnalytics(new Analytics());

        var endpointGroup = new EndpointGroupV4();
        endpointGroup.setType("http-get");
        var endpoint = new EndpointV4();
        endpoint.setType("http-get");

        var _configuration = JsonNodeFactory.instance.objectNode();
        _configuration.put("bootstrapServers", "kafka:9092");
        _configuration.putArray("topics").add("demo");
        _configuration.putObject("producer").put("enabled", false);
        _configuration.putObject("consumer").put("encodeMessageId", true).put("enabled", true).put("autoOffsetReset", "earliest");

        endpoint.setConfiguration(new ObjectMapper().convertValue(_configuration, new TypeReference<LinkedHashMap<String, Object>>() {}));
        endpointGroup.setEndpoints(List.of(endpoint));
        apiV4.setEndpointGroups(List.of(endpointGroup));

        var flow = new FlowV4();
        flow.setName("flowName");
        flow.setEnabled(true);

        var step = new StepV4();
        step.setEnabled(true);
        step.setPolicy("my-policy");
        step.setCondition("my-condition");
        flow.setRequest(List.of(step));
        flow.setTags(Set.of("tag1", "tag2"));

        var httpSelector = new HttpSelector();
        httpSelector.setPath("/test");
        httpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        httpSelector.setPathOperator(Operator.STARTS_WITH);

        var channelSelector = new ChannelSelector();
        channelSelector.setChannel("my-channel");
        channelSelector.setChannelOperator(Operator.STARTS_WITH);
        channelSelector.setOperations(Set.of(ChannelSelector.OperationsEnum.SUBSCRIBE));
        channelSelector.setEntrypoints(Set.of("my-entrypoint"));

        var conditionSelector = new ConditionSelector();
        conditionSelector.setCondition("my-condition");

        flow.setSelectors(List.of(new Selector(httpSelector), new Selector(channelSelector), new Selector(conditionSelector)));
        apiV4.setFlows(List.of(flow));

        return new Api(apiV4);
    }

    private Set<Member> fakeApiMembers() {
        var role = new Role();
        role.setName("OWNER");
        var user = new Member();
        user.setId("memberId");
        user.setDisplayName("John Doe");
        user.setRoles(List.of(role));

        var poRole = new Role();
        poRole.setName("PRIMARY_OWNER");
        var poUser = new Member();
        poUser.setId("poMemberId");
        poUser.setDisplayName("Thomas Pesquet");
        poUser.setRoles(List.of(poRole));

        return Set.of(user, poUser);
    }

    private Set<Metadata> fakeApiMetadata() {
        var firstMetadata = new Metadata();
        firstMetadata.setKey("my-metadata-1");
        firstMetadata.setName("My first metadata");
        firstMetadata.setFormat(MetadataFormat.NUMERIC);
        firstMetadata.setValue("1");
        firstMetadata.setDefaultValue("5");

        var secondMetadata = new Metadata();
        secondMetadata.setKey("my-metadata-2");
        secondMetadata.setName("My second metadata");
        secondMetadata.setFormat(MetadataFormat.STRING);
        secondMetadata.setValue("Very important data !!");
        secondMetadata.setDefaultValue("Important data");

        return Set.of(firstMetadata, secondMetadata);
    }

    private Set<PlanV4> fakeApiPlans() {
        var plan = new PlanV4();
        plan.setApiId(API_ID);
        plan.setCharacteristics(List.of("characteristic1", "characteristic2"));
        plan.setCommentMessage("commentMessage");
        plan.setCommentRequired(true);
        plan.setCrossId("crossId");
        plan.setCreatedAt(OffsetDateTime.parse("1970-01-01T01:23:45Z")); //new Date(5025000)
        plan.setClosedAt(null);
        plan.setDescription("description");
        plan.setExcludedGroups(List.of("excludedGroup"));
        plan.setGeneralConditions("generalConditions");
        plan.setId("planId");
        plan.setName("planName");
        plan.setOrder(1);
        plan.setPublishedAt(OffsetDateTime.parse("1970-01-01T01:23:46Z")); //new Date(5026000)
        plan.setStatus(PlanStatus.PUBLISHED);
        plan.setSelectionRule(null);
        plan.setTags(List.of("tag1", "tag2"));
        plan.setType(PlanType.API);
        plan.setUpdatedAt(OffsetDateTime.parse("1970-01-01T01:23:47Z")); //new Date(5027000)
        plan.setValidation(PlanValidation.AUTO);

        var step = new StepV4();
        step.setName("stepName");
        step.setDescription("stepDescription");
        step.setConfiguration("stepConfiguration");
        step.setCondition("stepCondition");
        step.setPolicy("stepPolicy");
        step.setEnabled(true);
        step.setMessageCondition("stepMessageCondition");

        var planFlow = new FlowV4();
        planFlow.setEnabled(true);
        planFlow.setName("planFlowName");
        planFlow.setPublish(null);
        planFlow.setRequest(List.of(step));
        planFlow.setResponse(null);
        planFlow.setSubscribe(null);

        var httpSelector = new HttpSelector();
        httpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        httpSelector.setPath("/test");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        planFlow.setSelectors(List.of(new Selector(httpSelector)));
        planFlow.setTags(null);

        plan.setFlows(List.of(planFlow));

        var planSecurity = new PlanSecurity();
        planSecurity.setType(PlanSecurityType.KEY_LESS);
        planSecurity.setConfiguration("{}");
        plan.setSecurity(planSecurity);

        return Set.of(plan);
    }

    private Set<Page> fakeApiPages() {
        var page = new Page();
        var accessControl = new AccessControl();
        accessControl.setReferenceId("role-id");
        accessControl.setReferenceType("ROLE");
        page.setAccessControls(List.of(accessControl));
        var pageMedia = new PageMedia();
        pageMedia.setHash("media-hash");
        pageMedia.setName("media-name");
        pageMedia.setAttachedAt(OffsetDateTime.parse("1970-01-01T00:00:00Z")); //new Date(0)
        page.setAttachedMedia(List.of(pageMedia));
        page.setConfiguration(Map.of("page-config-key", "page-config-value"));
        page.setContent("#content");
        page.setContentRevision(new Revision().id("page-revision-id").revision(1));
        page.setContentType("text/markdown");
        page.setCrossId("crossId");
        page.setExcludedGroups(List.of("excludedGroup"));
        page.setExcludedAccessControls(false);
        page.setGeneralConditions(true);
        page.setHomepage(false);
        page.setId("page-id");
        page.setLastContributor("last-contributor-id");
        page.setUpdatedAt(OffsetDateTime.parse("1970-01-01T00:00:00Z")); //new Date(0)
        page.setMetadata(Map.of("page-metadata-key", "page-metadata-value"));
        page.setName("page-name");
        page.setOrder(1);
        page.setParentId("parent-id");
        page.setParentPath("parent-path");
        page.setPublished(true);
        var pageSource = new PageSource();
        pageSource.setType("GITHUB");
        pageSource.setConfiguration(JsonNodeFactory.instance.objectNode());
        page.setSource(pageSource);
        page.setType(PageType.MARKDOWN);
        page.setTranslations(Collections.emptyList());
        page.setVisibility(Visibility.PUBLIC);

        return Set.of(page);
    }

    private List<Media> fakeApiMedia() {
        var media = new Media();
        media.setId("media-id");
        media.setHash("media-hash");
        media.setSize(1_000L);
        media.setFileName("media-file-name");
        media.setType("media-type");
        media.setSubType("media-sub-type");
        media.setData("media-data".getBytes(StandardCharsets.UTF_8));
        media.setCreatedAt(OffsetDateTime.parse("1970-01-01T00:00:00Z")); //new Date(0)

        return List.of(media);
    }

    private ApiEntity fakeApiEntityV4() {
        var apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(io.gravitee.definition.model.DefinitionVersion.V4);
        apiEntity.setId(API_ID);
        apiEntity.setName(API_ID);
        apiEntity.setApiVersion("v1.0");
        io.gravitee.definition.model.v4.listener.http.HttpListener httpListener =
            new io.gravitee.definition.model.v4.listener.http.HttpListener();
        httpListener.setPaths(List.of(new Path("my.fake.host", "/test")));
        httpListener.setPathMappings(Set.of("/test"));

        io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener subscriptionListener =
            new io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener();
        io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint entrypoint =
            new io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        entrypoint.setDlq(new io.gravitee.definition.model.v4.listener.entrypoint.Dlq("my-endpoint"));
        entrypoint.setConfiguration("{\"nice\": \"configuration\"}");
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        subscriptionListener.setType(io.gravitee.definition.model.v4.listener.ListenerType.SUBSCRIPTION);

        io.gravitee.definition.model.v4.listener.tcp.TcpListener tcpListener =
            new io.gravitee.definition.model.v4.listener.tcp.TcpListener();
        tcpListener.setType(io.gravitee.definition.model.v4.listener.ListenerType.TCP);
        tcpListener.setEntrypoints(List.of(entrypoint));

        apiEntity.setListeners(List.of(httpListener, subscriptionListener, tcpListener));
        apiEntity.setProperties(List.of(new io.gravitee.definition.model.v4.property.Property()));
        apiEntity.setServices(new io.gravitee.definition.model.v4.service.ApiServices());
        apiEntity.setResources(List.of(new io.gravitee.definition.model.v4.resource.Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setAnalytics(new io.gravitee.definition.model.v4.analytics.Analytics());

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

        io.gravitee.definition.model.v4.flow.selector.HttpSelector httpSelector =
            new io.gravitee.definition.model.v4.flow.selector.HttpSelector();
        httpSelector.setPath("/test");
        httpSelector.setMethods(Set.of(io.gravitee.common.http.HttpMethod.GET, io.gravitee.common.http.HttpMethod.POST));
        httpSelector.setPathOperator(io.gravitee.definition.model.flow.Operator.STARTS_WITH);

        io.gravitee.definition.model.v4.flow.selector.ChannelSelector channelSelector =
            new io.gravitee.definition.model.v4.flow.selector.ChannelSelector();
        channelSelector.setChannel("my-channel");
        channelSelector.setChannelOperator(io.gravitee.definition.model.flow.Operator.STARTS_WITH);
        channelSelector.setOperations(Set.of(io.gravitee.definition.model.v4.flow.selector.ChannelSelector.Operation.SUBSCRIBE));
        channelSelector.setEntrypoints(Set.of("my-entrypoint"));

        io.gravitee.definition.model.v4.flow.selector.ConditionSelector conditionSelector =
            new io.gravitee.definition.model.v4.flow.selector.ConditionSelector();
        conditionSelector.setCondition("my-condition");

        flow.setSelectors(List.of(httpSelector, channelSelector, conditionSelector));
        apiEntity.setFlows(List.of(flow));

        return apiEntity;
    }

    // Tests
    private void testReturnedApi(ApiV4 responseApi) {
        assertNotNull(responseApi);
        assertEquals(API_ID, responseApi.getName());
        assertNotNull(responseApi.getLinks());
        assertNotNull(responseApi.getLinks().getPictureUrl());
        assertNotNull(responseApi.getLinks().getBackgroundUrl());
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

    private void testConvertedExportApi(ExportApiEntity exportApiEntity) throws JsonProcessingException {
        assertNotNull(exportApiEntity.getMembers());
        assertNotNull(exportApiEntity.getMetadata());
        assertNotNull(exportApiEntity.getPlans());
        assertNotNull(exportApiEntity.getPages());
        assertNotNull(exportApiEntity.getApiMedia());
        assertNotNull(exportApiEntity.getApiEntity());

        testApi(exportApiEntity.getApiEntity());
        testMembers(exportApiEntity.getMembers());
        testMetadata(exportApiEntity.getMetadata());
        testPlans(exportApiEntity.getPlans());
        testPages(exportApiEntity.getPages());
        testMedia(exportApiEntity.getApiMedia());
    }

    private void testApi(ApiEntity apiEntity) throws JsonProcessingException {
        assertEquals(API_ID, apiEntity.getName());
        assertEquals(API_ID, apiEntity.getName());
        assertNotNull(apiEntity.getProperties());
        assertEquals(1, apiEntity.getProperties().size());
        assertNotNull(apiEntity.getServices());
        assertNotNull(apiEntity.getResources());
        assertEquals(1, apiEntity.getResources().size());
        assertNotNull(apiEntity.getResponseTemplates());
        assertEquals(1, apiEntity.getResponseTemplates().size());

        assertNotNull(apiEntity.getListeners());
        assertEquals(3, apiEntity.getListeners().size());

        assertEquals(io.gravitee.definition.model.v4.listener.http.HttpListener.class, apiEntity.getListeners().get(0).getClass());
        var httpListener = (io.gravitee.definition.model.v4.listener.http.HttpListener) apiEntity.getListeners().get(0);
        assertNotNull(httpListener);
        assertNotNull(httpListener.getPathMappings());
        assertNotNull(httpListener.getPaths());
        assertNotNull(httpListener.getPaths().get(0).getHost());

        assertEquals(
            (io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener.class),
            apiEntity.getListeners().get(1).getClass()
        );
        var subscriptionListener = (io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener) apiEntity
            .getListeners()
            .get(1);
        assertNotNull(subscriptionListener);
        assertNotNull(subscriptionListener.getEntrypoints());
        var foundEntrypoint = subscriptionListener.getEntrypoints().get(0);
        assertNotNull(foundEntrypoint);
        assertEquals("{\n  \"nice\" : \"configuration\"\n}", foundEntrypoint.getConfiguration());
        assertEquals("Entrypoint type", foundEntrypoint.getType());
        assertEquals("SUBSCRIPTION", subscriptionListener.getType().toString());

        assertEquals((io.gravitee.definition.model.v4.listener.tcp.TcpListener.class), apiEntity.getListeners().get(2).getClass());
        var tcpListener = (io.gravitee.definition.model.v4.listener.tcp.TcpListener) apiEntity.getListeners().get(2);
        assertNotNull(tcpListener);
        assertNotNull(tcpListener.getEntrypoints());
        var tcpFoundEntrypoint = tcpListener.getEntrypoints().get(0);
        assertNotNull(tcpFoundEntrypoint);
        assertEquals("{\n  \"nice\" : \"configuration\"\n}", tcpFoundEntrypoint.getConfiguration());
        assertEquals("Entrypoint type", tcpFoundEntrypoint.getType());
        assertEquals("TCP", tcpListener.getType().toString());

        assertNotNull(apiEntity.getEndpointGroups());
        assertEquals(1, apiEntity.getEndpointGroups().size());
        assertNotNull(apiEntity.getEndpointGroups().get(0));
        assertNotNull(apiEntity.getEndpointGroups().get(0).getEndpoints());
        assertEquals(1, apiEntity.getEndpointGroups().get(0).getEndpoints().size());

        var endpoint = apiEntity.getEndpointGroups().get(0).getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("http-get", endpoint.getType());

        JsonNode endpointConfig = new ObjectMapper().readTree(endpoint.getConfiguration());
        assertEquals("kafka:9092", endpointConfig.get("bootstrapServers").asText());
        assertEquals("demo", endpointConfig.get("topics").get(0).asText());

        assertNotNull(apiEntity.getFlows());
        assertEquals(1, apiEntity.getFlows().size());

        var flow = apiEntity.getFlows().get(0);
        assertNotNull(flow);
        assertEquals("flowName", flow.getName());
        assertEquals(Boolean.TRUE, flow.isEnabled());
        assertNotNull(flow.getTags());
        assertEquals(2, flow.getTags().size());
        assertEquals(Set.of("tag1", "tag2"), flow.getTags());
        assertNotNull(flow.getRequest());
        assertEquals(1, flow.getRequest().size());

        var step = flow.getRequest().get(0);
        assertNotNull(step);
        assertEquals(Boolean.TRUE, step.isEnabled());
        assertEquals("my-policy", step.getPolicy());
        assertEquals("my-condition", step.getCondition());

        assertNotNull(flow.getSelectors());
        assertEquals(3, flow.getSelectors().size());

        assertEquals(io.gravitee.definition.model.v4.flow.selector.HttpSelector.class, flow.getSelectors().get(0).getClass());
        var httpSelector = (io.gravitee.definition.model.v4.flow.selector.HttpSelector) flow.getSelectors().get(0);
        assertNotNull(httpSelector);
        assertEquals("/test", httpSelector.getPath());
        assertEquals(io.gravitee.definition.model.flow.Operator.STARTS_WITH, httpSelector.getPathOperator());
        assertEquals(2, httpSelector.getMethods().size());
        assertEquals(Set.of(io.gravitee.common.http.HttpMethod.GET, io.gravitee.common.http.HttpMethod.POST), httpSelector.getMethods());

        assertEquals(io.gravitee.definition.model.v4.flow.selector.ChannelSelector.class, flow.getSelectors().get(1).getClass());
        var channelSelector = (io.gravitee.definition.model.v4.flow.selector.ChannelSelector) flow.getSelectors().get(1);
        assertEquals("my-channel", channelSelector.getChannel());
        assertEquals(io.gravitee.definition.model.flow.Operator.STARTS_WITH, channelSelector.getChannelOperator());
        assertEquals(1, channelSelector.getOperations().size());
        assertEquals(
            Set.of(io.gravitee.definition.model.v4.flow.selector.ChannelSelector.Operation.SUBSCRIBE),
            channelSelector.getOperations()
        );
        assertEquals(1, channelSelector.getEntrypoints().size());
        assertEquals(Set.of("my-entrypoint"), channelSelector.getEntrypoints());

        assertEquals(io.gravitee.definition.model.v4.flow.selector.ConditionSelector.class, flow.getSelectors().get(2).getClass());
        var conditionSelector = (io.gravitee.definition.model.v4.flow.selector.ConditionSelector) flow.getSelectors().get(2);
        assertEquals("my-condition", conditionSelector.getCondition());
    }

    private void testMembers(Set<MemberEntity> members) {
        assertEquals(2, members.size());

        var userMember = new MemberEntity();
        userMember.setDisplayName("John Doe");
        userMember.setId("memberId");
        userMember.setType(MembershipMemberType.USER);
        userMember.setReferenceId(API_ID);
        userMember.setReferenceType(MembershipReferenceType.API);

        var ownerRole = new RoleEntity();
        ownerRole.setName("OWNER");
        userMember.setRoles(List.of(ownerRole));

        var poUserMember = new MemberEntity();
        poUserMember.setDisplayName("Thomas Pesquet");
        poUserMember.setId("poMemberId");
        poUserMember.setType(MembershipMemberType.USER);
        poUserMember.setReferenceId(API_ID);
        poUserMember.setReferenceType(MembershipReferenceType.API);

        var primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setName("PRIMARY_OWNER");
        poUserMember.setRoles(List.of(primaryOwnerRole));

        assertEquals(Set.of(userMember, poUserMember), members);
    }

    private void testMetadata(Set<ApiMetadataEntity> metadata) {
        assertEquals(2, metadata.size());

        var firstMetadata = new ApiMetadataEntity();
        firstMetadata.setApiId(API_ID);
        firstMetadata.setKey("my-metadata-1");
        firstMetadata.setName("My first metadata");
        firstMetadata.setFormat(io.gravitee.rest.api.model.MetadataFormat.NUMERIC);
        firstMetadata.setValue("1");
        firstMetadata.setDefaultValue("5");

        var secondMetadata = new ApiMetadataEntity();
        secondMetadata.setApiId(API_ID);
        secondMetadata.setKey("my-metadata-2");
        secondMetadata.setName("My second metadata");
        secondMetadata.setFormat(io.gravitee.rest.api.model.MetadataFormat.STRING);
        secondMetadata.setValue("Very important data !!");
        secondMetadata.setDefaultValue("Important data");

        assertEquals(Set.of(firstMetadata, secondMetadata), metadata);
    }

    private void testPlans(Set<PlanEntity> plans) {
        assertEquals(1, plans.size());

        var plan = plans.iterator().next();
        assertEquals(API_ID, plan.getApiId());
        assertEquals(List.of("characteristic1", "characteristic2"), plan.getCharacteristics());
        assertEquals("commentMessage", plan.getCommentMessage());
        assertEquals(true, plan.isCommentRequired());
        assertEquals("crossId", plan.getCrossId());
        assertEquals(new Date(5025000), plan.getCreatedAt());
        assertNull(plan.getClosedAt());
        assertEquals("description", plan.getDescription());
        assertEquals(List.of("excludedGroup"), plan.getExcludedGroups());
        assertEquals("generalConditions", plan.getGeneralConditions());
        assertEquals("planId", plan.getId());
        assertEquals("planName", plan.getName());
        assertEquals(1, plan.getOrder());
        assertEquals(new Date(5026000), plan.getPublishedAt());
        assertEquals(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED, plan.getStatus());
        assertNull(plan.getSelectionRule());
        assertEquals(List.of("tag1", "tag2"), plan.getTags().stream().sorted().collect(Collectors.toList()));
        assertEquals(io.gravitee.rest.api.model.v4.plan.PlanType.API, plan.getType());
        assertEquals(new Date(5027000), plan.getUpdatedAt());
        assertEquals(PlanValidationType.AUTO, plan.getValidation());

        assertNotNull(plan.getFlows());
        assertEquals(1, plan.getFlows().size());

        var flowV4 = plan.getFlows().get(0);
        assertNotNull(flowV4);
        assertEquals(true, flowV4.isEnabled());
        assertEquals("planFlowName", flowV4.getName());
        assertNull(flowV4.getPublish());

        assertNotNull(flowV4.getRequest());
        assertEquals(1, flowV4.getRequest().size());

        var step = flowV4.getRequest().get(0);
        assertEquals(true, step.isEnabled());
        assertEquals("stepConfiguration", step.getConfiguration());
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

        assertEquals(io.gravitee.definition.model.v4.flow.selector.HttpSelector.class, flowV4.getSelectors().get(0).getClass());
        var httpSelector = (io.gravitee.definition.model.v4.flow.selector.HttpSelector) flowV4.getSelectors().get(0);
        assertNotNull(httpSelector);
        assertEquals("/test", httpSelector.getPath());
        assertEquals(io.gravitee.definition.model.flow.Operator.STARTS_WITH, httpSelector.getPathOperator());
        assertEquals(2, httpSelector.getMethods().size());
        assertEquals(Set.of(io.gravitee.common.http.HttpMethod.GET, io.gravitee.common.http.HttpMethod.POST), httpSelector.getMethods());

        assertNotNull(plan.getSecurity());
        var planSecurity = plan.getSecurity();
        assertEquals("key-less", planSecurity.getType());
        assertEquals("{}", planSecurity.getConfiguration());
    }

    private void testPages(List<PageEntity> pages) {
        assertEquals(1, pages.size());

        var page = pages.get(0);
        assertNotNull(page.getAccessControls());
        assertEquals(1, page.getAccessControls().size());

        var accessControl = page.getAccessControls().iterator().next();
        assertNotNull(accessControl);
        assertEquals("role-id", accessControl.getReferenceId());
        assertEquals("ROLE", accessControl.getReferenceType());

        assertNotNull(page.getAttachedMedia());
        assertEquals(1, page.getAttachedMedia().size());

        var pageMedia = page.getAttachedMedia().get(0);
        assertNotNull(pageMedia);
        assertEquals("media-hash", pageMedia.getMediaHash());
        assertEquals("media-name", pageMedia.getMediaName());
        assertEquals(new Date(0), pageMedia.getAttachedAt());

        assertEquals(Map.of("page-config-key", "page-config-value"), page.getConfiguration());
        assertEquals("#content", page.getContent());

        assertNotNull(page.getContentRevisionId());
        var contentRevision = page.getContentRevisionId();
        assertEquals("page-revision-id", contentRevision.getPageId());
        assertEquals(1, contentRevision.getRevision());

        assertEquals("text/markdown", page.getContentType());
        assertEquals("crossId", page.getCrossId());
        assertEquals(List.of("excludedGroup"), page.getExcludedGroups());
        assertEquals(false, page.isExcludedAccessControls());
        assertEquals(true, page.isGeneralConditions());
        assertEquals(false, page.isHomepage());
        assertEquals("page-id", page.getId());
        assertEquals("last-contributor-id", page.getLastContributor());
        assertEquals(new Date(0), page.getLastModificationDate());
        assertEquals(Map.of("page-metadata-key", "page-metadata-value"), page.getMetadata());
        assertEquals("page-name", page.getName());
        assertEquals(1, page.getOrder());
        assertEquals("parent-id", page.getParentId());
        assertEquals("parent-path", page.getParentPath());
        assertEquals(true, page.isPublished());

        assertNotNull(page.getSource());
        var source = page.getSource();
        assertEquals("GITHUB", source.getType());
        assertEquals("{}", source.getConfiguration());

        assertEquals("MARKDOWN", page.getType());

        assertNotNull(page.getTranslations());
        assertEquals(0, page.getTranslations().size());

        assertEquals(io.gravitee.rest.api.model.Visibility.PUBLIC, page.getVisibility());
    }

    private void testMedia(List<MediaEntity> mediaList) {
        assertEquals(1, mediaList.size());

        var media = mediaList.get(0);
        assertNotNull(media);

        assertEquals("media-id", media.getId());
        assertEquals("media-hash", media.getHash());
        assertEquals(1_000L, media.getSize());
        assertEquals("media-file-name", media.getFileName());
        assertEquals("media-type", media.getType());
        assertEquals("media-sub-type", media.getSubType());
        assertArrayEquals("media-data".getBytes(StandardCharsets.UTF_8), media.getData());
        assertEquals(new Date(0), media.getCreateAt());
    }
}
