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
package io.gravitee.definition.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode
@Getter
@Builder
public class DefinitionContext implements Serializable {

    @Serial
    private static final long serialVersionUID = -8942499479175567992L;

    public static final String ORIGIN_KUBERNETES = "kubernetes";
    public static final String ORIGIN_MANAGEMENT = "management";

    /**
     * Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api.
     */
    public static final String MODE_FULLY_MANAGED = "fully_managed";

    /**
     * Mode indicating the api is partially managed by the origin and so, only the origin should be able to manage the the api definition part of the api.
     * This includes everything regarding the definition of the apis (plans, flows, metadata, ...)
     */
    public static final String MODE_API_DEFINITION_ONLY = "api_definition_only";

    private String origin;

    private String mode;

    private String syncFrom;

    /**
     * Creates a default context with 'management' and 'fully_managed' mode.
     */
    public DefinitionContext() {
        this(ORIGIN_MANAGEMENT, MODE_FULLY_MANAGED, ORIGIN_MANAGEMENT);
    }

    /**
     * Creates a context with the specified origin and mode.
     * If origin is <code>null</code>, the 'management' origin will be set.
     * If mode is <code>null</code>, the 'fully_managed' mode will be set.
     *
     * @param origin the origin ('kubernetes', 'management')
     * @param mode the management mode ('fully_managed', 'api_definition_only')
     */
    public DefinitionContext(String origin, String mode, String syncFrom) {
        this.origin = origin != null ? origin : ORIGIN_MANAGEMENT;
        this.mode = mode != null ? mode : MODE_FULLY_MANAGED;
        this.syncFrom = syncFrom == null ? ORIGIN_MANAGEMENT : syncFrom;
    }

    public DefinitionContext(String origin, String mode) {
        this(origin, mode, null);
    }

    public boolean isOriginManagement() {
        return isManagement(origin);
    }

    public boolean isOriginKubernetes() {
        return isKubernetes(origin);
    }

    public boolean isSyncFromKubernetes() {
        return isKubernetes(syncFrom);
    }

    public boolean isSyncFromManagement() {
        return isManagement(syncFrom);
    }

    public static boolean isManagement(String value) {
        return ORIGIN_MANAGEMENT.equalsIgnoreCase(value);
    }

    public static boolean isKubernetes(String value) {
        return ORIGIN_KUBERNETES.equalsIgnoreCase(value);
    }
}
