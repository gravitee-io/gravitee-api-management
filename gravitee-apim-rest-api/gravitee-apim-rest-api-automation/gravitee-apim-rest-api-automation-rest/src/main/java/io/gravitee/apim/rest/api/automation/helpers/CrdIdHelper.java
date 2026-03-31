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
package io.gravitee.apim.rest.api.automation.helpers;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Generates deterministic IDs from HRIDs for CRD resources.
 * This logic belongs in the Automation API layer, not in domain services.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrdIdHelper {

    public static void generateApiIds(ApiCRDSpec spec, AuditInfo audit) {
        var api = HRIDToUUID.api().context(audit).hrid(spec.getHrid());
        if (spec.getId() == null) {
            spec.setId(api.id());
        }
        if (spec.getCrossId() == null) {
            spec.setCrossId(api.crossId());
        }
    }

    public static void generatePlanIds(Map<String, PlanCRD> plans, String apiHrid, AuditInfo audit) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        plans.forEach((key, planCRD) -> {
            if (planCRD.getId() == null) {
                planCRD.setId(HRIDToUUID.plan().context(audit).api(apiHrid).plan(key).id());
            }
            if (
                (planCRD.getGeneralConditions() == null || planCRD.getGeneralConditions().isEmpty()) &&
                (planCRD.getGeneralConditionsHrid() != null && !planCRD.getGeneralConditionsHrid().isEmpty())
            ) {
                planCRD.setGeneralConditions(HRIDToUUID.page().context(audit).api(apiHrid).page(planCRD.getGeneralConditionsHrid()).id());
            }
        });
    }

    public static void generatePageIds(Map<String, PageCRD> pages, String apiHrid, AuditInfo audit) {
        if (pages == null || pages.isEmpty()) {
            return;
        }
        pages.forEach((key, pageCRD) -> {
            if (pageCRD.getId() == null) {
                pageCRD.setId(HRIDToUUID.page().context(audit).api(apiHrid).page(key).id());
            }
            if (pageCRD.getParentId() == null && pageCRD.getParentHrid() != null) {
                pageCRD.setParentId(HRIDToUUID.page().context(audit).api(apiHrid).page(pageCRD.getParentHrid()).id());
            }
        });
    }

    public static void generateApplicationId(ApplicationCRDSpec spec, AuditInfo audit) {
        if (spec.getId() == null) {
            spec.setId(HRIDToUUID.application().context(audit).hrid(spec.getHrid()).id());
        }
    }

    public static void generateSharedPolicyGroupIds(SharedPolicyGroupCRD crd, AuditInfo audit) {
        var spg = HRIDToUUID.sharedPolicyGroup().context(audit).hrid(crd.getHrid());
        if (crd.getSharedPolicyGroupId() == null) {
            crd.setSharedPolicyGroupId(spg.id());
        }
        if (crd.getCrossId() == null) {
            crd.setCrossId(spg.crossId());
        }
    }
}
