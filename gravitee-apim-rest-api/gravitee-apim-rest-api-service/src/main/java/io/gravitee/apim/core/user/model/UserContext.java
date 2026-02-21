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
package io.gravitee.apim.core.user.model;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record UserContext(
    AuditInfo auditInfo,
    Optional<Map<String, String>> apiNameById,
    Optional<Map<String, String>> applicationNameById,
    Optional<Map<String, String>> planNameById,
    Optional<Map<String, String>> gatewayHostnameById,
    Optional<List<Api>> apis
) {
    public UserContext(AuditInfo auditInfo) {
        this(auditInfo, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public UserContext withApiNamesById(Map<String, String> apiNameById) {
        return new UserContext(auditInfo, Optional.ofNullable(apiNameById), applicationNameById, planNameById, gatewayHostnameById, apis);
    }

    public UserContext withApplicationNameById(Map<String, String> applicationNameById) {
        return new UserContext(auditInfo, apiNameById, Optional.ofNullable(applicationNameById), planNameById, gatewayHostnameById, apis);
    }

    public UserContext withPlanNameById(Map<String, String> planNameById) {
        return new UserContext(auditInfo, apiNameById, applicationNameById, Optional.ofNullable(planNameById), gatewayHostnameById, apis);
    }

    public UserContext withGatewayHostnameById(Map<String, String> gatewayHostnameById) {
        return new UserContext(auditInfo, apiNameById, applicationNameById, planNameById, Optional.ofNullable(gatewayHostnameById), apis);
    }

    public UserContext withApis(List<Api> apis) {
        return new UserContext(auditInfo, apiNameById, applicationNameById, planNameById, gatewayHostnameById, Optional.ofNullable(apis));
    }
}
