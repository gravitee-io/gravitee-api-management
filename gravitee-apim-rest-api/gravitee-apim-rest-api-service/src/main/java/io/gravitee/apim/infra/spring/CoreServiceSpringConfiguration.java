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
package io.gravitee.apim.infra.spring;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiHostValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPolicyValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.query_service.AuditMetadataQueryService;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageRevisionCrudService;
import io.gravitee.apim.core.documentation.domain_service.*;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.infra.domain_service.documentation.FreemarkerTemplateResolver;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.node.api.license.LicenseManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceSpringConfiguration {

    @Bean
    public AuditDomainService auditDomainService(
        AuditCrudService auditCrudService,
        UserCrudService userCrudService,
        JacksonJsonDiffProcessor jacksonJsonDiffProcessor
    ) {
        return new AuditDomainService(auditCrudService, userCrudService, jacksonJsonDiffProcessor);
    }

    @Bean
    public CloseSubscriptionDomainService closeSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        AuditDomainService auditDomainService,
        ApplicationCrudService applicationCrudService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        RejectSubscriptionDomainService rejectSubscriptionDomainService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        return new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            rejectSubscriptionDomainService,
            revokeApiKeyDomainService
        );
    }

    @Bean
    public RejectSubscriptionDomainService rejectSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        PlanCrudService planCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService
    ) {
        return new RejectSubscriptionDomainService(
            subscriptionCrudService,
            planCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            userCrudService
        );
    }

    @Bean
    public RevokeApiKeyDomainService revokeApiKeyDomainService(
        ApiKeyCrudService apiKeyCrudService,
        ApiKeyQueryService apiKeyQueryService,
        SubscriptionCrudService subscriptionCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService
    ) {
        return new RevokeApiKeyDomainService(
            apiKeyCrudService,
            apiKeyQueryService,
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationDomainService
        );
    }

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService(
        ApiQueryService apiSearchService,
        InstallationAccessQueryService installationAccessQueryService,
        ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        return new VerifyApiPathDomainService(apiSearchService, installationAccessQueryService, apiHostValidatorDomainService);
    }

    @Bean
    public ApiDocumentationDomainService apiDocumentationDomainService(
        PageQueryService pageQueryService,
        PlanQueryService planQueryService
    ) {
        return new ApiDocumentationDomainService(pageQueryService, planQueryService);
    }

    @Bean
    public DeleteApiDocumentationDomainService deleteApiDocumentationDomainService(
        PageCrudService pageCrudService,
        PageQueryService pageQueryService,
        AuditDomainService auditDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        PlanQueryService planQueryService
    ) {
        return new DeleteApiDocumentationDomainService(
            pageCrudService,
            pageQueryService,
            auditDomainService,
            updateApiDocumentationDomainService,
            planQueryService
        );
    }

    @Bean
    public CreateApiDocumentationDomainService createApiDocumentationDomainService(
        PageCrudService pageCrudService,
        PageRevisionCrudService pageRevisionCrudService,
        AuditDomainService auditDomainService
    ) {
        return new CreateApiDocumentationDomainService(pageCrudService, pageRevisionCrudService, auditDomainService);
    }

    @Bean
    public UpdateApiDocumentationDomainService updateApiDocumentationDomainService(
        PageCrudService pageCrudService,
        PageRevisionCrudService pageRevisionCrudService,
        AuditDomainService auditDomainService
    ) {
        return new UpdateApiDocumentationDomainService(pageCrudService, pageRevisionCrudService, auditDomainService);
    }

    @Bean
    public HomepageDomainService homepageDomainService(PageQueryService pageQueryService, PageCrudService pageCrudService) {
        return new HomepageDomainService(pageQueryService, pageCrudService);
    }

    @Bean
    public GraviteeLicenseDomainService graviteeLicenseDomainService(LicenseManager licenseManager) {
        return new GraviteeLicenseDomainService(licenseManager);
    }

    @Bean
    public TemplateResolverDomainService templateResolverDomainService() {
        return new FreemarkerTemplateResolver();
    }

    @Bean
    DocumentationValidationDomainService documentationValidationDomainService(
        HtmlSanitizer htmlSanitizer,
        TemplateResolverDomainService templateResolverDomainService,
        ApiCrudService apiCrudService
    ) {
        return new DocumentationValidationDomainService(htmlSanitizer, templateResolverDomainService, apiCrudService);
    }

    @Bean
    public ApiPolicyValidatorDomainService apiPolicyValidatorDomainService(PolicyValidationDomainService policyValidationDomainService) {
        return new ApiPolicyValidatorDomainService(policyValidationDomainService);
    }

    @Bean
    public PlanValidatorDomainService planValidatorDomainService(
        ParametersQueryService parametersQueryService,
        PolicyValidationDomainService policyValidationDomainService,
        PageCrudService pageCrudService
    ) {
        return new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService);
    }

    @Bean
    FlowValidationDomainService flowValidationDomainService(
        PolicyValidationDomainService policyValidationDomainService,
        EntrypointPluginQueryService entrypointPluginQueryService
    ) {
        return new FlowValidationDomainService(policyValidationDomainService, entrypointPluginQueryService);
    }

    @Bean
    public CreatePlanDomainService planDomainService(
        PlanValidatorDomainService planValidatorDomainService,
        FlowValidationDomainService flowValidationDomainService,
        PlanCrudService planCrudService,
        FlowCrudService flowCrudService,
        AuditDomainService auditDomainService
    ) {
        return new CreatePlanDomainService(
            planValidatorDomainService,
            flowValidationDomainService,
            planCrudService,
            flowCrudService,
            auditDomainService
        );
    }

    @Bean
    public ReorderPlanDomainService reorderPlanDomainService(PlanQueryService planQueryService, PlanCrudService planCrudService) {
        return new ReorderPlanDomainService(planQueryService, planCrudService);
    }

    @Bean
    public UpdatePlanDomainService updatePlansDomainService(
        PlanQueryService planQueryService,
        PlanCrudService planCrudService,
        PlanValidatorDomainService planValidatorDomainService,
        FlowValidationDomainService flowValidationDomainService,
        FlowCrudService flowCrudService,
        AuditDomainService auditDomainService,
        PlanSynchronizationService planSynchronizationService,
        ReorderPlanDomainService reorderPlanDomainService
    ) {
        return new UpdatePlanDomainService(
            planQueryService,
            planCrudService,
            planValidatorDomainService,
            flowValidationDomainService,
            flowCrudService,
            auditDomainService,
            planSynchronizationService,
            reorderPlanDomainService
        );
    }

    @Bean
    public DeletePlanDomainService deletePlanDomainService(
        PlanCrudService planCrudService,
        SubscriptionQueryService subscriptionQueryService,
        AuditDomainService auditDomainService
    ) {
        return new DeletePlanDomainService(planCrudService, subscriptionQueryService, auditDomainService);
    }

    @Bean
    public VerifyApiHostsDomainService verifyApiHostsDomainService(ApiQueryService apiSearchService) {
        return new VerifyApiHostsDomainService(apiSearchService);
    }

    @Bean
    public LicenseDomainService licenseDomainService(LicenseCrudService licenseCrudService) {
        return new LicenseDomainService(licenseCrudService);
    }

    @Bean
    public PluginFilterByLicenseDomainService pluginFilterByLicenseDomainService(LicenseManager licenseManager) {
        return new PluginFilterByLicenseDomainService(licenseManager);
    }

    @Bean
    public SearchAuditDomainService searchAuditDomainService(
        AuditQueryService auditQueryService,
        AuditMetadataQueryService auditMetadataQueryService
    ) {
        return new SearchAuditDomainService(auditQueryService, auditMetadataQueryService);
    }
}
