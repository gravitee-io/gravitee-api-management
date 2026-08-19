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
package io.gravitee.apim.plugin.gamma.api.automation;

import java.util.Objects;

/**
 * Where an automation call happens. Permissions and license have already been checked by the
 * Automation API when a module receives it.
 *
 * @param organizationId the organization addressed by the request
 * @param environmentId the environment addressed by the request
 * @param hrids resolves HRIDs of this module's other resources to their ids, for cross-references
 */
public record AutomationContext(String organizationId, String environmentId, HridResolver hrids) {
    public AutomationContext {
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(environmentId, "environmentId");
        Objects.requireNonNull(hrids, "hrids");
    }
}
