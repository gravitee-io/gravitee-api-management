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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
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
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.AccessControlEntity;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apiguardian.api.API;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiImportExportServiceImplTest {

    private static final String PRIMARY_OWNER = "PRIMARY_OWNER";
    private static final String OWNER = "OWNER";
    private static final String MEMBER_ID = "memberId";
    private static final String PO_MEMBER_ID = "poMemberId";
    public static final String DUMMY = "dummy";

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private MediaService mediaService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PlanService planService;

    @Mock
    private RoleService roleService;

    private ApiImportExportService cut;

    private static final String API_ID = "my-api";
    private static final String USER_ID = "my-user";

    @BeforeEach
    public void setUp() {
        cut =
            new ApiImportExportServiceImpl(
                apiMetadataService,
                apiService,
                apiSearchService,
                mediaService,
                membershipService,
                pageService,
                permissionService,
                planService,
                roleService
            );
        reset(
            apiMetadataService,
            apiService,
            apiSearchService,
            mediaService,
            membershipService,
            pageService,
            permissionService,
            planService,
            roleService
        );
    }

    @Test
    public void should_not_export_v2_apis() {
        doReturn(this.fakeApiEntityV2()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        assertThrows(
            ApiDefinitionVersionNotSupportedException.class,
            () -> cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID)
        );
    }

    @Test
    public void should_export_only_api_when_only_definition_permission() throws JsonProcessingException {
        mockPermissions(false, false, false, false);
        doReturn(this.fakeApiEntityV4()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);

        final ExportApiEntity export = cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
        assertNull(export.getMembers());
        assertNull(export.getMetadata());
        assertNull(export.getPlans());
        assertNull(export.getPages());
        assertNotNull(export.getApiEntity());

        verify(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        verify(apiMetadataService, never()).findAllByApi(API_ID);
        verify(planService, never()).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(pageService, never()).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        verify(mediaService, never()).findAllByApiId(API_ID);
    }

    @Test
    public void should_export_members_and_api_when_member_and_definition_permission() throws JsonProcessingException {
        mockPermissions(true, false, false, false);
        doReturn(this.fakeApiEntityV4()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        doReturn(this.fakeApiMembers())
            .when(membershipService)
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);

        final ExportApiEntity export = cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
        assertNotNull(export.getMembers());
        assertNull(export.getMetadata());
        assertNull(export.getPlans());
        assertNull(export.getPages());
        assertNotNull(export.getApiEntity());

        verify(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        verify(membershipService).getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(apiMetadataService, never()).findAllByApi(API_ID);
        verify(planService, never()).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(pageService, never()).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        verify(mediaService, never()).findAllByApiId(API_ID);
    }

    @Test
    public void should_export_metadata_and_api_when_metadata_and_definition_permission() throws JsonProcessingException {
        mockPermissions(false, true, false, false);
        doReturn(this.fakeApiEntityV4()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        doReturn(new ArrayList<>(this.fakeApiMetadata())).when(apiMetadataService).findAllByApi(API_ID);

        final ExportApiEntity export = cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
        assertNull(export.getMembers());
        assertNotNull(export.getMetadata());
        assertNull(export.getPlans());
        assertNull(export.getPages());
        assertNotNull(export.getApiEntity());

        verify(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        verify(membershipService, never())
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(apiMetadataService).findAllByApi(API_ID);
        verify(planService, never()).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(pageService, never()).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        verify(mediaService, never()).findAllByApiId(API_ID);
    }

    @Test
    public void should_export_plans_and_api_when_plan_and_definition_permission() throws JsonProcessingException {
        mockPermissions(false, false, true, false);

        doReturn(this.fakeApiEntityV4()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        doReturn(this.fakeApiPlans()).when(planService).findByApi(GraviteeContext.getExecutionContext(), API_ID);

        final ExportApiEntity export = cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
        assertNull(export.getMembers());
        assertNull(export.getMetadata());
        assertNotNull(export.getPlans());
        assertNull(export.getPages());
        assertNotNull(export.getApiEntity());

        verify(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        verify(membershipService, never())
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(apiMetadataService, never()).findAllByApi(API_ID);
        verify(planService).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(pageService, never()).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        verify(mediaService, never()).findAllByApiId(API_ID);
    }

    @Test
    public void should_export_pages_and_api_when_documentation_and_definition_permission() throws JsonProcessingException {
        mockPermissions(false, false, false, true);

        doReturn(this.fakeApiEntityV4()).when(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        doReturn(this.fakeApiPages()).when(pageService).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        doReturn(this.fakeApiMedia()).when(mediaService).findAllByApiId(API_ID);

        final ExportApiEntity export = cut.exportApi(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
        assertNull(export.getMembers());
        assertNull(export.getMetadata());
        assertNull(export.getPlans());
        assertNotNull(export.getPages());
        assertNotNull(export.getApiEntity());

        verify(apiSearchService).findGenericById(GraviteeContext.getExecutionContext(), API_ID);
        verify(membershipService, never())
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(apiMetadataService, never()).findAllByApi(API_ID);
        verify(planService, never()).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(pageService).findByApi(GraviteeContext.getCurrentEnvironment(), API_ID);
        verify(mediaService).findAllByApiId(API_ID);
    }

    private void mockPermissions(boolean member, boolean metadata, boolean plan, boolean documentation) {
        doReturn(member)
            .when(permissionService)
            .hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_MEMBER, API_ID, RolePermissionAction.READ);
        doReturn(metadata)
            .when(permissionService)
            .hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_METADATA, API_ID, RolePermissionAction.READ);
        doReturn(plan)
            .when(permissionService)
            .hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_PLAN, API_ID, RolePermissionAction.READ);
        doReturn(documentation)
            .when(permissionService)
            .hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_DOCUMENTATION, API_ID, RolePermissionAction.READ);
    }

    @Test
    public void should_not_import_v2_apis() {
        assertThrows(
            ApiDefinitionVersionNotSupportedException.class,
            () -> cut.createFromExportedApi(GraviteeContext.getExecutionContext(), fakeExportApiEntityV2(), USER_ID)
        );
    }

    @Test
    public void should_import() {
        ExportApiEntity exportApiEntityV4 = fakeExportApiEntity();

        doReturn(exportApiEntityV4.getApiEntity())
            .when(apiService)
            .createWithImport(GraviteeContext.getExecutionContext(), exportApiEntityV4.getApiEntity(), USER_ID);

        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(PRIMARY_OWNER);
        primaryOwnerRole.setName(PRIMARY_OWNER);
        doReturn(primaryOwnerRole)
            .when(roleService)
            .findPrimaryOwnerRoleByOrganization(GraviteeContext.getCurrentOrganization(), RoleScope.API);

        RoleEntity ownerRole = new RoleEntity();
        ownerRole.setId(OWNER);
        ownerRole.setName(OWNER);
        doReturn(Optional.of(ownerRole))
            .when(roleService)
            .findByScopeAndName(eq(RoleScope.API), eq(OWNER), eq(GraviteeContext.getDefaultOrganization()));

        doReturn(emptyList()).when(pageService).search(any(), any());

        GenericApiEntity fromExportedApi = cut.createFromExportedApi(GraviteeContext.getExecutionContext(), exportApiEntityV4, USER_ID);
        assertEquals(exportApiEntityV4.getApiEntity(), fromExportedApi);

        verify(apiService).createWithImport(GraviteeContext.getExecutionContext(), exportApiEntityV4.getApiEntity(), USER_ID);

        verify(membershipService, times(0))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                PO_MEMBER_ID,
                PRIMARY_OWNER
            );

        verify(membershipService)
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                MEMBER_ID,
                OWNER
            );
        verify(pageService).createOrUpdatePages(GraviteeContext.getExecutionContext(), exportApiEntityV4.getPages(), API_ID);

        ArgumentCaptor<UpdateApiMetadataEntity> updateApiMetadataEntityArgumentCaptor = ArgumentCaptor.forClass(
            UpdateApiMetadataEntity.class
        );
        verify(apiMetadataService, times(2))
            .update(eq(GraviteeContext.getExecutionContext()), updateApiMetadataEntityArgumentCaptor.capture());
        List<UpdateApiMetadataEntity> allValues = updateApiMetadataEntityArgumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertEquals(API_ID, allValues.get(0).getApiId());
        assertEquals(API_ID, allValues.get(1).getApiId());

        verify(mediaService).saveApiMedia(API_ID, fakeApiMedia().get(0));
        verify(pageService).createAsideFolder(GraviteeContext.getExecutionContext(), API_ID);
        verify(roleService).findByScopeAndName(RoleScope.API, OWNER, GraviteeContext.getDefaultOrganization());
    }

    @Test
    public void should_not_find_role_to_import() {
        ExportApiEntity exportApiEntityV4 = fakeExportApiEntity();

        var dummyRole = new RoleEntity();
        dummyRole.setId(DUMMY);
        dummyRole.setName(DUMMY);
        var dummyMember = new MemberEntity();
        dummyMember.setId(MEMBER_ID);
        dummyMember.setDisplayName("Dummy John Doe");
        dummyMember.setRoles(List.of(dummyRole));
        dummyMember.setType(MembershipMemberType.USER);
        exportApiEntityV4.setMembers(Set.of(dummyMember));

        doReturn(exportApiEntityV4.getApiEntity())
            .when(apiService)
            .createWithImport(GraviteeContext.getExecutionContext(), exportApiEntityV4.getApiEntity(), USER_ID);

        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(PRIMARY_OWNER);
        primaryOwnerRole.setName(PRIMARY_OWNER);
        doReturn(primaryOwnerRole)
            .when(roleService)
            .findPrimaryOwnerRoleByOrganization(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        doReturn(Optional.empty())
            .when(roleService)
            .findByScopeAndName(eq(RoleScope.API), eq(DUMMY), eq(GraviteeContext.getDefaultOrganization()));

        doReturn(emptyList()).when(pageService).search(any(), any());

        cut.createFromExportedApi(GraviteeContext.getExecutionContext(), exportApiEntityV4, USER_ID);

        verifyNoInteractions(membershipService);
    }

    // Fakers
    private io.gravitee.rest.api.model.api.ApiEntity fakeApiEntityV2() {
        var apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        return apiEntity;
    }

    private ExportApiEntity fakeExportApiEntityV2() {
        var exportApiEntity = new ExportApiEntity();
        ApiEntity apiEntity = fakeApiEntityV4();
        apiEntity.setDefinitionVersion(DefinitionVersion.V2);
        exportApiEntity.setApiEntity(apiEntity);

        return exportApiEntity;
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
        apiEntity.setId(API_ID);
        apiEntity.setName(API_ID);
        apiEntity.setApiVersion("v1.0");
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("my.fake.host", "/test")));
        httpListener.setPathMappings(Set.of("/test"));

        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        entrypoint.setDlq(new Dlq("my-endpoint"));
        entrypoint.setConfiguration("{\"nice\": \"configuration\"}");
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
        role.setId(OWNER);
        role.setName(OWNER);
        var userMember = new MemberEntity();
        userMember.setId(MEMBER_ID);
        userMember.setDisplayName("John Doe");
        userMember.setRoles(List.of(role));
        userMember.setType(MembershipMemberType.USER);

        var poRole = new RoleEntity();
        poRole.setId(PRIMARY_OWNER);
        poRole.setName(PRIMARY_OWNER);
        var poUserMember = new MemberEntity();
        poUserMember.setId(PO_MEMBER_ID);
        poUserMember.setDisplayName("Thomas Pesquet");
        poUserMember.setRoles(List.of(poRole));
        poUserMember.setType(MembershipMemberType.USER);

        return Set.of(userMember, poUserMember);
    }

    private Set<ApiMetadataEntity> fakeApiMetadata() {
        ApiMetadataEntity firstMetadata = new ApiMetadataEntity();
        firstMetadata.setApiId(API_ID);
        firstMetadata.setKey("my-metadata-1");
        firstMetadata.setName("My first metadata");
        firstMetadata.setFormat(MetadataFormat.NUMERIC);
        firstMetadata.setValue("1");
        firstMetadata.setDefaultValue("5");

        ApiMetadataEntity secondMetadata = new ApiMetadataEntity();
        secondMetadata.setApiId(API_ID);
        secondMetadata.setKey("my-metadata-2");
        secondMetadata.setName("My second metadata");
        secondMetadata.setFormat(MetadataFormat.STRING);
        secondMetadata.setValue("Very important data !!");
        secondMetadata.setDefaultValue("Important data");

        return Set.of(firstMetadata, secondMetadata);
    }

    private Set<PlanEntity> fakeApiPlans() {
        PlanEntity planEntity = new PlanEntity();
        planEntity.setApiId(API_ID);
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
        step.setConfiguration("stepConfiguration");
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
        pageEntity.setTranslations(emptyList());
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
}
