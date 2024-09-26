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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.jackson.ser.api.*;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.springframework.context.ApplicationContext;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiExportService_gRPC_ExportAsJsonTestSetup {

    private static final String API_ID = "id-api";

    @InjectMocks
    protected ApiExportServiceImpl apiExportService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private PageConverter pageConverter;

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
    private MediaService mediaService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private RoleService roleService;

    @Before
    public void setUp() throws TechnicalException, JsonProcessingException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );

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

        apiCompositeSerializer.setSerializers(Arrays.asList(apiDefaultSerializer));
        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiCompositeSerializer);
        objectMapper.registerModule(module);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setCrossId("test-api-cross-id");
        apiEntity.setDescription("Gravitee.io");
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
        Endpoint endPointGrpc = new Endpoint("grpc", "EndPoint GRPC", "grpc://localhost:8888");
        HttpClientOptions httpClientOptions = new HttpClientOptions();
        httpClientOptions.setVersion(ProtocolVersion.HTTP_2);
        httpClientOptions.setClearTextUpgrade(true);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("http", httpClientOptions);

        endPointGrpc.setConfiguration(objectMapper.writeValueAsString(configuration));

        LinkedHashSet<Endpoint> endpoints = new LinkedHashSet<>();
        endpoints.add(endpoint);
        endpoints.add(endPointGrpc);
        endpointGroup.setEndpoints(endpoints);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);
        proxy.setGroups(Collections.singleton(endpointGroup));

        ResponseTemplate responseTemplate = new ResponseTemplate();
        responseTemplate.setStatusCode(400);
        responseTemplate.setBody("{\"bad\":\"news\"}");
        responseTemplate.setPropagateErrorKeyToLogs(false);
        apiEntity.setResponseTemplates(Collections.singletonMap("API_KEY_MISSING", Collections.singletonMap("*/*", responseTemplate)));

        apiEntity.setPaths(null);
        apiEntity.setProxy(proxy);

        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        primaryOwnerEntity.setDisplayName("johndoe-sourceId");
        primaryOwnerEntity.setId("johndoe");
        primaryOwnerEntity.setType(MembershipMemberType.USER.toString());
        apiEntity.setPrimaryOwner(primaryOwnerEntity);

        when(apiService.findById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(prepareApiEntity(apiEntity));

        PageEntity folder = new PageEntity();
        folder.setName("My Folder");
        folder.setOrder(1);
        folder.setType(PageType.FOLDER.toString());
        folder.setVisibility(Visibility.PUBLIC);
        PageEntity markdownPage = new PageEntity();
        markdownPage.setName("My Title");
        markdownPage.setOrder(1);
        markdownPage.setType(PageType.MARKDOWN.toString());
        markdownPage.setContent("Read the doc");
        markdownPage.setVisibility(Visibility.PUBLIC);
        markdownPage.setAccessControls(Set.of(new AccessControlEntity("my-group", "GROUP")));
        PageEntity asideFolder = new PageEntity();
        asideFolder.setName("Aside");
        asideFolder.setOrder(1);
        asideFolder.setPublished(true);
        asideFolder.setType(PageType.SYSTEM_FOLDER.toString());
        asideFolder.setVisibility(Visibility.PUBLIC);
        PageEntity swaggerPage = new PageEntity();
        swaggerPage.setName("My Swagger");
        swaggerPage.setOrder(1);
        swaggerPage.setType(PageType.SWAGGER.toString());
        swaggerPage.setContent("Read the doc");
        swaggerPage.setVisibility(Visibility.PUBLIC);
        PageEntity linkPage = new PageEntity();
        linkPage.setName("My Link");
        linkPage.setOrder(1);
        linkPage.setType(PageType.LINK.toString());
        linkPage.setContent("Read the doc");
        linkPage.setVisibility(Visibility.PUBLIC);
        PageEntity translationPage = new PageEntity();
        translationPage.setName("My Translation");
        translationPage.setOrder(1);
        translationPage.setType(PageType.TRANSLATION.toString());
        translationPage.setContent("Lire la documentation");
        translationPage.setVisibility(Visibility.PUBLIC);
        PageEntity markdownTemplatePage = new PageEntity();
        markdownTemplatePage.setName("My Template");
        markdownTemplatePage.setOrder(1);
        markdownTemplatePage.setType(PageType.MARKDOWN_TEMPLATE.toString());
        markdownTemplatePage.setContent("Read the doc");
        markdownTemplatePage.setVisibility(Visibility.PUBLIC);
        PageEntity asciidocPage = new PageEntity();
        asciidocPage.setName("My asciidoc");
        asciidocPage.setOrder(1);
        asciidocPage.setType(PageType.ASCIIDOC.toString());
        asciidocPage.setContent("Read the asciidoc");
        asciidocPage.setVisibility(Visibility.PUBLIC);
        when(pageService.search(eq(GraviteeContext.getCurrentEnvironment()), any(), eq(true)))
            .thenReturn(
                Arrays.asList(folder, markdownPage, swaggerPage, asideFolder, linkPage, translationPage, markdownTemplatePage, asciidocPage)
            );

        RoleEntity poRole = new RoleEntity();
        poRole.setName("PRIMARY_OWNER");
        poRole.setId("API_PRIMARY_OWNER");
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("johndoe");
        membership.setMemberType(MembershipMemberType.USER);
        membership.setRoleId("API_PRIMARY_OWNER");

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

        apiEntity.setGroups(Collections.singleton("my-group"));
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId("my-group");
        groupEntity.setName("My Group");
        when(groupService.findByIds(apiEntity.getGroups())).thenReturn(Collections.singleton(groupEntity));
        when(groupService.findById(GraviteeContext.getExecutionContext(), "my-group")).thenReturn(groupEntity);

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-id");
        publishedPlan.setCrossId("test-plan-cross-id");
        publishedPlan.setApi(API_ID);
        publishedPlan.setDescription("free plan");
        publishedPlan.setType(PlanType.API);
        publishedPlan.setSecurity(PlanSecurityType.API_KEY);
        publishedPlan.setValidation(PlanValidationType.AUTO);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        publishedPlan.setExcludedGroups(List.of("my-group"));
        Map<String, List<Rule>> paths = new HashMap<>();
        Rule rule = new Rule();
        rule.setEnabled(true);
        rule.setMethods(Sets.newSet(HttpMethod.GET));
        Policy policy = new Policy();
        policy.setName("rate-limit");
        String ls = System.lineSeparator();
        policy.setConfiguration(
            "{" +
            ls +
            "          \"rate\": {" +
            ls +
            "            \"limit\": 1," +
            ls +
            "            \"periodTime\": 1," +
            ls +
            "            \"periodTimeUnit\": \"SECONDS\"" +
            ls +
            "          }" +
            ls +
            "        }"
        );
        rule.setPolicy(policy);
        paths.put("/", Collections.singletonList(rule));
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
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(set);
        ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setApiId(API_ID);
        apiMetadataEntity.setKey("metadata-key");
        apiMetadataEntity.setName("metadata-name");
        apiMetadataEntity.setValue("metadata-value");
        apiMetadataEntity.setDefaultValue("metadata-default-value");
        apiMetadataEntity.setFormat(MetadataFormat.STRING);
        when(apiMetadataService.findAllByApi(API_ID)).thenReturn(Collections.singletonList(apiMetadataEntity));
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    protected ApiEntity prepareApiEntity(ApiEntity apiEntity) {
        return apiEntity;
    }

    protected void shouldConvertAsJsonForExport(ApiSerializer.Version version, String filename) throws IOException {
        String jsonForExport = apiExportService.exportAsJson(
            GraviteeContext.getExecutionContext(),
            API_ID,
            version.getVersion(),
            SystemRole.PRIMARY_OWNER.name()
        );

        URL url = Resources.getResource(
            "io/gravitee/rest/api/management/service/export-gRPC-convertAsJsonForExport" +
            (filename != null ? "-" + filename : "") +
            ".json"
        );
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }
}
