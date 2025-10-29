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
package io.gravitee.rest.api.management.v2.rest.spring;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.spring.FakeConfiguration;
import inmemory.ApiCRDExportDomainServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiExposedEntrypointDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.GroupCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.NewtAIProviderInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageSourceDomainServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import inmemory.spring.InMemoryConfiguration;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiEventQueryService;
import io.gravitee.apim.core.api.use_case.ExportApiUseCase;
import io.gravitee.apim.core.api.use_case.GetApiDefinitionUseCase;
import io.gravitee.apim.core.api.use_case.GetExposedEntrypointsUseCase;
import io.gravitee.apim.core.api.use_case.RollbackApiUseCase;
import io.gravitee.apim.core.api.use_case.ValidateApiCRDUseCase;
import io.gravitee.apim.core.apim.service_provider.ApimProductInfo;
import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.domain_service.ValidateApplicationSettingsDomainService;
import io.gravitee.apim.core.application.use_case.ValidateApplicationCRDUseCase;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.query_service.AuditMetadataQueryService;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.use_case.CreateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.DeleteClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.GetClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.SearchClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterGroupsUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.members.AddClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.DeleteClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterMembersUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterPermissionsUseCase;
import io.gravitee.apim.core.cluster.use_case.members.TransferClusterOwnershipUseCase;
import io.gravitee.apim.core.cluster.use_case.members.UpdateClusterMemberUseCase;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageAccessControlsDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageSourceDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.group.crud_service.GroupCrudService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupCRDDomainService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.group.use_case.ImportGroupCRDUseCase;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.newtai.service_provider.NewtAIProvider;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.core.promotion.use_case.CreatePromotionUseCase;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupPolicyPluginsUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.ImportSharedPolicyGroupCRDCRDUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.InitializeSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupHistoryUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UndeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UpdateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.specgen.use_case.SpecGenRequestUseCase;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDSpecDomainService;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.DeleteSubscriptionSpecUseCase;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.apim.infra.adapter.SubscriptionAdapterImpl;
import io.gravitee.apim.infra.domain_service.application.ValidateApplicationSettingsDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.documentation.ValidatePageSourceDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.group.ValidateGroupCRDDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.permission.PermissionDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.domain_service.subscription.SubscriptionCRDSpecDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonSpringConfiguration;
import io.gravitee.apim.infra.query_service.gateway.InstanceQueryServiceLegacyWrapper;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.impl.configuration.application.ApplicationTypeServiceImpl;
import io.gravitee.rest.api.service.v4.ApiDuplicateService;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.env.MockEnvironment;

@Configuration
@Import({ UsecaseSpringConfiguration.class, JacksonSpringConfiguration.class, InMemoryConfiguration.class, FakeConfiguration.class })
@PropertySource("classpath:/io/gravitee/rest/api/management/v2/rest/resource/jwt.properties")
public class ResourceContextConfiguration {

    @Bean
    public ApiRepository apiRepository() {
        return mock(ApiRepository.class);
    }

    @Bean
    public ApplicationRepository applicationRepository() {
        return mock(ApplicationRepository.class);
    }

