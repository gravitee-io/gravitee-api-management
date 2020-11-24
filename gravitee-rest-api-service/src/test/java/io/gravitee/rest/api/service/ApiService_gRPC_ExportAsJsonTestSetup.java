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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.GrpcEndpoint;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.jackson.ser.api.*;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiService_gRPC_ExportAsJsonTestSetup {

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
    @Mock
    private ParameterService parameterService;
    @Mock
    private ApiMetadataService apiMetadataService;
    @Mock
    private MediaService mediaService;

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));

        when(parameterService.find(Key.PORTAL_ENTRYPOINT, ParameterReferenceType.ENVIRONMENT)).thenReturn(Key.PORTAL_ENTRYPOINT.defaultValue());
        // register API Entity serializers
        when(applicationContext.getBean(MembershipService.class)).thenReturn(membershipService);
        when(applicationContext.getBean(PlanService.class)).thenReturn(planService);
        when(applicationContext.getBean(PageService.class)).thenReturn(pageService);
        when(applicationContext.getBean(GroupService.class)).thenReturn(groupService);
        when(applicationContext.getBean(UserService.class)).thenReturn(userService);
        when(applicationContext.getBean(ApiMetadataService.class)).thenReturn(apiMetadataService);
        when(applicationContext.getBean(MediaService.class)).thenReturn(mediaService);
        ApiCompositeSerializer apiCompositeSerializer = new ApiCompositeSerializer();
        ApiSerializer apiDefaultSerializer = new ApiDefaultSerializer();
        apiDefaultSerializer.setApplicationContext(applicationContext);

        //V_1_15
        ApiSerializer apiPrior115VersionSerializer = new Api1_15VersionSerializer();
        apiPrior115VersionSerializer.setApplicationContext(applicationContext);
        //V_1_20
        ApiSerializer apiPrior120VersionSerializer = new Api1_20VersionSerializer();
        apiPrior120VersionSerializer.setApplicationContext(applicationContext);
        //V_1_25
        ApiSerializer apiPrior125VersionSerializer = new Api1_25VersionSerializer();
        apiPrior125VersionSerializer.setApplicationContext(applicationContext);
        //V_3_0
        ApiSerializer apiPrior30VersionSerializer = new Api3_0VersionSerializer();
        apiPrior30VersionSerializer.setApplicationContext(applicationContext);

        apiCompositeSerializer.setSerializers(Arrays.asList(apiDefaultSerializer, apiPrior115VersionSerializer, apiPrior120VersionSerializer, apiPrior125VersionSerializer, apiPrior30VersionSerializer));
        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiCompositeSerializer);
        objectMapper.registerModule(module);

        Api api = new Api();
        api.setId(API_ID);
        String definition = null;
        try {
            definition = objectMapper.writeValueAsString(buildApiDefinition(api));
        } catch (JsonProcessingException e) {
        }
        api.setDefinition(definition);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PageEntity page = new PageEntity();
        page.setName("My Title");
        page.setOrder(1);
        page.setType(PageType.MARKDOWN.toString());
        page.setContent("Read the doc");
        PageEntity asideFolder = new PageEntity();
        asideFolder.setName("Aside");
        asideFolder.setOrder(1);
        asideFolder.setPublished(true);
        asideFolder.setType(PageType.SYSTEM_FOLDER.toString());
        when(pageService.search(any(), eq(true))).thenReturn(Arrays.asList(page, asideFolder));

        RoleEntity poRole = new RoleEntity();
        poRole.setName("PRIMARY_OWNER");
        poRole.setId("API_PRIMARY_OWNER");
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("johndoe");
        membership.setRoleId("API_PRIMARY_OWNER");
        when(membershipService.getPrimaryOwner(eq(MembershipReferenceType.API), eq(API_ID)))
            .thenReturn(membership);

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(membership.getMemberId());
        memberEntity.setRoles(Collections.singletonList(poRole));
        when(membershipService.getMembersByReference(eq(MembershipReferenceType.API), eq(API_ID)))
            .thenReturn(Collections.singleton(memberEntity));
        UserEntity userEntity = new UserEntity();
        userEntity.setId(memberEntity.getId());
        userEntity.setSource(userEntity.getId() + "-source");
        userEntity.setSourceId(userEntity.getId() + "-sourceId");
        when(userService.findById(memberEntity.getId())).thenReturn(userEntity);

        api.setGroups(Collections.singleton("my-group"));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId("my-group");
        groupEntity.setName("My Group");
        when(groupService.findByIds(api.getGroups())).thenReturn(Collections.singleton(groupEntity));

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-id");
        publishedPlan.setApi(API_ID);
        publishedPlan.setDescription("free plan");
        publishedPlan.setType(PlanType.API);
        publishedPlan.setSecurity(PlanSecurityType.API_KEY);
        publishedPlan.setValidation(PlanValidationType.AUTO);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        Map<String, Path> paths = new HashMap<>();
        Path path = new Path();
        path.setPath("/");
        Rule rule = new Rule();
        rule.setEnabled(true);
        rule.setMethods(Sets.newSet(HttpMethod.GET));
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
        closedPlan.setApi(API_ID);
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
        ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setApiId(API_ID);
        apiMetadataEntity.setKey("metadata-key");
        apiMetadataEntity.setName("metadata-name");
        apiMetadataEntity.setValue("metadata-value");
        apiMetadataEntity.setDefaultValue("metadata-default-value");
        apiMetadataEntity.setFormat(MetadataFormat.STRING);
        when(apiMetadataService.findAllByApi(API_ID)).thenReturn(Collections.singletonList(apiMetadataEntity));
    }

    protected io.gravitee.definition.model.Api buildApiDefinition(Api api) {
        api.setDescription("Gravitee.io");
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
        Endpoint endpoint = new HttpEndpoint("default", "http://test");
        Endpoint endPointGrpc = new GrpcEndpoint("EndPoint GRPC", "grpc://localhost:8888");
        LinkedHashSet endpoints = new LinkedHashSet();
        endpoints.add(endpoint);
        endpoints.add(endPointGrpc);
        endpointGroup.setEndpoints(endpoints);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);
        endpointGroup.setLoadBalancer(loadBalancer);
        proxy.setGroups(Collections.singleton(endpointGroup));
        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setPaths(Collections.emptyMap());
        apiDefinition.setProxy(proxy);
        ResponseTemplates responseTemplates = new ResponseTemplates();
        ResponseTemplate responseTemplate = new ResponseTemplate();
        responseTemplate.setStatusCode(400);
        responseTemplate.setBody("{\"bad\":\"news\"}");
        responseTemplates.setTemplates(Collections.singletonMap("*/*", responseTemplate));
        apiDefinition.setResponseTemplates(Collections.singletonMap("API_KEY_MISSING", responseTemplates));
        return apiDefinition;
    }


    protected void shouldConvertAsJsonForExport(ApiSerializer.Version version, String filename) throws TechnicalException, IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, version.getVersion(), SystemRole.PRIMARY_OWNER.name());

        URL url = Resources.getResource("io/gravitee/rest/api/management/service/export-gRPC-convertAsJsonForExport" + (filename != null ? "-" + filename : "") + ".json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(expectedJson)).isEqualTo(objectMapper.readTree(jsonForExport));
    }
}
