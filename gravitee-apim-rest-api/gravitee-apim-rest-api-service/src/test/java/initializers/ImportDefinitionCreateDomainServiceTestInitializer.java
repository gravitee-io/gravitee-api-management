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
package initializers;

import static org.mockito.Mockito.mock;

import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CreateCategoryApiDomainServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NoopSwaggerOpenApiResolver;
import inmemory.NoopTemplateResolverDomainService;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PageSourceDomainServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIdsCalculatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.category.domain_service.CreateCategoryApiDomainService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.domain_service.api.ApiImportDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import java.util.List;
import org.springframework.mock.env.MockEnvironment;

public class ImportDefinitionCreateDomainServiceTestInitializer {

    private static final String ORGANIZATION_ID = "organization-id";
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;
    private final DocumentationValidationDomainService documentationValidationDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;

    // Mocks
    public final ApiImportDomainServiceLegacyWrapper apiImportDomainService = mock(ApiImportDomainServiceLegacyWrapper.class);
    public final PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    public final ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);

    // In Memory
    public final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    public final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    public final ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    public final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    public final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    public final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    public final MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    public final ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    public final IndexerInMemory indexer = new IndexerInMemory();
    public final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    public final NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    public final WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    public final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    public final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    public final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    public final PageSourceDomainServiceInMemory pageSourceDomainService = new PageSourceDomainServiceInMemory();
    public final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    public final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    public final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    public final CreateCategoryApiDomainService createCategoryApiDomainService = new CreateCategoryApiDomainServiceInMemory();

    public ImportDefinitionCreateDomainServiceTestInitializer(ApiCrudServiceInMemory apiCrudService) {
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        apiMetadataDomainService = new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryServiceInMemory, auditDomainService);
        createApiDomainService = new CreateApiDomainService(
            apiCrudService,
            auditDomainService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            ),
            apiMetadataDomainService,
            apiPrimaryOwnerDomainService,
            flowCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService,
            createCategoryApiDomainService
        );

        var planValidatorService = new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService);
        var flowValidationDomainService = new FlowValidationDomainService(
            policyValidationDomainService,
            new EntrypointPluginQueryServiceInMemory()
        );
        createPlanDomainService = new CreatePlanDomainService(
            planValidatorService,
            flowValidationDomainService,
            planCrudService,
            flowCrudService,
            auditDomainService
        );

        createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            auditDomainService,
            indexer
        );
        apiIdsCalculatorDomainService = new ApiIdsCalculatorDomainService(apiQueryService, pageQueryService, planQueryService);
        var htmlSanitizer = new HtmlSanitizer(new MockEnvironment());
        documentationValidationDomainService = new DocumentationValidationDomainService(
            new HtmlSanitizerImpl(htmlSanitizer),
            new NoopTemplateResolverDomainService(),
            apiCrudService,
            new NoopSwaggerOpenApiResolver(),
            new ApiMetadataQueryServiceInMemory(),
            new ApiPrimaryOwnerDomainService(
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                groupQueryService,
                membershipCrudService,
                membershipQueryService,
                roleQueryService,
                userCrudService
            ),
            new ApiDocumentationDomainService(pageQueryService, planQueryService),
            pageCrudService,
            pageSourceDomainService,
            groupQueryService,
            roleQueryService
        );
        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
    }

    public ImportDefinitionCreateDomainService initialize() {
        return new ImportDefinitionCreateDomainService(
            apiImportDomainService,
            apiPrimaryOwnerFactory,
            createApiDomainService,
            validateApiDomainService,
            apiMetadataDomainService,
            createPlanDomainService,
            createApiDocumentationDomainService,
            apiIdsCalculatorDomainService,
            metadataCrudService,
            documentationValidationDomainService
        );
    }
}
