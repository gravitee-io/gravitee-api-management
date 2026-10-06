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
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.rest.api.automation.mapper.ApiMapper;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiCRDMapper;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Turns an exported API CRD into the {@link ApiV4State} the Automation API answers on a read, so the single
 * and the collection reads share one pipeline.
 *
 * @author GraviteeSource Team
 */
public final class ApiV4StateHelper {

    private ApiV4StateHelper() {}

    public static ApiV4State toApiV4State(ApiCRDSpec apiCRDSpec, ExecutionContext executionContext, GroupQueryService groupQueryService) {
        var apiV4Spec = toApiV4Spec(apiCRDSpec);
        replaceGroupNamesWithHrids(groupQueryService, executionContext.getEnvironmentId(), apiV4Spec);
        return toApiV4State(apiV4Spec, apiCRDSpec, executionContext);
    }

    public static ApiV4Spec toApiV4Spec(ApiCRDSpec apiCRDSpec) {
        ApiV4Spec apiV4Spec = ApiMapper.INSTANCE.apiCRDSpecToApiV4Spec(ApiCRDMapper.INSTANCE.map(apiCRDSpec));
        SharedPolicyGroupIdHelper.removeSharedPolicyGroupId(apiV4Spec);
        return apiV4Spec;
    }

    public static ApiV4State toApiV4State(ApiV4Spec apiV4Spec, ApiCRDSpec apiCRDSpec, ExecutionContext executionContext) {
        return ApiMapper.INSTANCE.apiV4SpecToApiV4State(
            apiV4Spec,
            apiCRDSpec.getId(),
            apiCRDSpec.getCrossId(),
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId()
        );
    }

    public static void replaceGroupNamesWithHrids(GroupQueryService groupQueryService, String environmentId, ApiV4Spec apiV4Spec) {
        if (apiV4Spec.getGroups() != null && !apiV4Spec.getGroups().isEmpty()) {
            var groups = new ArrayList<>(apiV4Spec.getGroups());
            List<String> notificationGroups = new ArrayList<>();
            if (apiV4Spec.getConsoleNotification() != null && apiV4Spec.getConsoleNotification().getGroups() != null) {
                notificationGroups.addAll(apiV4Spec.getConsoleNotification().getGroups());
            }
            // all groups in notifications are included in API groups
            groupQueryService
                .findByNames(environmentId, new LinkedHashSet<>(groups))
                .stream()
                .filter(group -> group.getHrid() != null)
                .forEach(group -> {
                    groups.set(groups.indexOf(group.getName()), group.getHrid());
                    // update notification groups if this group is included in notification groups
                    int index = notificationGroups.indexOf(group.getName());
                    if (index != -1) {
                        notificationGroups.set(index, group.getHrid());
                    }
                });
            apiV4Spec.setGroups(groups);
            if (!notificationGroups.isEmpty()) {
                apiV4Spec.getConsoleNotification().setGroups(notificationGroups);
            }
        }
    }
}
