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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionApi;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionApiAvailability;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionAvailability;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionDetails;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionPlan;
import io.gravitee.rest.api.portal.rest.model.ApiType;
import io.gravitee.rest.api.portal.rest.model.PlanMode;
import io.gravitee.rest.api.portal.rest.model.PortalDocumentationTarget;
import java.util.UUID;

public final class ApiProductSubscriptionDetailsMapper {

    public static final ApiProductSubscriptionDetailsMapper INSTANCE = new ApiProductSubscriptionDetailsMapper();

    private ApiProductSubscriptionDetailsMapper() {}

    public ApiProductSubscriptionDetails map(PortalApiProductSubscriptionDetails source) {
        var target = new ApiProductSubscriptionDetails();
        target.setId(UUID.fromString(source.id()));
        target.setName(source.name());
        target.setVersion(source.version());
        target.setAvailability(ApiProductSubscriptionAvailability.fromValue(source.availability().name()));
        target.setPlan(map(source.plan()));
        target.setApis(source.apis().stream().map(this::map).toList());
        return target;
    }

    private ApiProductSubscriptionPlan map(PortalApiProductSubscriptionDetails.PlanSummary source) {
        if (source == null) {
            return null;
        }
        var target = new ApiProductSubscriptionPlan();
        target.setId(source.id());
        target.setName(source.name());
        if (source.security() != null) {
            target.setSecurity(ApiProductSubscriptionPlan.SecurityEnum.fromValue(PlanSecurityType.valueOfLabel(source.security()).name()));
        }
        if (source.mode() != null) {
            target.setMode(PlanMode.fromValue(source.mode()));
        }
        return target;
    }

    private ApiProductSubscriptionApi map(PortalApiProductSubscriptionDetails.ApiSummary source) {
        var target = new ApiProductSubscriptionApi();
        target.setId(source.id());
        target.setName(source.name());
        target.setVersion(source.version());
        if (source.type() != null) {
            target.setType(ApiType.fromValue(source.type()));
        }
        target.setAvailability(ApiProductSubscriptionApiAvailability.fromValue(source.availability().name()));
        target.setEntrypoints(source.entrypoints());
        target.setDocumentation(map(source.documentation()));
        return target;
    }

    private PortalDocumentationTarget map(PortalApiProductSubscriptionDetails.DocumentationTarget source) {
        if (source == null) {
            return null;
        }
        var target = new PortalDocumentationTarget();
        target.setRootId(UUID.fromString(source.rootId()));
        target.setNavigationItemId(UUID.fromString(source.navigationItemId()));
        return target;
    }
}
