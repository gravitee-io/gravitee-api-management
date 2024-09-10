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
package io.gravitee.rest.api.portal.rest.spring;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.spring.FakeConfiguration;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.PageSourceDomainServiceInMemory;
import inmemory.spring.InMemoryConfiguration;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPolicyValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiEventQueryService;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api.use_case.RollbackApiUseCase;
import io.gravitee.apim.core.application.domain_service.ValidateApplicationSettingsDomainService;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.query_service.AuditMetadataQueryService;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageSourceDomainService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.parameters.domain_service.ParametersDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupPolicyPluginsUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.InitializeSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupHistoryUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UndeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UpdateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.infra.domain_service.api.ApiHostValidatorDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.application.ValidateApplicationSettingsDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.documentation.ValidatePageSourceDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonSpringConfiguration;
import io.gravitee.apim.infra.sanitizer.SanitizerSpringConfiguration;
import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.portal.rest.mapper.AnalyticsMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.LogMapper;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalNotificationMapper;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.mapper.ReferenceMetadataMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.portal.rest.mapper.TicketMapper;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.FetcherService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.HealthCheckService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.QualityMetricsService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.TicketService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(
    {
        UsecaseSpringConfiguration.class,
        FakeConfiguration.class,
        InMemoryConfiguration.class,
        JacksonSpringConfiguration.class,
        SanitizerSpringConfiguration.class,
    }
)
@PropertySource("classpath:/io/gravitee/rest/api/portal/rest/resource/jwt.properties")
public class ResourceContextConfiguration {

    @Bean
    public ApiService apiService() {
        return mock(ApiService.class);
    }

    @Bean
    public ApiSearchService apiSearchService() {
        return mock(ApiSearchService.class);
    }

    @Bean
    public ApiAuthorizationService apiAuthorizationService() {
        return mock(ApiAuthorizationService.class);
    }

