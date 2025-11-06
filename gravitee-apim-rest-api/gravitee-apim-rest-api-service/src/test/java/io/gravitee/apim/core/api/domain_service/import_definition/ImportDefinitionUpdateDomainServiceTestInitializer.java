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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIdsCalculatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateNativeApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.service_provider.ApiImagesServiceProvider;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.infra.domain_service.api.ApiImagesServiceProviderImpl;
import io.gravitee.apim.infra.domain_service.api.UpdateApiDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.stream.Stream;

public class ImportDefinitionUpdateDomainServiceTestInitializer {

    // Mocks
    public final ApiService apiService = mock(ApiService.class);
    public final ApiImagesService apiImagesService = mock(ApiImagesService.class);
    public final CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);
    public final ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);

    // In Memory
    public final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    public final ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    public final ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory();
    public final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    public final FlowCrudServiceInMemory flowCrudServiceInMemory = new FlowCrudServiceInMemory();
    public final GroupQueryServiceInMemory groupQueryServiceInMemory = new GroupQueryServiceInMemory();
    public final IndexerInMemory indexer = new IndexerInMemory();
    public final MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    public final MembershipQueryServiceInMemory membershipQueryServiceInMemory = new MembershipQueryServiceInMemory();
    public final PageQueryServiceInMemory pageQueryServiceInMemory = new PageQueryServiceInMemory();
    public final PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();
    public final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
    public final RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();
    public final TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    public final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    // Domain Services
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;
    private final UpdateNativeApiDomainService updateNativeApiDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final ApiImagesServiceProvider apiImagesServiceProvider;

    public ImportDefinitionUpdateDomainServiceTestInitializer() {
        apiIdsCalculatorDomainService = new ApiIdsCalculatorDomainService(
            apiQueryServiceInMemory,
            pageQueryServiceInMemory,
            planQueryServiceInMemory
        );
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());
        var deprecatePlanDomainService = new DeprecatePlanDomainService(planCrudServiceInMemory, auditDomainService);

        apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryServiceInMemory,
            membershipCrudServiceInMemory,
            membershipQueryServiceInMemory,
            roleQueryServiceInMemory,
            userCrudServiceInMemory
        );

        var apiIndexerDomainService = new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(apiMetadataQueryServiceInMemory, new FreemarkerTemplateProcessor()),
            apiPrimaryOwnerDomainService,
            new ApiCategoryQueryServiceInMemory(),
            indexer
        );
        updateNativeApiDomainService = new UpdateNativeApiDomainService(
            apiCrudServiceInMemory,
            planQueryServiceInMemory,
            deprecatePlanDomainService,
            triggerNotificationDomainService,
            flowCrudServiceInMemory,
            categoryDomainService,
            auditDomainService,
            apiIndexerDomainService
        );
        updateApiDomainService = new UpdateApiDomainServiceImpl(apiService, apiCrudServiceInMemory);
        apiImagesServiceProvider = new ApiImagesServiceProviderImpl(apiImagesService);
    }

    public ImportDefinitionUpdateDomainService initialize() {
        return new ImportDefinitionUpdateDomainService(
            updateApiDomainService,
            apiImagesServiceProvider,
            apiIdsCalculatorDomainService,
            updateNativeApiDomainService,
            validateApiDomainService,
            apiPrimaryOwnerDomainService
        );
    }

    public void tearDown() {
        Stream.of(
            apiCrudServiceInMemory,
            apiQueryServiceInMemory,
            apiMetadataQueryServiceInMemory,
            auditCrudServiceInMemory,
            flowCrudServiceInMemory,
            groupQueryServiceInMemory,
            membershipCrudServiceInMemory,
            membershipQueryServiceInMemory,
            pageQueryServiceInMemory,
            planQueryServiceInMemory,
            planCrudServiceInMemory,
            roleQueryServiceInMemory,
            userCrudServiceInMemory
        ).forEach(InMemoryAlternative::reset);
        reset(apiService);
        reset(apiImagesService);
        reset(categoryDomainService);
        reset(validateApiDomainService);
    }
}
