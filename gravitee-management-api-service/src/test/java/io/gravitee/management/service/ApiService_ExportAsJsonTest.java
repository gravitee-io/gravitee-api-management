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
package io.gravitee.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.management.service.jackson.ser.api.Api1_15VersionSerializer;
import io.gravitee.management.service.jackson.ser.api.ApiCompositeSerializer;
import io.gravitee.management.service.jackson.ser.api.ApiDefaultSerializer;
import io.gravitee.management.service.jackson.ser.api.ApiSerializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_ExportAsJsonTest {

    private static final String API_ID = "id-api";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipRepository membershipRepository;

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

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));

        // register API Entity serializers
        when(applicationContext.getBean(MembershipService.class)).thenReturn(membershipService);
        when(applicationContext.getBean(PlanService.class)).thenReturn(planService);
        when(applicationContext.getBean(PageService.class)).thenReturn(pageService);
        when(applicationContext.getBean(GroupService.class)).thenReturn(groupService);
        ApiCompositeSerializer apiCompositeSerializer = new ApiCompositeSerializer();
        ApiSerializer apiDefaultSerializer = new ApiDefaultSerializer();
        apiDefaultSerializer.setApplicationContext(applicationContext);

        ApiSerializer apiPrior117VersionSerializer = new Api1_15VersionSerializer();
        apiPrior117VersionSerializer.setApplicationContext(applicationContext);

        apiCompositeSerializer.setSerializers(Arrays.asList(apiDefaultSerializer, apiPrior117VersionSerializer));
        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiCompositeSerializer);
        objectMapper.registerModule(module);

        Api api = new Api();
        api.setId(API_ID);
        api.setDescription("Gravitee.io");

        // set proxy
        Proxy proxy = new Proxy();
        proxy.setContextPath("/test");
        proxy.setStripContextPath(false);
        proxy.setLoggingMode(LoggingMode.NONE);
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("default-group");
        Endpoint endpoint = new HttpEndpoint("default", "http://test");
        endpointGroup.setEndpoints(Collections.singleton(endpoint));
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);
        proxy.setGroups(Collections.singleton(endpointGroup));

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setPaths(Collections.emptyMap());
            apiDefinition.setProxy(proxy);
            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
        } catch (Exception e) {
            // ignore
        }

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PageEntity page = new PageEntity();
        page.setName("My Title");
        page.setOrder(1);
        page.setType(PageType.MARKDOWN.toString());
        page.setContent("Read the doc");
        when(pageService.findApiPagesByApi(API_ID)).thenReturn(Collections.singletonList(new PageListItem()));
        when(pageService.findById(any())).thenReturn(page);
        Membership membership = new Membership();
        membership.setUserId("johndoe");
        membership.setReferenceId(API_ID);
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferenceAndRole(eq(MembershipReferenceType.API), eq(API_ID), any(), any()))
                .thenReturn(Collections.singleton(membership));
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setUsername(membership.getUserId());
        memberEntity.setRole(SystemRole.PRIMARY_OWNER.name());
        when(membershipService.getMembers(eq(MembershipReferenceType.API), eq(API_ID), eq(RoleScope.API)))
                .thenReturn(Collections.singleton(memberEntity));
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(memberEntity.getId());
        when(userService.findByUsername(memberEntity.getId(), false)).thenReturn(userEntity);

        api.setGroups(Collections.singleton("my-group"));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId("my-group");
        groupEntity.setName("My Group");
        when(groupService.findByIds(api.getGroups())).thenReturn(Collections.singleton(groupEntity));

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-id");
        publishedPlan.setApis(Collections.singleton(API_ID));
        publishedPlan.setDescription("free plan");
        publishedPlan.setType(PlanType.API);
        publishedPlan.setSecurity(PlanSecurityType.API_KEY);
        publishedPlan.setValidation(PlanValidationType.AUTO);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        Map<String, Path> paths = new HashMap<>();
        Path path = new Path();
        path.setPath("/");
        io.gravitee.definition.model.Rule rule = new io.gravitee.definition.model.Rule();
        rule.setEnabled(true);
        rule.setMethods(Collections.singletonList(HttpMethod.GET));
        Policy policy = new Policy();
        policy.setName("rate-limit");
        String ls = System.lineSeparator();
        policy.setConfiguration("{" + ls +
                "          \"rate\": {" + ls +
                "            \"limit\": 1," + ls +
                "            \"periodTime\": 1," + ls +
                "            \"periodTimeUnit\": \"SECONDS\"" + ls +
                "          }" + ls +
                "        }");
        rule.setPolicy(policy);
        path.setRules(Collections.singletonList(rule));
        paths.put("/", path);
        publishedPlan.setPaths(paths);
        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closedPlan-id");
        closedPlan.setApis(Collections.singleton(API_ID));
        closedPlan.setDescription("free closedPlan");
        closedPlan.setType(PlanType.API);
        closedPlan.setSecurity(PlanSecurityType.API_KEY);
        closedPlan.setValidation(PlanValidationType.AUTO);
        closedPlan.setStatus(PlanStatus.CLOSED);
        closedPlan.setPaths(paths);
        Set<PlanEntity> set = new HashSet<>();
        set.add(publishedPlan);
        set.add(closedPlan);
        when(planService.findByApi(API_ID)).thenReturn(set);
    }

    @Test
    public void shouldConvertAsJsonForExport() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExport_1_15() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_1_15() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutPages() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPages_1_15() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans_1_15() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonMultipleGroups_1_15() throws IOException, TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDescription("Gravitee.io");
        api.setGroups(Collections.singleton("my-group"));

        // set proxy
        Proxy proxy = new Proxy();
        proxy.setContextPath("/test");
        proxy.setStripContextPath(false);
        proxy.setLoggingMode(LoggingMode.NONE);
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("default-group");
        Endpoint endpoint = new HttpEndpoint("default", "http://test");
        endpointGroup.setEndpoints(Collections.singleton(endpoint));
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);

        EndpointGroup endpointGroup2 = new EndpointGroup();
        endpointGroup2.setName("backup-group");
        Endpoint endpoint2 = new HttpEndpoint("backup", "http://test2");
        endpointGroup2.setEndpoints(Collections.singleton(endpoint2));
        proxy.setGroups(new HashSet<>(Arrays.asList(endpointGroup, endpointGroup2)));

        Failover failover = new Failover();
        failover.setMaxAttempts(5);
        failover.setRetryTimeout(2000);
        proxy.setFailover(failover);

        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("*"));
        cors.setAccessControlAllowHeaders(Collections.singleton("content-type"));
        cors.setAccessControlAllowMethods(Collections.singleton("GET"));
        proxy.setCors(cors);

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setPaths(Collections.emptyMap());
            apiDefinition.setProxy(proxy);
            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
        } catch (Exception e) {
            // ignore
        }

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        String jsonForExport = apiService.exportAsJson(API_ID, ApiSerializer.Version.V_1_15.getVersion(), SystemRole.PRIMARY_OWNER.name());

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportMultipleEndpointGroups-1_15.json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }

    private void shouldConvertAsJsonForExport(ApiSerializer.Version version, String filename) throws TechnicalException, IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, version.getVersion(), SystemRole.PRIMARY_OWNER.name());

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExport" + (filename != null ? "-"+ filename : "") +".json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }

    private void shouldConvertAsJsonWithoutMembers(ApiSerializer.Version version, String filename) throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, version.getVersion(),SystemRole.PRIMARY_OWNER.name(), "members");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutMembers" + (filename != null ? "-"+ filename : "") +".json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }

    private void shouldConvertAsJsonWithoutPages(ApiSerializer.Version version, String filename) throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, version.getVersion(), SystemRole.PRIMARY_OWNER.name(), "pages");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutPages" + (filename != null ? "-"+ filename : "") +".json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }

    private void shouldConvertAsJsonWithoutPlans(ApiSerializer.Version version, String filename) throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, version.getVersion(), SystemRole.PRIMARY_OWNER.name(), "plans");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutPlans" + (filename != null ? "-"+ filename : "") +".json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }
}
