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
package io.gravitee.apim.infra.domain_service.plan;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class CreatePlanDomainServiceLegacyWrapper implements CreatePlanDomainService {

    private final PlanService delegate;

    public CreatePlanDomainServiceLegacyWrapper(PlanService delegate) {
        this.delegate = delegate;
    }

    @Override
    public PlanEntity create(PlanEntity plan, AuditInfo auditInfo) {
        return delegate.create(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            PlanAdapter.INSTANCE.entityToNewPlanEntity(plan)
        );
    }

    @Override
    public Plan create(Plan plan, List<Flow> flows, Api api, AuditInfo auditInfo) {
        return null;
    }
}