    @Bean
    public ApiEntrypointService apiEntrypointService() {
        return mock(ApiEntrypointService.class);
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
    public LogsService logsService() {
        return mock(LogsService.class);
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
    public ApiMetadataService metadataService() {
        return mock(ApiMetadataService.class);
    }

    @Bean
    public PlanService planService() {
        return mock(PlanService.class);
    }

    @Bean
    public PlanSearchService planSearchService() {
        return mock(PlanSearchService.class);
    }

    @Bean
    public SubscriptionService subscriptionService() {
        return mock(SubscriptionService.class);
    }

    @Bean
    public EntrypointService entrypointService() {
        return mock(EntrypointService.class);
    }

    @Bean
    public ApiKeyService apiKeyService() {
        return mock(ApiKeyService.class);
    }

    @Bean
    public AnalyticsService analyticsService() {
        return mock(AnalyticsService.class);
    }

    @Bean
    public PortalNotificationConfigService portalNotificationConfigService() {
        return mock(PortalNotificationConfigService.class);
    }

    @Bean
    public PortalNotificationService portalNotificationService() {
        return mock(PortalNotificationService.class);
    }

    @Bean
    public GenericNotificationConfigService genericNotificationConfigService() {
        return mock(GenericNotificationConfigService.class);
    }

    @Bean
    public CategoryService categoryService() {
        return mock(CategoryService.class);
    }

    @Bean
    public ApiCategoryService apiCategoryService() {
        return mock(ApiCategoryService.class);
    }

    @Bean
    public TicketService ticketService() {
        return mock(TicketService.class);
    }

    @Bean
    public ConfigService configService() {
        return mock(ConfigService.class);
    }

    @Bean
    public ApiMapper apiMapper() {
        return mock(ApiMapper.class);
    }

    @Bean
    public PageMapper pageMapper() {
        return mock(PageMapper.class);
    }

    @Bean
    public PlanMapper planMapper() {
        return mock(PlanMapper.class);
    }

    @Bean
    public RatingMapper ratingMapper() {
        return mock(RatingMapper.class);
    }

    @Bean
    public SubscriptionMapper subscriptionMapper() {
        return mock(SubscriptionMapper.class);
    }

    @Bean
    public KeyMapper keyMapper() {
        return mock(KeyMapper.class);
    }

    @Bean
    public ApplicationMapper applicationMapper() {
        return mock(ApplicationMapper.class);
    }

    @Bean
    public MemberMapper memberMapper() {
        return mock(MemberMapper.class);
    }

    @Bean
    public UserMapper userMapper() {
        return mock(UserMapper.class);
    }

    @Bean
    public LogMapper logMapper() {
        return mock(LogMapper.class);
    }

    @Bean
    public AnalyticsMapper analyticsMapper() {
        return mock(AnalyticsMapper.class);
    }

    @Bean
    public PortalNotificationMapper portalNotificationMapper() {
        return mock(PortalNotificationMapper.class);
    }

    @Bean
    public io.gravitee.rest.api.portal.rest.mapper.CategoryMapper categoryMapper() {
        return mock(CategoryMapper.class);
    }

    @Bean
    public TicketMapper ticketMapper() {
        return mock(TicketMapper.class);
    }

    @Bean
    public ConfigurationMapper configMapper() {
        return mock(ConfigurationMapper.class);
    }

    @Bean
    public IdentityProviderMapper identityProviderMapper() {
        return mock(IdentityProviderMapper.class);
    }

    @Bean
    public HealthCheckService healthCheckService() {
        return mock(HealthCheckService.class);
    }

    @Bean
    public IdentityService identityService() {
        return mock(IdentityService.class);
    }

    @Bean
    public FilteringService filteringService() {
        return mock(FilteringService.class);
    }

    @Bean
    public ApplicationMetadataService applicationMetadataService() {
        return mock(ApplicationMetadataService.class);
    }

    @Bean
    public ReferenceMetadataMapper referenceMetadataMapper() {
        return mock(ReferenceMetadataMapper.class);
    }

    @Bean
    public CustomUserFieldService customUserFieldService() {
        return mock(CustomUserFieldService.class);
    }

    @Bean
    public IdentityProviderActivationService identityProviderActivationService() {
        return mock(IdentityProviderActivationService.class);
    }

    @Bean
    public AuthoritiesProvider authoritiesProvider() {
        return mock(AuthoritiesProvider.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return mock(EnvironmentService.class);
    }

    @Bean
    public AccessControlService accessControlService() {
        return mock(AccessControlService.class);
    }

    @Bean
    public ThemeService themeService() {
        return mock(ThemeService.class);
    }

    @Bean
    public ThemeMapper themeMapper() {
        return mock(ThemeMapper.class);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }

    @Bean
    public CategoryDomainService categoryDomainService() {
        return mock(CategoryDomainService.class);
    }

    @Bean
    public AccessPointQueryService accessPointQueryService() {
        return mock(AccessPointQueryService.class);
    }

    @Bean
    public InstallationTypeDomainService installationTypeService() {
        return mock(InstallationTypeDomainService.class);
    }

    @Bean
    public InstallationAccessQueryService installationAccessService() {
        return mock(InstallationAccessQueryService.class);
    }

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService(
        ApiQueryService apiQueryService,
        InstallationAccessQueryService installationAccessQueryService
    ) {
        return new VerifyApiPathDomainService(apiQueryService, installationAccessQueryService, new ApiHostValidatorDomainServiceImpl());
    }

    @Bean
    public CloseSubscriptionDomainService closeSubscriptionDomainService() {
        return mock(CloseSubscriptionDomainService.class);
    }

    @Bean
    public LicenseManager licenseManager() {
        return mock(LicenseManager.class);
    }

    @Bean
    public GraviteeLicenseDomainService graviteeLicenseDomainService(LicenseManager licenseManager) {
        return new GraviteeLicenseDomainService(licenseManager);
    }

    @Bean
    public ParametersDomainService parametersDomainService() {
        return mock(ParametersDomainService.class);
    }

    @Bean
    public CreateApiDomainService createApiDomainService() {
        return mock(CreateApiDomainService.class);
    }

    @Bean
    public CreatePlanDomainService createPlanDomainService() {
        return mock(CreatePlanDomainService.class);
    }

    @Bean
    public ApiMetadataDomainService apiMetadataDomainService() {
        return mock(ApiMetadataDomainService.class);
    }

    @Bean
    public ApiPolicyValidatorDomainService apiPolicyValidatorDomainService() {
        return mock(ApiPolicyValidatorDomainService.class);
    }

    @Bean
    public PolicyValidationDomainService policyValidationDomainService() {
        return mock(PolicyValidationDomainService.class);
    }

    @Bean
    public PlanSynchronizationService planSynchronizationService() {
        return mock(PlanSynchronizationService.class);
    }

    @Bean
    public UpdateApiDomainService updateApiDomainService() {
        return mock(UpdateApiDomainService.class);
    }

    @Bean
    public SearchAuditDomainService searchAuditDomainService(
        AuditQueryService auditQueryService,
        AuditMetadataQueryService auditMetadataQueryService
    ) {
        return new SearchAuditDomainService(auditQueryService, auditMetadataQueryService);
    }

    @Bean
    public ApiStateDomainService apiStateDomainService() {
        return mock(ApiStateDomainService.class);
    }

    @Bean
    public ApiEventQueryService apiEventQueryService() {
        return mock(ApiEventQueryService.class);
    }

    @Bean
    public ApiMetadataDecoderDomainService apiMetadataDecoderDomainService() {
        return mock(ApiMetadataDecoderDomainService.class);
    }

    @Bean
    public ApiMetadataQueryService apiMetadataQueryService() {
        return mock(ApiMetadataQueryService.class);
    }

    @Bean
    public ValidateApiDomainService validateApiDomainService() {
        return mock(ValidateApiDomainService.class);
    }

    @Bean
    public ApiImportDomainService apiImportDomainService() {
        return mock(ApiImportDomainService.class);
    }

    @Bean
    public RollbackApiUseCase rollbackApiUseCase() {
        return mock(RollbackApiUseCase.class);
    }

    @Bean
    public OAIDomainService oaiDomainService() {
        return mock(OAIDomainService.class);
    }

    @Bean
    public EndpointConnectorPluginDomainService endpointConnectorPluginDomainService() {
        return mock(EndpointConnectorPluginDomainService.class);
    }

    @Bean
    public PageSourceDomainServiceInMemory pageSourceDomainService() {
        return new PageSourceDomainServiceInMemory();
    }

    @Bean
    public CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase() {
        return mock(CreateSharedPolicyGroupUseCase.class);
    }

    @Bean
    public GetSharedPolicyGroupUseCase getSharedPolicyGroupUseCase() {
        return mock(GetSharedPolicyGroupUseCase.class);
    }

    @Bean
    public UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase() {
        return mock(UpdateSharedPolicyGroupUseCase.class);
    }

    @Bean
    public SearchSharedPolicyGroupUseCase searchSharedPolicyGroupUseCase() {
        return mock(SearchSharedPolicyGroupUseCase.class);
    }

    @Bean
    public DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase() {
        return mock(DeleteSharedPolicyGroupUseCase.class);
    }

    @Bean
    public GetSharedPolicyGroupPolicyPluginsUseCase getSharedPolicyGroupPolicyPluginsUseCase() {
        return mock(GetSharedPolicyGroupPolicyPluginsUseCase.class);
    }

    @Bean
    public DeploySharedPolicyGroupUseCase deploySharedPolicyGroupUseCase() {
        return mock(DeploySharedPolicyGroupUseCase.class);
    }

    @Bean
    public UndeploySharedPolicyGroupUseCase undeploySharedPolicyGroupUseCase() {
        return mock(UndeploySharedPolicyGroupUseCase.class);
    }

    @Bean
    public SearchSharedPolicyGroupHistoryUseCase searchSharedPolicyGroupHistoryUseCase() {
        return mock(SearchSharedPolicyGroupHistoryUseCase.class);
    }

    @Bean
    public CRDMembersDomainServiceInMemory crdMembersDomainService() {
        return new CRDMembersDomainServiceInMemory();
    }

    @Bean
    public ApplicationCrudServiceInMemory applicationCrudService() {
        return new ApplicationCrudServiceInMemory();
    }

    @Bean
    public ApplicationRepository applicationRepository() {
        return mock(ApplicationRepository.class);
    }

    @Bean
    public ValidateApplicationSettingsDomainService validateApplicationSettingsDomainService(
        ApplicationRepository applicationRepository,
        ApplicationTypeService applicationTypeService,
        ParameterService parameterService
    ) {
        return new ValidateApplicationSettingsDomainServiceImpl(applicationRepository, applicationTypeService, parameterService);
    }

    @Bean
    public PolicyPluginCrudService policyPluginCrudService() {
        return mock(PolicyPluginCrudService.class);
    }

    @Bean
    public InitializeSharedPolicyGroupUseCase initializeSharedPolicyGroupUseCase() {
        return mock(InitializeSharedPolicyGroupUseCase.class);
    }

    @Bean
    public ValidatePageSourceDomainService validatePageSourceDomainService() {
        return new ValidatePageSourceDomainServiceImpl();
    }
}
