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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.kubernetes.mapper.CustomResourceDefinitionMapper;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.kubernetes.v1alpha1.ApiExportQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.jackson.ser.api.ApiCompositeSerializer;
import io.gravitee.rest.api.service.jackson.ser.api.ApiDefaultSerializer;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiExportService_ExportAsCustomResourceTest extends ApiExportService_ExportAsJsonTestSetup {

    @Mock
    private ApiService apiService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PlanConverter planConverter;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private GroupService groupService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private RoleService roleService;

    @Mock
    private MediaService mediaService;

    private ApiEntity apiEntity;

    private ApiExportServiceImpl apiExportService;

    @Override
    @Before
    public void setUp() throws io.gravitee.repository.exceptions.TechnicalException {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );

        // register API Entity serializers
        when(applicationContext.getBean(MembershipService.class)).thenReturn(membershipService);
        when(applicationContext.getBean(PlanService.class)).thenReturn(planService);
        when(applicationContext.getBean(PageService.class)).thenReturn(pageService);
        when(applicationContext.getBean(UserService.class)).thenReturn(userService);
        when(applicationContext.getBean(ApiMetadataService.class)).thenReturn(apiMetadataService);
        when(applicationContext.getBean(MediaService.class)).thenReturn(mediaService);
        when(applicationContext.getBean(GroupService.class)).thenReturn(groupService);

        ApiCompositeSerializer apiCompositeSerializer = new ApiCompositeSerializer();
        ApiSerializer apiDefaultSerializer = new ApiDefaultSerializer();
        apiDefaultSerializer.setApplicationContext(applicationContext);

        apiExportService =
            new ApiExportServiceImpl(
                objectMapper,
                pageService,
                planService,
                apiService,
                roleService,
                apiConverter,
                planConverter,
                new CustomResourceDefinitionMapper()
            );

        apiCompositeSerializer.setSerializers(Arrays.asList(apiDefaultSerializer));
        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiCompositeSerializer);
        objectMapper.registerModule(module);

        apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setCrossId("test-api-cross-id");
        apiEntity.setName("Export Test API");
        apiEntity.setDescription("Gravitee.io");
        apiEntity.setExecutionMode(ExecutionMode.V3);
        apiEntity.setFlowMode(FlowMode.DEFAULT);
        apiEntity.setFlows(null);
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V1.getLabel());
        // set proxy
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        proxy.setStripContextPath(false);
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("condition");
        proxy.setLogging(logging);
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("default-group");
        Endpoint endpoint = Endpoint.builder().name("default").target("http://test").build();
        endpointGroup.setEndpoints(Collections.singleton(endpoint));
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);
        proxy.setGroups(Collections.singleton(endpointGroup));

        apiEntity.setPaths(null);
        apiEntity.setProxy(proxy);

        String groupName = "developers";
        apiEntity.setGroups(Set.of(groupName));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupName);
        when(groupService.findByIds(apiEntity.getGroups())).thenReturn(Set.of(groupEntity));

        ResponseTemplate responseTemplate = new ResponseTemplate();
        responseTemplate.setStatusCode(400);
        responseTemplate.setBody("{\"bad\":\"news\"}");
        responseTemplate.setPropagateErrorKeyToLogs(false);
        apiEntity.setResponseTemplates(Collections.singletonMap("API_KEY_MISSING", Collections.singletonMap("*/*", responseTemplate)));

        when(apiService.findById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiEntity);
        PageEntity folder = new PageEntity();
        folder.setName("My Folder");
        folder.setOrder(1);
        folder.setType(PageType.FOLDER.toString());
        folder.setVisibility(Visibility.PUBLIC);

        when(pageService.search(eq(GraviteeContext.getCurrentEnvironment()), any(), eq(true))).thenReturn(List.of(folder));

        String poRoleId = "9ca07fdb-e143-45be-9c7d-44946a94968e";
        String poRoleName = "API_PRIMARY_OWNER";

        RoleEntity poRole = new RoleEntity();
        poRole.setName(poRoleName);
        poRole.setId(poRoleId);

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getDefaultOrganization())).thenReturn(List.of(poRole));

        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("johndoe");
        membership.setMemberType(MembershipMemberType.USER);
        membership.setRoleId(poRoleId);

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(membership.getMemberId());
        memberEntity.setType(membership.getMemberType());
        memberEntity.setRoles(Collections.singletonList(poRole));
        when(
            membershipService.getMembersByReference(eq(GraviteeContext.getExecutionContext()), eq(MembershipReferenceType.API), eq(API_ID))
        )
            .thenReturn(Collections.singleton(memberEntity));
        UserEntity userEntity = new UserEntity();
        userEntity.setId(memberEntity.getId());
        userEntity.setSource(userEntity.getId() + "-source");
        userEntity.setSourceId(userEntity.getId() + "-sourceId");
        when(userService.findById(GraviteeContext.getExecutionContext(), memberEntity.getId())).thenReturn(userEntity);

        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        primaryOwnerEntity.setDisplayName("johndoe-sourceId");
        primaryOwnerEntity.setId("johndoe");
        primaryOwnerEntity.setType(MembershipMemberType.USER.toString());
        apiEntity.setPrimaryOwner(primaryOwnerEntity);

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-id");
        publishedPlan.setCrossId("test-plan-cross-id");
        publishedPlan.setApi(API_ID);
        publishedPlan.setDescription("free plan");
        publishedPlan.setType(PlanType.API);
        publishedPlan.setSecurity(PlanSecurityType.API_KEY);
        publishedPlan.setValidation(PlanValidationType.AUTO);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);

        Set<PlanEntity> set = new HashSet<>();
        set.add(publishedPlan);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(set);
    }

    @Test
    public void shouldConvertAsCustomResourceDefinition() throws Exception {
        ApiExportQuery exportQuery = ApiExportQuery.builder().build();
        String actualExport = apiExportService.exportAsCustomResourceDefinition(GraviteeContext.getExecutionContext(), API_ID, exportQuery);
        String expectedExport = getExpected("io/gravitee/rest/api/management/service/export-convertAsCustomResource.yml");
        Assertions.assertThat(actualExport).isEqualTo(expectedExport);
    }

    @Test
    public void shouldConvertAsCustomResourceDefinition_removingIds() throws Exception {
        ApiExportQuery exportQuery = ApiExportQuery.builder().removeIds(true).build();
        String actualExport = apiExportService.exportAsCustomResourceDefinition(GraviteeContext.getExecutionContext(), API_ID, exportQuery);
        String expectedExport = getExpected("io/gravitee/rest/api/management/service/export-convertAsCustomResource-noIds.yml");
        Assertions.assertThat(actualExport).isEqualTo(expectedExport);
    }

    @Test
    public void shouldConvertAsCustomResourceDefinition_settingContextRef() throws Exception {
        ApiExportQuery exportQuery = ApiExportQuery.builder().contextRefName("apim-dev-ctx").contextRefNamespace("default").build();
        String actualExport = apiExportService.exportAsCustomResourceDefinition(GraviteeContext.getExecutionContext(), API_ID, exportQuery);
        String expectedExport = getExpected("io/gravitee/rest/api/management/service/export-convertAsCustomResource-contextRef.yml");
        Assertions.assertThat(actualExport).isEqualTo(expectedExport);
    }

    @Test
    public void shouldConvertAsCustomResourceDefinition_settingVersion() throws Exception {
        ApiExportQuery exportQuery = ApiExportQuery.builder().version("1.0.0-alpha").build();
        String actualExport = apiExportService.exportAsCustomResourceDefinition(GraviteeContext.getExecutionContext(), API_ID, exportQuery);
        String expectedExport = getExpected("io/gravitee/rest/api/management/service/export-convertAsCustomResource-version.yml");
        Assertions.assertThat(actualExport).isEqualTo(expectedExport);
    }

    @Test
    public void shouldConvertAsCustomResourceDefinition_settingContextPath() throws Exception {
        ApiExportQuery exportQuery = ApiExportQuery.builder().contextPath("/test-updated").build();
        String actualExport = apiExportService.exportAsCustomResourceDefinition(GraviteeContext.getExecutionContext(), API_ID, exportQuery);
        String expectedExport = getExpected("io/gravitee/rest/api/management/service/export-convertAsCustomResource-contextPath.yml");
        Assertions.assertThat(actualExport).isEqualTo(expectedExport);
    }

    private String getExpected(String path) throws Exception {
        URL url = getClass().getClassLoader().getResource(path);

        assertNotNull(url);

        return Files.readString(Path.of(url.toURI()));
    }
}
