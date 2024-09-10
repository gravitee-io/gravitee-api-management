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
package io.gravitee.rest.api.management.rest.spring;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
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
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.infra.domain_service.api.ApiHostValidatorDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.application.ValidateApplicationSettingsDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonSpringConfiguration;
import io.gravitee.apim.infra.sanitizer.SanitizerSpringConfiguration;
import io.gravitee.apim.infra.spring.CoreServiceSpringConfiguration;
import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.AlertAnalyticsService;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiCRDService;
import io.gravitee.rest.api.service.ApiDefinitionContextService;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApiValidationService;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.FetcherService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.LogsService;
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
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.TicketService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(
    {
        InMemoryConfiguration.class,
        FakeConfiguration.class,
        CoreServiceSpringConfiguration.class,
        UsecaseSpringConfiguration.class,
        JacksonSpringConfiguration.class,
        SanitizerSpringConfiguration.class,
    }
)
@PropertySource("classpath:/io/gravitee/rest/api/management/rest/resource/jwt.properties")
public class ResourceContextConfiguration {

    @Bean
    public ApiService apiService() {
        return mock(ApiService.class);
    }

    @Bean
    public ApiCRDService apiCRDService() {
        return mock(ApiCRDService.class);
    }

    @Bean
    public ApiValidationService apiValidationService() {
        return mock(ApiValidationService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiService apiServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiSearchService apiSearchServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiSearchService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiAuthorizationService apiAuthorizationServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiAuthorizationService.class);
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
    public PlanSearchService planSearchService() {
        return mock(PlanSearchService.class);
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
    public CategoryMapper categoryMapper() {
        return mock(CategoryMapper.class);
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

    @Bean
    public WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }

    @Bean
    public LogsService logsService() {
        return mock(LogsService.class);
    }

    @Bean
    public ApiDefinitionContextService definitionContextService() {
        return mock(ApiDefinitionContextService.class);
    }

    @Bean
    public ThemeService themeService() {
        return mock(ThemeService.class);
    }

    @Bean
    public CategoryDomainService categoryDomainService() {
        return mock(CategoryDomainService.class);
    }

    @Bean
    public ResourceService resourceService() {
        return mock(ResourceService.class);
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
    public LicenseManager licenseManager() {
        return mock(LicenseManager.class);
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
    @Primary
    public AcceptSubscriptionUseCase spiedAcceptSubscriptionUseCase(AcceptSubscriptionUseCase usecase) {
        return spy(usecase);
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
}
