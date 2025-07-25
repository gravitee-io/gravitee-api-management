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
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.v4.flow.Flow;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class MigrateApiUseCase {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PlanCrudService planService;
    private final FlowCrudService flowCrudService;
    private final ApiStateDomainService apiStateService;

    private final V2toV4MigrationOperator migrationOperator;

    @Inject
    public MigrateApiUseCase(
        ApiCrudService apiCrudService,
        AuditDomainService auditService,
        ApiIndexerDomainService apiIndexerDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        PlanCrudService planService,
        FlowCrudService flowCrudService,
        ApiStateDomainService apiStateService
    ) {
        this(
            apiCrudService,
            auditService,
            apiIndexerDomainService,
            apiPrimaryOwnerDomainService,
            planService,
            flowCrudService,
            apiStateService,
            new V2toV4MigrationOperator()
        );
    }

    public Output execute(Input input) {
        var api = apiCrudService.findById(input.apiId()).orElseThrow(() -> new ApiNotFoundException(input.apiId()));
        MigrationResult<?> precondition = MigrationResult.value(1);
        if (api.getDefinitionVersion() != DefinitionVersion.V2) {
            // Fatal issue, we don’t try to do anything in this case
            return new Output(
                input.apiId(),
                List.of(new MigrationResult.Issue("Cannot migrate an API which is not a v2 definition", MigrationResult.State.IMPOSSIBLE))
            );
        }
        if (!apiStateService.isSynchronized(api, input.auditInfo())) {
            precondition =
                precondition.addIssue(
                    new MigrationResult.Issue("Cannot migrate an API which is out of sync", MigrationResult.State.CAN_BE_FORCED)
                );
        }
        if (api.getApiDefinition().getExecutionMode() == ExecutionMode.V3) {
            precondition =
                precondition.addIssue(
                    new MigrationResult.Issue("Cannot migrate an API not using V4 emulation", MigrationResult.State.IMPOSSIBLE)
                );
        }

        // Migration
        var migrationResult = precondition.flatMap(ignored -> migrationOperator.mapApi(api)).map(Migration::new);

        var plans = planService.findByApiId(input.apiId());
        for (var plan : plans) {
            var migratedPlan = migrationOperator.mapPlan(plan);
            migrationResult = migrationResult.foldLeft(migratedPlan, Migration::withPlan);
        }

        var apiV2Flows = flowCrudService.getApiV2Flows(input.apiId());
        MigrationResult<Migration.ReferencedFlow> result = migrationOperator
            .mapFlows(apiV2Flows)
            .map(flows -> new Migration.ReferencedFlow.Api(input.apiId(), flows));
        migrationResult = migrationResult.foldLeft(result, Migration::withFlow);

        for (String planId : stream(plans).map(io.gravitee.apim.core.plan.model.Plan::getId).toList()) {
            var planV2Flows = flowCrudService.getPlanV2Flows(planId);
            result = migrationOperator.mapFlows(planV2Flows).map(flows -> new Migration.ReferencedFlow.Plan(planId, flows));
            migrationResult = migrationResult.foldLeft(result, Migration::withFlow);
        }

        // Apply
        MigrationResult.State state = applyMigration(
            migrationResult,
            input.mode(),
            migration -> {
                var upgraded = apiCrudService.update(migration.api());
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
                        .oldValue(api)
                        .newValue(upgraded)
                        .properties(Map.of(AuditProperties.API, input.apiId()))
                        .build()
                );
                var indexerContext = new ApiIndexerDomainService.Context(input.auditInfo(), false);
                apiIndexerDomainService.delete(indexerContext, api);
                apiIndexerDomainService.index(indexerContext, upgraded, apiPrimaryOwner);
                // Plans
                migration.plans().forEach(planService::update);

                for (var a : migration.flows()) {
                    switch (a) {
                        case Migration.ReferencedFlow.Plan planFlows -> flowCrudService.savePlanFlows(planFlows.id(), planFlows.flows());
                        case Migration.ReferencedFlow.Api apiFlows -> flowCrudService.saveApiFlows(apiFlows.id(), apiFlows.flows());
                    }
                }
            }
        );
        return new Output(input.apiId(), migrationResult.issues(), state);
    }

    private <T> MigrationResult.State applyMigration(MigrationResult<T> result, Input.UpgradeMode mode, Consumer<T> consumer) {
        if (mode == Input.UpgradeMode.DRY_RUN) {
            return result.state();
        }
        var acceptableLimit = mode == Input.UpgradeMode.FORCE ? MigrationResult.State.CAN_BE_FORCED : MigrationResult.State.MIGRATABLE;
        if (result.state().getWeight() <= acceptableLimit.getWeight()) {
            consumer.accept(result.value());
            return MigrationResult.State.MIGRATED;
        } else {
            return result.state();
        }
    }

    private record Migration(Api api, Collection<Plan> plans, Collection<ReferencedFlow> flows) {
        public Migration(Api api) {
            this(api, List.of(), List.of());
        }

        @Nullable
        public static Migration withPlan(@Nullable Migration migration, Plan plan) {
            return migration == null
                ? null
                : new Migration(migration.api, Stream.concat(migration.plans.stream(), Stream.of(plan)).toList(), migration.flows);
        }

        @Nullable
        public static Migration withFlow(@Nullable Migration migration, ReferencedFlow flow) {
            return migration == null
                ? null
                : new Migration(migration.api, migration.plans, Stream.concat(migration.flows.stream(), Stream.of(flow)).toList());
        }

        public sealed interface ReferencedFlow {
            String id();
            List<Flow> flows();

            record Api(String id, List<Flow> flows) implements ReferencedFlow {}

            record Plan(String id, List<Flow> flows) implements ReferencedFlow {}
        }
    }

    public record Input(String apiId, UpgradeMode mode, AuditInfo auditInfo) {
        public enum UpgradeMode {
            DRY_RUN,
            FORCE,
        }
    }

    public record Output(String apiId, Collection<MigrationResult.Issue> issues, MigrationResult.State state) {
        private static final Comparator<MigrationResult.State> STATE_COMPARATOR = Comparator.comparing(MigrationResult.State::getWeight);

        public Output(String apiId, Collection<MigrationResult.Issue> issues) {
            this(
                apiId,
                issues,
                stream(issues).map(MigrationResult.Issue::state).max(STATE_COMPARATOR).orElse(MigrationResult.State.MIGRATABLE)
            );
        }
    }
}
