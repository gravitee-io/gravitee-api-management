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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.mapper.V2toV4Mapper;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class MigrateApiUseCase {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PlanCrudService planService;

    private final V2toV4Mapper upgradeApiOperator;

    @Inject
    public MigrateApiUseCase(
        ApiCrudService apiCrudService,
        AuditDomainService auditService,
        ApiIndexerDomainService apiIndexerDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        PlanCrudService planService
    ) {
        this(apiCrudService, auditService, apiIndexerDomainService, apiPrimaryOwnerDomainService, planService, new V2toV4Mapper());
    }

    public Output execute(Input input) {
        var api = apiCrudService.findById(input.apiId());
        if (api.isEmpty()) {
            return new Output.Fail(input.apiId(), List.of("API not found"));
        } else if (api.get().getDefinitionVersion() != DefinitionVersion.V2) {
            return new Output.Fail(input.apiId(), List.of("Cannot upgrade an API which is not a v2 definition"));
        }
        var plans = planService.findByApiId(input.apiId());
        for (var plan : plans) {
            planService.update(upgradeApiOperator.mapPlan(plan));
        }
        Api upgraded = upgradeApiOperator.mapApi(api.get());
        if (input.mode() != Input.UpgradeMode.DRY_RUN) {
            upgraded = apiCrudService.update(upgraded);
            var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.ctx().organizationId(), input.apiId());

            auditService.createApiAuditLog(
                ApiAuditLogEntity
                    .builder()
                    .event(ApiAuditEvent.API_UPDATED)
                    .actor(AuditActor.builder().userId(input.user).build())
                    .apiId(input.apiId())
                    .environmentId(input.ctx().environmentId())
                    .organizationId(input.ctx().organizationId())
                    .createdAt(TimeProvider.now())
                    .oldValue(api.get())
                    .newValue(upgraded)
                    .properties(Map.of(AuditProperties.API, input.apiId()))
                    .build()
            );
            var auditInfo = new ApiIndexerDomainService.Context(input.ctx(), false);
            apiIndexerDomainService.index(auditInfo, upgraded, apiPrimaryOwner);
        }
        return new Output.Success(upgraded.getId());
    }

    public record Input(String apiId, UpgradeMode mode, AuditInfo ctx, String user) {
        public enum UpgradeMode {
            DRY_RUN,
            FORCE,
        }
    }

    public sealed interface Output {
        String apiId();

        record Success(String apiId) implements Output {}

        record Fail(String apiId, Collection<String> errors) implements Output {}
    }
}
