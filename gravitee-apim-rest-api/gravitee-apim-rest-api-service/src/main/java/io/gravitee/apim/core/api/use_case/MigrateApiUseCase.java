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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.mapper.V2toV4MigrationOperator;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.Getter;
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
    private final ApiStateDomainService apiStateService;

    private final V2toV4MigrationOperator upgradeApiOperator;

    @Inject
    public MigrateApiUseCase(
        ApiCrudService apiCrudService,
        AuditDomainService auditService,
        ApiIndexerDomainService apiIndexerDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        PlanCrudService planService,
        ApiStateDomainService apiStateService
    ) {
        this(
            apiCrudService,
            auditService,
            apiIndexerDomainService,
            apiPrimaryOwnerDomainService,
            planService,
            apiStateService,
            new V2toV4MigrationOperator()
        );
    }

    public Output execute(Input input) {
        var api = apiCrudService.findById(input.apiId());
        // Preconditions
        if (api.isEmpty()) {
            throw new ApiNotFoundException(input.apiId());
        } else if (api.get().getDefinitionVersion() != DefinitionVersion.V2) {
            return new Output(
                input.apiId(),
                List.of(new Output.Issue("Cannot upgrade an API which is not a v2 definition", Output.State.IMPOSSIBLE))
            );
        } else if (!apiStateService.isSynchronized(api.get(), input.auditInfo())) {
            return new Output(
                input.apiId(),
                List.of(new Output.Issue("Cannot upgrade an API which is out of sync", Output.State.CAN_BE_FORCED))
            );
        }

        // Migration
        var plans = planService.findByApiId(input.apiId());
        for (var plan : plans) {
            Plan migratedPlan = upgradeApiOperator.mapPlan(plan);
            if (input.mode() != Input.UpgradeMode.DRY_RUN) {
                planService.update(migratedPlan);
            }
        }
        Api upgraded = upgradeApiOperator.mapApi(api.get());

        // Apply
        Output.State state = Output.State.MIGRATABLE;
        if (input.mode() != Input.UpgradeMode.DRY_RUN) {
            upgraded = apiCrudService.update(upgraded);
            var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo().organizationId(), input.apiId());

            auditService.createApiAuditLog(
                ApiAuditLogEntity
                    .builder()
                    .event(ApiAuditEvent.API_UPDATED)
                    .actor(AuditActor.builder().userId(input.auditInfo().actor().userId()).build())
                    .apiId(input.apiId())
                    .environmentId(input.auditInfo().environmentId())
                    .organizationId(input.auditInfo().organizationId())
                    .createdAt(TimeProvider.now())
                    .oldValue(api.get())
                    .newValue(upgraded)
                    .properties(Map.of(AuditProperties.API, input.apiId()))
                    .build()
            );
            var auditInfo = new ApiIndexerDomainService.Context(input.auditInfo(), false);
            apiIndexerDomainService.index(auditInfo, upgraded, apiPrimaryOwner);
            state = Output.State.MIGRATED;
        }
        return new Output(upgraded.getId(), List.of(), state);
    }

    public record Input(String apiId, UpgradeMode mode, AuditInfo auditInfo) {
        public enum UpgradeMode {
            DRY_RUN,
            FORCE,
        }
    }

    public record Output(String apiId, Collection<Issue> issues, State state) {
        private static final Comparator<Output.State> STATE_COMPARATOR = Comparator.comparing(Output.State::getWeight);

        public Output(String apiId, Collection<Issue> issues) {
            this(apiId, issues, stream(issues).map(Issue::state).max(STATE_COMPARATOR).orElse(State.MIGRATABLE));
        }

        public record Issue(String message, State state) {}

        @RequiredArgsConstructor
        public enum State implements Comparable<State> {
            MIGRATABLE(0),
            MIGRATED(1),
            CAN_BE_FORCED(2),
            IMPOSSIBLE(3);

            @Getter
            private final int weight;
        }
    }
}
