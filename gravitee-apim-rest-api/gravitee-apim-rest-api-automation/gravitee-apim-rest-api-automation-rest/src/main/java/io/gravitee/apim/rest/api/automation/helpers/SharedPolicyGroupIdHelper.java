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

import static io.gravitee.apim.rest.api.automation.helpers.HRIDHelper.nameToHRID;
import static io.gravitee.apim.rest.api.automation.resource.ApisResource.HRID_FIELD;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.FlowV4;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SharedPolicyGroupIdHelper {

    public static final String SHARED_POLICY_GROUP_ID_FIELD = "sharedPolicyGroupId";

    public static void addSharedPolicyGroupIdFromHrid(ApiV4Spec spec, AuditInfo audit) {
        CollectionUtils.stream(spec.getFlows()).forEach(f -> addSharedPolicyGroupIdFromHrid(f, audit));
        if (spec.getPlans() != null) {
            CollectionUtils.stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(f -> addSharedPolicyGroupIdFromHrid(f, audit));
        }
    }

    private static void addSharedPolicyGroupIdFromHrid(@Valid FlowV4 flowV4, AuditInfo audit) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getResponse()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getPublish()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getInteract()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
        CollectionUtils.stream(flowV4.getEntrypointConnect()).forEach(s -> addSharedPolicyGroupIdFromHrid(s, audit));
    }

    private static void addSharedPolicyGroupIdFromHrid(StepV4 stepV4, AuditInfo auditInfo) {
        if (Objects.equals(stepV4.getPolicy(), SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)) {
            Object configuration = stepV4.getConfiguration();
            if (configuration instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> struct = (Map<String, Object>) rawMap;
                String hrid = (String) struct.get(HRID_FIELD);
                if (hrid != null && !hrid.isEmpty()) {
                    struct.put(SHARED_POLICY_GROUP_ID_FIELD, HRIDToUUID.sharedPolicyGroup().context(auditInfo).hrid(hrid).crossId());
                }
            }
        }
    }

    public static void removeSharedPolicyGroupId(ApiV4Spec spec) {
        CollectionUtils.stream(spec.getFlows()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        if (spec.getPlans() != null) {
            CollectionUtils.stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        }
    }

    public static void addHRID(ApiV4Spec spec) {
        CollectionUtils.stream(spec.getFlows()).forEach(SharedPolicyGroupIdHelper::addHRID);
        if (spec.getPlans() != null) {
            CollectionUtils.stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(SharedPolicyGroupIdHelper::addHRID);
        }
    }

    private static void removeSharedPolicyGroupId(@Valid FlowV4 flowV4) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        CollectionUtils.stream(flowV4.getResponse()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        CollectionUtils.stream(flowV4.getPublish()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        CollectionUtils.stream(flowV4.getEntrypointConnect()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
        CollectionUtils.stream(flowV4.getInteract()).forEach(SharedPolicyGroupIdHelper::removeSharedPolicyGroupId);
    }

    private static void addHRID(@Valid FlowV4 flowV4) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(SharedPolicyGroupIdHelper::addHRID);
        CollectionUtils.stream(flowV4.getResponse()).forEach(SharedPolicyGroupIdHelper::addHRID);
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(SharedPolicyGroupIdHelper::addHRID);
        CollectionUtils.stream(flowV4.getPublish()).forEach(SharedPolicyGroupIdHelper::addHRID);
        CollectionUtils.stream(flowV4.getEntrypointConnect()).forEach(SharedPolicyGroupIdHelper::addHRID);
        CollectionUtils.stream(flowV4.getInteract()).forEach(SharedPolicyGroupIdHelper::addHRID);
    }

    private static void removeSharedPolicyGroupId(StepV4 stepV4) {
        getConfiguration(stepV4).ifPresent(configuration -> configuration.remove(SHARED_POLICY_GROUP_ID_FIELD));
    }

    private static void addHRID(StepV4 stepV4) {
        getConfiguration(stepV4).ifPresent(configuration -> configuration.computeIfAbsent(HRID_FIELD, k -> nameToHRID(stepV4.getName())));
    }

    private static Optional<Map<Object, Object>> getConfiguration(StepV4 stepV4) {
        if (!Objects.equals(stepV4.getPolicy(), SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)) {
            return Optional.empty();
        }
        if (!(stepV4.getConfiguration() instanceof Map<?, ?> configuration)) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> typed = (Map<Object, Object>) configuration;
        return Optional.of(typed);
    }
}
