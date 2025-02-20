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
package inmemory.spring;

import inmemory.AccessPointQueryServiceInMemory;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCRDExportDomainServiceInMemory;
import inmemory.ApiCategoryOrderQueryServiceInMemory;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApiSpecGenCrudServiceInMemory;
import inmemory.ApiSpecGenQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ApplicationMetadataCrudServiceInMemory;
import inmemory.ApplicationMetadataQueryServiceInMemory;
import inmemory.ApplicationQueryServiceInMemory;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.AsyncJobQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.AuditMetadataQueryServiceInMemory;
import inmemory.AuditQueryServiceInMemory;
import inmemory.CategoryApiCrudServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.EndpointPluginQueryServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.ImportApplicationCRDDomainServiceInMemory;
import inmemory.IndexerInMemory;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.InstanceQueryServiceInMemory;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.IntegrationQueryServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MessageLogCrudServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NoopSwaggerOpenApiResolver;
import inmemory.NoopTemplateResolverDomainService;
import inmemory.OasProviderInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.ParametersDomainServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.PolicyPluginQueryServiceInMemory;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import inmemory.PortalMenuLinkQueryServiceInMemory;
import inmemory.PrimaryOwnerDomainServiceInMemory;
import inmemory.ResourcePluginCrudServiceInMemory;
import inmemory.ResourcePluginQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.ScoringFunctionCrudServiceInMemory;
import inmemory.ScoringFunctionQueryServiceInMemory;
import inmemory.ScoringProviderInMemory;
import inmemory.ScoringReportCrudServiceInMemory;
import inmemory.ScoringReportQueryServiceInMemory;
import inmemory.ScoringRulesetCrudServiceInMemory;
import inmemory.ScoringRulesetQueryServiceInMemory;
import inmemory.SpecGenNotificationProviderInMemory;
import inmemory.SpecGenProviderInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemePortalNextAssetsDomainServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import inmemory.ThemeServiceLegacyWrapperInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UpdateCategoryApiDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import inmemory.ValidateResourceDomainServiceInMemory;
import inmemory.WorkflowQueryServiceInMemory;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.specgen.crud_service.ApiSpecGenCrudService;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.OasProvider;
import io.gravitee.apim.core.specgen.service_provider.SpecGenNotificationProvider;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.gravitee.apim.core.workflow.query_service.WorkflowQueryService;
import io.gravitee.apim.infra.query_service.audit.AuditEventQueryServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryConfiguration {

    @Bean
    public ApiQueryServiceInMemory apiQueryService(ApiCrudServiceInMemory apiCrudServiceInMemory) {
        return new ApiQueryServiceInMemory(apiCrudServiceInMemory);
    }

    @Bean
    public ApiCrudServiceInMemory apiCrudService() {
        return new ApiCrudServiceInMemory();
    }

    @Bean
    public ApiMetadataQueryServiceInMemory apiMetadataQueryService() {
        return new ApiMetadataQueryServiceInMemory();
    }

    @Bean
    public ApplicationCrudServiceInMemory applicationRepository() {
        return new ApplicationCrudServiceInMemory();
    }

    @Bean
    public ApplicationMetadataCrudServiceInMemory applicationMetadataCrudService() {
        return new ApplicationMetadataCrudServiceInMemory();
    }

    @Bean
    public ApiKeyCrudServiceInMemory apiKeyCrudService() {
        return Mockito.spy(new ApiKeyCrudServiceInMemory());
    }

    @Bean
    public ApiKeyQueryServiceInMemory apiKeyQueryService(ApiKeyCrudServiceInMemory apiKeyCrudService) {
        return new ApiKeyQueryServiceInMemory(apiKeyCrudService);
    }

    @Bean
    public AuditCrudServiceInMemory auditCrudService() {
        return new AuditCrudServiceInMemory();
    }

    @Bean
    public ConnectionLogsCrudServiceInMemory connectionLogRepository() {
        return new ConnectionLogsCrudServiceInMemory();
    }

    @Bean
    public EnvironmentCrudServiceInMemory environmentCrudService() {
        return new EnvironmentCrudServiceInMemory();
    }

    @Bean
    public MessageLogCrudServiceInMemory messageLogRepository() {
        return new MessageLogCrudServiceInMemory();
    }

    @Bean
    public PlanCrudServiceInMemory planCrudService() {
        return new PlanCrudServiceInMemory();
    }

    @Bean
    public PlanQueryServiceInMemory planQueryService(PlanCrudServiceInMemory planCrudService) {
        return new PlanQueryServiceInMemory(planCrudService);
    }

    @Bean
    public SubscriptionCrudServiceInMemory subscriptionCrudService() {
        return new SubscriptionCrudServiceInMemory();
    }

    @Bean
    public SubscriptionQueryServiceInMemory subscriptionQueryService(SubscriptionCrudServiceInMemory subscriptionCrudService) {
        return new SubscriptionQueryServiceInMemory(subscriptionCrudService);
    }

    @Bean
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService() {
        return new TriggerNotificationDomainServiceInMemory();
    }

    @Bean
    public UserCrudServiceInMemory userCrudService() {
        return new UserCrudServiceInMemory();
    }

    @Bean
    public UserDomainServiceInMemory userDomainService() {
        return new UserDomainServiceInMemory();
    }

    @Bean
    public PageQueryServiceInMemory pageQueryService() {
        return new PageQueryServiceInMemory();
    }

    @Bean
    public PageCrudServiceInMemory pageCrudService() {
        return new PageCrudServiceInMemory();
    }

    @Bean
    public PageRevisionCrudServiceInMemory pageRevisionCrudService() {
        return new PageRevisionCrudServiceInMemory();
    }

    @Bean
    public AccessPointQueryServiceInMemory accessPointQueryService() {
        return new AccessPointQueryServiceInMemory();
    }

    @Bean
    public InstallationAccessQueryServiceInMemory installationAccessService() {
        return new InstallationAccessQueryServiceInMemory();
    }

    @Bean
    public ParametersDomainServiceInMemory parametersDomainService() {
        return new ParametersDomainServiceInMemory();
    }

    @Bean
    public InstanceQueryServiceInMemory instanceQueryService() {
        return new InstanceQueryServiceInMemory();
    }

    @Bean
    public EventCrudInMemory eventCrudService() {
        return new EventCrudInMemory();
    }

    @Bean
    public ParametersQueryServiceInMemory parametersQueryService() {
        return new ParametersQueryServiceInMemory();
    }

    @Bean
    public EntrypointPluginQueryServiceInMemory entrypointPluginQueryService() {
        return new EntrypointPluginQueryServiceInMemory();
    }

    @Bean
    public EndpointPluginQueryServiceInMemory endpointPluginQueryService() {
        return new EndpointPluginQueryServiceInMemory();
    }

    @Bean
    public PolicyPluginQueryServiceInMemory policyPluginQueryService() {
        return new PolicyPluginQueryServiceInMemory();
    }

    @Bean
    public ResourcePluginQueryServiceInMemory resourcePluginQueryService() {
        return new ResourcePluginQueryServiceInMemory();
    }

    @Bean
    public ResourcePluginCrudServiceInMemory resourcePluginCrudService() {
        return new ResourcePluginCrudServiceInMemory();
    }

    @Bean
    public FlowCrudServiceInMemory flowCrudService() {
        return new FlowCrudServiceInMemory();
    }

    @Bean
    public LicenseCrudServiceInMemory licenseCrudService() {
        return new LicenseCrudServiceInMemory();
    }

    @Bean
    public AuditQueryServiceInMemory auditQueryService(AuditCrudServiceInMemory auditCrudServiceInMemory) {
        return new AuditQueryServiceInMemory(auditCrudServiceInMemory);
    }

    @Bean
    public AuditMetadataQueryServiceInMemory auditMetadataQueryService() {
        return new AuditMetadataQueryServiceInMemory();
    }

    @Bean
    public AuditEventQueryServiceImpl auditEventQueryService() {
        return new AuditEventQueryServiceImpl();
    }

    @Bean
    public EventQueryServiceInMemory eventQueryService() {
        return new EventQueryServiceInMemory();
    }

    @Bean
    public GroupQueryServiceInMemory groupQueryService() {
        return new GroupQueryServiceInMemory();
    }

    @Bean
    public MembershipCrudServiceInMemory membershipCrudService() {
        return new MembershipCrudServiceInMemory();
    }

    @Bean
    public MembershipQueryServiceInMemory membershipQueryService(MembershipCrudServiceInMemory membershipCrudService) {
        return new MembershipQueryServiceInMemory(membershipCrudService);
    }

    @Bean
    public RoleQueryServiceInMemory roleQueryService() {
        return new RoleQueryServiceInMemory();
    }

    @Bean
    public IndexerInMemory indexer() {
        return new IndexerInMemory();
    }

    @Bean
    public NoopTemplateResolverDomainService noopTemplateResolverDomainService() {
        return new NoopTemplateResolverDomainService();
    }

    @Bean
    public NoopSwaggerOpenApiResolver noopSwaggerOpenApiResolver() {
        return new NoopSwaggerOpenApiResolver();
    }

    @Bean
    public IntegrationCrudServiceInMemory integrationCrudService() {
        return new IntegrationCrudServiceInMemory();
    }

    @Bean
    public AsyncJobCrudServiceInMemory asyncJobCrudService() {
        return new AsyncJobCrudServiceInMemory();
    }

    @Bean
    public IntegrationQueryServiceInMemory integrationQueryService(IntegrationCrudServiceInMemory integrationCrudServiceInMemory) {
        return new IntegrationQueryServiceInMemory(integrationCrudServiceInMemory);
    }

    @Bean
    public AsyncJobQueryServiceInMemory asyncJobQueryService(AsyncJobCrudServiceInMemory asyncJobCrudServiceInMemory) {
        return new AsyncJobQueryServiceInMemory(asyncJobCrudServiceInMemory);
    }

    @Bean
    public MetadataCrudServiceInMemory metadataCrudService() {
        return new MetadataCrudServiceInMemory();
    }

    @Bean
    public IntegrationAgentInMemory integrationAgent() {
        return new IntegrationAgentInMemory();
    }

    @Bean
    public ApiCategoryQueryServiceInMemory apiCategoryQueryService() {
        return new ApiCategoryQueryServiceInMemory();
    }

    @Bean
    public PrimaryOwnerDomainServiceInMemory primaryOwnerDomainService() {
        return new PrimaryOwnerDomainServiceInMemory();
    }

    @Bean
    public ImportApplicationCRDDomainServiceInMemory applicationCRDDomainService() {
        return new ImportApplicationCRDDomainServiceInMemory();
    }

    @Bean
    public ApplicationMetadataQueryServiceInMemory applicationMetadataQueryService() {
        return new ApplicationMetadataQueryServiceInMemory();
    }

    @Bean
    public ApiCRDExportDomainServiceInMemory apiCRDExportDomainService() {
        return new ApiCRDExportDomainServiceInMemory();
    }

    @Bean
    public ThemeQueryServiceInMemory themeQueryService() {
        return new ThemeQueryServiceInMemory();
    }

    @Bean
    public TagQueryServiceInMemory tagQueryService() {
        return new TagQueryServiceInMemory();
    }

    @Bean
    public ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService() {
        return new ApiAuthorizationDomainServiceInMemory();
    }

    @Bean
    public ApiCategoryOrderQueryServiceInMemory categoryApiQueryService() {
        return new ApiCategoryOrderQueryServiceInMemory();
    }

    @Bean
    public CategoryQueryServiceInMemory categoryQueryService() {
        return new CategoryQueryServiceInMemory();
    }

    @Bean
    public ValidateResourceDomainServiceInMemory validateResourceDomainService() {
        return new ValidateResourceDomainServiceInMemory();
    }

    @Bean
    public CategoryApiCrudServiceInMemory categoryApiCrudService() {
        return new CategoryApiCrudServiceInMemory();
    }

    @Bean
    public UpdateCategoryApiDomainServiceInMemory updateCategoryApiDomainService() {
        return new UpdateCategoryApiDomainServiceInMemory();
    }

    @Bean
    public ThemeCrudServiceInMemory themeCrudService() {
        return new ThemeCrudServiceInMemory();
    }

    @Bean
    public ThemeServiceLegacyWrapperInMemory themeServiceLegacyWrapper() {
        return new ThemeServiceLegacyWrapperInMemory();
    }

    @Bean
    public ThemePortalNextAssetsDomainServiceInMemory themePortalNextAssetsDomainServiceInMemory() {
        return new ThemePortalNextAssetsDomainServiceInMemory();
    }

    @Bean
    public ScoringProviderInMemory scoringProviderInMemory() {
        return new ScoringProviderInMemory();
    }

    @Bean
    public PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService() {
        return new PortalMenuLinkCrudServiceInMemory();
    }

    @Bean
    public PortalMenuLinkQueryServiceInMemory portalMenuLinkQueryService(
        PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory
    ) {
        return new PortalMenuLinkQueryServiceInMemory(portalMenuLinkCrudServiceInMemory);
    }

    @Bean
    public ScoringProviderInMemory scoringProvider() {
        return new ScoringProviderInMemory();
    }

    @Bean
    public ScoringReportCrudServiceInMemory scoringReportCrudService() {
        return new ScoringReportCrudServiceInMemory();
    }

    @Bean
    public ScoringReportQueryServiceInMemory scoringReportQueryService() {
        return new ScoringReportQueryServiceInMemory();
    }

    @Bean
    public ScoringRulesetCrudServiceInMemory scoringRulesetCrudService() {
        return new ScoringRulesetCrudServiceInMemory();
    }

    @Bean
    public ScoringRulesetQueryServiceInMemory scoringRulesetQueryService(ScoringRulesetCrudServiceInMemory scoringRulesetCrudService) {
        return new ScoringRulesetQueryServiceInMemory(scoringRulesetCrudService);
    }

    @Bean
    public ScoringFunctionCrudServiceInMemory scoringFunctionCrudService() {
        return new ScoringFunctionCrudServiceInMemory();
    }

    @Bean
    public ScoringFunctionQueryServiceInMemory scoringFunctionQueryService(ScoringFunctionCrudServiceInMemory scoringFunctionCrudService) {
        return new ScoringFunctionQueryServiceInMemory(scoringFunctionCrudService);
    }

    @Bean
    public ApiSpecGenQueryService apiSpecGenQueryService() {
        return new ApiSpecGenQueryServiceInMemory();
    }

    @Bean
    public SpecGenProvider specGenProvider() {
        return new SpecGenProviderInMemory();
    }

    @Bean
    public SpecGenNotificationProvider specGenNotificationProvider() {
        return new SpecGenNotificationProviderInMemory();
    }

    @Bean
    public OasProvider oasProvider() {
        return new OasProviderInMemory();
    }

    @Bean
    public ApiSpecGenCrudService apiSpecGenCrudService() {
        return new ApiSpecGenCrudServiceInMemory();
    }

    @Bean
    public WorkflowQueryService workflowQueryService() {
        return new WorkflowQueryServiceInMemory();
    }

    @Bean
    public ApplicationQueryServiceInMemory applicationQueryService(ApplicationCrudServiceInMemory applicationCrudService) {
        return new ApplicationQueryServiceInMemory(applicationCrudService);
    }
}
