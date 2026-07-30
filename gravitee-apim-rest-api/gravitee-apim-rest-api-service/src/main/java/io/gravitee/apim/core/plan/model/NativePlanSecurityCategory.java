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
package io.gravitee.apim.core.plan.model;

import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;

/**
 * Security categories that are mutually exclusive on a Native (Kafka) API: a plan of a given
 * category cannot coexist with a plan of a different category. All authentication mechanisms
 * (API key, JWT, OAuth2) share the same {@link #AUTHENTICATION} category and can coexist together.
 *
 * <p>Single source of truth shared by the two enforcement sites:
 * {@code NativePlanSecurityValidator} (publish paths) and {@code ValidatePlanDomainService} (CRD
 * import). It mirrors the gateway's native-Kafka reactor guard.
 */
public enum NativePlanSecurityCategory {
    KEY_LESS,
    MTLS,
    AUTHENTICATION;

    public static NativePlanSecurityCategory fromSecurityTypeLabel(String securityTypeLabel) {
        if (PlanSecurityType.KEY_LESS.getLabel().equals(securityTypeLabel)) {
            return KEY_LESS;
        }
        if (PlanSecurityType.MTLS.getLabel().equals(securityTypeLabel)) {
            return MTLS;
        }
        return AUTHENTICATION;
    }
}
