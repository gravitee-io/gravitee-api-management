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
package io.gravitee.rest.api.management.rest.resource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.AlertAnalyticsService;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.DebugApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.FetcherService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.QualityMetricsService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.TicketService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Autowired
    protected ApiGroupService apiGroupService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ApplicationTypeService applicationTypeService;

    @Autowired
    protected PolicyService policyService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected FetcherService fetcherService;

    @Autowired
    protected SwaggerService swaggerService;

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    @Qualifier("oauth2")
    protected AuthenticationProvider authenticationProvider;

    @Autowired
    protected PageService pageService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected RatingService ratingService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected NotifierService notifierService;

    @Autowired
    protected QualityMetricsService qualityMetricsService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    protected TagService tagService;

    @Autowired
    protected ApiMetadataService apiMetadataService;

    @Autowired
    protected ApplicationMetadataService applicationMetadataService;

    @Autowired
    protected ParameterService parameterService;

    @Autowired
    protected VirtualHostService virtualHostService;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected PolicyOperationVisitorManager policyOperationVisitorManager;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected OrganizationService organizationService;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected CustomUserFieldService customUserFieldService;

    @Autowired
    protected AuthoritiesProvider authoritiesProvider;

    @Autowired
    protected DictionaryService dictionaryService;

    @Autowired
    protected TicketService ticketService;

    @Autowired
    protected PlanService planService;

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected IdentityProviderActivationService identityProviderActivationService;

    @Autowired
    protected IdentityProviderService identityProviderService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected ClientRegistrationService clientRegistrationService;

    @Autowired
    protected SpelService spelService;

    @Autowired
    protected AnalyticsService analyticsService;

    @Autowired
    protected InstallationService installationService;

    @Autowired
    protected SearchEngineService searchEngineService;

    @Autowired
    protected GroupRepository groupRepository;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    protected EventManager eventManager;

    @Autowired
    protected PromotionService promotionService;

    @Autowired
    protected ApiDuplicatorService apiDuplicatorService;

    @Autowired
    protected DebugApiService debugApiService;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected ApiExportService apiExportService;

    @Autowired
    protected ApiConverter apiConverter;

    @Autowired
    protected AlertService alertService;

    @Autowired
    protected AlertAnalyticsService alertAnalyticsService;

    @Autowired
    protected JsonPatchService jsonPatchService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AuditService auditService;

    @Before
    public void setUp() throws Exception {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Configuration
    @PropertySource("classpath:/io/gravitee/rest/api/management/rest/resource/jwt.properties")
    static class ContextConfiguration {

        @Bean
        public ApiService apiService() {
            return mock(ApiService.class);
        }

        @Bean
        public io.gravitee.rest.api.service.v4.ApiService apiServiceV4() {
            return mock(io.gravitee.rest.api.service.v4.ApiService.class);
        }

        @Bean
        public ApiGroupService apiGroupService() {
            return mock(ApiGroupService.class);
        }

        @Bean
        public ApplicationService applicationService() {
            return mock(ApplicationService.class);
        }

        @Bean
        public ApplicationTypeService applicationTypeService() {
            return mock(ApplicationTypeService.class);
        }

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public PolicyService policyService() {
            return mock(PolicyService.class);
        }

        @Bean
        public FetcherService fetcherService() {
            return mock(FetcherService.class);
        }

        @Bean
        public SwaggerService swaggerService() {
            return mock(SwaggerService.class);
        }

        @Bean
        public MembershipService membershipService() {
            return mock(MembershipService.class);
        }

        @Bean
        public RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean("oauth2")
        public AuthenticationProvider authenticationProvider() {
            return mock(AuthenticationProvider.class);
        }

        @Bean
        public PageService pageService() {
            return mock(PageService.class);
        }

        @Bean
        public GroupService groupService() {
            return mock(GroupService.class);
        }

        @Bean
        public RatingService ratingService() {
            return mock(RatingService.class);
        }

        @Bean
        public PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        public NotifierService notifierService() {
            return mock(NotifierService.class);
        }

        @Bean
        public TopApiService topApiService() {
            return mock(TopApiService.class);
        }

        @Bean
        public CookieGenerator jwtCookieGenerator() {
            return mock(CookieGenerator.class);
        }

        @Bean
        public TaskService taskService() {
            return mock(TaskService.class);
        }

        @Bean
        public QualityMetricsService qualityMetricsService() {
            return mock(QualityMetricsService.class);
        }

        @Bean
        public MessageService messageService() {
            return mock(MessageService.class);
        }

        @Bean
        public SocialIdentityProviderService socialIdentityProviderService() {
            return mock(SocialIdentityProviderService.class);
        }

        @Bean
        public TagService tagService() {
            return mock(TagService.class);
        }

        @Bean
        public MediaService mediaService() {
            return mock(MediaService.class);
        }

        @Bean
        public ParameterService parameterService() {
            return mock(ParameterService.class);
        }

        @Bean
        public ApiMetadataService apiMetadataService() {
            return mock(ApiMetadataService.class);
        }

        @Bean
        public ApplicationMetadataService applicationMetadataService() {
            return mock(ApplicationMetadataService.class);
        }

        @Bean
        public VirtualHostService virtualHostService() {
            return mock(VirtualHostService.class);
        }

        @Bean
        public CategoryService categoryService() {
            return mock(CategoryService.class);
        }

        @Bean
        public PolicyOperationVisitorManager policyOperationVisitorManager() {
            return mock(PolicyOperationVisitorManager.class);
        }

        @Bean
        public ConfigService configService() {
            return mock(ConfigService.class);
        }

        @Bean
        public OrganizationService organizationService() {
            return mock(OrganizationService.class);
        }

        @Bean
        public EnvironmentService environmentService() {
            return mock(EnvironmentService.class);
        }

        @Bean
        public CustomUserFieldService customUserFieldService() {
            return mock(CustomUserFieldService.class);
        }

        @Bean
        public AuthoritiesProvider authoritiesProvider() {
            return mock(AuthoritiesProvider.class);
        }

        @Bean
        public DictionaryService dictionaryService() {
            return mock(DictionaryService.class);
        }

        @Bean
        public TicketService ticketService() {
            return mock(TicketService.class);
        }

        @Bean
        public PlanService planService() {
            return mock(PlanService.class);
        }

        @Bean
        public SubscriptionService subscriptionService() {
            return mock(SubscriptionService.class);
        }

        @Bean
        public ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        public IdentityProviderService identityProviderService() {
            return mock(IdentityProviderService.class);
        }

        @Bean
        public ClientRegistrationService clientRegistrationService() {
            return mock(ClientRegistrationService.class);
        }

        @Bean
        public IdentityProviderActivationService identityProviderActivationService() {
            return mock(IdentityProviderActivationService.class);
        }

        @Bean
        public FlowService flowService() {
            return mock(FlowService.class);
        }

        @Bean
        public SpelService spelService() {
            return mock(SpelService.class);
        }

        @Bean
        public AnalyticsService analyticsService() {
            return mock(AnalyticsService.class);
        }

        @Bean
        public InstallationService installationService() {
            return mock(InstallationService.class);
        }

        @Bean
        public SearchEngineService searchEngineService() {
            return mock(SearchEngineService.class);
        }

        @Bean
        public GroupRepository groupRepository() {
            return mock(GroupRepository.class);
        }

        @Bean
        public AccessControlService accessControlService() {
            return mock(AccessControlService.class);
        }

        @Bean
        public EventManager eventManager() {
            return mock(EventManager.class);
        }

        @Bean
        public PromotionService promotionService() {
            return mock(PromotionService.class);
        }

        @Bean
        public ApiDuplicatorService apiDuplicatorService() {
            return mock(ApiDuplicatorService.class);
        }

        @Bean
        public DebugApiService debugApiService() {
            return mock(DebugApiService.class);
        }

        @Bean
        public TokenService tokenService() {
            return mock(TokenService.class);
        }

        @Bean
        public ApiExportService apiExportService() {
            return mock(ApiExportService.class);
        }

        @Bean
        public ApiConverter apiConverter() {
            return mock(ApiConverter.class);
        }

        @Bean
        public AlertService alertService() {
            return mock(AlertService.class);
        }

        @Bean
        public AlertAnalyticsService alertAnalyticsService() {
            return mock(AlertAnalyticsService.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return mock(GraviteeMapper.class);
        }

        @Bean
        public JsonPatchService jsonPatchService() {
            return mock(JsonPatchService.class);
        }

        @Bean
        public AuditService auditService() {
            return mock(AuditService.class);
        }
    }
}
