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
package io.gravitee.gamma.rest.core.observability.dashboard.exception;

import io.gravitee.apim.core.exception.NotFoundDomainException;

/**
 * Raised when a dashboard id doesn't resolve within the caller's environment — either it doesn't
 * exist at all, or it belongs to a different environment. Both cases collapse to this single 404 so
 * cross-environment existence cannot be probed (see {@code DashboardRepository#findByIdAndEnvironmentId}).
 *
 * <p>Distinct from the unrelated, differently-packaged legacy
 * {@code io.gravitee.rest.api.service.exceptions.DashboardNotFoundException} (v2 analytics dashboards).
 *
 * @author GraviteeSource Team
 */
public class DashboardNotFoundException extends NotFoundDomainException {

    public DashboardNotFoundException(String dashboardId) {
        super("Dashboard '" + dashboardId + "' not found", dashboardId);
    }
}
