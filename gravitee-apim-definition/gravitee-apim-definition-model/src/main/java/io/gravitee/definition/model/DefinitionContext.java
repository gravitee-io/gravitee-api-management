/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model;

import java.io.Serializable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefinitionContext implements Serializable {

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

    /**
     * Creates a default context with 'management' and 'fully_managed' mode.
     */
    public DefinitionContext() {
        origin = ORIGIN_MANAGEMENT;
        mode = MODE_FULLY_MANAGED;
    }

    /**
     * Creates a context with the specified origin and mode.
     * If origin is <code>null</code>, the 'management' origin will be set.
     * If mode is <code>null</code>, the 'fully_managed' mode will be set.
     *
     * @param origin the origin ('kubernetes', 'management')
     * @param mode the management mode ('fully_managed', 'api_definition_only')
     */
    public DefinitionContext(String origin, String mode) {
        setOrigin(origin);
        setMode(mode);
    }

    public static boolean isKubernetes(DefinitionContext context) {
        return context != null && isKubernetes(context.origin);
    }

    public static boolean isKubernetes(String origin) {
        return ORIGIN_KUBERNETES.equalsIgnoreCase(origin);
    }

    public static boolean isManagement(DefinitionContext context) {
        return context != null && isKubernetes(context.origin);
    }

    public static boolean isManagement(String origin) {
        return ORIGIN_MANAGEMENT.equalsIgnoreCase(origin);
    }

    public String getOrigin() {
        return origin;
    }

    /**
     * Set the origin if not <code>null</code>, else set 'management'.
     *
     * @param origin the origin to set.
     */
    public void setOrigin(String origin) {
        this.origin = origin != null ? origin : ORIGIN_MANAGEMENT;
    }

    public String getMode() {
        return mode;
    }

    /**
     * Set the mode if not <code>null</code>, else set 'fully_managed'.
     *
     * @param mode the mode to set.
     */
    public void setMode(String mode) {
        this.mode = mode != null ? mode : MODE_FULLY_MANAGED;
    }
}
