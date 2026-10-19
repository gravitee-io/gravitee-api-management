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
package io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider;

/**
 * The caller's {@code ENVIRONMENT_DASHBOARD} permissions, which cover the dashboard definitions only.
 *
 * @author GraviteeSource Team
 */
public interface DashboardPermissionPort {
    enum Action {
        READ,
        CREATE,
        UPDATE,
    }

    /** Whether the caller holds {@code ENVIRONMENT_DASHBOARD} with this action on the environment. */
    boolean holds(String organizationId, String environmentId, Action action);
}
