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

import static io.gravitee.apim.rest.api.automation.resource.ApisResource.HRID_FIELD;
import static io.gravitee.apim.rest.api.automation.resource.ApisResource.SHARED_POLICY_GROUP_ID_FIELD;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.FlowV4;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SharedPolicyGroupIdHelper {

    public static void addSPGIDFromHrid(ApiV4Spec spec, AuditInfo audit) {
        CollectionUtils.stream(spec.getFlows()).forEach(f -> addSPGIDFromHrid(f, audit));
        if (spec.getPlans() != null) {
            CollectionUtils.stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(f -> addSPGIDFromHrid(f, audit));
        }
    }

    private static void addSPGIDFromHrid(@Valid FlowV4 flowV4, AuditInfo audit) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(s -> addSPGIDFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getResponse()).forEach(s -> addSPGIDFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(s -> addSPGIDFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getPublish()).forEach(s -> addSPGIDFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getInteract()).forEach(s -> addSPGIDFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getEntrypointConnect()).forEach(s -> addSPGIDFromHrid(s, audit));
    }

    private static void addSPGIDFromHrid(StepV4 stepV4, AuditInfo auditInfo) {
        if (Objects.equals(stepV4.getPolicy(), SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)) {
            Object configuration = stepV4.getConfiguration();
            if (configuration instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> struct = (Map<String, Object>) rawMap;
                String hrid = (String) struct.get(HRID_FIELD);
                if (hrid != null) {
                    struct.put(SHARED_POLICY_GROUP_ID_FIELD, IdBuilder.builder(auditInfo, hrid).buildCrossId());
                }
            }
        }
    }

    public static void removeSPGID(ApiV4Spec spec) {
        CollectionUtils.stream(spec.getFlows()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        if (spec.getPlans() != null) {
            CollectionUtils.stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(SharedPolicyGroupIdHelper::removeSPGID);
        }
    }

    private static void removeSPGID(@Valid FlowV4 flowV4) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        CollectionUtils.stream(flowV4.getResponse()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        CollectionUtils.stream(flowV4.getPublish()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        CollectionUtils.stream(flowV4.getEntrypointConnect()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
        CollectionUtils.stream(flowV4.getInteract()).forEach(SharedPolicyGroupIdHelper::removeSPGID);
    }

    private static void removeSPGID(StepV4 stepV4) {
        if (
            Objects.equals(stepV4.getPolicy(), SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID) &&
            stepV4.getConfiguration() instanceof Map<?, ?> rawMap
        ) {
            rawMap.remove(SHARED_POLICY_GROUP_ID_FIELD);
        }
    }
}