    @Bean
    public ApiService apiService() {
        return mock(ApiService.class);
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
    public io.gravitee.rest.api.service.v4.ApiStateService apiStateServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiStateService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiImagesService apiImageService() {
        return mock(io.gravitee.rest.api.service.v4.ApiImagesService.class);
    }

    @Bean
    public ExportApiUseCase exportApiUseCase() {
        return mock(ExportApiUseCase.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiAuthorizationService apiAuthorizationServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiAuthorizationService.class);
    }

    @Bean
    public MembershipService membershipService() {
        return mock(MembershipService.class);
    }

    @Bean
    public RoleService roleService() {
        return mock(RoleService.class);
    }

    @Bean
    public GroupService groupService() {
        return mock(GroupService.class);
    }

    @Bean
    public PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    @Bean
    public ParameterService parameterService() {
        return mock(ParameterService.class);
    }

    @Bean
    public PlanService planServiceV4() {
        return mock(PlanService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.PlanService planService() {
        return mock(io.gravitee.rest.api.service.PlanService.class);
    }

    @Bean
    public PlanSearchService planSearchService() {
        return mock(PlanSearchService.class);
    }

    @Bean
    public EntrypointConnectorPluginService entrypointConnectorPluginService() {
        return mock(EntrypointConnectorPluginService.class);
    }

    @Bean
    public CategoryDomainService categoryDomainService() {
        return mock(CategoryDomainService.class);
    }

    @Bean
    public EndpointConnectorPluginService endpointConnectorPluginService() {
        return mock(EndpointConnectorPluginService.class);
    }

    @Bean
    public PolicyPluginService policyPluginService() {
        return mock(PolicyPluginService.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return mock(EnvironmentService.class);
    }

    @Bean
    public OrganizationService organizationService() {
        return mock(OrganizationService.class);
    }

    @Bean
    public ApiMetadataService apiMetadataService() {
        return mock(ApiMetadataService.class);
    }

    @Bean
    public PageService pageService() {
        return mock(PageService.class);
    }

    @Bean
    public MediaService mediaService() {
        return mock(MediaService.class);
    }

    @Bean
    public WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }

    @Bean
    public SubscriptionService subscriptionService() {
        return mock(SubscriptionService.class);
    }

    @Bean
    public ApplicationService applicationService() {
        return mock(ApplicationService.class);
    }

    @Bean
    public ApiKeyService apiKeyService() {
        return mock(ApiKeyService.class);
    }

    @Bean
    public UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    public ApiLicenseService apiLicenseService() {
        return mock(ApiLicenseService.class);
    }

    @Bean
    public ApiWorkflowStateService apiWorkflowStateService() {
        return mock(ApiWorkflowStateService.class);
    }

    @Bean
    public ApiDuplicatorService apiDuplicatorService() {
        return mock(ApiDuplicatorService.class);
    }

    @Bean
    public ApiDuplicateService apiDuplicateService() {
        return mock(ApiDuplicateService.class);
    }

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService() {
        return mock(VerifyApiPathDomainService.class);
    }

    @Bean
    public ValidatePlanDomainService validatePlanDomainService() {
        return mock(ValidatePlanDomainService.class);
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
    public PolicyValidationDomainService policyDomainService() {
        return mock(PolicyValidationDomainService.class);
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
    public ValidateApiDomainService validateApiDomainService() {
        return mock(ValidateApiDomainService.class);
    }

    @Bean
    @Primary
    public AcceptSubscriptionUseCase spiedAcceptSubscriptionUseCase(AcceptSubscriptionUseCase usecase) {
        return spy(usecase);
    }

    @Bean
    @Primary
    public RejectSubscriptionUseCase spiedRejectSubscriptionUseCase(RejectSubscriptionUseCase usecase) {
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
    public GetApiDefinitionUseCase getApiDefinitionUseCase() {
        return mock(GetApiDefinitionUseCase.class);
    }

    @Bean
    public ApiCRDExportDomainServiceInMemory apiCRDExportDomainService() {
        return new ApiCRDExportDomainServiceInMemory();
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
    public GroupQueryServiceInMemory groupQueryService() {
        return new GroupQueryServiceInMemory();
    }

    @Bean
    public ParametersQueryService parametersQueryService() {
        return new ParametersQueryServiceInMemory();
    }

    @Bean
    public PageCrudService pageCrudService() {
        return new PageCrudServiceInMemory();
    }

    @Bean
    public CRDMembersDomainServiceInMemory crdMembersDomainService() {
        return new CRDMembersDomainServiceInMemory();
    }

    @Bean
    public ValidateApiCRDUseCase validateApiCRDUseCase(
        CategoryQueryServiceInMemory categoryQueryService,
        UserDomainServiceInMemory userDomainService,
        VerifyApiPathDomainService verifyApiPathDomainService,
        GroupQueryService groupQueryService,
        ValidateResourceDomainService validateResourceDomainService,
        ValidatePageSourceDomainService validatePageSourceDomainService,
        ValidatePageAccessControlsDomainService validatePageAccessControlsDomainService,
        DocumentationValidationDomainService validationDomainService,
        RoleQueryServiceInMemory roleQueryService,
        ApiQueryServiceInMemory apiQueryService,
        ParametersQueryService parametersQueryService,
        PolicyValidationDomainService policyValidationDomainService,
        PageCrudService pageCrudService
    ) {
        ValidateGroupsDomainService groupsValidator = new ValidateGroupsDomainService(groupQueryService);
        return new ValidateApiCRDUseCase(
            new ValidateApiCRDDomainService(
                new ValidateCategoryIdsDomainService(categoryQueryService),
                verifyApiPathDomainService,
                new VerifyApiHostsDomainService(apiQueryService),
                new ValidateCRDMembersDomainService(userDomainService, roleQueryService),
                groupsValidator,
                validateResourceDomainService,
                new ValidatePagesDomainService(
                    validatePageSourceDomainService,
                    validatePageAccessControlsDomainService,
                    validationDomainService
                ),
                new ValidatePlanDomainService(
                    new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService)
                ),
                new ValidatePortalNotificationDomainService(groupsValidator)
            )
        );
    }

    @Bean
    public ValidateApplicationCRDUseCase validateApplicationCRDUseCase(
        GroupQueryService groupQueryService,
        UserDomainService userDomainService,
        RoleQueryServiceInMemory roleQueryService,
        ValidateApplicationSettingsDomainService validateApplicationSettingsDomainService
    ) {
        return new ValidateApplicationCRDUseCase(
            new ValidateApplicationCRDDomainService(
                new ValidateGroupsDomainService(groupQueryService),
                new ValidateCRDMembersDomainService(userDomainService, roleQueryService),
                validateApplicationSettingsDomainService
            )
        );
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
    public ConfigService configService() {
        return mock(ConfigService.class);
    }

    @Bean
    public ApplicationTypeService applicationTypeService() {
        return new ApplicationTypeServiceImpl();
    }

    @Bean
    public CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase() {
        return mock(CreateSharedPolicyGroupUseCase.class);
    }

    @Bean
    public ImportSharedPolicyGroupCRDCRDUseCase importSharedPolicyGroupCRDCRDUseCase() {
        return mock(ImportSharedPolicyGroupCRDCRDUseCase.class);
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
    public ApplicationCrudServiceInMemory applicationCrudService() {
        return new ApplicationCrudServiceInMemory();
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
        return new ValidatePageSourceDomainServiceImpl(new ObjectMapper(), Vertx.vertx());
    }

    @Bean
    public ValidatePageAccessControlsDomainService validatePageAccessControlsDomainService(GroupQueryService groupQueryService) {
        return new ValidatePageAccessControlsDomainService(groupQueryService);
    }

    @Bean
    public PermissionDomainService permissionDomainService(MembershipService membershipService, PermissionService permissionService) {
        return new PermissionDomainServiceLegacyWrapper(membershipService, permissionService);
    }

    @Bean
    public SpecGenRequestUseCase specGenRequestUseCase() {
        return mock(SpecGenRequestUseCase.class);
    }

    @Bean
    public SubscriptionCRDSpecDomainService subscriptionSpecDomainService(
        SubscriptionService subscriptionService,
        SubscriptionAdapter subscriptionAdapter,
        AcceptSubscriptionDomainService acceptSubscriptionDomainService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        return new SubscriptionCRDSpecDomainServiceImpl(
            subscriptionService,
            subscriptionAdapter,
            acceptSubscriptionDomainService,
            closeSubscriptionDomainService
        );
    }

    @Bean
    public ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase(SubscriptionCRDSpecDomainService subscriptionSpecDomainService) {
        return new ImportSubscriptionSpecUseCase(subscriptionSpecDomainService);
    }

    @Bean
    public DeleteSubscriptionSpecUseCase deleteSubscriptionSpecUseCase(SubscriptionCRDSpecDomainService subscriptionSpecDomainService) {
        return new DeleteSubscriptionSpecUseCase(subscriptionSpecDomainService);
    }

    @Bean
    public SubscriptionAdapter subscriptionAdapter() {
        return new SubscriptionAdapterImpl();
    }

    @Bean
    public ApiExportDomainService apiExportDomainService() {
        return mock(ApiExportDomainService.class);
    }

    @Bean
    public DataEncryptor dataEncryptor() {
        return mock(DataEncryptor.class);
    }

    @Bean
    public ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService() {
        return mock(ApplicationPrimaryOwnerDomainService.class);
    }

    @Bean
    public UpdatePlanDomainService updatePlanDomainService() {
        return mock(UpdatePlanDomainService.class);
    }

    @Bean
    public GroupCrudServiceInMemory groupCrudService() {
        return new GroupCrudServiceInMemory();
    }

    @Bean
    public ValidateGroupCRDDomainService validateGroupCRDDomainService(
        ValidateCRDMembersDomainService validateCRDMembersDomainService,
        RoleService roleService
    ) {
        return new ValidateGroupCRDDomainServiceImpl(validateCRDMembersDomainService, roleService);
    }

    @Bean
    public ImportGroupCRDUseCase importGroupCRDUseCase(
        ValidateGroupCRDDomainService validateGroupCRDDomainService,
        GroupQueryService groupQueryService,
        GroupCrudService groupCrudService,
        CRDMembersDomainService crdMembersDomainService
    ) {
        return new ImportGroupCRDUseCase(validateGroupCRDDomainService, groupQueryService, groupCrudService, crdMembersDomainService);
    }

    @Bean
    public SharedPolicyGroupCrudService sharedPolicyGroupCrudService() {
        return new SharedPolicyGroupCrudServiceInMemory();
    }

    @Bean
    public io.gravitee.rest.api.service.sanitizer.HtmlSanitizer legacyHtmlSanitizer() {
        return new io.gravitee.rest.api.service.sanitizer.HtmlSanitizer(new MockEnvironment());
    }

    @Bean
    public HtmlSanitizer htmlSanitizer(io.gravitee.rest.api.service.sanitizer.HtmlSanitizer delegate) {
        return new HtmlSanitizerImpl(delegate);
    }

    @Bean
    public GetExposedEntrypointsUseCase getExposedEntrypointsUseCase(
        ApiCrudServiceInMemory apiCrudServiceInMemory,
        ApiExposedEntrypointDomainServiceInMemory apiExposedEntrypointDomainServiceInMemory
    ) {
        return new GetExposedEntrypointsUseCase(apiCrudServiceInMemory, apiExposedEntrypointDomainServiceInMemory);
    }

    @Bean
    public CreateClusterUseCase createClusterUseCase() {
        return mock(CreateClusterUseCase.class);
    }

    @Bean
    public GetClusterUseCase getClusterUseCase() {
        return mock(GetClusterUseCase.class);
    }

    @Bean
    public UpdateClusterUseCase updateClusterUseCase() {
        return mock(UpdateClusterUseCase.class);
    }

    @Bean
    public DeleteClusterUseCase deleteClusterUseCase() {
        return mock(DeleteClusterUseCase.class);
    }

    @Bean
    public SearchClusterUseCase searchClusterUseCase() {
        return mock(SearchClusterUseCase.class);
    }

    @Bean
    public TransferClusterOwnershipUseCase transferClusterOwnershipUseCase() {
        return mock(TransferClusterOwnershipUseCase.class);
    }

    @Bean
    public NewtAIProvider newtAIProvider() {
        return new NewtAIProviderInMemory();
    }

    @Bean
    public ApimProductInfo apimProductInfo() {
        return mock(ApimProductInfo.class);
    }

    @Bean
    public GetClusterMembersUseCase getClusterMembersUseCase() {
        return mock(GetClusterMembersUseCase.class);
    }

    @Bean
    public GetClusterPermissionsUseCase getClusterPermissionsUseCase() {
        return mock(GetClusterPermissionsUseCase.class);
    }

    @Bean
    public AddClusterMemberUseCase addClusterMemberUseCase() {
        return mock(AddClusterMemberUseCase.class);
    }

    @Bean
    public UpdateClusterMemberUseCase updateClusterMemberUseCase() {
        return mock(UpdateClusterMemberUseCase.class);
    }

    @Bean
    public DeleteClusterMemberUseCase deleteClusterMemberUseCase() {
        return mock(DeleteClusterMemberUseCase.class);
    }

    @Bean
    public InstanceQueryServiceLegacyWrapper instanceQueryServiceLegacyWrapper() {
        return new InstanceQueryServiceLegacyWrapper(mock(InstanceService.class));
    }

    @Bean
    public UpdateClusterGroupsUseCase updateClusterGroupsUseCase() {
        return mock(UpdateClusterGroupsUseCase.class);
    }

    @Bean
    public CockpitPromotionServiceProvider cockpitPromotionServiceProvider() {
        return mock(CockpitPromotionServiceProvider.class);
    }

    @Bean
    public CreatePromotionUseCase createPromotionUseCase() {
        return mock(CreatePromotionUseCase.class);
    }
}
